---
name: git-workflow
description: Git and project-tracking conventions for the Senpai Supply backend - phase base branches with serial step branches, small conventional commits with module scopes, no attribution, what Claude may commit/push, verifying before push, and keeping docs/DEV_LOG.md and docs/ROADMAP.md current. Use at the start of every session and before any commit, branch, push or PR.
---

# Git workflow and dev log

The human-readable version is `docs/GIT_WORKFLOW.md`; keep the two in sync.

## Start of every session

Read `docs/DEV_LOG.md` ("Where we are", "Next up", "Open items") and `docs/ROADMAP.md`, then
`git status` and the current branch. Do not re-derive what the log already says.

## Branches

- `main` is the integration branch; never commit to it directly.
- Phase base branch `feat/<phase>-base`; each roadmap step is its own branch
  (`feat/`, `fix/`, `docs/`, `chore/`, `test/`, `refactor/`) taken from the previous step's
  branch, PR'd serially into the phase base.
- A cloud session pinned to a `claude/...` branch works and pushes there, and says so.

## Commits

- `type(scope): subject` - types `feat fix docs test refactor chore`; scopes are module names
  (`common user catalog cart order review api`). Imperative subject, <= 72 chars.
- Body explains why (finding, decision, trade-off) when not obvious.
- One coherent change per commit that builds and passes tests; regression test in the same
  commit as its fix. Split steps that touch more than ~10-15 files.
- **No `Co-Authored-By` and no Claude/AI attribution.** Never commit `.env`, keys, tokens, dumps.

## Before pushing

`./mvnw -B verify`. For auth, checkout, stock or migration changes also run
`scripts/smoke_test.py` against a fresh development database. Push with
`git push -u origin <branch>`; on network failure retry up to 4 times (2, 4, 8, 16 s).

## What Claude may do

Commit when asked; push only when asked; open PRs only when asked. Never force-push,
`reset --hard`, `clean -fd`, rewrite published history or delete branches without an explicit
instruction naming the action.

## Dev log and roadmap - same PR as the step

Tick the step in `docs/ROADMAP.md` (update the progress count) and add a dated entry, newest
first, to `docs/DEV_LOG.md`: **Done**, **Decisions**, **Breaking API changes**, **Known
limitations**; refresh "Where we are", "Next up", "Open items".
