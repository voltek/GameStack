#!/bin/sh
# Every case is an input and the exit code rules.sh must return for it.
# Run by commits.yml before the checks, and by hand after editing rules.sh.

LC_ALL=C
export LC_ALL
unset GITHUB_ACTIONS
GIT_CONFIG_GLOBAL=/dev/null GIT_CONFIG_NOSYSTEM=1
export GIT_CONFIG_GLOBAL GIT_CONFIG_NOSYSTEM

rules=$(cd "$(dirname "$0")" && pwd)/rules.sh
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
failures=0

expect() {
    want=$1 label=$2
    shift 2
    "$@" >"$tmp/out" 2>&1
    got=$?
    if [ "$got" -ne "$want" ]; then
        printf 'FAIL %s: exit %s, expected %s\n' "$label" "$got" "$want"
        sed 's/^/    /' "$tmp/out"
        failures=$((failures + 1))
    fi
}

message() {
    printf '%b' "$3" >"$tmp/message"
    expect "$1" "message: $2" sh "$rules" message "$tmp/message"
}

pad() {
    printf '%*s' "$1" '' | tr ' ' x
}

body() {
    i=1
    while [ "$i" -le "$1" ]; do printf 'line %s\\n' "$i"; i=$((i + 1)); done
}

message 0 'plain' 'docs(ship): Fix a thing\n'
message 0 'no scope' 'docs: Fix a thing\n'
message 1 'unknown scope' 'docs(nope): Fix a thing\n'
message 1 'lowercase summary' 'docs(ship): fix a thing\n'
message 1 'trailing period' 'docs(ship): Fix a thing.\n'
message 1 'unknown type' 'wip: Fix a thing\n'
message 1 'no space after colon' 'docs(ship):Fix\n'
message 0 '72 code points, em dashes' "docs(ship): Fix — — — $(pad 50)\n"
message 1 '73 code points' "docs(ship): Fix — — — $(pad 51)\n"
message 1 'second line not blank' 'docs(ship): Fix\nmore\n'
message 0 'revert is exempt' 'Revert "docs(ship): fix a thing."\n'
message 0 'reapply is exempt' 'Reapply "docs(ship): fix a thing."\n'
message 0 'git squash message is exempt, body included' "Squashed commit of the following:\n\n$(body 13)"
message 1 'hand-written Merge subject' 'Merge the two caches\n'
printf "Merge branch 'main' into x\n" >"$tmp/message"
expect 0 'message: Merge subject on a merge commit' sh "$rules" message "$tmp/message" merge
printf 'wip\n' >"$tmp/message"
expect 1 'message: hand-written subject on a merge commit' sh "$rules" message "$tmp/message" merge
message 1 'fixup! fails outside the local hook' 'fixup! docs(ship): Fix a thing\n'
message 1 'squash! fails outside the local hook' 'squash! docs(ship): Fix a thing\n'
message 1 'amend! fails outside the local hook' 'amend! docs(ship): Fix a thing\n\ndocs(ship): Fix the thing\n'
message 0 '12 body lines and trailers' "docs(ship): Fix\n\n$(body 12)\nDrift: doc updated\nCo-Authored-By: A <a@b>\n"
message 1 '13 body lines' "docs(ship): Fix\n\n$(body 13)"
message 1 'a # line is body in a stored message' "docs(ship): Fix\n\n$(body 12)#55 introduced this\n"
message 1 'unknown keys in the last paragraph are body' "docs(ship): Fix\n\n$(body 11)\nNote: a\nCause: b\n"
message 0 'Drift: code fixed' 'docs(ship): Fix\n\nDrift: code fixed\n'
message 0 'Drift: both' 'docs(ship): Fix\n\nDrift: both\n'
message 1 'Drift: unknown value' 'docs(ship): Fix\n\nDrift: docs updated\n'
message 1 'drift: in lowercase' 'docs(ship): Fix\n\ndrift: doc updated\n'
message 0 'Bypassed: well-formed' 'docs(ship): Fix\n\nBypassed: enforce_admins — outage\n'
message 1 'Bypassed: no reason' 'docs(ship): Fix\n\nBypassed: because\n'
message 1 'an exempt subject keeps its trailers checked' 'Revert "docs(ship): X"\n\nDrift: docs updated\n'
message 0 'a whitespace-only line is blank' "docs(ship): Fix\n\n$(body 11)  \nCo-Authored-By: A <a@b>\nSigned-off-by: A <a@b>\n"
message 0 'a whitespace-only second line is blank' 'docs(ship): Fix\n  \nbody\n'
message 1 "a plan's branch label" 'docs(ship): Fix\n\nOn branch 3b the rule moved.\n'
message 1 'a numbered branch label' 'docs(ship): Fix\n\nLands before Branch 4.\n'
message 0 'a branch named, not numbered' 'docs(ship): Fix\n\nBranch protection holds.\nOne branch per claim, 2 commits.\n'
message 1 'a cycle in the subject' 'docs(ship): Close cycle 4\n'
message 1 'a cycle item' 'docs(ship): Fix\n\nCycle 4 item 7 takes it.\n'
message 0 'a lifecycle is no cycle' 'docs(ship): Fix\n\nThe lifecycle 2 hooks stay.\n'
message 1 'a plan file' 'docs(ship): Fix\n\nAs plans/harness-audit.md says.\n'
message 1 'a plan file in inline code' 'docs(ship): Fix\n\nAs `plans/x.md` says.\n'
message 0 'a plans directory in the repository' 'docs(ship): Fix\n\nSee docs/plans/x.md and the PR plans.\n'
message 1 'the handoff directory' 'docs(ship): Fix\n\nKept in ~/Documents/GameStack.\n'
message 1 'the handoff directory through HOME' 'docs(ship): Fix\n\nKept in $HOME/Documents/x.\n'
message 0 'user settings are on every machine' 'docs(ship): Fix\n\nRead from ~/.claude/settings.json.\n'
message 1 'the handoff directory with no slash' 'docs(ship): Fix\n\nKept in ~/Documents.\n'
message 1 'a Windows home in full' 'docs(ship): Fix\n\nKept in C:/Users/x/a.md.\n'
cat >"$tmp/message" <<'MESSAGE'
docs(ship): Fix

