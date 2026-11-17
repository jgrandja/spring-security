---
name: ghsa-populate-advisory
description: >-
  Update a defined subset of a GitHub Security Advisory (title, CVE ID,
  affected products - ecosystem, package, affected/patched versions - CVSS
  vector string, and CWEs) from the CVE report /ghsa-remediate generated.
  Resolves affected/patched versions per supported branch from api.spring.io,
  shows the exact request body, and submits only after the user confirms. Use
  when asked to "populate/update the advisory", "fill in the GHSA", or given a
  security-advisory-issues URL whose ghsa-<issue-no> branch already has a
  CVE report from /ghsa-remediate.
argument-hint: "<security-advisory-issue-url> [--source-dir <path>]"
allowed-tools:
  - Read
  - Write
  - Grep
  - AskUserQuestion
  - Bash(gh issue view:*)
  - Bash(gh api:*)
  - Bash(git:*)
  - Bash(curl https://api.spring.io/*)
  - Bash(./gradlew:*)
  - Bash(find:*)
  - Bash(ls:*)
  - Bash(python3:*)
---

# ghsa-populate-advisory

**Invocation:**
`/ghsa-populate-advisory <security-advisory-issue-url> [--source-dir <path>]`

Throughout this skill, `<project-name>` is the Spring project slug used by
api.spring.io (e.g. the segment in `https://api.spring.io/projects/<slug>`).
It is resolved in Step 1 — never assume or hardcode it.

---

## Inputs

Parse `$ARGUMENTS` (same as `/ghsa-remediate`):

- `<security-advisory-issue-url>` (positional, required): the issue in
  https://github.com/spring-projects/security-advisory-issues/issues already
  passed to `/ghsa-validate` and `/ghsa-remediate`.
- `--source-dir <path>` (optional): local checkout of the source repo.
  Defaults to the current working directory. Must have the `ghsa-<issue-no>`
  branch.

If `--source-dir` isn't a git checkout, or the issue URL is missing, stop and
ask rather than guessing.

---

## Prerequisites / safety preamble

State these before doing anything else:

1. `gh` must be authenticated with access to
   `spring-projects/security-advisory-issues` and
   `spring-projects/security-advisories`.
2. **`/ghsa-remediate` MUST have been run.** Required: branch
   `ghsa-<issue-no>` containing `reports/CVE-REPORT-<ghsa-id>.md`. Otherwise
   stop: `ERROR: no CVE report found — run /ghsa-remediate <issue-url> first.`
3. **Only these advisory fields are ever sent:** `summary` (title), `cve_id`,
   `vulnerabilities`, `cvss_vector_string`, `cwe_ids`. Never send
   `description`, `severity`, `state`, `credits`, collaborators, or anything
   else. Never publish, change state, or post comments.
4. **The PATCH is the only outward action** and happens only after the user
   explicitly accepts the displayed request body (Step 6). Re-confirm after
   every edit. Never pre-approve on the user's behalf.
5. Read-only against git: files are read with `git show <ref>:<path>` — no
   checkout, no commits, no pushes. The only write is `git fetch` of
   remote-tracking refs for the applicability check (Step 4); the user's
   working tree is never touched.
6. Never invent data. If a value can't be resolved from the CVE report, the
   advisory, the source checkout, or api.spring.io, ask the user.

---

## Step 1 — Resolve issue, advisory, project, and require prior remediation

1. `gh issue view <n> --repo spring-projects/security-advisory-issues --json title,body,url,number`;
   extract `<ghsa-id>` from the body, as in `/ghsa-validate` Step 1.
2. Require the evidence, without checking anything out:
   - `git -C <source-dir> show-ref --verify --quiet refs/heads/ghsa-<issue-no>`
   - `git -C <source-dir> cat-file -e ghsa-<issue-no>:reports/CVE-REPORT-<ghsa-id>.md`

   If either fails, stop with the Prerequisite 2 error.
3. `gh api repos/spring-projects/security-advisories/security-advisories/<ghsa-id>`
   — keep the current values of the five target fields for the Step 6
   before/after comparison.
4. **Resolve `<project-name>`**: candidates, in order — the project named in
   the issue (title, body, labels), then the `--source-dir` origin repo name.
   Verify with `curl https://api.spring.io/projects/<project-name>` (must
   return the project, not 404). If no candidate verifies, ask the user.

---

## Step 2 — Extract from the reports

Read via `git show ghsa-<issue-no>:reports/<file>`:

- From `CVE-REPORT-<ghsa-id>.md`: proposed **title**; applicable **CWE** IDs
  (normalize to `CWE-<n>`, keep root-cause-first order); **CVSS 3.1 vector
  string** (`CVSS:3.1/AV:…/A:_`).
- From `GHSA-REMEDIATION-<ghsa-id>.md`: the **Files changed** list, production
  files only, not tests (used to find the affected module in Step 4 and the
  vulnerable code in the applicability check).
- From `GHSA-VALIDATION-<ghsa-id>.md`: any recorded first-affected version
  (used as the affected-range lower bound if present).

Each report's header must name the same `<ghsa-id>` as Step 1; if not, stop.
If the title, CWEs, or vector is missing from the CVE report, say so and ask
the user instead of defaulting.

---

## Step 3 — Confirm scalar fields with the user

Ask via `AskUserQuestion`, each with the CVE-report value as the recommended
default:

1. **Title** — accept or edit.
2. **CVE ID** — optional; free text in the form `CVE-YYYY-NNNNN`. Validate
   `^CVE-\d{4}-\d{5,}$`. If the user has none, **omit** `cve_id` from the
   body (never send `null`, which would clear an existing value).
3. **CVSS vector string** — accept or edit; validate the
   `CVSS:3.1/AV:_/AC:_/PR:_/UI:_/S:_/C:_/I:_/A:_` format.
4. **CWEs** — accept the full list or edit.

`AskUserQuestion` rejects questions with fewer than 2 options. If a field has
a single candidate (typically the CVSS vector or CWE list), offer **Accept
`<value>`** / **Edit**, or — if it equals the advisory's current value — state
it as the value in use and show it at the Step 6 gate. Never add filler options.

---

## Step 4 — Resolve affected products

1. **Ecosystem**: default `maven`; user may override.
2. **Package name**: `<group_id>:<artifact_id>`.
   - `<group_id>`: read from the source checkout's build (Gradle `group`
     property in `gradle.properties`/build files, or Maven `<groupId>`); if
     the advisory already lists packages, reuse its group. Never hardcode.
   - `<artifact_id>`: the project **module** containing the affected
     component. Map each changed production file from Step 2 to its module by
     walking up to the nearest build file, and use the module's published
     artifact name (for Gradle repos that name the project after the build
     file, that is the build file's name; confirm with `./gradlew projects`
     or the Maven `<artifactId>` if unsure). If the fix spans several
     modules, create one package per module and ask the user to confirm.
