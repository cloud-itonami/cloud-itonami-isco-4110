(ns office-admin.governor
  "OfficeAdminGovernor — the independent safety/traceability layer for
  the ISCO-08 4110 independent office-administration actor. The
  Correspondence Advisor proposes actions (correspondence, disclosure);
  it has no notion of client provenance or sensitive-disclosure risk, so
  this MUST be a separate system able to *reject* a proposal and fall
  back to HOLD — the itonami-actor pattern (independent Governor gates a
  proposing actor) applied to this occupation.

  Charter (mirrors ADR-2607011000 robotics premise + ADR-2607012000
  cloud-itonami-isco): the actor never dispatches a robot action or writes an
  operating record the governor refuses. A disclosure of a `:financial`
  or `:medical` data-category ALWAYS requires human sign-off — it can
  never be auto-approved.

  HARD invariants for :office-admin/propose:
    1. Client provenance    — a correspondence or disclosure must
       reference a registered (consented) client.
    2. No-actuation         — the proposal must not directly mutate a
       correspondence/disclosure record outside the
       record-correspondence!/record-disclosure! path (effect must be
       :propose, never a raw store write).
    3. Sensitive-disclosure safety — a disclosure whose `data-category`
       is `:financial` or `:medical` always requires :high or higher
       safety-class, forcing human sign-off; it is never auto-approved
       regardless of confidence.
  SOFT:
    4. Confidence floor → escalate."
  (:require [office-admin.store :as store]))

(def confidence-floor 0.6)
(def safety-classes [:none :low :medium :high :safety-critical])
(def sensitive-categories #{:financial :medical})

(defn- safety-rank [safety-class]
  (let [idx (.indexOf safety-classes safety-class)]
    (if (neg? idx) 0 idx)))

(defn- sensitive-disclosure? [proposal]
  (and (= :disclosure (:kind proposal))
       (contains? sensitive-categories (:data-category proposal))))

(defn- hard-violations [{:keys [client-fn]} proposal]
  (let [{:keys [client-id safety-class effect]} proposal
        found-client (client-fn client-id)]
    (cond-> []
      (nil? found-client)
      (conj {:rule :no-client :detail (str "未登録/未同意 client " client-id)})

      (not= :propose effect)
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（直接書込禁止）"})

      (and (sensitive-disclosure? proposal)
           (< (safety-rank (or safety-class :none)) (safety-rank :high)))
      (conj {:rule :sensitive-disclosure-safety
             :detail "financial/medical data-category の disclosure は :high 以上の safety-class が必須"}))))

(defn assess
  "Assess a proposal against `env` (a map with `:client-fn` lookup,
  decoupled from any concrete Store so this stays pure). Returns
  `{:decision :proceed|:hold|:human-approval :violations [...] :confidence n}`."
  [env proposal]
  (let [violations (hard-violations env proposal)
        safety-class (or (:safety-class proposal) :none)
        confidence (or (:confidence proposal) 1.0)]
    (cond
      (seq violations)
      {:decision :hold :violations violations :confidence confidence}

      (>= (safety-rank safety-class) (safety-rank :high))
      {:decision :human-approval :violations [] :confidence confidence}

      (< confidence confidence-floor)
      {:decision :human-approval :violations [] :confidence confidence
       :reason :low-confidence}

      :else
      {:decision :proceed :violations [] :confidence confidence})))

(defn env-for-store
  "Build the decoupled env map `assess` needs from a concrete
  `office-admin.store/Store` implementation."
  [store]
  {:client-fn #(store/client store %)})