Kept in C:\Users\x\a.md.
MESSAGE
expect 1 'message: a Windows home with backslashes' sh "$rules" message "$tmp/message"
cat >"$tmp/message" <<'MESSAGE'
docs(ship): Fix

Kept in %USERPROFILE%\Documents\x.
MESSAGE
expect 1 'message: a Windows home through USERPROFILE' sh "$rules" message "$tmp/message"
message 1 'a Windows home from Git Bash' 'docs(ship): Fix\n\nKept in /c/Users/x/a.md.\n'
message 0 "a feature's home folder" 'docs(ship): Fix\n\nSee app/feature/home/x.kt and https://e.com/home/x.\n'
message 0 'a route under /home' 'docs(home): Fix\n\nNavigates to `/home/{id}`.\n'
message 0 'a deep link under home' 'docs(home): Fix\n\nOpens gamestack://home/detail.\n'
message 1 'a triage id in a message' 'docs(ship): Fix\n\nAnswers r1-4.\n'
message 0 'a triage id in inline code in a message' 'docs(ship): Fix\n\nPrints `r1-4`.\n'
message 0 'an exempt subject quotes what it reverts' 'Revert "docs(ship): Close cycle 4"\n'
message 1 'the text under a Revert is read' 'Revert "docs(ship): X"\n\nRedone in ~/Documents/GameStack.\n'
message 1 'a triage id under a Reapply is read' 'Reapply "docs(ship): X"\n\nAnswers r1-4.\n'
message 1 'an amend! replacement is read' 'amend! docs(ship): Fix\n\ndocs(ship): Fix\n\nOn branch 3b.\n'

printf 'docs(ship): Fix\n\nDrift: bogus\nDrift: also bogus\n' >"$tmp/message"
reported=$(sh "$rules" message "$tmp/message" 2>&1 | grep -c '^A Drift: trailer')
if [ "$reported" -ne 2 ]; then
    printf 'FAIL message: every bad Drift: line reported: saw %s of 2\n' "$reported"
    failures=$((failures + 1))
fi

