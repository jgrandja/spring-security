---
name: ghsa-remediate
description: >-
  Remediate a Spring Advisory issue already confirmed VALID_CVE by
  /ghsa-validate: write the source fix, prove it with a dedicated mitigation
  test, independently re-verify (original PoC now blocked, mitigation test
  passes, no regressions), and draft external-facing CVE report content
  (title, high-level description, CVSS 3.1 base vector + rationale). Commits
  the fix, test, and reports to the same long-lived `ghsa-<issue-no>` branch
  /ghsa-validate created. Use when asked to "fix this advisory", "remediate
  this GHSA", or given a security-advisory-issues URL that already has a
  VALID_CVE verdict on its ghsa-<issue-no> branch.
argument-hint: "<security-advisory-issue-url> [--source-dir <path>]"
allowed-tools:
  - Read
  - Write
  - Glob
  - Grep
  - Edit
  - Agent
  - Bash(gh issue view:*)
  - Bash(gh api:*)
  - Bash(git:*)
  - Bash(mvn:*)
  - Bash(./mvnw:*)
  - Bash(gradle:*)
  - Bash(./gradlew:*)
  - Bash(find:*)
  - Bash(ls:*)
  - Bash(cat:*)
---

# ghsa-remediate

**Invocation:**
`/ghsa-remediate <security-advisory-issue-url> [--source-dir <path>]`

---

## Inputs

Parse `$ARGUMENTS`:

- `<security-advisory-issue-url>` (positional, required): the same issue in
  https://github.com/spring-projects/security-advisory-issues/issues that was
  already passed to `/ghsa-validate`.
- `--source-dir <path>` (optional): local checkout of the source repo.
  Defaults to the current working directory if omitted. Must already have a
  `ghsa-<issue-no>` branch produced by a prior `/ghsa-validate` run — this
  skill does not validate from scratch.

If the resolved `--source-dir` isn't a git checkout, or the positional issue
URL is missing, stop and ask rather than guessing.

---

## Prerequisites / safety preamble

State these before doing anything else:

1. `gh` must be authenticated with access to
   `spring-projects/security-advisory-issues` and
   `spring-projects/security-advisories`, same as `/ghsa-validate`.
2. **This skill writes production source code.** Before touching anything,
   `git -C <source-dir> status` and stop if it has uncommitted changes — a
   fix's own edits must not get conflated with the user's in-progress work.
3. **This skill requires a prior `/ghsa-validate` run with outcome
   `VALID_CVE` already committed on `ghsa-<issue-no>`.** It trusts that
   report rather than re-deriving the verdict — see Step 1. If the branch or
   the `VALID_CVE` outcome isn't there, stop and tell the user to run
   `/ghsa-validate <issue-url>` first.
4. **The fix, mitigation test, and both reports are committed to the same
   `ghsa-<issue-no>` branch** `/ghsa-validate` created — never a new branch,
   never rebased/force-pushed. Re-running remediation on the same advisory
   after a prior attempt is expected (e.g. after fixing a verification
   failure), not an error.
5. No network access from inside the mitigation test. Same constraint as the
   validation PoC.
6. This skill never pushes `ghsa-<issue-no>` to a remote, and never posts
   anything to GitHub. Both are visible/hard-to-reverse actions left to the
   user — including publishing the actual CVE report, which this skill only
   drafts.
7. If any git step fails unexpectedly, stop and surface the exact `git
   status`/`git diff` output rather than attempting further recovery — never
   force through a git conflict.
8. **Step 3's verification sub-agent run is bounded by a hard 10-minute
   (600s) wall-clock timeout** (module test suites run longer than a single
   PoC). A run that can't complete in that window fails fast as an explicit
   stop condition, surfaced as `RESULT: ERROR` rather than forced into a
   pass/fail verdict from a run that never finished.
9. **A fix that doesn't clear Step 3 verification is never committed.** No
   partial commits, no "fix without proof" — if the original PoC still
   passes against the patched code, or the mitigation test fails, or the
   module suite regresses, the skill stops and reports the failure instead
   of proceeding to Step 4/5.

---

