---
name: ghsa-validate
description: >-
  Analyze a Spring Advisory issue and determine whether the reported
  vulnerability is valid (CVE-worthy) or not, against a specific local source
  checkout — and prove the verdict with an executed PoC/test rather than prose
  reasoning alone. Writes a validation report and commits it together with the
  PoC to a long-lived `ghsa-<issue-no>` branch. Use when asked to "validate
  this advisory", "check if this GHSA is a real CVE", or given a
  security-advisory-issues URL plus a source checkout.
argument-hint: "<security-advisory-issue-url> [--source-dir <path>]"
allowed-tools:
  - Read
  - Write
  - Glob
  - Grep
  - Edit
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

# ghsa-validate

**Invocation:**
`/ghsa-validate <security-advisory-issue-url> [--source-dir <path>]`

---

## Inputs

Parse `$ARGUMENTS`:

- `<security-advisory-issue-url>` (positional, required): an issue in
  https://github.com/spring-projects/security-advisory-issues/issues.
  **This issue is a synced mirror** of a draft advisory in
  https://github.com/spring-projects/security-advisories/security/advisories
  — the `<security-advisory-url>`, linked from the issue body. Both must be
  fetched: the issue for the triage-facing summary/discussion, the advisory
  for the authoritative CWE/CVSS/affected-version/patch data GitHub tracks on
  the advisory object itself.
- `--source-dir <path>` (optional): local checkout of the source repo the
  advisory concerns. Defaults to the current working directory if omitted.
  Must be a git checkout with a working Maven (`pom.xml`) or Gradle
  (`build.gradle[.kts]`) build, since Step 4 actually runs a test against it.
  Git commit authorship (`user.name`/`user.email`) must already be configured
  for this checkout, since Step 6 commits to it.

If the resolved `--source-dir` (explicit or defaulted to the current working
directory) isn't a git checkout with a Maven/Gradle build, or the positional
issue URL is missing, stop and ask rather than guessing.

---

## Prerequisites / safety preamble

State these before doing anything else:

1. `gh` must be authenticated with access to
   `spring-projects/security-advisory-issues` and
   `spring-projects/security-advisories` — the latter is typically private
   pre-publication; draft advisories 404 for an unauthenticated or
   under-scoped token.
2. **This skill executes code.** Step 4 builds `--source-dir` and runs one
   test against it. Before touching anything, `git -C <source-dir> status`
   and stop if it has uncommitted changes — otherwise a PoC's own edits get
   conflated with the user's in-progress work.
