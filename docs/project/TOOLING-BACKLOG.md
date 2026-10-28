# Tooling backlog — where each item went

Tooling not yet built is a GitHub issue opened through the issue form, like any
other pending work
([ADR 0038](decisions/0038-pending-work-is-an-issue-opened-through-one-form.md)).
This file keeps only the table below, because frozen ADRs and DRIFT-LOG rows
cite its items by number. Nothing is added to it: a new item is a new issue.

## Where each item went
| Item | Now |
|---|---|
| 1. Automate the regression-test verification | #64 |
| 2. Run the affected test suite after each agent edit | #65 |
| 3. Automated drift checker | #66 |
| 4. Split the Robolectric screen tests into their own Gradle task | #67 |
| 5. A `commit-msg` hook for the mechanical half of the message rules | Shipped: TOOLING.md § 11 |
| 6. Make the project-coupled Skills exportable | #68 |
| 7. Check the ADR index against `decisions/` | #69 |
| 8. Fix the gate's Gradle-cache packaging failure | #70 |
| 9. A `PreToolUse` hook that judges git and gh commands by parsing them | #71 |
| 10. Check CLAUDE.md's size bound in the build | #72 |
| 11. Block edits to the frozen drift log | #73 |
