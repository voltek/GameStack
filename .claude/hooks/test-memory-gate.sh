#!/bin/sh
# Every case is a PreToolUse input and whether memory-gate.mjs denies it.
# Run by commits.yml, and by hand after editing memory-gate.mjs.

here=$(cd "$(dirname "$0")" && pwd)
hook=$here/memory-gate.mjs
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
failures=0
CLAUDE_MEMORY_GATE_DIR=$tmp/state
export CLAUDE_MEMORY_GATE_DIR
# Paths as JSON carries them: each backslash doubled.
mem='C:\\Users\\a\\.claude\\projects\\C--x-Repo\\memory'

# $1 session, $2 tool, $3 file path, $4 the other tool_input fields.
run() {
    printf '{"session_id":"%s","hook_event_name":"PreToolUse","tool_name":"%s","tool_input":{"file_path":"%s"%s}}' \
        "$1" "$2" "$3" "$4" | node "$hook" >"$tmp/out" 2>&1
}
denied() {
    grep -q '"permissionDecision":"deny"' "$tmp/out" && return
    printf 'FAIL %s: expected a deny\n' "$1"; sed 's/^/    /' "$tmp/out"; failures=$((failures + 1))
}
passed() {
    [ ! -s "$tmp/out" ] && return
    printf 'FAIL %s: expected no output\n' "$1"; sed 's/^/    /' "$tmp/out"; failures=$((failures + 1))
}

run s1 Write "$mem\\\\a-lesson.md" ',"content":"x"'
denied 'the first write to a memory'
grep -q 'a-lesson.md: would it serve a clone' "$tmp/out" \
    || { printf 'FAIL the question names the file\n'; failures=$((failures + 1)); }
run s1 Write "$mem\\\\a-lesson.md" ',"content":"x"'
passed 'the same write sent again'
run s1 Edit 'c:/users/a/.claude/projects/c--x-repo/memory/A-LESSON.md' ',"old_string":"a","new_string":"b"'
passed 'the same memory spelled in another case'
run s1 Edit "$mem\\\\other.md" ',"old_string":"a","new_string":"b"'
denied 'an edit of another memory'
run s2 Write "$mem\\\\a-lesson.md" ',"content":"x"'
denied 'the same memory in another session'
run s1 Write '/home/a/.claude/projects/-home-a-repo/memory/posix.md' ',"content":"x"'
denied 'a POSIX path'
run s1 Write '/home/a/.claude/projects/-home-a-repo/memory/../memory/dotted.md' ',"content":"x"'
denied 'a memory reached through ..'
run s1 Edit "$mem\\\\MEMORY.md" ',"old_string":"a","new_string":"b"'
passed 'the index'
run s1 Write 'C:\\Users\\a\\Repo\\docs\\memory\\notes.md' ',"content":"x"'
passed 'a memory folder in the repository'
run s1 Write "$mem\\\\sub\\\\deep.md" ',"content":"x"'
passed 'below the memory folder'
run s1 Write 'C:\\Repo\\a.md' ',"content":"see /home/a/.claude/projects/p/memory/m.md"'
passed 'a memory path only in the content'
# An expired entry the cleanup cannot remove, a folder here, must not let the write through unasked.
mkdir -p "$tmp/state/old/inner" && touch -d '3 days ago' "$tmp/state/old"
run s3 Write "$mem\\\\after-a-failed-cleanup.md" ',"content":"x"'
denied 'an expired entry the cleanup cannot remove'
run '' Write "$mem\\\\no-session.md" ',"content":"x"'
passed 'no session id'
printf 'not json' | node "$hook" >"$tmp/out" 2>&1
passed 'input that is not JSON'

if [ "$failures" -gt 0 ]; then
    printf '%s case(s) failed.\n' "$failures"
    exit 1
fi
printf 'All memory-gate.mjs cases pass.\n'
