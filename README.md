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

## License

AGPL-3.0-or-later.