# The local hook sees the message before git strips comments, inside a repo.
# GIT_EDITOR is ':' when git opens no editor and keeps every comment line.
repo=$tmp/repo
git init -q "$repo"
commit() {
    git -C "$repo" -c user.name=t -c user.email=t@t commit -q --allow-empty "$@"
}
commit -m init
hook() {
    printf '%b' "$3" >"$tmp/message"
    expect "$1" "commit-msg: $2" sh -c 'cd "$1" && GIT_EDITOR=$4 sh "$2" commit-msg "$3"' \
        sh "$repo" "$rules" "$tmp/message" "${4:-vi}"
}
hook 0 'comment template stripped' 'docs(ship): Fix\n\n# Please enter the commit message\n#\n# On branch x\n'
hook 0 'scissors cut' "docs(ship): Fix\n\n# ------------------------ >8 ------------------------\n$(body 13)"
hook 1 'a # line inside the body counts under -F' "docs(ship): Fix\n\n$(body 6)#55 introduced this\n$(body 6)" :
hook 1 'a trailing # line counts under -F' "docs(ship): Fix\n\n$(body 12)#55 caused it\n" :
hook 1 'Merge subject outside a merge' "Merge branch 'b'\n" :
hook 0 'fixup! passes the local hook' 'fixup! docs(ship): Fix a thing\n' :
hook 0 'squash! passes the local hook' 'squash! docs(ship): Fix a thing\n' :
hook 0 'amend! passes the local hook' 'amend! docs(ship): Fix a thing\n\ndocs(ship): Fix the thing\n' :
hook 1 'an amend! checks its replacement message' 'amend! docs(ship): Fix a thing\n\nwip fix stuff.\n' :
hook 1 'a fold marker needs its space' 'fixup!docs(ship): Fix a thing\n' :
git -C "$repo" rev-parse HEAD >"$repo/.git/MERGE_HEAD"
hook 0 'Merge subject during a merge' "Merge branch 'b'\n" :
rm "$repo/.git/MERGE_HEAD"
git -C "$repo" switch -q -c b && commit -m b && git -C "$repo" switch -q -
commit -m main && git -C "$repo" -c user.name=t -c user.email=t@t merge -q --no-edit b
hook 0 'amending a merge keeps its subject' "$(git -C "$repo" log -1 --format=%s)\n" :
hook 1 'a new Merge subject on top of a merge' "Merge the two caches\n" :
git -C "$repo" config core.commentChar ';'
hook 0 'core.commentChar honoured' 'docs(ship): Fix\n\n; a comment\n'
git -C "$repo" config core.commentChar auto
hook 0 'core.commentChar=auto strips # lines' "docs(ship): Fix\n\n$(body 13 | sed 's/line/#/g')"
hook 1 'core.commentChar=auto guesses nothing from the last line' "docs(ship): Fix\n\n$(body 12)@mention\n"
git -C "$repo" config core.commentChar '>'
hook 0 'an explicit character outside the auto set' "docs(ship): Fix\n\n$(body 13 | sed 's/line/>/g')"
git -C "$repo" config --unset core.commentChar
git -C "$repo" config core.commentString ';;'
hook 0 'core.commentString honoured' "docs(ship): Fix\n\n$(body 13 | sed 's/line/;;/g')"

# A branch off main in its own repository; each case adds commits, then checks
# main..HEAD in rules.sh and through pre-push against a remote-tracking main.
push=$tmp/push
git init -q "$push"
on_branch() {
    git -C "$push" -c user.name=t -c user.email=t@t commit -q --allow-empty -m "$1"
}
reset_branch() {
    git -C "$push" switch -q -C x main
}
on_branch 'docs(ship): Base' && git -C "$push" branch -M main && git -C "$push" update-ref refs/remotes/origin/main main
pre_push() {
    printf 'refs/heads/x %s refs/heads/docs/x 0000000000000000000000000000000000000000\n' "$(git -C "$push" rev-parse x)" >"$tmp/push-input"
    expect "$1" "pre-push: $2" sh -c 'cd "$1" && sh "$2" origin url <"$3"' \
        sh "$push" "$(dirname "$rules")/pre-push" "$tmp/push-input"
}
repeats() {
    expect "$1" "repeats: $2" sh -c 'cd "$1" && sh "$2" repeats main x' sh "$push" "$rules"
    pre_push "$@"
}
reset_branch
repeats 0 'an empty branch'
on_branch 'docs(ship): One' && on_branch 'docs(ship): Two'
repeats 0 'every subject its own'
on_branch 'docs(ship): One'
repeats 1 'a subject twice'
reset_branch && on_branch 'docs(ship): Base'
repeats 0 'a subject only main already holds'
reset_branch && on_branch 'fixup! docs(ship): One' && on_branch 'fixup! docs(ship): One'
repeats 0 'two markers, left to the form check'
reset_branch
for n in 1 2; do
    git -C "$push" switch -q main && on_branch "docs(ship): Main $n" && git -C "$push" switch -q x
    git -C "$push" -c user.name=t -c user.email=t@t merge -q --no-ff --no-edit -m "Merge branch 'main' into x" main
    on_branch "docs(ship): After $n"