## Step 1 — Resolve and require prior validation

1. Resolve `<security-advisory-issue-url>` -> issue number, same as
   `/ghsa-validate` Step 1 item 1:
   `gh issue view <n> --repo spring-projects/security-advisory-issues --json title,body,url,number`.
   Extract the linked `<security-advisory-url>` / `<ghsa-id>` from the issue
   body, same as `/ghsa-validate` Step 1 item 2.
2. Record the checkout's current ref before doing anything else — a branch
   name (`git -C <source-dir> rev-parse --abbrev-ref HEAD`) or, if detached,
   the commit sha (`git -C <source-dir> rev-parse HEAD`). This is
   `$ORIGINAL_REF`; Step 5 returns the checkout here at the end.
3. `git -C <source-dir> show-ref --verify --quiet refs/heads/ghsa-<issue-no>`
   — if this fails (branch doesn't exist), **stop**:
   `ERROR: no ghsa-<issue-no> branch found — run /ghsa-validate <issue-url> first.`
4. `git -C <source-dir> checkout ghsa-<issue-no>`. Read
   `reports/GHSA-VALIDATION-<ghsa-id>.md`. If the file is missing, **stop**
   with the same error as item 3. If present, require
   `Outcome: VALID_CVE` — any other outcome (`NOT_A_CVE`, or the file
   reporting a different `ghsa-id` than expected), **stop** and report why
   remediation doesn't apply, quoting the recorded `Outcome`/`Disposition`
   line, e.g.:
   `Last recorded outcome for <ghsa-id> is NOT_A_CVE (not-reproducible) — nothing to remediate.`
5. From that report, extract and hold for later steps: entry point, trust
   boundary, mechanism, impact, every precondition, and the **PoC test file
   path** (Step 4's "PoC" section in the `/ghsa-validate` report shape).
   These are consumed directly — Step 2 does not re-derive them from
   scratch.
6. Re-fetch the advisory for CWE and any CVSS the advisory author already
   proposed:
   `gh api repos/spring-projects/security-advisories/security-advisories/<ghsa-id>`
   — same call `/ghsa-validate` Step 1 item 2 makes; reused here as
   reference input for Step 4, discarding `state`/withdrawal metadata per
   the same no-verdict-peeking discipline `/ghsa-validate` follows for
   disposition fields (this skill only needs CWE/CVSS/version data, not the
   advisory's publication status).

---

## Step 2 — Design and implement the fix

1. Using the Step 1 extracted facts plus direct reading of the affected
   class(es), design the **minimal** fix for the reported weakness — no
   refactor, no unrelated cleanup, no speculative hardening beyond what the
   advisory actually reported. This is a bug fix, not an opportunity to
   redesign the surrounding code.
2. Check whether an equivalent reactive or servlet counterpart of the fixed
   component exists (per the root `CLAUDE.md` servlet/reactive-duality
   note) and fix both if a parallel implementation shares the same
   weakness.
3. Apply the fix directly in `--source-dir`, still on `ghsa-<issue-no>`.
4. Write a **mitigation test**, distinct from the validation PoC. Unlike the
   PoC (which the validation skill deliberately isolates into its own
   scratch class), the mitigation test belongs in the **existing test
   class** for the fixed production class if one exists — add a test method
   there, following that class's own conventions (naming, fixture/setup
   style, assertion library, `given`/`when`/`then` structure, etc.) so it
   reads as a normal addition to the suite, not a bolt-on. Only fall back to
   a new test class if no existing test class covers the fixed production
   class. Name the method so it's identifiable as covering this advisory,
   e.g. `<behaviorDescription>WhenGhsaIdThen<expectedOutcome>` or a comment
   referencing `<ghsa-id>` directly above it if the class's convention
   doesn't encode scenarios in method names. The test asserts the *fixed*
   behavior directly (attack blocked / input rejected / access denied)
   rather than merely re-running the old PoC and checking it now fails.
5. Bump the copyright header's end year on every file touched, per this
   repo's contribution conventions, if not already current.

---

## Step 3 — Independent verification (dedicated sub-agent)

Spawn exactly one verification sub-agent and wait for it (foreground —
remediation is not reported done until this returns). Give it, verbatim:
the original PoC test's file path and invocation command (from Step 1 item
5), the mitigation test's class + method name (from Step 2 item 4 — an
existing test class in most cases, not a new file), the affected Gradle
module name, and these instructions:

