# A branch working `review-debt`

Read when a branch's whole unit of work is findings from an issue that settles an
earlier branch's review leftovers (ADR 0038), none of them deferred because its fix
needs a plan. Any other issue, labelled or not, takes the ordinary path.

The branch that deferred those findings already spent its rounds, so this one is
routed by its weakest fix:
- **Every fix is cheap and passes a test of *A fix no round will read*, and the
  human confirms that:** no `/code-review`, only the gate, the detectors and, on a
  branch that carries an OpenSpec change, `openspec-verify-change` (`review-round`
  § Which branches get a round). The PR body's *Review* says `Not run — exempt:
  review-debt, every fix cheap and tested`.
- **Otherwise:** the whole diff gets a cap of one round, and the branch opens no new
  debt. A finding that is not a real defect is fixed if it passes a test of *A fix
  no round will read*, and otherwise proposed as Accept, which the human
  confirms. A real defect the human defers still opens its issue.