3. **The PoC and the report are persisted, not discarded, on a per-issue
   branch.** `<issue-no>` is the issue number from
   `<security-advisory-issue-url>`, resolved in Step 1 item 1 — distinct from
   `<ghsa-id>` (the advisory's own identifier, resolved in Step 1 item 2),
   which is what the PoC test and report files are named after. See Step 6
   for the full branch/commit mechanics; re-validating the same advisory a
   second time is expected, not an error.
4. No network access from inside the PoC test itself. The vulnerability must
   be demonstrable via the checkout's own build/test tooling, not by
   reaching out to external services.
5. This skill never pushes `ghsa-<issue-no>` (or anything else) to a
   remote, and never posts the verdict to GitHub. Both are visible/hard-to-
   reverse actions left to the user.
6. If any git step fails unexpectedly, stop and surface the exact `git
   status`/`git diff` output rather than attempting further recovery — never
   force through a git conflict. The one expected exception is a stash-pop
   conflict on this run's own `ghsa-id` output — see Step 6, item 5.
7. **Step 4's PoC run is bounded by a hard 3-minute (180s) wall-clock
   timeout.** A PoC that can't prove the claimed behavior in 3 minutes fails
   fast as its own explicit stop condition, surfaced as an error rather than
   forced into a `NOT_A_CVE`/`insufficient-information` verdict from a run
   that never actually finished. Unlike item 6's git-step failures, this is
   not a git problem — it is surfaced as this issue's `RESULT: ERROR`.
   See Step 4, item 2.

---

## Step 1 — Fetch and parse the advisory

**No-verdict-peeking rule:** Steps 1–3 must never read, quote, or retain any
signal of the issue/advisory's *disposition* — open/closed state, close
reason, triage labels (e.g. `valid`, `invalid`, `duplicate`), or maintainer
comments discussing the verdict — so Step 3's conclusion is independent
validation, not confirmation of a verdict it already saw. Concretely:

- `gh issue view` calls must pass an explicit `--json` field list containing
  only content fields (`title,body,url,number` — add `comments` only if the
  reporter's own repro/PoC lives there, and if so read only the reporter's/
  reporter-provided comments, skipping any maintainer replies). Never request
  or read `state`, `stateReason`, `closed`, `closedAt`, or `labels`.
- `gh api .../security-advisories/<ghsa-id>` calls must similarly ignore the
  GHSA's own `state` field (`draft`/`published`/`closed`/`withdrawn`) and any
  `withdrawn_at`/`ghsa_id`-status metadata — extract only CWE, CVSS, affected
  version range, and patch/fix references.
- If disposition information appears unavoidably alongside content that must
  be read (e.g. an inline label mention inside the issue body text itself),
  do not carry it into Step 3's reasoning or the eventual report — treat it
  as noise, not evidence.

1. Resolve `<security-advisory-issue-url>` -> issue number/repo:
   `gh issue view <n> --repo spring-projects/security-advisory-issues --json title,body,url,number`
2. Extract the linked `<security-advisory-url>` from the issue body and fetch
   it: `gh api repos/spring-projects/security-advisories/security-advisories/<ghsa-id>`.
   `gh` has no `advisory` subcommand, so this raw `gh api` call is the only
   path; it works directly against a live `draft`-state advisory given the
   token's `repo` scope plus admin/triage access on
   `spring-projects/security-advisories`. Extract CWE (`.cwes[].cwe_id`),
   CVSS (`.cvss`), affected version range, and any patch/fix reference
   already on file — discarding the response's `state`/withdrawal metadata
   per the no-verdict-peeking rule above.
3. Summarize: reported component, claimed weakness class (CWE), claimed
   severity, affected version range, reporter-supplied repro steps/PoC (if
   any), any patch commit referenced.

---

## Step 2 — Resolve source & scope

1. Record the checkout's current ref before doing anything else — a branch
   name (`git -C <source-dir> rev-parse --abbrev-ref HEAD`) or, if that
   returns `HEAD` (detached), the commit sha
   (`git -C <source-dir> rev-parse HEAD`). This is `$ORIGINAL_REF`; Step 6
   returns the checkout here at the end.
2. Confirm `--source-dir`'s checked-out version falls inside (or just
   outside) the claimed affected range. If it's checked out at a version
   already containing a referenced fix commit, note this up front — it
   shapes whether the eventual verdict is `already-fixed`.
3. Detect build tool (Maven vs Gradle) by presence of `pom.xml` /
   `build.gradle[.kts]`; this determines the Step 4 test-invocation command.

---

## Step 3 — Investigate

- Read the reported class(es) and trace the claimed data/control flow from
  entry point to sink, exactly as the advisory describes it.
- Check for existing protections (validation, auth gates, framework
  defaults) the advisory may have missed.
- Investigate related/sibling classes **only for variants of the same
  reported weakness** — e.g. if the bug is a missing check in one component,
  check sibling components for the identical omission. This is validation of
  one advisory, not a general audit; unrelated vulnerabilities noticed
  incidentally are out of scope for this skill's verdict (do not silently
  drop them from the user's awareness — mention once, briefly, at the very
  end, but they do not affect this advisory's verdict).
- If a patch/fix commit was referenced in Step 1, diff it against
  `--source-dir` to see whether it's present, partially applied, or absent.

