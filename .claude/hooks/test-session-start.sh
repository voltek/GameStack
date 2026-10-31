#!/bin/sh
# Every case is a SessionStart input and a line the hook must, or must not, print.
# Run by commits.yml, and by hand after editing session-start.sh.

LC_ALL=C
export LC_ALL
GIT_CONFIG_GLOBAL=/dev/null GIT_CONFIG_NOSYSTEM=1
export GIT_CONFIG_GLOBAL GIT_CONFIG_NOSYSTEM

hook=$(cd "$(dirname "$0")" && pwd)/session-start.sh
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
failures=0
repo=$tmp/repo
dir=$tmp/handoff

# A fake gh: the pool query gets one issue, the Triage log query its body. A newer
# unpinned decoy names main's tip; only a jq select on .isPinned skips it.
mkdir -p "$tmp/bin"
cat >"$tmp/bin/gh" <<'EOF'
#!/bin/sh
case "$*" in
    *--search*"and .isPinned"*) printf 'Last triage: 2026-10-30 at main %s.\n' "$FAKE_LAST" ;;
    *--search*) printf 'Last triage: 2026-10-31 at main %s.\n' "$FAKE_DECOY" "$FAKE_LAST" ;;
    *) printf '1|,defect,|fix(search): A defect\n' ;;
esac
EOF
chmod +x "$tmp/bin/gh"

g() { git -C "$repo" -c user.name=t -c user.email=t@t "$@"; }
commit() { mkdir -p "$repo/$(dirname "$1")" && echo "$2" >"$repo/$1" && g add -A && g commit -qm "$2"; }

git init -q -b main "$repo"
commit README.md base
FAKE_LAST=$(g rev-parse HEAD)
export FAKE_LAST
g switch -qc docs/x && commit docs/a.md doc && g switch -q main
g merge -q --no-ff -m 'Merge docs/x' docs/x
merged=$(g rev-parse HEAD)
commit app/src/main/A.kt squashed
squashed=$(g rev-parse HEAD)
FAKE_DECOY=$squashed
export FAKE_DECOY

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

seq 81 >"$dir/HANDOFF.md"
run startup s2
has 'over the line bound' 'WARNING: HANDOFF.md is 81 lines'

g update-ref refs/remotes/origin/main "$merged"
run startup s3
has 'one docs merge' 'Triage not owed: 1 merges'
g update-ref refs/remotes/origin/main "$squashed"
run startup s3
has 'a squash merge into app/src/main' 'TRIAGE OWED (ADR 0038): 2 merges'
lacks 'an unpinned decoy Triage log' 'Triage not owed: 0 merges'
g update-ref -d refs/remotes/origin/main
run startup s3
lacks 'no origin/main' 'Triage not owed'
has 'no origin/main' 'TRIAGE: could not count'

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
