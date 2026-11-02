---
name: ghsa-verify-fix
description: >-
  Produce a high-level, GitHub-issue-ready markdown summary proving that the
  fix for a Spring Advisory issue mitigates the reported vulnerability. Finds
  and confirms the fix commit on the target branches (embargoed/lts/*,
  embargoed/frozen/*, embargoed/active/*), runs the PoC committed by
  /ghsa-validate once per unique fix commit (on the latest-generation branch
  carrying it), and reports the verified branches with provenance (AI model,
  prompt, skill URLs). Runs /ghsa-validate first if the ghsa-<issue-no> branch
  or its validation report doesn't exist yet. Use when asked to "verify the
  fix", "summarize the fix verification", "write the proof-of-fix comment for
  this advisory", or given a security-advisory-issues URL.
argument-hint: "<security-advisory-issue-url> [--source-dir <path>]"
allowed-tools:
  - Read
  - Write
  - Glob
  - Grep
  - Skill
  - Bash(gh issue view:*)
  - Bash(gh api:*)
  - Bash(git:*)
  - Bash(mvn:*)
  - Bash(./mvnw:*)
  - Bash(gradle:*)
  - Bash(./gradlew:*)
---

# ghsa-verify-fix

**Invocation:**
`/ghsa-verify-fix <security-advisory-issue-url> [--source-dir <path>]`

## Inputs

- `<security-advisory-issue-url>` (required): the issue in
  https://github.com/spring-projects/security-advisory-issues/issues that was
  passed to `/ghsa-validate`.
- `--source-dir <path>` (optional, default: current directory): local
  checkout. Its `ghsa-<issue-no>` branch comes from `/ghsa-validate`, which
  this skill runs if needed.

If `--source-dir` isn't a git checkout, or the URL is missing, stop and ask.

## Prerequisites / safety preamble

State these before doing anything else:

1. `gh` must be authenticated with access to
   `spring-projects/security-advisory-issues` and
   `spring-projects/security-advisories`.
2. The fix is only ever taken from the target branches, never from a commit
   on `ghsa-<issue-no>`.
3. Never writes or edits tests, and never invents or paraphrases code
   excerpts (copy verbatim from committed files). Never states a result the
   validation report or this skill's own recorded run doesn't show; if a
   needed fact is missing, say so or ask.
4. Sole execution exception: it runs the committed PoC, unmodified, in a
   temporary worktree and removes it afterwards. It never checks out, commits
   to, or pushes a target branch, never pushes anything else, and never posts
   to GitHub; the user does that manually.
5. `git -C <source-dir> status` must be clean (Step 5 commits to
   `ghsa-<issue-no>`). If a git step fails unexpectedly, stop and surface the
   exact `git status`/`git diff` output rather than attempting recovery.

## Step 1 — Load evidence (no checkout yet)

1. `gh issue view <n> --repo spring-projects/security-advisory-issues --json title,body,url,number`;
   extract `<ghsa-id>` from the body, as in `/ghsa-validate` Step 1.
2. Record `$ORIGINAL_REF` (`git -C <source-dir> rev-parse --abbrev-ref HEAD`,
   or `rev-parse HEAD` if detached). Step 5 returns here.
3. **Ensure the validation exists:** `refs/heads/ghsa-<issue-no>` exists
   (`git show-ref --verify --quiet`) and
   `git -C <source-dir> cat-file -e ghsa-<issue-no>:reports/GHSA-VALIDATION-<ghsa-id>.md`
   succeeds. If not, invoke the `ghsa-validate` skill via the Skill tool with
   `<issue-url> --source-dir <source-dir>` and let it finish. Then confirm both
   exist, `$ORIGINAL_REF` is checked out again and `git status --porcelain` is
   empty; otherwise stop and surface the state. Remember whether this skill
   ran it (Step 3.5).
4. Read from the branch with `git -C <source-dir> show ghsa-<issue-no>:<path>`:
   the validation report and the PoC test file from its `## PoC` section.
   Also read the `ghsa-validate:` commit's trailers
   (`git log --format='%H%n%s%n%b' ghsa-<issue-no>`).
5. `gh api repos/spring-projects/security-advisories/security-advisories/<ghsa-id>`
   — keep summary/title, CWE, CVSS, affected/patched ranges; ignore
   `state`/withdrawal metadata.

## Step 2 — Applicability gate

- `NOT_A_CVE` + `already-fixed`, or `VALID_CVE` -> proceed.
- Anything else (`not-reproducible`, `intended-behavior`,
  `duplicate-of-existing-cve`, `insufficient-information`) -> stop:
  `Last recorded outcome for <ghsa-id> is <outcome> (<disposition>) — there is no applied fix to summarize.`

## Step 3 — Gather facts

1. **Locate and prove the fix on the target branches.** The fix MUST be in at
   least one. It was applied by hand, so its sha is unknown: find it, have the
   user confirm it, never assume it.

   a. **Target branches**: every branch matching `embargoed/lts/*`,
      `embargoed/frozen/*` or `embargoed/active/*`, local or on the
      `commercial` remote
      (`git@github.com:spring-projects/spring-security-commercial.git`):
      ```
      git -C <source-dir> for-each-ref --format='%(refname:short)' \
        'refs/heads/embargoed/lts/*' \
        'refs/heads/embargoed/frozen/*' \
        'refs/heads/embargoed/active/*' \
        'refs/remotes/commercial/embargoed/lts/*' \
        'refs/remotes/commercial/embargoed/frozen/*' \
        'refs/remotes/commercial/embargoed/active/*'
      ```
      If none exist, stop and suggest `git fetch commercial`. If a branch
      exists both locally and on `commercial`, use the remote-tracking ref.
      Skip branches whose line isn't in the advisory's affected ranges. A
      version outside the listed ranges (e.g. a newer `main`) is "unclear":
      include it in the 3.1c question, default to testing it, and note in
      `Notes` that the ranges don't cover it. List every skipped branch in
      `Notes`.

   b. **Find candidates per branch:**
      `git -C <source-dir> log <branch> -n 20 --format='%h %s' -- <sink-paths>`
      using the sink file(s) from the validation report (use `--follow` for
      renames). For `already-fixed`, also check whether the cited fix commit
      is an ancestor (`git merge-base --is-ancestor <sha> <branch>`). A
      message mentioning `<ghsa-id>`, the CVE id, or `gh-<issue-no>` is only
      a hint.

   c. **Confirm with the user.** Show branch -> candidate sha(s) and
      subject(s); ask the user to confirm or supply the fix commit per
      branch. With zero or several candidates, ask — never pick one. Skip a
      branch only if the user says the fix isn't on it. Check each confirmed
      commit is reachable from its branch (`git merge-base --is-ancestor`).

   d. **Run the PoC once per unique fix commit.**

      *Grouping:* group the confirmed branches by confirmed sha. In each group
      the *representative* is the latest generation: the highest `version` in
      `gradle.properties` (numeric compare, e.g. `7.2.0` > `7.1.3` > `7.0.9`);
      on a tie prefer `active` > `frozen` > `lts`; if still tied or unreadable,
      ask. Show the user the groups and representatives before the first run.
      Only representatives are run; the rest of a group is "not run (same
      commit)". If a representative ends `INCONCLUSIVE`, its whole group does;
      don't fall back to another branch unless the user asks. Below, "branch"
      means a representative.

      *Execution:* a cold build takes ~1.5–3 minutes per branch (the
      validation skill's 180s limit doesn't apply). Run branches one at a time
      (they share the Gradle daemon and caches) as one background Bash command
      with a generous `timeout`; wait for its notification, don't poll. If you
      script the loop, keep the script in `<scratch>`, pass each branch name
      once (the script owns the `refs/remotes/<remote>/` prefix), write full
      logs to files in `<scratch>`, and summarize with narrow patterns
      (`FAILED`, `BUILD`, `tests completed`, `What went wrong`, the sink stack
      line). Add `--no-build-cache` so a cached task can't hide whether the
      test ran.

      1. `git -C <source-dir> worktree add --detach <scratch>/<branch-slug> <branch>`
         (`<scratch>` = the session scratch directory, outside `<source-dir>`;
         `<branch-slug>` = branch name with `/` replaced by `-`).
      2. Copy the PoC byte-for-byte from `ghsa-<issue-no>` to the same path in
         the worktree. Never edit it; if it doesn't compile (API drift) the
         result is `INCONCLUSIVE` — report and ask.
      3. From the worktree, run the report's exact test command (plus
         `--no-build-cache`); record command, JDK, tests/failures/errors from
         the build output or result files. A sink-side exception is a JUnit
         **error** but Gradle prints it as "failed". If the result files can't
         be read and the output only says "N tests completed, M failed", report
         `tests` = N, `failures` = M, `errors` = `-`, and say under the table
         that the split wasn't read. Never split by guessing.
      4. Classify per the report's description of the PoC.
         - `BLOCKED`: for `VALID_CVE`, the exploit tests that passed on
           vulnerable code now fail, on an exploit assertion or because the
           sink call itself throws (confirm from the stack line that the throw
           is from the sink call, not setup); for `already-fixed`, the PoC
           passes as recorded. Control/no-regression tests still pass.
         - `INCONCLUSIVE`: compile/setup errors or unrelated failures. If the
           build fails before any test runs (uncached plugin under
           `--offline`, 401/403 from a repository), retry once without
           `--offline` and record the deviation. If it still fails, mark it
           `INCONCLUSIVE`, quote the failing URL and HTTP status, and note it
           needs a re-run with repository access. Never guess a result.
         - `STILL VULNERABLE`: the exploit succeeds — after step 5, stop,
           surface it, and write no summary claiming mitigation.
      5. Always, even after a failure:
         `git -C <source-dir> worktree remove --force <scratch>/<branch-slug>`;
         confirm `git worktree list` no longer shows it and `git status
         --porcelain` is empty.

   e. **Record** each `BLOCKED` representative with its sha and the other
      branches in its group; list all other target branches in `Notes` with
      the reason (fix absent, `INCONCLUSIVE`). **If no group is `BLOCKED`,
      stop** and write and commit nothing:
      `ERROR: fix for <ghsa-id> is not verified in any target branch — it cannot be reported as mitigated.`

   f. **Fix line**, per `BLOCKED` commit: its subject, minimal sink diff
      (`git show <sha> -- <main-source-paths>`), and containing release tags
      (`git tag --contains <sha>`). Keep only tags of the branch's own release
      line; if a sha spans several lines or the line can't be told apart, list
      every containing tag once, compare each line's tags to its patched
      version in the advisory, and put any mismatch (e.g. patched `7.0.9` but
      only `7.0.8.1` contains the commit) in `Notes`. Never drop a tag to make
      lines agree.
2. **Code under test.** The report's `Source` commit (vulnerable code for
   `VALID_CVE`) and its project version
   (`git -C <source-dir> show <sha>:gradle.properties` or `pom.xml`), plus each
   run branch's tested short sha and version.
3. **PoC excerpts** — verbatim, each at most ~15 lines, trimmed only at
   statement boundaries:
   - (a) **vulnerable behavior**: the pre-fix control test or, for
     `VALID_CVE`, the exploit test plus the report's result on the `Source`
     commit. If none exists, say so — don't write one.
   - (b) **blocked with the fix**: the assertion(s) showing the exploit is
     rejected / has no effect. If one test serves as both (a) and (b), quote
     it in full once under (a) and, under (b), only its final statement(s)
     verbatim plus the recorded failure (exception type, message, sink stack
     line) as plain text.
   - Constants an excerpt depends on (e.g. `PAYLOAD`), quoted once beneath.
4. **Further evidence, if present:** bypass/escape attempts (count +
   one-line list) and no-regression checks of documented behavior.
5. **Provenance.**
   - *AI models*: the validation model from the `Co-Authored-By:` trailer of
     the `ghsa-validate:` commit, which carries the model ID as `Co-Authored-By: <name> (<model-id>) <…>` (ask if absent or if it has no ID), and the model running this
     skill (ask if unknown). They are the same only if this skill ran
     `/ghsa-validate` itself (Step 1.3). Take a model ID only from the trailer
     or this session's environment for the model it describes. If this skill
     ran `/ghsa-validate` and the trailer has only a name, use this session's
     ID for both and say they are the same session; otherwise, if the trailer
     has no ID, ask for it.
   - *Prompts*: `/ghsa-validate <issue-url>` — the Step 1.3 invocation if this
     skill ran it, else reconstructed from the report's `Issue` and `Source`
     lines (when unsure, ask) — and `/ghsa-verify-fix <issue-url>`. Omit
     `--source-dir` when `<source-dir>` is the launch directory; otherwise
     write `--source-dir <local-checkout>` (no local paths in the summary).
   - *Skill URLs* for `ghsa-validate` and `ghsa-verify-fix`:
     `https://github.com/<owner>/<repo>/blob/<skills-branch>/.agents/skills/<skill>/SKILL.md`.
     `<skills-branch>` is the one remote-tracking ref (from
     `git for-each-ref refs/remotes`) whose
     `git rev-parse <ref>:.agents/skills/<skill>/SKILL.md` equals
     `git rev-parse HEAD:.agents/skills/<skill>/SKILL.md`; several or none ->
     ask. Map its remote to `<owner>/<repo>` via `git remote get-url`. Never
     link a branch that isn't on a remote.
   - *Base branch*: on the upstream remote (URL matching
     `spring-projects/<repo>`), the `main` or `<N>.x` candidate with the
     fewest commits in `git rev-list --count <merge-base>..<skills-branch>`.

## Step 4 — Write the summary

Audience: Spring Security maintainers and the security team, on a private
advisory issue. High-level, factual, paste-ready — no local filesystem paths
or local-only branches. Drop optional items the evidence doesn't support:

````markdown
## Fix verification: <ghsa-id>

**Result: the fix mitigates the vulnerability on <the PoC-run branches>.**
<one sentence: verdict (`NOT_A_CVE / already-fixed` or `VALID_CVE`) and what
it means for affected vs. fixed code. Per `BLOCKED` commit add: "The PoC was
run once for `<sha>` on `<representative>`; `<other branches in the group>`
carry the identical commit and were not run separately." If any other target
branch is unverified add: "The remaining branches are not PoC-verified (see
Notes)." Never imply a PoC result on a branch that wasn't run.>

### What was verified
- **Vulnerability:** <2–3 sentences: entry point -> sink -> impact.>
- **Fix:** commit `<short-sha>` ("<subject>"), contained in tags <tags, or "not yet released">:

  ```diff
  <minimal sink diff>
  ```
- **Fix located in branches:** `<branch>` (`<short-sha>`), ... (every branch in
  a `BLOCKED` group, with its confirmed commit; mark representatives "(PoC run)").
- **Code under test:** `<repo>` `<version>` (<line, e.g. 7.0.x>) @ `<short-sha>`.

### How it was proven
<one sentence: the chain the PoC drives, and that payloads are benign.>

1. **Pre-fix control:** <what it shows>.

   ```java
   <verbatim excerpt (a)>
   ```

2. **With the fix:** <what it shows>.

   ```java
   <verbatim excerpt (b)>
   ```

   <constants the excerpts depend on>

3. **Bypass attempts:** <N> techniques, all rejected with no side effect: <list>.   (optional)
4. **No functional regression:** <documented behavior still works>.                 (optional)

<Number items consecutively, renumbering after dropping optional ones.>

**Execution:** `<command>`, per branch (<JDK>):
<list every target branch. Use `-` for tested sha/tests/failures/errors and
`INCONCLUSIVE` where the build didn't reach test execution, or
`not run (same commit as <representative>)` for non-representatives>

| branch | tested sha | tests | failures | errors | result |
|---|---|---|---|---|---|

<If the failure/error split was not read, one sentence saying so (Step 3.1d.3).>

### Notes                                                                          (optional)
- <hardening suggestions not required for this fix>
- <advisory metadata mismatches, e.g. patched ranges vs. containing tags>
- <target branches where the fix is not verified or not affected, with reason>

### Validation provenance
- **AI models:** <validation model> (`<model-id>`); proof run: <model> (`<model-id>`)
- **Prompts sent to the agent:**
  ```
  <exact invocations>
  ```
- **Skills used:** `ghsa-validate`: <url>; `ghsa-verify-fix`: <url>
  (the `<branch>` branch of the `<owner>/<repo>` fork, based on `spring-projects/<repo>` `<base-branch>`)
- The validation skill is barred from reading the issue's labels, state, or
  maintainer comments before reaching a verdict, and every conclusion is
  backed by executed PoC tests rather than prose reasoning alone.
````

## Step 5 — Persist the summary in one commit

1. `git -C <source-dir> checkout ghsa-<issue-no>`.
2. Write `reports/GHSA-VERIFY-FIX-<ghsa-id>.md` with exactly the Step 4
   content, overwriting if it exists (re-generation is expected).
3. `git -C <source-dir> status --porcelain` must show only that path;
   otherwise stop.
4. `git -C <source-dir> add reports/GHSA-VERIFY-FIX-<ghsa-id>.md` then
   `git -C <source-dir> commit -m "ghsa-verify-fix: <ghsa-id>"` — append
   ` [re-generated]` if the file already existed on the branch.
   Add a trailer carrying the exact model ID of the model running this skill
   (taken from this session's environment, never guessed), as a second `-m`:
   `-m "Co-Authored-By: <model name> (<model-id>) <noreply@anthropic.com>"`,
   e.g. `Co-Authored-By: Claude Sonnet 5.5 (claude-sonnet-5-5) <noreply@anthropic.com>`.
   This replaces any name-only attribution line the harness suggests.
5. `git -C <source-dir> checkout $ORIGINAL_REF`; confirm
   `git -C <source-dir> status --porcelain` is empty.

## Hand back to the user

Print:

1. The full summary markdown inline, ready to paste into the issue.
2. The path `reports/GHSA-VERIFY-FIX-<ghsa-id>.md` and the commit sha on
   `ghsa-<issue-no>`.
3. Confirmation that `$ORIGINAL_REF` is checked out with a clean tree and
   every temporary worktree was removed.
4. A reminder that the skill URLs resolve only while `<skills-branch>` stays
   pushed and isn't rewritten.
