(ns statanalysis.advisor
  "Analysis Advisor — the advisor named in this repository's README,
  proposing a statistical-analysis operation (finalize analysis,
  publish report) from a client dataset batch, analysis methodology
  and publication policy. Swappable mock/llm; the advisor ONLY proposes —
  `statanalysis.governor` checks the data-source verification and
  confidentiality-tier ceiling independently and always escalates
  finalize-analysis and publish-report decisions. Modeled on
  cloud-itonami-isco-3313's accountingsupport.advisor.

  A proposal: {:op :finalize-analysis|:publish-report
               :effect :propose :dataset-id str :report-confidentiality-level
               number :stake kw :confidence n :rationale str}")

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- infer [_store {:keys [op stake dataset-id report-confidentiality-level] :as request}]
  {:op op
   :effect :propose
   :dataset-id dataset-id
   :report-confidentiality-level (or report-confidentiality-level 1)
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (str "proposed " (name op) " for client " (:client-id request))})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a statistical analysis advisor. Given a request, propose an
   :op, the :dataset-id, :report-confidentiality-level, an honest
   :confidence and a :stake. Never propose a report above the client's
   registered confidentiality-tier ceiling — the governor checks
   it against the registered client record. Finalize-analysis and
   publish-report always require human sign-off regardless of
   confidence.")

(defn- parse-proposal [content]
  (try
    (let [p (read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "analysis request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
