(ns statanalysis.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [statanalysis.store :as store]
            [statanalysis.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Research Institute"
                                :confidentiality-tier 3})
    (store/register-dataset! st {:dataset-id "D-1" :client-id "client-1"
                                 :name "survey-data-2024"
                                 :verified? true})
    st))

(defn- analysis-op [op confidence confidentiality-level]
  {:op op :effect :propose :dataset-id "D-1"
   :report-confidentiality-level confidentiality-level
   :confidence confidence :stake :low})

(def ^:private req {:client-id "client-1"})

(deftest ok-within-confidentiality-and-verified
  (let [st (fresh-store)
        v (governor/check req {} (analysis-op :analyze-dataset 0.9 2) st)]
    (is (:ok? v))))

(deftest ok-at-exact-confidentiality-ceiling-boundary
  (testing "the confidentiality-tier ceiling is inclusive"
    (let [st (fresh-store)
          v (governor/check req {} (analysis-op :analyze-dataset 0.9 3) st)]
      (is (:ok? v)))))

(deftest hard-on-report-exceeds-confidentiality-ceiling
  (testing "publishing a report above the client's registered confidentiality-tier ceiling is unauthorized publication, not routine analysis"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (analysis-op :analyze-dataset 0.99 5) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :report-exceeds-confidentiality-ceiling (:rule %)) (:violations v))))))

(deftest hard-on-unverified-data-source
  (testing "publishing analysis from an unverified data source is an invented analysis, not evidence-based service"
    (let [st (fresh-store)
          _ (store/register-dataset! st {:dataset-id "D-ghost" :client-id "client-1"
                                         :name "unverified-data"
                                         :verified? false})
          v (governor/check req {} (assoc (analysis-op :analyze-dataset 0.99 2) :dataset-id "D-ghost") st)]
      (is (:hard? v))
      (is (some #(= :unverified-data-source (:rule %)) (:violations v))))))

(deftest hard-on-unknown-dataset
  (let [st (fresh-store)
        v (governor/check req {} (assoc (analysis-op :analyze-dataset 0.9 2) :dataset-id "D-ghost") st)]
    (is (:hard? v))
    (is (some #(= :unknown-dataset (:rule %)) (:violations v)))))

(deftest hard-on-foreign-dataset
  (let [st (fresh-store)]
    (store/register-client! st {:client-id "client-2" :name "Other Institute"
                                :confidentiality-tier 3})
    (store/register-dataset! st {:dataset-id "D-2" :client-id "client-2"
                                 :name "other-data"
                                 :verified? true})
    (let [v (governor/check {:client-id "client-2"} {} (assoc (analysis-op :analyze-dataset 0.9 2) :dataset-id "D-1") st)]
      (is (:hard? v))
      (is (some #(= :dataset-wrong-client (:rule %)) (:violations v))))))

(deftest hard-on-unregistered-client
  (let [st (fresh-store)
        v (governor/check {:client-id "nobody"} {} (analysis-op :analyze-dataset 0.9 2) st)]
    (is (:hard? v))
    (is (some #(= :no-client (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check req {} (assoc (analysis-op :analyze-dataset 0.9 2) :effect :direct-write) st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest always-escalates-finalize-analysis-even-at-high-confidence
  (testing "analysis finalization always requires human sign-off per Trust Control 2"
    (let [st (fresh-store)
          v (governor/check req {} {:op :finalize-analysis :effect :propose
                                    :dataset-id "D-1" :confidence 0.99 :stake :low} st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest always-escalates-publish-report-even-at-high-confidence
  (testing "report publication always requires human sign-off per Trust Control 1"
    (let [st (fresh-store)
          v (governor/check req {} {:op :publish-report :effect :propose
                                    :dataset-id "D-1" :confidence 0.99 :stake :low} st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest escalates-low-confidence
  (let [st (fresh-store)
        v (governor/check req {} (assoc (analysis-op :analyze-dataset 0.3 2) :confidence 0.3) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))
