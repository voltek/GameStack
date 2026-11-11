#!/bin/sh
# Every case is a SessionStart input and a line the hook must, or must not, print.
# Run by commits.yml, and by hand after editing session-start.sh.

LC_ALL=C
export LC_ALL
GIT_CONFIG_GLOBAL=/dev/null GIT_CONFIG_NOSYSTEM=1
export GIT_CONFIG_GLOBAL GIT_CONFIG_NOSYSTEM

here=$(cd "$(dirname "$0")" && pwd)
hook=$here/session-start.sh
skill=$here/../skills/checkpoint/SKILL.md
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
failures=0
repo=$tmp/repo
dir=$tmp/handoff

# A fake gh: one open issue per kind the pool tells apart, or a failure when FAKE_GH_FAIL is set.
mkdir -p "$tmp/bin"
cat >"$tmp/bin/gh" <<'EOF'
#!/bin/sh
[ -n "$FAKE_GH_FAIL" ] && exit 1
printf '%s\n' '1|,defect,|fix(search): A defect' '2|,improvement,p2,|docs(x): Prioritised' \
    '8|,blocks-product,p1,|docs(x): A blocker' \
    '3|,idea,gated,|docs(x): Gated' '4|,product,|feat(detail): A product item' \
    '7|,product,bug,|fix(search): An app defect' \
    '5|,review-debt,|docs(x): Waiting for its bucket' '6|,improvement,|docs(x): Rename p1,p2,p3, a title only'
EOF
chmod +x "$tmp/bin/gh"

g() { git -C "$repo" -c user.name=t -c user.email=t@t "$@"; }
git init -q -b main "$repo"
mkdir -p "$repo/docs" && echo base >"$repo/docs/a.md" && g add -A && g commit -qm base

run() {
    printf '{"session_id":"%s","source":"%s"}' "$2" "$1" \
        | CLAUDE_PROJECT_DIR=$repo CLAUDE_HANDOFF_DIR=$dir PATH="$tmp/bin:$PATH" \
          bash "$hook" >"$tmp/out" 2>&1
}
has() {
    grep -qF "$2" "$tmp/out" && return
    printf 'FAIL %s: expected "%s"\n' "$1" "$2"
    sed 's/^/    /' "$tmp/out"
    failures=$((failures + 1))
}
lacks() {
    grep -qF "$2" "$tmp/out" || return
    printf 'FAIL %s: did not expect "%s"\n' "$1" "$2"
    sed 's/^/    /' "$tmp/out"
    failures=$((failures + 1))
}
# A cycle file with $1 insertion rows, padded with $2 blank lines.
cycle() {
    { echo '# Cycle'; echo '| # | Item |'; echo '|---|---|'; echo '| 1 | An item |'
      echo; echo '## Insertions'; echo '| # | Item |'; echo '|---|---|'
      i=0; while [ $i -lt "$1" ]; do i=$((i + 1)); echo "| $i | An insertion |"; done
      i=0; while [ $i -lt "$2" ]; do i=$((i + 1)); echo; done; } >"$dir/CYCLE.md"
}

run startup s0
has 'no handoff' 'No handoff at'
has 'no handoff' 'Live repo facts'
[ -d "$dir" ] && { printf 'FAIL no handoff: created %s\n' "$dir"; failures=$((failures + 1)); }
run resume s0
lacks 'resume, no handoff' 'changed since the last injection'
has 'resume, no handoff' 'No handoff at'

mkdir -p "$dir"
echo '# Handoff v1' >"$dir/HANDOFF.md"
run startup s1
has 'startup' '# Handoff v1'
run resume s1
has 'resume, unchanged' 'unchanged since they were injected'
echo '# Handoff v2' >"$dir/HANDOFF.md"
run resume s1
has 'resume, changed' 'changed since the last injection'
has 'resume, changed' '# Handoff v2'

run fork f1
lacks 'fork' '# Handoff v2'
run resume f1
has 'resume of a fork' '# Handoff v2'

printf '## Now\n## In flight\n## Owed by the owner\n' >"$dir/HANDOFF.md"
run startup s4
lacks 'the allowed sections' 'a section the checkpoint Skill does not allow'
printf '## Now\r\n## In flight \r\n## Owed by the owner\r\n' >"$dir/HANDOFF.md"
run startup s4
lacks 'the allowed sections with CRLF or a trailing space' 'a section the checkpoint Skill does not allow'
printf '## Now\n## Constraints not written anywhere else\n' >"$dir/HANDOFF.md"
run startup s4
has 'a section outside the allowed ones' 'a section the checkpoint Skill does not allow: ## Constraints not written anywhere else. It holds'