3. **Affected and patched versions** — see "Version resolution", then filter
   with the "Applicability check". Produces one `vulnerabilities[]` entry per
   vulnerable branch per package.

### Version resolution

Data (fetched with `curl https://api.spring.io/...`) — a single call:

- Generations: `https://api.spring.io/projects/<project-name>/generations`

Field names as exposed by the API:

| Concept | API field |
|---|---|
| Branch name | generation `name` (e.g. `6.5.x`) |
| OSS support end | `ossSupportEndDate` |
| Enterprise support end | `commercialSupportEndDate` |
| Latest OSS release | `latestPatch.oss` (may be absent) |
| Latest Enterprise release | `latestPatch.enterprise` (may be absent) |

Algorithm:

1. A generation is **OSS-supported** if `ossSupportEndDate >= today` and
   **Enterprise-supported** if `commercialSupportEndDate >= today`. Target set
   = union. Label each OSS / Enterprise / both.
2. For each target generation `X.Y.x`, take the latest released version `V`:
   - OSS-supported branch (including OSS + Enterprise) → `latestPatch.oss`.
   - Enterprise-only branch (OSS support ended) → `latestPatch.enterprise`.

   Enterprise versions may have a 4th component (e.g. `7.0.8.1`).
3. From `V`, infer the **last affected** and **patched** versions (label them
   as inferred in Step 6 for the user to confirm or correct). For `V` =
   `X.Y.Z[.N]`:
   - **OSS-supported branch** (special case): the next release `X.Y.(Z+1)`
     ships *without* the fix; the fix first appears as the hotfix
     `X.Y.(Z+1).1`, and the release after that, `X.Y.(Z+2)`, carries it
     publicly. Last affected = `X.Y.(Z+1)`; patched =
     `X.Y.(Z+1).1, X.Y.(Z+2)`.
   - **Enterprise-only branch**: last affected = `V`; patched = the next
     3-component release `X.Y.(Z+1)`.
4. **Affected range** = `>= <introduced>, <= <last affected>`;
   `<introduced>` defaults to `X.Y.0`, or the first-affected version from the
   validation report when it falls in that generation. State this assumption
   in Step 6.
5. Missing data:
   - No `latestPatch` on either track and no remote branch (an upcoming
     `X.Y.x`): exclude as "unreleased" and list it in Step 6; don't ask.
   - Only the selected track's `latestPatch` is absent (e.g. an OSS branch
     with no `latestPatch.oss`): ask the user for both versions — never fall
     back to the other track or guess.

### Applicability check (per branch)

Never assume every supported branch is vulnerable. After computing the
candidate entries, check each one against the branch's current source:

1. **Fix hunks**: find the remediation commit
   (`git -C <source-dir> log ghsa-<issue-no> --grep='ghsa-remediate: <ghsa-id>' --format=%H -n 1`)
   and get the removed (`-`) lines of the production files from
   `git diff <sha>^ <sha> -- <changed production files>`. These identify the
   vulnerable code.
