# cloud-itonami-isco-4110

Open Occupation Blueprint for **ISCO-08 4110**: General Office Clerks.

This repository designs a forkable OSS business for an independent office administration practice: a document-handling robot performs scanning and filing tasks under a governor-gated actor, so the practice keeps its own records instead of renting a closed office-management SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a document-handling robot performs scanning, filing and mail-sorting tasks in the office under an actor that proposes
actions and an independent **Office Admin Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
handling confidential records, or processing financial documents) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
client request + filing scope + retention policy
        |
        v
Admin Advisor -> Office Admin Governor -> file/process, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `4110`). Required capabilities:

- :robotics
- :forms
- :audit-ledger
- :bpmn

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## Reference implementation

`src/office_admin/{store,governor}.cljc` is a minimal but real
implementation of the Core Contract above (pure cljc, no external deps):

- `office-admin.store` — `Store` protocol + `MemStore`: consented
  clients, correspondence events, disclosure events. A
  correspondence/disclosure can only be recorded against a registered
  (consented) client (client provenance).
- `office-admin.governor` — `OfficeAdminGovernor`: `assess` gates a
  proposal against the client env. Hard invariants force `:hold` (no
  client, direct-write instead of `:propose`, or a `:financial`/
  `:medical` disclosure below `:high` safety-class); a sensitive
  disclosure always requires `:high`+ safety-class and thus
  `:human-approval` — it can never be auto-approved; low-confidence
  proposals also escalate.

```bash
clojure -M:test   # 8 tests, 13 assertions, green
```

This is what backs this repo's `:maturity :implemented` entry in
[`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation) —
the 17th `cloud-itonami-isco-*` occupation to reach that tier, after
`cloud-itonami-isco-6112`, `-2221`, `-7126`, `-4321`, `-9312`, `-5322`,
`-8332`, `-1321`, `-3253`, `-6210`, `-5223`, `-7231`, `-8121`, `-9111`,
`-2512` and `-1120` (ADR-2607012000).

## License

AGPL-3.0-or-later.
