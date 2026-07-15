# cloud-itonami-isco-3314

Open Occupation Blueprint for **ISCO-08 3314**: Statistical, Mathematical and Related Associate Professionals.

This repository designs a forkable OSS business for an independent statistical and data analysis practice: a data-intake and report-printing robot manages dataset processing under a governor-gated actor, so the practice keeps its own analysis records instead of renting a closed analytics SaaS.

**Maturity: `:implemented`.** `src/statanalysis/` implements the
`StatisticalAnalysisActor` as a `langgraph.graph/state-graph`
(`statanalysis.actor`) wired to an `Analysis Advisor`
(`statanalysis.advisor`) and an independent `StatisticalAnalysisGovernor`
(`statanalysis.governor`), following the itonami actor pattern
(ADR-2607011000): `:intake -> :advise -> :govern -> :decide -+-> :commit
(:ok?) +-> :request-approval (:escalate?, human-in-the-loop interrupt)
+-> :hold (:hard?)`. 14 tests / 29 assertions green (`clojure -M:test`).
HARD invariants (always hold, never overridable): client provenance,
no-actuation (`:effect` must be `:propose`), a registered dataset
basis for any analysis proposal, the proposed report's confidentiality
level not exceeding the client's registered confidentiality-tier
ceiling (publication beyond it is unauthorized publication, not routine
analysis), and data-source verification before any analysis
can be finalized (finalizing analysis from unverified data is an invented
analysis, not evidence-based service). Always-escalate ops (human sign-off
regardless of confidence, mapping this repo's Trust Controls in
[`docs/business-model.md`](docs/business-model.md)):
`:finalize-analysis` and `:publish-report`.

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
