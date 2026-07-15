(ns statanalysis.store
  "SSoT for the ISCO-08 3314 independent statistical and data analysis
  practice actor (itonami actor pattern, ADR-2607011000 / CLAUDE.md Actors
  section; README's 'Robotics premise' — a data-intake and report-printing
  robot performs dataset scanning, report binding and physical archival under
  this advisor/governor pair, which never dispatches hardware itself and never
  publishes a report above the client's registered confidentiality-tier
  ceiling). Modeled on cloud-itonami-isco-3313's accountingsupport.store.

  Domain:

    client  — a registered small business/researcher/nonprofit
              (:client-id, :name, :confidentiality-tier)
    dataset — a registered dataset for analysis {:dataset-id :client-id
              :name :verified?}. `:verified?` is whether the data source
              has been verified by human review — publishing an analysis
              from an unverified data source is an invented analysis, not
              evidence-based service.
    record  — a committed operating record (a published report or
              finalized analysis) — written ONLY via commit-record!.
    ledger  — append-only audit trail, commit or hold."
  )

(defprotocol Store
  (client [s client-id])
  (dataset [s dataset-id])
  (records-of [s client-id])
  (ledger [s])
  (register-client! [s client])
  (register-dataset! [s d])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (client [_ client-id] (get-in @a [:clients client-id]))
  (dataset [_ dataset-id] (get-in @a [:datasets dataset-id]))
  (records-of [_ client-id] (filter #(= client-id (:client-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-client! [s client]
    (swap! a assoc-in [:clients (:client-id client)] client) s)
  (register-dataset! [s d]
    (swap! a assoc-in [:datasets (:dataset-id d)] d) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:clients {} :datasets {} :records [] :ledger []}
                                   seed)))))
