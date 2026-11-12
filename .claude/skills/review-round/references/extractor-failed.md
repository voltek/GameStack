# When `save-review.mjs` fails

Read this when the extractor of § The sequence, step 1, exits 1. Its notice says
what it expected, the shape it found, never the content, and the Claude Code
version. The transcript format is undocumented, so a new version can change it.
A notice of 0 matches asks first whether the task id was copied right; only a
right id that still fails, or a shape the script refuses, is a format change.

1. Save the record from the notification's `<result>` instead, whole and before any
   digest, into `reviews/<branch>/` beside the handoff, the branch exactly as
   `git branch --show-current` prints it, with this first line: `copied from the notification, HTML-escaped`. The
   notification escapes `<` and `>` and can neutralize text that looks like an
   instruction, so the line tells every later reader the record is not literal.
   Read `&lt;` and `&gt;` as `<` and `>` when verifying a finding against it.
2. Write the table's `Reviewer:` line from what the round was launched with: the
   session's model and the level passed to `/code-review`.
3. Leave the script alone on this branch. A repair is a change that is not this
   branch's unit of work, and a parser fixed without a fixture can save a wrong
   record in silence. The repair goes on a branch of its own, guided by the notice.
4. For a format change only, add a row under *To decide*: open an issue with the
   notice, titled `chore(ship): Repair save-review for Claude Code <version>`,
   unless `gh issue list --search` finds one open with that title. Open it only on
   the human's yes, like a Defer's issue: nothing goes to GitHub before the message.
