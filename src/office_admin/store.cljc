(ns office-admin.store
  "SSoT for the ISCO-08 4110 independent office-administration
  sole-proprietor actor, behind a `Store` protocol so the backend is a
  swap (MemStore default ‖ a real Datomic/kotoba-server backend, per the
  itonami actor pattern).

  Domain = independent office administration practice:

    client            — a consented client (clientId, name, consentedAt)
    correspondence    — a correspondence event under a client
                        (corrId, clientId, kind #{:mail :email :courier})
    disclosure        — a disclosure event under a client (disclosureId,
                        clientId, recipient, dataCategory
                        #{:general :financial :medical})

  The append-only records are the operating ledger: a correspondence or
  disclosure must reference a registered (consented) client, and these
  records are never mutated in place, only appended.")

(defprotocol Store
  (client [st client-id])
  (correspondence-of [st client-id])
  (disclosures-of [st client-id])
  (register-client! [st client])
  (record-correspondence! [st correspondence])
  (record-disclosure! [st disclosure]))

(defrecord MemStore [state]
  Store
  (client [_ client-id]
    (get-in @state [:clients client-id]))
  (correspondence-of [_ client-id]
    (filter #(= client-id (:client-id %)) (:correspondence @state)))
  (disclosures-of [_ client-id]
    (filter #(= client-id (:client-id %)) (:disclosures @state)))
  (register-client! [_ client]
    (swap! state assoc-in [:clients (:client-id client)] client))
  (record-correspondence! [_ correspondence]
    (swap! state update :correspondence (fnil conj []) correspondence))
  (record-disclosure! [_ disclosure]
    (swap! state update :disclosures (fnil conj []) disclosure)))

(defn mem-store
  ([] (mem-store {}))
  ([seed]
   (->MemStore (atom (merge {:clients {} :correspondence [] :disclosures []} seed)))))
