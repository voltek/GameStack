# Archive the OpenSpec change before the merge

Read when the branch carries `openspec/changes/<branch-kebab-name>/`, after the bot's
round on the PR or once the bot is unavailable, and before asking to merge. A
merged branch leaves no open change on `main`.

1. Run `openspec-verify-change` again, then `openspec validate <change> --strict`.
2. Archive with `openspec-archive-change`.
3. No review reads the merge, so check it: `openspec validate --specs --strict`
   passes, and every requirement and scenario the delta held is in the merged spec.
4. A folder the archive created under `openspec/specs/` fills its capability's
   *Spec* cell in `docs/project/PRODUCT.md` and removes that capability's section
   (ADR 0043).
5. Commit the merged specs and `PRODUCT.md` with the archive, and push.
