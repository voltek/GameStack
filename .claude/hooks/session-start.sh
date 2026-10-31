#!/usr/bin/env bash
# SessionStart hook: prints the hot handoff, the open cycle, and repo facts derived live (checkpoint Skill).
# Stdout becomes session context. Claude Code cuts hook output over 10,000 chars to a 2,000-char preview.
# On resume the transcript already holds the earlier injection, so both files are re-sent only if either changed.

repo="${CLAUDE_PROJECT_DIR:-$PWD}"
# The main checkout's name, not a worktree's: every worktree shares one handoff.
common=$(git -C "$repo" rev-parse --path-format=absolute --git-common-dir 2>/dev/null)
dir="${CLAUDE_HANDOFF_DIR:-$HOME/Documents/$(basename "$(dirname "${common:-$repo/.git}")")}"
handoff="$dir/HANDOFF.md"
cycle="$dir/CYCLE.md"
state_dir="$dir/.hook-state"
max_lines=80
max_bytes=6000
max_cycle_lines=30
max_issues=300
cap=10000

input=$(cat)
field() { printf '%s' "$input" | sed -n "s/.*\"$1\" *: *\"\([^\"]*\)\".*/\1/p" | head -1; }
source=$(field source)
session=$(field session_id)

facts() {
  echo "== Live repo facts (derived now; never copy them into HANDOFF.md) =="
  echo "\$ git status -sb";  git -C "$repo" status -sb | head -15
  echo "\$ git branch -vv";  git -C "$repo" branch -vv
  echo "\$ git log --oneline -8"; git -C "$repo" log --oneline -8
  local top
  top=$(for ref in $(git -C "$repo" for-each-ref --format='%(refname:short)' refs/heads refs/remotes); do
          git -C "$repo" ls-tree --name-only "$ref" docs/project/decisions/ 2>/dev/null
        done | sed -n 's|.*/\([0-9]\{4\}\)-.*|\1|p' | sort -u | tail -1)
  [ -n "$top" ] && printf 'Highest ADR on any branch: %s, so the next free number is %04d.\n' "$top" $((10#$top + 1))
  pool
}

# The issue queue and ADR 0038's triage rule, as of the last fetch. Facts only: counts and a merge count.
pool() {
  local rows bucket n last landed merges product
  rows=$(cd "$repo" && timeout 15 gh issue list --state open --limit $max_issues --json number,title,labels \
         --jq '.[] | select(.title != "Triage log") | "\(.number)|,\([.labels[].name]|join(",")),|\(.title)"' 2>/dev/null) \
    || { echo "Issue pool: gh unavailable, not read."; return; }
  # The Triage log is fetched and then dropped, so a capped list holds one row fewer.
  [ "$(printf '%s\n' "$rows" | grep -c .)" -ge $((max_issues - 1)) ] \
    && echo "Issue pool: only the first $max_issues open issues were read; every count below may be low."
  printf 'Product backlog (ADR 0043): %s open; horizons live in the GameStack Product Project.\n' \
    "$(printf '%s\n' "$rows" | grep -c ',product,')"
  rows=$(printf '%s\n' "$rows" | grep -v ',product,')
  printf 'Issue pool: %s open —' "$(printf '%s\n' "$rows" | grep -c .)"
  for bucket in blocks-product defect improvement idea; do
    printf ' %s %s,' "$bucket" "$(printf '%s\n' "$rows" | grep -c ",$bucket,")"
  done
  n=$(printf '%s\n' "$rows" | grep -v -e ',blocks-product,' -e ',defect,' -e ',improvement,' -e ',idea,' | grep -c .)
  printf ' no bucket %s; gated %s.\n' "$n" "$(printf '%s\n' "$rows" | grep -c ',gated,')"
  printf '%s\n' "$rows" | grep -e ',blocks-product,' -e ',defect,' | grep -v ',gated,' \
    | sed 's/^\([0-9]*\)|[^|]*|/  needs attention: #\1 /'
  last=$(cd "$repo" && timeout 15 gh issue list --state open --search 'in:title "Triage log"' --json title,body,isPinned \
         --jq '.[] | select(.title == "Triage log" and .isPinned) | .body' 2>/dev/null \
         | sed -n 's/^Last triage: .*at main \([0-9a-f]\{7,40\}\).*/\1/p' | head -1)
  if ! git -C "$repo" cat-file -e "${last:-x}^{commit}" 2>/dev/null; then
    echo "TRIAGE: no 'Last triage:' line with a known commit in the pinned Triage log issue."; return
  fi
  # Every first-parent commit on main is a pull request landing, a squash merge included.
  landed=$(git -C "$repo" rev-list --first-parent "$last"..origin/main 2>/dev/null) \
    || { echo "TRIAGE: could not count the merges since the last one against origin/main."; return; }
  merges=$(printf '%s\n' "$landed" | grep -c .)
  product=$(printf '%s\n' "$landed" | while read -r m; do [ -n "$m" ] || continue
              git -C "$repo" diff --quiet "$m^1" "$m" -- app/src/main || echo x; done | grep -c x)
  if [ "$product" -gt 0 ] || [ "$merges" -ge 3 ]; then
    echo "TRIAGE OWED (ADR 0038): $merges merges since the last one, $product of them changed app/src/main."
  else
    echo "Triage not owed: $merges merges since the last one, none changed app/src/main."
  fi
}

handoff_block() {
  if [ ! -f "$handoff" ]; then echo "No handoff at $handoff."; return; fi
  echo "== HANDOFF.md ($handoff) =="
  cat "$handoff"
  echo
  if [ -f "$cycle" ]; then echo "== CYCLE.md ($cycle) =="; cat "$cycle"
  else echo "No open cycle at $cycle: the next one is chosen at a triage."; fi
}

hash=$( [ -f "$handoff" ] && cat "$handoff" "$cycle" 2>/dev/null | cksum | cut -d' ' -f1 )
[ -f "$handoff" ] && mkdir -p "$state_dir" && find "$state_dir" -type f -mtime +30 -delete 2>/dev/null
seen=""; [ -n "$session" ] && [ -f "$state_dir/$session" ] && seen=$(cat "$state_dir/$session")

case "$source" in
  fork)   body="HANDOFF.md and CYCLE.md are already in the inherited conversation; re-read them if they may have changed."$'\n'"$(facts)" ;;
  resume) if [ -z "$hash" ]; then
            body="$(handoff_block)"$'\n\n'"$(facts)"
          elif [ "$hash" = "$seen" ]; then
            body="HANDOFF.md and CYCLE.md are unchanged since they were injected earlier in this conversation."$'\n'"$(facts)"
          else
            body="HANDOFF.md or CYCLE.md changed since the last injection; this copy supersedes it."$'\n'"$(handoff_block)"$'\n\n'"$(facts)"
          fi ;;
  *)      body="$(handoff_block)"$'\n\n'"$(facts)" ;;
esac
# A fork injected nothing, so it has seen nothing: its first resume re-sends both files.
[ "$source" != fork ] && [ -n "$session" ] && [ -n "$hash" ] && echo "$hash" > "$state_dir/$session"

warn=""
if [ -f "$handoff" ]; then
  lines=$(wc -l < "$handoff"); bytes=$(wc -c < "$handoff")
  if [ "$lines" -gt $max_lines ] || [ "$bytes" -gt $max_bytes ]; then
    warn="WARNING: HANDOFF.md is $lines lines / $bytes bytes, over its $max_lines / $max_bytes bound. Trim it at the next checkpoint."$'\n'
  fi
fi
if [ -f "$cycle" ] && [ "$(wc -l < "$cycle")" -gt $max_cycle_lines ]; then
  warn+="WARNING: CYCLE.md is over $max_cycle_lines lines: a growing cycle is the circuit breaker, re-triage."$'\n'
fi
[ ${#body} -gt $((cap - 1000)) ] && warn+="WARNING: this hook output is ${#body} chars; above $cap Claude Code shows only a 2,000-char preview."$'\n'

printf '%s%s\n' "$warn" "$body"
