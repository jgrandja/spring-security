# Claude Code Harness Setup — spring-security

Based on ["How Claude Code works in large codebases: best practices and where to start"](https://claude.com/blog/how-claude-code-works-in-large-codebases-best-practices-and-where-to-start).

This repo has no `.claude/` config or `CLAUDE.md` yet. It's a large multi-module Gradle
project (~40 modules: `core`, `config`, `web`, `oauth2`, `saml2`, `ldap`, `cas`,
`crypto`, `acl`, `messaging`, `rsocket`, etc.), so the article's guidance on making a
large codebase "legible" to Claude applies directly. Below is a concrete setup checklist
for this repository, in priority order.

## 1. Root `CLAUDE.md` — lean, high-signal only

Create `./CLAUDE.md` with only what's true everywhere and expensive to rediscover.
Keep it short — this is not a place to document architecture (Claude can read the code).

Suggested content for spring-security:
- Build system: Gradle wrapper (`./gradlew`), JDK version pinned via `.sdkmanrc`
  (`java=25-librca`) — run `sdk env` before invoking Gradle directly.
- Formatting rules from `.editorconfig` (tabs for `.java`/`.xml`, size 4, 120 col limit)
  and point to `CONTRIBUTING.adoc`'s "Mind the Whitespace" section.
- How to run a single module's tests vs. the whole suite (whole-suite is slow — see §3).
- Where cross-module conventions live: `buildSrc/`, `gradle/`, root `build.gradle`.
- License header requirements (Apache 2.0) if CI enforces them.
- Link out to `CONTRIBUTING.adoc` and `RELEASE.adoc` rather than duplicating them.

## 2. Subdirectory `CLAUDE.md` files — local conventions only

Per the article, subdirectory files apply *additively* as Claude walks into them. Good
candidates here, added only if/when there's real local guidance to capture (don't
pre-populate empty ones):
- `oauth2/` — OAuth2/OIDC-specific conventions, spec references.
- `saml2/` — SAML-specific test fixtures/conventions.
- `config/` — DSL and `SecurityFilterChain` configuration conventions.
- `docs/` — Antora/Asciidoctor doc build notes if Claude will edit reference docs.

Rule of thumb: if the content is reusable expertise (e.g. "how to write a new
`AuthenticationProvider`"), it likely belongs in a **skill**, not a CLAUDE.md file.

## 3. Scope test/lint commands per module

Running `./gradlew build` or `./gradlew test` at the root is slow across ~40 modules
and can time out or produce noisy irrelevant output. Document (in root `CLAUDE.md` or a
skill) the scoped equivalents, e.g.:

```
./gradlew :spring-security-core:test
./gradlew :spring-security-oauth2-client:test --tests "*OidcSomething*"
```

This matches the article's advice to scope commands per subdirectory so Claude isn't
running the entire build for a one-module change.

## 4. `.claude/settings.json` — permissions

Create `.claude/settings.json` (checked into git, so the whole team shares it) with:
- `permissions.deny` rules for generated/build output: `build/`, `.gradle/`, `out/`,
  `**/bin/`, `.idea/`, `.vscode/` (already largely covered by `.gitignore` — mirror it).
- Any allowlist for safe, repeated commands (e.g. `./gradlew *:test`, `git status`,
  `git diff`) to reduce permission prompts — the `fewer-permission-prompts` skill can
  generate this from actual usage after a few sessions.
- Keep `.claude/settings.local.json` (gitignored) for personal overrides — don't put
  personal preferences in the shared `settings.json`.

## 5. LSP integration (Java)

The article stresses that symbol-level navigation is *not* automatic — without it,
Claude falls back to text/grep pattern-matching, which is risky in a codebase this size
with many overloaded/overridden methods (e.g. many `AuthenticationProvider`,
`AuthenticationFilter` implementations across modules). Set up a Java language server
integration so Claude can navigate by symbol (find usages, go-to-definition,
implementations) rather than guessing from string matches.

## 6. Hooks

Candidates for this repo:
- **Formatting hook** (post-edit or pre-commit): run the project's formatter/checkstyle
  equivalent automatically after Claude edits `.java` files, enforcing the
  `.editorconfig` rules deterministically instead of relying on prompted instructions.
- **Stop hook**: periodically prompt for CLAUDE.md updates when Claude discovers a new
  convention worth persisting (e.g. a module-specific test pattern).
- Use the `update-config` skill to wire these into `.claude/settings.json` rather than
  hand-editing.

## 7. Skills

Package reusable, on-demand expertise instead of bloating CLAUDE.md, e.g.:
- "Add a new `AuthenticationProvider`/`Filter`" scaffolding conventions.
- "Update Antora reference docs" (the `docs/` module's Asciidoctor conventions).
- Release process helpers referencing `RELEASE.adoc`.

## 8. MCP servers (optional, later)

Once the basics above work, consider MCP servers for:
- Spring Security's GitHub issue tracker (already reachable via `gh`, but an MCP server
  could expose structured search/filtering).
- Internal Broadcom/Spring docs or ticketing if applicable to your workflow.

Per the article: get the fundamentals (CLAUDE.md, permissions, LSP) working first —
don't start with MCP.

## 9. Subagents

Use subagents (this session already has `Explore`, `general-purpose`, `Plan`, etc.
available) for read-only exploration across the ~40 modules — e.g. "find all
implementations of `AuthenticationManager`" — keeping the main session's context
reserved for editing. Don't mix broad exploration and editing in the same context.

## 10. Governance & review cadence

- Assign a DRI (or small group) owning `.claude/settings.json`, root `CLAUDE.md`, and
  any shared skills/plugins for the repo, so configuration doesn't fragment across
  contributors.
- Re-review CLAUDE.md/hooks every 3–6 months — instructions tuned for the current model
  can become unnecessary overhead (or actively wrong) as models improve.
- Treat AI-authored changes to security-sensitive code (this *is* Spring Security) with
  the same or stricter PR review bar as human-authored changes — see `CONTRIBUTING.adoc`
  for existing review expectations.

## Immediate next steps (suggested order)

1. [ ] Write root `CLAUDE.md` (build/test commands, formatting, JDK version).
2. [ ] Add `.claude/settings.json` with permission excludes for build artifacts.
3. [ ] Set up Java LSP integration.
4. [ ] Document scoped per-module test commands.
5. [ ] Add subdirectory `CLAUDE.md`s only where real local convention exists
       (start with `oauth2/`, `saml2/`, `config/` if needed).
6. [ ] Add a formatting hook enforcing `.editorconfig`.
7. [ ] Revisit MCP/skills/plugins once the above is in daily use.