lacks 'no plans folder' 'neither CYCLE.md nor HANDOFF.md cites'
mkdir -p "$dir/plans/probe"
: >"$dir/plans/cited.md"; : >"$dir/plans/cited"; : >"$dir/plans/stale.md"
printf 'Points at `plans/cited.md` and plans/probe/run.js.\n' >"$dir/HANDOFF.md"
run startup s4
has 'a plan nothing cites' 'WARNING: neither CYCLE.md nor HANDOFF.md cites plans/cited plans/stale.md. Delete'
cycle 0 0
echo 'plans/stale.md' >>"$dir/CYCLE.md"
run startup s4
has 'a plan the cycle cites' 'cites plans/cited. Delete'
cycle 0 0
echo 'Not a plans folder: oldplans/stale.md' >>"$dir/CYCLE.md"
run startup s4
has 'a longer folder name is not a citation' 'cites plans/cited plans/stale.md. Delete'
cycle 0 0
echo 'A full path: ~/Documents/GameStack/plans/stale.md' >>"$dir/CYCLE.md"
run startup s4
has 'a plan cited by a fuller path' 'cites plans/cited. Delete'
printf '%s\n' 'Its plan is plans/stale.md.' 'Another is plans/cited.' 'And plans/cited.md, plans/probe/run.js' >"$dir/HANDOFF.md"
run startup s4
lacks 'a plan cited at the end of a sentence' 'neither CYCLE.md nor HANDOFF.md cites'
: >"$dir/plans/a"
printf 'Its plan is plans/stale.md, plans/cited. and plans/cited.md, plans/probe/x and plans/a' >"$dir/HANDOFF.md"
printf 'bc\n## Insertions\n' >"$dir/CYCLE.md"
run startup s4
lacks 'a handoff with no final newline' 'neither CYCLE.md nor HANDOFF.md cites'
rm "$dir/plans/a"
printf '%s\n' 'Its plan is plans/stale.md.' 'Another is plans/cited.' 'And plans/cited.md, plans/probe/run.js' >"$dir/HANDOFF.md"
# Only where the file system takes a backslash in a name, which Windows does not.
if (: >"$dir/plans/a\\tb.md") 2>/dev/null && [ -f "$dir/plans/a\\tb.md" ]; then
    printf '%s\n' 'Its plan is plans/a\tb.md' >>"$dir/HANDOFF.md"
    run startup s4
    lacks 'a backslash in a plan name' 'neither CYCLE.md nor HANDOFF.md cites'
fi
rm -r "$dir/plans" "$dir/CYCLE.md"
# The cap counts the whole output: a body under it, with the warnings over it.
awk 'BEGIN { while (n++ < 8050) printf "x"; print "" }' >"$dir/HANDOFF.md"
mkdir -p "$dir/plans"
i=0; while [ $i -lt 25 ]; do i=$((i + 1)); : >"$dir/plans/a-plan-with-a-long-name-$i.md"; done
run startup s4
has 'warnings counted against the cap' 'WARNING: this hook output is'
has 'the plan list capped' 'a-plan-with-a-long-name-18.md … and 15 more. Delete'
[ "$(grep -v '^WARNING:' "$tmp/out" | wc -c)" -lt 9000 ] \
    || { printf 'FAIL warnings counted against the cap: the body alone is over 9,000 chars\n'; failures=$((failures + 1)); }
head -1 "$tmp/out" | grep -q '^No cycle is kept' \
    || { printf 'FAIL the triage line before the warnings: line 1 is "%s"\n' "$(head -1 "$tmp/out")"; failures=$((failures + 1)); }
rm "$dir/HANDOFF.md"
run startup s4
lacks 'plans with neither file to cite them' 'neither CYCLE.md nor HANDOFF.md cites'
rm -r "$dir/plans"

seq 41 >"$dir/HANDOFF.md"
run startup s2
has 'over the line bound' 'WARNING: HANDOFF.md is 41 lines'
awk 'BEGIN { for (l = 0; l < 39; l++) { s = ""; while (length(s) < 130) s = s "x"; print s } }' >"$dir/HANDOFF.md"
run startup s2
has 'over the byte bound' 'WARNING: HANDOFF.md is 39 lines / 5109 bytes'

