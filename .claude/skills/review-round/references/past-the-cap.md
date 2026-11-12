# A round past the cap, and the one redesign

Read when the stopping rule's cap denies a round you think is needed, or when the
rule of three calls for a redesign.

## A round past the cap
The human may always ask for another round. The agent may propose one, as `stop,
back to the human`, only for a failure it can state to a `Fix (defect)`'s standard:
the concrete scenario, quoting the text, in which the product breaks or an agent
acts wrongly. The gate and the detectors must be unable to check it, and what the
rounds did must make it likely. Wording, friction and a reviewer that might find
more are no reason. The saved table records the decision.

## Where a redesign runs
A redesign runs in the session that holds the failed attempts, since a subagent's
brief would carry only a summary of them (ADR 0077). When an earlier touch was made
in another branch, read it from that branch's history, and ask whether this branch
needs the defect gone. If it does, the redesign is this branch's one redesign; if
not, open an issue for it, and leave that code as `main` has it.

## One redesign per branch
A redesign restarts the round count, under the same cap, for what it rewrote only;
a fix outside it follows *A fix no round will read*. If the rule of three fires
again on it, there is no second redesign: that part leaves the branch for a plan,
and the rest goes on.