1. Re-run the **original validation PoC** against the now-patched code —
   expected: it now fails/is blocked (the exploit no longer works). If it
   still passes, the fix is incomplete: report `FAIL` on this check and stop
   — do not run the remaining checks.
2. Run the **mitigation test method** specifically (e.g.
   `--tests "*ClassName.methodName*"`) — expected: passes. Report `FAIL` if
   not.
3. Run the affected module's full existing test suite —
   `./gradlew :spring-security-<module>:test` (confirm the module's Gradle
   project name first with `./gradlew projects` if unsure, per the
   build-file-based naming rule in the root `CLAUDE.md`) — expected: no
   regressions. Invoke via the Bash tool with a `timeout` of 600000
   (milliseconds) rather than a shell-level `timeout` command.
4. Report back, per check: pass/fail and the raw command output. Do not
   editorialize past this — a verdict roll-up happens in the parent skill,
   not the sub-agent.

Back in the parent flow: if any of the three checks failed, **stop** —
surface the sub-agent's exact findings, do not proceed to Step 4/5, and do
not commit anything. Revise the fix (back to Step 2) or report the error to
the user, per the Prerequisites' "never commit a fix without proof" rule.

---

## Step 4 — Draft CVE report content

Written to `reports/CVE-REPORT-<ghsa-id>.md` — a persisted draft/audit
record, not the authoritative advisory (the user publishes the real one
elsewhere).

1. **Proposed title**: component + weakness class, terse, matching the style
   of published Spring Security advisories, e.g. "Spring Security's `X`
   Allows Attackers To Bypass `Y` Via `Z`".
2. **Proposed description**: 2-4 sentences, high level. States the affected
   component, the weakness class (CWE, from Step 1 item 6), and the impact
   category (e.g. authentication bypass, information disclosure, privilege
   escalation). Draft strictly from the Step 1 entry point/mechanism/impact
   facts **at the category level** — never from the PoC's literal
   assertions or inputs. Explicitly exclude: exact trigger steps, request or
   configuration specifics, or anything that would let a reader reproduce
   the attack from the description alone. This exclusion is a hazard to
   actively guard against while drafting, not a wording preference.
