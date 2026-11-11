#!/bin/sh
# Usage: reword.sh <commit> <message file>. Makes the amend! commit that rewords
# <commit> on the next autosquash (clean-branch § Folding). git refuses -F and -m
# with --fixup=reword, so the editor keeps git's own marker line and appends the file.
# Test: test-reword.sh beside it.

fail() {
    printf 'reword.sh: %s\n' "$1" >&2
    exit 1
}

[ $# -eq 2 ] || fail 'usage: reword.sh <commit> <message file>'
target=$(git rev-parse -q --verify "$1^{commit}") || fail "no commit named '$1'"
# --autosquash folds only into a commit it replays, so a target off the branch
# would leave the amend! standing.
git merge-base --is-ancestor "$target" HEAD || fail "$1 is not on this branch"
main=$(git rev-parse -q --verify refs/remotes/origin/main) && git merge-base --is-ancestor "$target" "$main" &&
    fail "$1 is already on origin/main, which a fold does not rewrite"
[ -f "$2" ] || fail "no message file at '$2'"
[ -s "$2" ] || fail "the message file '$2' is empty"
case $(sed -n 1p "$2") in
    'amend! '* | 'fixup! '* | 'squash! '*)
        fail 'the file holds the new message only: the script writes the amend! line itself' ;;
esac
# The commit-msg hook strips # lines under any editor, and --cleanup=whitespace
# below keeps them, so the message is checked here as git will store it.
rules=$(dirname "$0")/../../../../.githooks/rules.sh
if [ -f "$rules" ]; then
    sh "$rules" message "$2" || fail 'the message breaks the rules above; nothing was made'
else
    printf 'reword.sh: no rules.sh at %s, so the message goes unchecked until CI\n' "$rules" >&2
fi

# git runs the editor from the top of the worktree, so the path must not be relative.
# --cleanup=whitespace keeps a body line that opens with #, such as an issue number.
REWORD_MESSAGE=$(CDPATH= cd -- "$(dirname "$2")" && pwd)/$(basename "$2")
export REWORD_MESSAGE
before=$(git rev-parse -q --verify HEAD)
GIT_EDITOR='f() { m=$(sed -n 1p "$1") && { printf "%s\n\n" "$m"; cat "$REWORD_MESSAGE"; } >"$1"; }; f' \
    git commit -q --allow-empty --cleanup=whitespace --fixup=reword:"$target" || fail 'git commit failed; nothing was made'

made=$(git log -1 --format=%s)
[ "$(git rev-parse HEAD)" != "$before" ] && [ "$made" = "amend! $(git log -1 --format=%s "$target")" ] ||
    fail "HEAD is '$made', not the amend! of $1: read git log, and undo a wrong commit with git reset --soft HEAD~1"
printf 'Made %s; fold it with clean-branch § Folding.\n' "$(git log -1 --format='%h %s')"
