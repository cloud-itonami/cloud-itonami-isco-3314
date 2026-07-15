(ns statanalysis.governor
  "StatisticalAnalysisGovernor — the independent safety/traceability
  layer named in this repository's README/business-model.md, gating
  every analysis finalization and report publication an advisor may
  propose for a dataset. The governor never dispatches hardware itself
  and never publishes a report above the client's registered
  confidentiality-tier ceiling. Modeled on
  cloud-itonami-isco-3313's accountingsupport.governor.
  Task twist: a report confidentiality level is an arithmetic ceiling
  against the client's registered confidentiality-tier ceiling, and a
  finalization cannot proceed until the dataset has been verified.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. client provenance      — the small business/researcher/nonprofit
                                must be registered.
    2. no-actuation           — proposal :effect must be :propose (the
                                governor never dispatches hardware and
                                never publishes a report above the
                                registered confidentiality-tier ceiling;
                                it only gates what the advisor may
                                publish).
    3. dataset basis          — an analysis proposal must cite a
                                REGISTERED dataset belonging to this
                                client.
    4. dataset-wrong-client   — a dataset must belong to the request's
                                client.
    5. unverified-data-source — the dataset must be verified before any
                                analysis can be finalized or report
                                published (publishing analysis from an
                                unverified data source is an invented
                                analysis, not evidence-based service).
    6. report-exceeds-confidentiality-ceiling — the report's
                                confidentiality level must not exceed
                                the client's registered
                                `:confidentiality-tier` (publishing
                                beyond the client's registered ceiling
                                is unauthorized publication, not
                                routine data analysis).
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off per
  business-model.md's Trust Controls — these are :high/
  :safety-critical regardless of confidence):
    7. :op :finalize-analysis (analysis finalization always requires
                                human sign-off per Trust Control 2).
    8. :op :publish-report (report publication always requires human
                            sign-off per Trust Control 1).
    9. low confidence (< `confidence-floor`)."
  (:require [statanalysis.store :as store]))

(def confidence-floor 0.6)

(def ^:private always-escalate-ops #{:finalize-analysis
                                     :publish-report})

(defn- hard-violations [{:keys [request proposal]} client-record d]
  (let [{:keys [op report-confidentiality-level]} proposal]
    (cond-> []
      (nil? client-record)
      (conj {:rule :no-client :detail "未登録 client"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（governor は登録機密度上限超過の公開を直接実行しない）"})

      (nil? d)
      (conj {:rule :unknown-dataset :detail "未登録 dataset への分析提案は不可"})

      (and d (not= (:client-id d) (:client-id request)))
      (conj {:rule :dataset-wrong-client :detail "dataset が別 client のもの"})

      (and d (not (:verified? d)))
      (conj {:rule :unverified-data-source
             :detail "未検証のデータソースから導かれた分析は根拠なき分析であって適切なサービスではない"})

      (and client-record d (number? report-confidentiality-level)
           (> report-confidentiality-level (:confidentiality-tier client-record)))
      (conj {:rule :report-exceeds-confidentiality-ceiling
             :detail (str "報告書機密度 " report-confidentiality-level " > 登録済み上限 "
                          (:confidentiality-tier client-record) "（登録上限を超える公開は無許可公開であって通常の分析業務ではない）")}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `statanalysis.store/Store`. Pure — never
  mutates the store, never publishes a report above the registered
  confidentiality-tier ceiling."
  [request context proposal store]
  (let [client-record (store/client store (:client-id request))
        d (some->> (:dataset-id proposal) (store/dataset store))
        hard (hard-violations {:request request :proposal proposal}
                              client-record d)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        always-risky? (contains? always-escalate-ops (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