---

## Step 4 — Prove it: write and execute a PoC (on `$ORIGINAL_REF`)

1. Write a single JUnit test (or the project's existing test idiom) into a
   scratch location inside `--source-dir` — e.g. a test class under the
   closest existing test source root, named so it's obviously produced by
   this skill and unique per advisory (include the GHSA id in the
   filename/package to avoid collisions across runs, e.g.
   `GhsaValidatePocTest_<ghsa-id>`).
2. Run only that test via the detected build tool (`mvn -pl <module> test -Dtest=...`,
   or the Gradle equivalent), under a hard **3-minute (180s) wall-clock
   timeout** — invoke the build command via the Bash tool with
   `timeout: 180000` (milliseconds) rather than relying on a shell-level
   `timeout` command that may not exist on every runner. Capture pass/fail
   and output.
3. **On a timeout (Prerequisites item 7), fail fast:**
   - Delete the PoC test file written in item 1 — with no completed run
     there's no evidence to preserve, and leaving a stray untracked file
     behind would trip a later run's clean-checkout gate (Prerequisites
     item 2).
   - Confirm `git -C <source-dir> status --porcelain` is empty again (the
     deletion should be the only change, and it undoes item 1 completely).
   - Stop the skill entirely and report the error status back to the
     caller — e.g. `ERROR: PoC execution timed out after 180s: <command>` —
     instead of proceeding to Step 5/6. No report is written and nothing is
     committed for this run.