2. **Remote branches for the generation** (never tags or local branches,
   which may be stale). For `X.Y.x`, the candidates are `X.Y.x` and
   `embargoed/{lts,frozen,active}/X.Y.x` on the `oss` and `commercial`
   remotes (`git -C <source-dir> remote -v`;
   `git ls-remote --heads <remote> 'X.Y.x' 'embargoed/*/X.Y.x'`). Check
   **every** candidate that exists and require them to agree (any
   disagreement → UNDETERMINED). Enterprise-only generations often have only
   `commercial/embargoed/lts/X.Y.x`; also check `oss/X.Y.x` when present.

   For each candidate, refresh with an explicit refspec (a bare
   `git fetch <remote> <branch>` may only write `FETCH_HEAD`):
   `git -C <source-dir> fetch <remote> refs/heads/<branch>:refs/remotes/<remote>/<branch>`,
   then confirm freshness — `git rev-parse refs/remotes/<remote>/<branch>`
   must equal `git ls-remote <remote> refs/heads/<branch>` — else treat as
   stale (UNDETERMINED). If no candidate exists or a fetch fails, the
   generation is UNDETERMINED. Print each ref and its sha as evidence, and
   avoid shell loops that reuse positional parameters (`read`/`set --`)
   across fetch and show.
3. **Check** `git show refs/remotes/<remote>/<branch>:<path>` for each changed
   file: the file is missing, or the removed lines are absent → **NOT_VULNERABLE**; removed
   lines present → **VULNERABLE**. Matching is by meaningful code lines
   (ignore whitespace/comments); partial matches → UNDETERMINED. If the
   branch already contains the fix's added lines (e.g. an `embargoed/*`
   branch the fix landed on), absence of the removed lines proves nothing →
   UNDETERMINED, never NOT_VULNERABLE.
4. Result handling: VULNERABLE → keep; NOT_VULNERABLE → **exclude** the
   branch (list it with its evidence in Step 6); UNDETERMINED → ask the user
   whether to include it. Never include or exclude a branch silently, and
   never claim a check you did not run. A PoC run on a branch is out of
   scope here (see `/ghsa-verify-fix`).

---

## Step 5 — Build the request body

Write to the scratchpad directory (never the repo):

```json
{
  "summary": "<title>",
  "cve_id": "CVE-YYYY-NNNNN",
  "cvss_vector_string": "CVSS:3.1/...",
  "cwe_ids": ["CWE-862", "CWE-200"],
  "vulnerabilities": [
    {
      "package": { "ecosystem": "maven", "name": "<group_id>:<artifact_id>" },
      "vulnerable_version_range": ">= X.Y.0, <= <last affected>",
      "patched_versions": "<patched>[, <patched>]"
    }
  ]
}
```

Before building the body, compare the advisory's current `vulnerabilities`
(Step 1 item 3) with the new set. GitHub replaces the whole array, so any
previously listed range the new entries don't cover (e.g. older generations
such as `>= 5.2.0.RELEASE`) would be dropped. Surface these in Step 6 and ask
whether to keep them as extra entries rather than dropping them silently.

`cve_id` is omitted when the user has none. `patched_versions` is comma
separated when a branch has two (OSS branches). GitHub **replaces** the whole
`vulnerabilities` array on PATCH, so always send the complete intended set.

---

## Step 6 — Confirm, then submit (the gate)

1. Print:
   - the exact JSON body;
   - the branch table: branch, OSS/Enterprise, `latestPatch` used,
     applicability result and evidence, affected range, patched, and the
     lower-bound source (validation report or the `X.Y.0` default). Say
     explicitly that last-affected/patched versions are inferred;
   - excluded generations (not vulnerable, unreleased, out of support) and
     any previously recorded ranges the PATCH would drop (Step 5), asking
     how to handle the latter;
   - a before/after of the five fields against Step 1 item 3.
2. Ask via `AskUserQuestion` with self-contained wording (the user may not
   see the output above while the dialog is open): **Submit as shown** /
   **Make changes**.
   - *Make changes*: apply the user's edits, re-validate (CVE regex, vector
     format, version strings), show again, ask again. Loop until accepted.
   - Anything other than explicit acceptance: stop; submit nothing.
3. On accept:
   `gh api -X PATCH repos/spring-projects/security-advisories/security-advisories/<ghsa-id> --input <body-file>`
4. On an HTTP error, surface the raw response and offer to fix and retry.
   Never retry silently.

---

## Step 7 — Verify and hand back

Re-fetch the advisory and compare the five fields to what was sent
(`cwe_ids` as a set — GitHub returns them sorted, so reordering is not a
mismatch). Print: success or mismatches, the advisory URL, and a reminder
that nothing else was modified and nothing was published or pushed.