has 'the product backlog counted apart' 'Product backlog (ADR 0043): 2 open'
has 'bugs counted in the product backlog' '2 open, 1 of them bugs (ADR 0048)'
has 'product issues kept out of the harness pool' 'Issue pool: 6 open'
has 'what blocks the product listed' '  blocks the product: #8 docs(x): A blocker'
lacks 'a defect left to the triage' 'A defect'
lacks 'the status left to gitStatus' 'git status'
has 'an ungated issue with no priority' 'A bucket and no priority, ungated (ADR 0047): #1 #6.'
has 'no cycle kept on this machine' 'No cycle is kept on this machine'
lacks 'no cycle kept on this machine' 'TRIAGE OWED'
mkdir -p "$dir/archive/cycles"
run startup s2
has 'no open cycle' 'TRIAGE OWED (ADR 0047): no CYCLE.md'
t=$(grep -n -m1 'TRIAGE OWED' "$tmp/out" | cut -d: -f1)
h=$(grep -n -m1 '== HANDOFF.md' "$tmp/out" | cut -d: -f1)
[ "${t:-99999}" -lt "${h:-0}" ] \
    || { printf 'FAIL the triage line before the handoff: line %s, handoff at %s\n' "$t" "$h"; failures=$((failures + 1)); }
cycle 2 0
run startup s3
has 'two insertions' 'Triage not owed: the open cycle has 2 insertions'
cycle 3 0
run startup s3
has 'a third insertion' 'TRIAGE OWED (ADR 0047): a breaker fired, CYCLE.md has 3 insertions'
# The fixture is 8 lines before its padding: 22 blank lines make 30, at the bound.
cycle 0 22
run startup s3
has 'a cycle at its line bound' 'Triage not owed: the open cycle has 0 insertions and 30 lines'
cycle 0 23
run startup s3
has 'a cycle over its line bound' 'TRIAGE OWED (ADR 0047): a breaker fired, CYCLE.md has 0 insertions (stops at 3) and 31 lines'
lacks 'a cycle with its insertions heading' "has no '## Insertions' heading"
printf '# Cycle\n### Insertions\n| 1 | An insertion |\n' >"$dir/CYCLE.md"
run startup s3
has 'a cycle with no insertions heading' "TRIAGE OWED (ADR 0047): CYCLE.md has no '## Insertions' heading"
printf '## Insertions\n| # | Item |\n|---|---|\n| a1 | One |\n| I2 | Two |\n| - | Three |\n' >"$dir/CYCLE.md"
run startup s3
has 'insertions however numbered' 'TRIAGE OWED (ADR 0047): a breaker fired, CYCLE.md has 3 insertions'
rm "$dir/CYCLE.md"
FAKE_GH_FAIL=1
export FAKE_GH_FAIL
run startup s3
unset FAKE_GH_FAIL
has 'gh unavailable' 'Issue pool: gh unavailable'
has 'gh unavailable' 'TRIAGE OWED (ADR 0047): no CYCLE.md'

# The hook's bounds are constants; the checkpoint Skill states them in prose.
bound() { sed -n "s/^$1=//p" "$hook"; }
thousands() { echo "$1" | sed 's/\([0-9]\)\([0-9]\{3\}\)$/\1,\2/'; }
case $(bound max_insertions) in 2) nth=second ;; 3) nth=third ;; 4) nth=fourth ;; *) nth="$(bound max_insertions)th" ;; esac
for phrase in "Bound: $(bound max_lines) lines and $(thousands "$(bound max_bytes)") bytes." \
              "past $(bound max_cycle_lines) lines" "A $nth insertion in one cycle stops it"; do
    grep -qF "$phrase" "$skill" && continue
    printf 'FAIL bounds match the checkpoint Skill: "%s" not in %s\n' "$phrase" "$skill"
    failures=$((failures + 1))
done
# The Skill's list of sections is the hook's, in its order and with nothing else.
phrase="then *$(bound sections | tr -d "'" | sed 's/|/*, */g')*, and no other"
grep -qF "$phrase" "$skill" \
    || { printf 'FAIL sections match the checkpoint Skill: "%s" not in %s\n' "$phrase" "$skill"; failures=$((failures + 1)); }

# No CLAUDE_HANDOFF_DIR: a worktree reads ~/Documents/<main checkout's name>.
mkdir -p "$tmp/home/Documents/repo"
echo '# Default path' >"$tmp/home/Documents/repo/HANDOFF.md"
g worktree add -q --detach "$tmp/wt"
printf '{"session_id":"w1","source":"startup"}' \
    | env -u CLAUDE_HANDOFF_DIR HOME="$tmp/home" CLAUDE_PROJECT_DIR="$tmp/wt" PATH="$tmp/bin:$PATH" \
      bash "$hook" >"$tmp/out" 2>&1
has 'default path from a worktree' '# Default path'

if [ "$failures" -gt 0 ]; then
    printf '%s case(s) failed.\n' "$failures"
    exit 1
fi
printf 'All session-start.sh cases pass.\n'