done
git -C "$push" update-ref refs/remotes/origin/main main
repeats 0 'two merges of main with one subject'
expect 1 'repeats: a base that does not exist' sh -c 'cd "$1" && sh "$2" repeats nope x' sh "$push" "$rules"

# With no origin/main, pre-push measures from the local main, and with neither it warns.
reset_branch && on_branch 'docs(ship): One' && on_branch 'docs(ship): One'
git -C "$push" update-ref -d refs/remotes/origin/main
pre_push 1 'a subject twice, measured from the local main'
git -C "$push" branch -q -m main trunk
pre_push 0 'no main at all: the push goes on'
if ! grep -q 'unchecked until CI' "$tmp/out"; then
    printf 'FAIL pre-push: no main at all: the warning is missing\n'
    failures=$((failures + 1))
fi

title() {
    expect "$1" "title: $2" sh "$rules" title "$3"
}
title 0 'one claim' 'chore(repo): Check commit messages mechanically'
title 0 'an "and" is a judgement, not a check' 'chore(repo): Check messages and branches'
title 1 'a cycle in the title' 'docs(ship): Close cycle 4'
title 1 'a triage id in the title' 'fix(ship): Answer r1-4'
title 0 'a triage id in inline code in the title' 'fix(ship): Print `r1-4`'

branch() {
    label=$1 want=$2
    shift 2
    expect "$want" "branch: $label" sh "$rules" branch "$@"
}
branch 'kebab' 0 chore/commit-msg-hook
branch 'feat/ is not a prefix' 1 feat/x
branch 'not kebab' 1 chore/Bad_Name
branch 'double dash' 1 chore/a--b
branch 'revert- lookalike' 1 revert-1x-Foo_Bar
branch 'retitled revert PR' 0 revert-55-fix/x 'fix(search): Restore x'
branch 'feature/ matches feat' 0 feature/x 'feat(home): Add x'
branch 'prefix against title type' 1 chore/x 'docs: Add x'
branch 'Revert-button PR' 0 revert-55-docs/x 'Revert "docs(ship): Fix x"'
branch 'Revert-button branch on push' 0 revert-55-docs/x
branch 'revert title on a typed branch' 0 fix/foo 'Revert "fix(search): Foo"'
branch 'malformed title left to the title check' 0 chore/x 'wip'

# Each case below changes one thing in this body.
good_body() {
    cat <<'BODY'
## What changed
<!-- A comment.
Over two lines. -->
The one claim.

## Why
The reason.

## Verification
- [x] Integrity detectors, found: none

Evidence: none

## Review
2 rounds. Fixed after the last:
- a test case added
BODY
}

pr_body() {
    want=$1 label=$2
    shift 2
    good_body | sed "$@" >"$tmp/body"
    expect "$want" "body: $label" sh "$rules" body "$tmp/body" 'chore(ship): Check a thing'
}

pr_text() {
    printf '%b' "$3" >"$tmp/body"
    expect "$1" "body: $2" sh "$rules" body "$tmp/body" "$4"
}

