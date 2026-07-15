# cloud-itonami-isco-3314

Open Occupation Blueprint for **ISCO-08 3314**: Statistical, Mathematical and Related Associate Professionals.

This repository designs a forkable OSS business for an independent statistical and data analysis practice: a data-intake and report-printing robot manages dataset processing under a governor-gated actor, so the practice keeps its own analysis records instead of renting a closed analytics SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a data-intake and report-printing robot performs dataset scanning, report binding and physical archival under an actor that proposes
actions and an independent **Statistical Analysis Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
report publication above the client's registered confidentiality-tier ceiling) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
client dataset + analysis brief + methodology
        |
        v
Analysis Advisor -> Statistical Analysis Governor -> publish report/analysis, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `3314`). Required capabilities:

- :robotics
- :forms
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
