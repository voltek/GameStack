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
message 1 'fixup! is not exempt' 'fixup! docs(ship): Fix a thing\n'
message 1 'squash! is not exempt' 'squash! docs(ship): Fix a thing\n'
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
message 0 'a whitespace-only line is blank' "docs(ship): Fix\n\n$(body 11)  \nCo-Authored-By: A <a@b>\nClaude-Session: x\n"
message 0 'a whitespace-only second line is blank' 'docs(ship): Fix\n  \nbody\n'

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

title() {
    expect "$1" "title: $2" sh "$rules" title "$3"
}
title 0 'one claim' 'chore(repo): Check commit messages mechanically'
title 1 'an "and"' 'chore(repo): Check messages and branches'
title 0 'Android is not "and"' 'feat(search): Support Android filters'

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

if [ "$failures" -gt 0 ]; then
    printf '%s case(s) failed.\n' "$failures"
    exit 1
fi
printf 'All rules.sh cases pass.\n'
