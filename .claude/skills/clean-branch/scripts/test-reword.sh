#!/bin/sh
# Each case runs reword.sh in a scratch repository and checks what it made, and
# that the fold gives the commit the file's message. Run: sh test-reword.sh (also on CI, commits.yml).

LC_ALL=C
export LC_ALL
GIT_CONFIG_GLOBAL=/dev/null GIT_CONFIG_NOSYSTEM=1
GIT_AUTHOR_NAME=t GIT_AUTHOR_EMAIL=t@t GIT_COMMITTER_NAME=t GIT_COMMITTER_EMAIL=t@t
export GIT_CONFIG_GLOBAL GIT_CONFIG_NOSYSTEM GIT_AUTHOR_NAME GIT_AUTHOR_EMAIL GIT_COMMITTER_NAME GIT_COMMITTER_EMAIL

script=$(CDPATH= cd -- "$(dirname "$0")" && pwd)/reword.sh
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
failures=0

check() {
    if ! eval "$2"; then
        printf 'FAIL %s: %s\n' "$1" "$2"
        sed 's/^/    /' "$tmp/out"
        failures=$((failures + 1))
    fi
}

# A fresh repository: base, which origin/main holds, then three commits a branch would hold.
setup() {
    rm -rf "$tmp/repo"
    git init -q "$tmp/repo"
    cd "$tmp/repo" || exit 1
    git commit -q --allow-empty -m 'chore: Base'
    git tag base
    git update-ref refs/remotes/origin/main base
    for n in One Two Three; do
        mkdir -p sub && printf '%s\n' "$n" >"sub/$n"
        git add sub && git commit -q -m "docs(ship): Add $n" -m "Old body of $n."
    done
}

# The command clean-branch § Folding gives, so the cases fold as an agent does.
fold() {
    git rebase -q --autosquash --rebase-merges "$(git merge-base origin/main HEAD)" >>"$tmp/out" 2>&1
}

new_message='docs(ship): Add Two, reworded

#55 opened this line, which a strip cleanup would drop.

Co-Authored-By: A <a@b>'

setup
printf '%s\n' "$new_message" >"$tmp/message"
tree=$(git rev-parse HEAD^{tree})
sh "$script" HEAD~1 "$tmp/message" >"$tmp/out" 2>&1
check 'a commit below the tip: it makes the amend!' '[ "$(git log -1 --format=%s)" = "amend! docs(ship): Add Two" ]'
check 'a commit below the tip: the amend! carries the file' '[ "$(git log -1 --format=%B | sed 1,2d)" = "$new_message" ]'
fold
check 'a commit below the tip: the fold rewords it' '[ "$(git log -1 --format=%B HEAD~1)" = "$new_message" ]'
check 'a commit below the tip: the fold keeps the count' '[ "$(git rev-list --count base..HEAD)" = 3 ]'
check 'a commit below the tip: the fold keeps the tree' '[ "$(git rev-parse HEAD^{tree})" = "$tree" ]'

setup
printf 'docs(ship): Add Three, reworded\n' >"$tmp/message"
sh "$script" HEAD "$tmp/message" >"$tmp/out" 2>&1 && fold
check 'the tip: the fold rewords it' '[ "$(git log -1 --format=%s)" = "docs(ship): Add Three, reworded" ]'

setup
printf 'docs(ship): Add One, reworded\n' >"$tmp/message"
(cd sub && sh "$script" HEAD~2 ../../message) >"$tmp/out" 2>&1 && fold
check 'a relative path from a subdirectory' '[ "$(git log -1 --format=%s HEAD~2)" = "docs(ship): Add One, reworded" ]'

refused() {
    label=$1
    shift
    setup
    head=$(git rev-parse HEAD)
    "$@" >"$tmp/out" 2>&1; status=$?
    check "$label: exits 1" '[ $status -eq 1 ]'
    check "$label: makes nothing" '[ "$(git rev-parse HEAD)" = "$head" ]'
}
printf 'amend! docs(ship): Add Two\n\ndocs(ship): Add Two, reworded\n' >"$tmp/marked"
refused 'a file that brings its own marker' sh "$script" HEAD~1 "$tmp/marked"
: >"$tmp/empty"
refused 'an empty file' sh "$script" HEAD~1 "$tmp/empty"
refused 'a missing file' sh "$script" HEAD~1 "$tmp/none"
refused 'a name that is no commit' sh "$script" nope "$tmp/message"
refused 'one argument' sh "$script" HEAD~1
{
    printf 'docs(ship): Add Two, reworded\n\n'
    for n in 1 2 3 4 5 6 7 8 9 10 11 12; do printf 'line %s\n' "$n"; done
    printf '#55 is a body line too\n'
} >"$tmp/long"
refused 'a body past the bound, a # line counted' sh "$script" HEAD~1 "$tmp/long"
refused 'a commit main already holds' sh "$script" base "$tmp/message"
setup
git switch -q -c side base && git commit -q --allow-empty -m 'docs(ship): Elsewhere' && git switch -q -
side=$(git rev-parse side)
head=$(git rev-parse HEAD)
sh "$script" "$side" "$tmp/message" >"$tmp/out" 2>&1; status=$?
check 'a commit off the branch: exits 1' '[ $status -eq 1 ]'
check 'a commit off the branch: makes nothing' '[ "$(git rev-parse HEAD)" = "$head" ]'

# A copy outside this repository has no rules.sh four directories up.
mkdir -p "$tmp/elsewhere" && cp "$script" "$tmp/elsewhere/reword.sh"
setup
sh "$tmp/elsewhere/reword.sh" HEAD~1 "$tmp/message" >"$tmp/out" 2>&1; status=$?
check 'a copy with no rules.sh: still makes the amend!' '[ $status -eq 0 ]'
check 'a copy with no rules.sh: says the message went unchecked' 'grep -q "unchecked" "$tmp/out"'

# git replaced, so the commit succeeds but is no amend!: the script must fail.
mkdir "$tmp/bin"
printf '#!/bin/sh\n[ "$1" = commit ] && exec "%s" commit -q --allow-empty -m "docs(ship): Add Two"\nexec "%s" "$@"\n' \
    "$(command -v git)" "$(command -v git)" >"$tmp/bin/git"
chmod +x "$tmp/bin/git"
setup
PATH=$tmp/bin:$PATH sh "$script" HEAD~1 "$tmp/message" >"$tmp/out" 2>&1; status=$?
check 'a commit that is not the amend!: exits 1' '[ $status -eq 1 ]'
check 'a commit that is not the amend!: says how to undo it' 'grep -q "git reset --soft HEAD~1" "$tmp/out"'

if [ "$failures" -gt 0 ]; then
    printf '%s case(s) failed.\n' "$failures"
    exit 1
fi
printf 'All reword.sh cases pass.\n'
