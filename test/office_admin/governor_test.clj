(ns office-admin.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [office-admin.store :as store]
            [office-admin.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "acme-llc" :consented-at "2026-01-01"})
    st))

(deftest proceeds-on-clean-correspondence
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :correspondence :client-id "client-1" :kind-of :email
                   :safety-class :low :effect :propose :confidence 0.9}]
    (is (= :proceed (:decision (governor/assess env proposal))))))

(deftest holds-on-unregistered-client
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :correspondence :client-id "no-such-client"
                   :safety-class :low :effect :propose :confidence 0.9}
        result (governor/assess env proposal)]
    (is (= :hold (:decision result)))
    (is (some #(= :no-client (:rule %)) (:violations result)))))

(deftest holds-on-no-actuation-violation
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :correspondence :client-id "client-1"
                   :safety-class :low :effect :direct-write :confidence 0.9}
        result (governor/assess env proposal)]
    (is (= :hold (:decision result)))
    (is (some #(= :no-actuation (:rule %)) (:violations result)))))

(deftest proceeds-on-general-disclosure
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :disclosure :client-id "client-1" :data-category :general
                   :recipient "vendor-x" :safety-class :low :effect :propose :confidence 0.9}]
    (is (= :proceed (:decision (governor/assess env proposal))))))

(deftest holds-on-financial-disclosure-without-high-safety-class
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :disclosure :client-id "client-1" :data-category :financial
                   :recipient "vendor-x" :safety-class :medium :effect :propose :confidence 0.9}
        result (governor/assess env proposal)]
    (is (= :hold (:decision result)))
    (is (some #(= :sensitive-disclosure-safety (:rule %)) (:violations result)))))

(deftest human-approval-on-medical-disclosure-with-high-safety-class
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :disclosure :client-id "client-1" :data-category :medical
                   :recipient "vendor-x" :safety-class :high :effect :propose :confidence 0.9}]
    (is (= :human-approval (:decision (governor/assess env proposal))))))

(deftest human-approval-on-low-confidence
  (let [st (fresh-store)
        env (governor/env-for-store st)
        proposal {:kind :correspondence :client-id "client-1"
                   :safety-class :none :effect :propose :confidence 0.2}
        result (governor/assess env proposal)]
    (is (= :human-approval (:decision result)))
    (is (= :low-confidence (:reason result)))))

(deftest store-records-append-only
  (let [st (fresh-store)]
    (store/record-correspondence! st {:corr-id "c1" :client-id "client-1" :kind :email})
    (store/record-disclosure! st {:disclosure-id "d1" :client-id "client-1"
                                    :recipient "vendor-x" :data-category :general})
    (is (= 1 (count (store/correspondence-of st "client-1"))))
    (is (= 1 (count (store/disclosures-of st "client-1"))))))