4. A **passing** test that reproduces the reported behavior is evidence for
   `VALID_CVE`. A test that cannot be made to trigger the claimed behavior
   (after genuinely trying the reporter's steps, not a strawman) is evidence
   for `NOT_A_CVE` / `not-reproducible`. This applies only to a run that
   completed within the 3-minute limit — see item 3 for timeouts.
5. Do **not** delete the test file — it's the evidence, and Step 6 commits it
   permanently to `ghsa-<issue-no>`. (This applies to a completed run, per
   item 4 — a timed-out run's test file was already deleted in item 3.)

---

## Step 5 — Verdict

Exactly one top-level outcome, plus a disposition:

- **`NOT_A_CVE`** — disposition one of: `not-reproducible`, `already-fixed`,
  `duplicate-of-existing-cve` (cite the prior GHSA/CVE id),
  `intended-behavior`, `insufficient-information` (state exactly what's
  missing and what would resolve it; this is a forced low-confidence
  verdict, not a stalling tactic).
- **`VALID_CVE`** — disposition one of: `novel`, `regression` (cite the prior
  GHSA/CVE id it re-introduces).

For `VALID_CVE`, also state, as discrete facts rather than leaving them
implicit in the PoC narrative:

- the **entry point** — the concrete API/endpoint/configuration surface the
  PoC actually drives the exploit through;
- the **trust boundary crossed**;
- the **mechanism** and **impact** of the weakness; and
- every **precondition** required for an application to be exploitable:
  specific configuration/mechanism enablement, required
  dependencies/versions, attacker prerequisites (e.g. a valid session, a
  specific role, network position).

Phrase all of the above as framework-level facts ("requires X mechanism
enabled," not "requires the demo app's Config").

For `NOT_A_CVE`, the reason must be as rigorous as a `VALID_CVE`
justification — cite the specific protection, fix commit, or reproduction
failure, not just "could not reproduce."

---

## Step 6 — Write output and persist everything in one commit

1. Write the report to `<source-dir>/reports/` (a directory at the
   **root of `--source-dir`**, created if absent) —
   `GHSA-VALIDATION-<ghsa-id>.md`. This still happens on `$ORIGINAL_REF`,
   alongside the PoC test file from Step 4. Structure it per the shape below,
   as plain markdown sections — no YAML frontmatter.
2. Confirm `git -C <source-dir> status --porcelain` shows exactly the
   expected new/untracked paths (the Step 4 test file and the new
   `reports/` files) and nothing else. Anything else present is
   unexpected — stop.
3. Set aside those exact paths without touching anything tracked:
   `git -C <source-dir> stash push -u -m "ghsa-validate:<ghsa-id>" -- <poc-test-path> reports/`.
4. Check whether the `ghsa-<issue-no>` branch already exists
   (`git -C <source-dir> show-ref --verify --quiet refs/heads/ghsa-<issue-no>`):
   - **Doesn't exist** → create it from `$ORIGINAL_REF`:
     `git -C <source-dir> checkout -b ghsa-<issue-no>`.
   - **Exists** → `git -C <source-dir> checkout ghsa-<issue-no>`. Never
     recreate, rebase, or force-push this branch — it's an append-only audit
     trail across every past run against this issue.
5. `git -C <source-dir> stash pop`. A conflict here means this issue was
   already validated on this branch by an earlier run — that's an expected
   re-validation, not a failure, and the fresh evidence wins: resolve every
   conflicting path in favor of the stash (this run's just-generated
   content), e.g.
   `git -C <source-dir> checkout --theirs -- <poc-test-path> reports/GHSA-VALIDATION-<ghsa-id>.md`,
   then `git -C <source-dir> add` those same paths, then
   `git -C <source-dir> stash drop` (the pop's conflict leaves the stash
   entry undropped). If the conflict touches any path outside this run's own
   `ghsa-id` output, don't auto-resolve it — stop and surface it per the
   Prerequisites' general git-failure rule instead.
6. Stage and commit **both** the PoC and the report in a single commit:
   `git -C <source-dir> add <poc-test-path> reports/` then
   `git -C <source-dir> commit -m "ghsa-validate: <ghsa-id> — <outcome> (<disposition>)"`
   — append ` [re-validation]` to the message when item 5 resolved a
   conflict, so `git log` on `ghsa-<issue-no>` distinguishes first-time
   validations from updates at a glance.
   Its sha is reported in the terminal summary, not stored in the report —
   `git log` against `ghsa-<issue-no>` is the source of truth for that.
7. Return the checkout to where the user had it:
   `git -C <source-dir> checkout $ORIGINAL_REF`. Confirm
   `git -C <source-dir> status --porcelain` is empty again — the user's
   checkout is exactly as they left it; only `ghsa-<issue-no>` gained a
   commit.

Never push `ghsa-<issue-no>` and never post the verdict back to GitHub —
both are visible-to-others, hard-to-reverse actions the user takes manually
if they choose to.

Report shape (`GHSA-VALIDATION-<ghsa-id>.md`), each as its own heading/labeled
line rather than folded into narrative prose:

```markdown
# GHSA-VALIDATION-<ghsa-id>

- Issue: <issue_url>
- Advisory: <advisory_url>
- Source: <source_dir> @ <source_commit>
- Outcome: VALID_CVE | NOT_A_CVE
- Disposition: novel|regression|not-reproducible|already-fixed|duplicate-of-existing-cve|intended-behavior|insufficient-information

## Reason
...

## PoC
- Test file: ...
- Command: ...
- Result: pass|fail
- Output: ...

## Entry point, trust boundary, mechanism, impact, preconditions (VALID_CVE only)
...

## Related variants checked / incidental findings
...
```

---

## Hand back to the user

Print:

1. `outcome` / `disposition`, one line.
2. The path to the `.md` report.
3. For `VALID_CVE`: entry point, trust boundary, mechanism, impact,
   preconditions — the same facts from Step 5, restated so they're visible
   without opening the file.
4. The commit sha on `ghsa-<issue-no>` (see Step 6, item 6) and confirmation
   that `$ORIGINAL_REF` is checked out again with a clean tree.
5. Any incidental findings from Step 3, flagged as out of scope for this
   advisory's verdict.