long=$(pad 600)
pr_body 0 'the shape' -e ''
pr_body 0 'CRLF from the web editor' -e 's/$/\r/'
pr_body 0 'a fence closed under CRLF' -e '/^The reason/a ```\n## Quoted\n```' -e 's/$/\r/'
pr_body 0 'optional sections' -e 's/^## Verification/## Issues\nCloses #1, closes #2\nRelated: #3, #4, PR #5\n\n&/' -e '$a ### Screenshots'
pr_body 0 'a heading in a comment' -e '1i <!-- ## Hidden -->'
pr_body 0 'a heading in a two-line comment' -e '/^Over two lines/i ## Hidden'
pr_body 0 'a heading in a fence' -e '/^The reason/a ```\n## Quoted\n```'
pr_body 0 'a heading in a tilde fence' -e '/^The reason/a ~~~\n## Quoted\n~~~'
pr_body 0 'a fence closes only on its own marker' -e '/^The reason/a ```\n~~~\n## Quoted\n```'
pr_body 0 'a marker in inline code' -e 's/^The reason\./Writes the `<!--` marker./'
pr_body 0 'a longer fence quoting a shorter one' -e '/^The reason/a ````\n```\n## Quoted\n```\n````'
pr_body 0 'inline triple backticks are no fence' -e 's/^The reason\./```x``` is inline./'
pr_body 1 'a heading after a closed comment' -e 's/^The reason\./<!-- x -->\n## Extra/'
pr_body 1 'text before the first heading' -e '1i Text above the claim.\n'
pr_body 0 'a comment before the first heading' -e '1i <!-- A note. -->\n'
pr_body 0 'an exempt branch' -e 's/^2 rounds.*/Not run — exempt: comments only/' -e '/^- a test/d'
pr_body 1 'an exempt line with no reason' -e 's/^2 rounds.*/Not run — exempt: /' -e '/^- a test/d'
pr_body 1 'a TODO for the exemption' -e 's/^2 rounds.*/Not run — exempt: TODO/' -e '/^- a test/d'
pr_body 1 'the comment sample for the exemption' -e 's/^2 rounds.*/Not run — exempt: <reason>/' -e '/^- a test/d'
pr_body 0 'an exemption naming a TODO' -e 's/^2 rounds.*/Not run — exempt: removes a TODO comment only/' -e '/^- a test/d'
pr_body 1 'a heading out of order' -e 's/^## Why/## Tmp/' -e 's/^## What changed/## Why/' -e 's/^## Tmp/## What changed/'
pr_body 1 'a heading of its own' -e 's/^## Why/## Notes\n\n&/'
pr_body 1 'a missing section' -e 's/^## Why/The/'
pr_body 1 'a heading at another level' -e 's/^## Why/# Why/'
pr_body 1 'Issues after Verification' -e '$a ## Issues'
pr_body 1 'an empty section' -e '/^The reason/d'
pr_body 0 '600 characters, em dashes' -e "s/^The reason\./——${long#xx}/"
pr_body 1 '601 characters' -e "s/^The reason\./x$long/"
pr_body 1 'What changed past the bound' -e "s/^The one claim\./x$long/"
pr_body 1 'Verification past the bound' -e "s/^- \[x\].*/x$long/"
pr_body 0 'a comment leaves the count' -e "s/^The reason\./short <!-- x$long -->/"
pr_body 0 'Review has no bound' -e "s/^- a test case added/- x$long/"
pr_body 1 'no Evidence:' -e '/^Evidence:/d'
pr_body 1 'two Evidence: lines' -e 's/^Evidence: none/&\nEvidence: x/'
pr_body 1 'no Review' -e '/^## Review/,$d'
pr_body 1 'an empty Review' -e '/^2 rounds/,$d'
pr_body 0 'one round' -e 's/^2 rounds/1 round/'
pr_body 0 'twelve rounds' -e 's/^2 rounds/12 rounds/'
pr_body 1 'one rounds' -e 's/^2 rounds/1 rounds/'
pr_body 1 'three round, nothing fixed' -e 's/^2 rounds.*/3 round. Fixed after the last: none/' -e '/^- a test/d'
pr_body 1 'no number of rounds' -e 's/^2 rounds/Two rounds/'
pr_body 1 'the old summary line' -e 's/^2 rounds\. Fixed after the last:/<details><summary>Review: 2 rounds<\/summary>/'
pr_body 0 'nothing fixed after the last round' -e 's/last:$/last: none/' -e '/^- a test/d'
pr_body 1 'no list after the colon' -e '/^- a test/d'
pr_body 0 'the lines after the first are not read' -e 's/^- a test case added/  see the commits/'
pr_body 1 'a TODO for the rounds' -e 's/^2 rounds/TODO rounds/'
pr_body 1 'the template item left' -e 's/^- a test case added/- TODO what changed/'
pr_body 1 'the template item numbered' -e 's/^- a test case added/1. TODO what changed/'
pr_body 1 'the template item left after none' -e 's/last:$/last: none/' -e 's/^- a test case added/- TODO what changed/'
pr_body 1 'the template item left after an exemption' -e 's/^2 rounds.*/Not run — exempt: comments only/' -e 's/^- a test case added/- TODO what changed/'
pr_body 0 'a TODO named in an item' -e 's/^- a test case added/- removed a stale TODO/'
pr_body 0 'a fix that starts with TODO' -e 's/^- a test case added/- TODO comments removed from the hook/'
pr_body 1 'a triage id in the list' -e 's/^- a test case added/- r2-4, a test case added/'
pr_body 1 'a split triage id in Why' -e 's/^The reason\./Fixes r1-2a./'
pr_body 0 'a triage id in inline code' -e 's/^The reason\./Prints `r1-2`./'
pr_body 0 'a range after a word ending in r' -e 's/^The reason\./Pages per2-3./'
pr_body 0 'an id-like name in a URL' -e '$a ### Screenshots\n![home](https://example.com/home-r1-2.png)\n![detail](https://example.com/r2-3/detail.png)'
pr_body 1 'a triage id after a comma' -e 's/^The reason\./Fixed,r1-2 too./'
pr_body 1 'a triage id in parentheses' -e 's/^The reason\./Fixed (r1-2)./'
pr_body 1 "a plan's branch label" -e 's/^The reason\./On branch 3b./'
pr_body 1 'a plan file in inline code' -e 's/^The reason\./As `plans\/x.md` says./'
pr_body 1 'the handoff directory in a fence' -e '/^The reason/a ```\n~/Documents/GameStack/HANDOFF.md\n```'
pr_body 1 'a cycle in the review list' -e 's/^- a test case added/- cycle 4 wording/'
pr_body 0 'a cycle in a comment' -e '1i <!-- cycle 4 -->'
pr_body 1 'a reference after a comment marker in inline code' -e 's/^The reason\./Writes the `<!--` marker.\nOn branch 3b./'
pr_body 1 'a reference after a comment marker in a fence' -e '/^The reason/a ```html\n<!--\n```\nOn branch 3b.'
pr_body 1 'a reference in a fenced comment' -e '/^The reason/a ```html\n<!-- cycle 4 -->\n```'
pr_body 0 'a plans path in a URL' -e '$a ### Screenshots\n![x](https://example.com/plans/x.png)'
pr_body 1 'a box with no value' -e 's/^- \[x\] .*/- [ ] OpenSpec change archived at:/'
pr_body 1 'an empty Evidence:' -e 's/^Evidence: none/Evidence:/'
pr_body 1 'an empty Issues' -e 's/^## Verification/## Issues\n\n&/'
pr_body 1 'the template number left in Issues' -e 's/^## Verification/## Issues\nRelated: #n\n\n&/'
pr_text 1 'an empty body' '' 'chore(ship): Check a thing'
pr_text 0 'the Revert button' 'Reverts voltek/GameStack#1\n' 'Revert "docs(ship): Fix x"'
pr_text 1 'text under the Revert button line is read' 'Reverts voltek/GameStack#1\n\nRedo on branch 3b.\n' 'Revert "docs(ship): Fix x"'
pr_text 1 'a triage id under the Revert button line' 'Reverts voltek/GameStack#1\n\nAnswers r1-4.\n' 'Revert "docs(ship): Fix x"'
pr_text 0 'a reverted revert' 'Reverts voltek/GameStack#2\n' 'Reapply "docs(ship): Fix x"'
pr_text 1 'a typed Revert title' 'nothing like the template\n' 'Revert "anything at all"'

template=$(dirname "$rules")/../.github/pull_request_template.md
expect 1 'body: the template as it ships' sh "$rules" body "$template" 'chore(ship): Check a thing'
sed -E -e 's/TODO/2/g' -e '/^## What changed/a The claim.' -e '/^## Why/a The reason.' \
    -e 's/#n, closes #n/#1, closes #2/' -e 's/^Related: .*/Related: #3/' -e 's/^(- \[ \] .*:)$/\1 x/' \
    "$template" >"$tmp/body"
expect 0 'body: the template once filled' sh "$rules" body "$tmp/body" 'chore(ship): Check a thing'

if [ "$failures" -gt 0 ]; then
    printf '%s case(s) failed.\n' "$failures"
    exit 1
fi
printf 'All rules.sh cases pass.\n'
