# Git workflow: how we branch, commit and push

Ported from delivery_app and AgroTrends so all three repos work the same way. The same rules
are in `.claude/skills/git-workflow/SKILL.md` so an AI assistant follows them too.

## Start of every session

1. Read `docs/DEV_LOG.md` (where we are, next up, open items) and `docs/ROADMAP.md`.
2. `git status`, `git branch --show-current`, `git fetch origin`.

## Branches

- `main` is the integration branch. **Never commit straight to `main`.**
- Each roadmap **phase** has a base branch: `feat/<phase>-base` (e.g. `feat/frontend-base`).
- Each roadmap **step** is its own branch, taken from the **previous step's branch** (not from
  `main`), so stacked PRs never conflict. Names: `feat/<step>`, `fix/<step>`, `docs/<topic>`,
  `chore/<topic>`, `test/<topic>`, `refactor/<topic>`.
- Step branches open PRs **serially into the phase base**; the base PRs into `main` when the
  phase is done.

```bash
git checkout feat/frontend-base
```

```bash
git checkout -b feat/product-page
```

## Commits

- Conventional prefix with an optional scope:
  `feat(order): ...`, `fix(cart): ...`, `docs: ...`, `test(auth): ...`, `refactor: ...`, `chore: ...`.
  Scopes are module names: `common`, `user`, `catalog`, `cart`, `order`, `review`, `api`.
- Subject: imperative, at most 72 characters, says **what changed for a reader**.
- Body (when the why is not obvious): the finding, decision or trade-off. Wrap at 72.
- **One coherent change per commit that builds and passes tests.** A regression test ships in
  the same commit as its fix.
- **Small PRs.** If a step would touch more than ~10 to 15 files, split the step first.
- Author identity only. **No `Co-Authored-By` and no AI attribution** in commits or PRs.
- Never commit `.env`, keys, tokens, dumps or binaries.

Example:

```text
fix(cart): stop catalog lookups from rolling back the cart transaction

Cart merge skips unavailable lines by catching the catalog's "unavailable"
exception, but the exception had already crossed the catalog's
@Transactional proxy and marked the shared transaction rollback-only, so
the commit failed with a 500. price() now declares noRollbackFor.
```

## Before every push

```bash
./mvnw -B verify
```

For a change to auth, checkout, stock or migrations, also run the end-to-end script against
a fresh development database (see `scripts/smoke_test.py` header):

```bash
python3 scripts/smoke_test.py app.log adminpw.txt
```

Then push the branch:

```bash
git push -u origin feat/product-page
```

If the push fails on a network error, retry up to 4 times, waiting 2, 4, 8, then 16 seconds.

## Pull requests

- Title in the same conventional form as the commit subject.
- Body: what changed, why, how it was tested, and any **Breaking API changes**.
- One PR per step, into the phase base branch.

## Dev log and roadmap: in the same PR as the step

When a step finishes:
1. Tick it in `docs/ROADMAP.md` and update the progress count.
2. Add a dated entry (newest first) to `docs/DEV_LOG.md`: **Done**, **Decisions**,
   **Breaking API changes** (if any), **Known limitations**.
3. Refresh "Where we are", "Next up" and "Open items".

A tiny `docs: tick roadmap x.y` commit on the step branch is the norm.

## What an AI assistant may do

- Commit when asked; push only when asked; open PRs only when asked.
- Never force-push, `reset --hard`, `clean -fd`, rewrite published history or delete branches
  without an explicit instruction naming the action. Purging leaked secrets from history is
  the owner's call; rotating them comes first.
- When a session is pinned to a specific branch by its environment (for example a
  `claude/...` branch in a cloud session), work and push there, and say so in the PR.
