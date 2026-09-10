(ns statanalysis.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [statanalysis.actor :as actor]
            [statanalysis.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Research Institute"
                                :confidentiality-tier 3})
    (store/register-dataset! st {:dataset-id "D-1" :client-id "client-1"
                                 :name "survey-data-2024"
                                 :verified? true})
    st))

(deftest commits-a-within-confidentiality-verified-analysis
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :analyze-dataset :stake :low
                 :dataset-id "D-1" :report-confidentiality-level 2}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "client-1"))))))

(deftest holds-an-above-confidentiality-ceiling-analysis
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :analyze-dataset :stake :low
                 :dataset-id "D-1" :report-confidentiality-level 5}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :hold (:disposition (:state result))))
    (is (empty? (store/records-of st "client-1")))))

(deftest interrupts-then-approves-publish-report-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :publish-report :stake :low
                 :dataset-id "D-1"}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "client-1")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "client-1")))))))