3. **CVSS 3.1 vector + base-metric rationale**: derive each metric from a
   concrete Step 1 fact — preconditions drive AV/AC/PR/UI, the trust
   boundary crossed drives Scope, and the impact drives C/I/A. Use the
   reference table below; justify each metric in one sentence citing the
   specific fact (e.g. "PR:N — the report's entry point sits before the
   authentication filter"), never generic CVSS boilerplate. Include the
   vector string and a direct NVD CVSS v3 calculator link with that vector
   pre-filled:
   `https://nvd.nist.gov/vuln-metrics/cvss/v3-calculator?vector=<vector-string>&version=3.1`
   (NVD's calculator takes the vector without the leading `CVSS:3.1/`
   prefix in the query param, e.g.
   `?vector=AV:N/AC:L/PR:N/UI:N/S:U/C:H/I:N/A:N&version=3.1`).

### CVSS 3.1 base-metric reference

Base metrics only (no Temporal/Environmental), per the standard model:

| Metric | Abbrev | Values | What it captures for a Spring Security fix |
|---|---|---|---|
| Attack Vector | AV | N(etwork)/A(djacent)/L(ocal)/P(hysical) | Almost always **N** for a web-framework filter/endpoint weakness — reachable over the network the app already serves on. **A/L/P** only if the advisory's precondition explicitly requires local/adjacent access. |
| Attack Complexity | AC | L(ow)/H(igh) | **L** if the attacker just sends a crafted request through normal application flow; **H** if it needs a race condition, non-default config, or timing beyond attacker's control. |
| Privileges Required | PR | N(one)/L(ow)/H(igh) | From the validation report's stated preconditions — **N** unauthenticated, **L** any authenticated role suffices, **H** requires an already-elevated/admin role. |
| User Interaction | UI | N(one)/R(equired) | **N** unless the exploit needs a victim to click/submit something (e.g. CSRF-shaped issues). |
| Scope | S | U(nchanged)/C(hanged) | **C** only if the vulnerable component can impact resources beyond its own security scope (e.g. a bypass granting access to a different authorization domain than the one attacked). **U** for most single-component bugs. |
| Confidentiality | C | N/L(ow)/H(igh) | From the impact category — **H** full data exposure, **L** partial/limited disclosure, **N** none. |
| Integrity | I | N/L/H | **H** if attacker can modify data/state they shouldn't (forge auth, escalate role); **L** limited tampering; **N** none. |
| Availability | A | N/L/H | Usually **N** for auth/authorization-class bugs unless the mechanism itself is a DoS vector (e.g. unbounded resource consumption via a bypassed check). |

Vector string format: `CVSS:3.1/AV:_/AC:_/PR:_/UI:_/S:_/C:_/I:_/A:_`.

---

## Step 5 — Persist: fix, test, remediation report, CVE report draft — one commit

Already sitting on `ghsa-<issue-no>` since Step 1 item 4 — no stash/pop
mechanics needed (unlike `/ghsa-validate`, which starts from
`$ORIGINAL_REF`).

1. Write `reports/GHSA-REMEDIATION-<ghsa-id>.md`: fix summary (files changed, one
   paragraph on the fix's mechanism), and the Step 3 sub-agent's
   verification results (all three checks, pass/fail, with output) — so
   "proving the fix mitigates further attacks" is a committed artifact, not
   a claim. Structure:

   ```markdown
   # GHSA-REMEDIATION-<ghsa-id>

   - Issue: <issue_url>
   - Advisory: <advisory_url>
   - Source: <source_dir> @ <commit>

   ## Fix summary
   - Files changed: ...
   - Mechanism: ...

   ## Verification (independent sub-agent)
   - Original PoC re-run: pass|fail — <expected: fail/blocked>
   - Mitigation test: pass|fail — <expected: pass>
   - Module suite (`:spring-security-<module>:test`): pass|fail — <expected: pass, no regressions>
   - Output: ...
   ```

2. Confirm `git -C <source-dir> status --porcelain` shows exactly the
   expected paths — fixed source file(s) (modified), the test file
   containing the mitigation test method (modified if it's an existing
   class per Step 2 item 4, new only in the fallback case), and the two new
   report files (`reports/GHSA-REMEDIATION-<ghsa-id>.md`,
   `reports/CVE-REPORT-<ghsa-id>.md`) — and nothing else. Anything else
   present is unexpected — stop.
3. `git -C <source-dir> add <fixed-files> <mitigation-test-file> reports/`.
4. Commit with a subject matching `/ghsa-validate`'s own commit message
   style on this branch — `ghsa-remediate: <ghsa-id>` — so `git log` on
   `ghsa-<issue-no>` reads as a matched pair of validate/remediate entries:
   `git -C <source-dir> commit -m "ghsa-remediate: <ghsa-id>"`.
5. Return the checkout to where the user had it:
   `git -C <source-dir> checkout $ORIGINAL_REF`. Confirm
   `git -C <source-dir> status --porcelain` is empty again.

Never push `ghsa-<issue-no>` and never post anything to GitHub — both are
visible-to-others, hard-to-reverse actions the user takes manually,
including publishing the drafted CVE report.

---

## Hand back to the user

Print:

1. Confirmation the fix, mitigation test, and both reports are committed,
   with the commit sha on `ghsa-<issue-no>`.
2. The Step 3 verification results inline (three pass/fail lines) — proof,
   not just a pointer to the file.
3. The full CVE report draft content, inline: proposed title, proposed
   description, CVSS 3.1 vector string, per-metric rationale, and the NVD
   calculator link — visible without opening `reports/CVE-REPORT-<ghsa-id>.md`.
4. Confirmation `$ORIGINAL_REF` is checked out again with a clean tree.
