#!/usr/bin/env bash
# SessionStart hook: prints the hot handoff, the open cycle, and repo facts derived live (checkpoint Skill).
# A line printed here is paid by every session: it acts on it at its start, and Claude Code does not inject it (ADR 0065).
# Stdout becomes session context. Claude Code cuts hook output over 10,000 chars to a 2,000-char preview.
# On resume the transcript already holds the earlier injection, so both files are re-sent only if either changed.

repo="${CLAUDE_PROJECT_DIR:-$PWD}"
# The main checkout's name, not a worktree's: every worktree shares one handoff.
common=$(git -C "$repo" rev-parse --path-format=absolute --git-common-dir 2>/dev/null)
dir="${CLAUDE_HANDOFF_DIR:-$HOME/Documents/$(basename "$(dirname "${common:-$repo/.git}")")}"
handoff="$dir/HANDOFF.md"
cycle="$dir/CYCLE.md"
state_dir="$dir/.hook-state"
plans="$dir/plans"
sections='Now|In flight|Owed by the owner'
max_plans=10
max_lines=40
max_bytes=3000
max_cycle_lines=30
max_insertions=3
max_issues=300
cap=10000

input=$(cat)
field() { printf '%s' "$input" | sed -n "s/.*\"$1\" *: *\"\([^\"]*\)\".*/\1/p" | head -1; }
source=$(field source)
session=$(field session_id)

facts() {
  echo "== Live repo facts (derived now; never copy them into HANDOFF.md) =="
  # Claude Code's own gitStatus already holds the status and the last commits; it lacks the other branches.
  # A snapshot from the start is enough: a session about to act on the tree reads git itself (ADR 0065).
  echo "\$ git branch -vv";  git -C "$repo" branch -vv
  local top
  top=$(for ref in $(git -C "$repo" for-each-ref --format='%(refname:short)' refs/heads refs/remotes); do
          git -C "$repo" ls-tree --name-only "$ref" docs/project/decisions/ 2>/dev/null
        done | sed -n 's|.*/\([0-9]\{4\}\)-.*|\1|p' | sort -u | tail -1)
  [ -n "$top" ] && printf 'Highest ADR on any branch: %s, so the next free number is %04d.\n' "$top" $((10#$top + 1))
  pool
}

# The issue queue as of the last fetch: counts, what blocks the product, and ADR 0047's issues awaiting a priority.
pool() {
  local rows product b n missing buckets='blocks-product defect improvement idea'
  # A label is matched in a row's label field only (num|,labels,|title), never in its title.
  local f='^[0-9]+\|[^|]*,' any
  any="$f($(printf '%s' "$buckets" | tr ' ' '|')),"
  rows=$(cd "$repo" && timeout 15 gh issue list --state open --limit $max_issues --json number,title,labels \
         --jq '.[] | select(.title != "Triage log") | "\(.number)|,\([.labels[].name]|join(",")),|\(.title)"' 2>/dev/null) \
    || { echo "Issue pool: gh unavailable, not read."; return; }
  # The Triage log is fetched and then dropped, so a capped list holds one row fewer.
  [ "$(printf '%s\n' "$rows" | grep -c .)" -ge $((max_issues - 1)) ] \
    && echo "Issue pool: only the first $max_issues open issues were read; every count below may be low."
  product=$(printf '%s\n' "$rows" | grep -E "${f}product,")
  printf 'Product backlog (ADR 0043): %s open, %s of them bugs (ADR 0048); horizons live in the GameStack Product Project.\n' \
    "$(printf '%s\n' "$product" | grep -c .)" "$(printf '%s\n' "$product" | grep -cE "${f}bug,")"
  rows=$(printf '%s\n' "$rows" | grep -vE "${f}product,")
  printf 'Issue pool: %s open —' "$(printf '%s\n' "$rows" | grep -c .)"
  for b in $buckets; do
    printf ' %s %s,' "$b" "$(printf '%s\n' "$rows" | grep -cE "$f$b,")"
  done
  n=$(printf '%s\n' "$rows" | grep . | grep -vcE "$any")
  printf ' no bucket %s; gated %s.\n' "$n" "$(printf '%s\n' "$rows" | grep -cE "${f}gated,")"
  # Only what blocks the product changes a session's task; a defect waits for the triage, which lists it.
  printf '%s\n' "$rows" | grep -E "${f}blocks-product," | grep -vE "${f}gated," \
    | sed 's/^\([0-9]*\)|[^|]*|/  blocks the product: #\1 /'
  # An issue with no bucket waits for the triage, and is counted above as "no bucket".
  missing=$(printf '%s\n' "$rows" | grep -E "$any" | grep -vE "$f(gated|p[123])," \
            | sed 's/^\([0-9]*\)|.*/#\1/' | tr '\n' ' ')
  [ -n "$missing" ] && echo "A bucket and no priority, ungated (ADR 0047): ${missing% }."
}

# ADR 0047: a triage is owed when no cycle is open or a breaker fires; the checkpoint Skill states the bounds.
triage() {
  local ins lines
  # Only the machine that keeps the cycles (a CYCLE.md, or an archive of them) owes a triage for having none.
  if [ ! -f "$cycle" ]; then
    if [ -d "$dir/archive/cycles" ]; then echo "TRIAGE OWED (ADR 0047): no CYCLE.md, so no cycle is open."
    else echo "No cycle is kept on this machine (no CYCLE.md or archive/cycles/ in $dir): cycles and triages run where they are kept."; fi
    return
  fi
  # A breaker that cannot be read is owed, never assumed clear.
  if ! grep -q '^## Insertions' "$cycle"; then
    echo "TRIAGE OWED (ADR 0047): CYCLE.md has no '## Insertions' heading, so its breaker cannot be read (checkpoint Skill)."; return
  fi
  # Every row of the table under the heading but its header and separator, however it is numbered.
  ins=$(sed -n '/^## Insertions/,/^## /p' "$cycle" | awk '/^\|/ { if (++n > 2) c++ } END { print c + 0 }')
  lines=$(($(wc -l < "$cycle")))
  if [ "$ins" -ge $max_insertions ] || [ "$lines" -gt $max_cycle_lines ]; then
    echo "TRIAGE OWED (ADR 0047): a breaker fired, CYCLE.md has $ins insertions (stops at $max_insertions) and $lines lines (bound $max_cycle_lines)."
  else
    echo "Triage not owed: the open cycle has $ins insertions and $lines lines."
  fi
}

handoff_block() {
  if [ ! -f "$handoff" ]; then echo "No handoff at $handoff."; return; fi
  echo "== HANDOFF.md ($handoff) =="
  cat "$handoff"
  echo
  if [ -f "$cycle" ]; then echo "== CYCLE.md ($cycle) =="; cat "$cycle"
  else echo "No CYCLE.md at $cycle."; fi
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
  extra=$(grep '^## ' "$handoff" | sed 's/[[:space:]]*$//' | grep -vxE "## ($sections)" | tr '\n' ' ')
  [ -n "$extra" ] && warn+="WARNING: HANDOFF.md has a section the checkpoint Skill does not allow: ${extra% }. It holds only ${sections//|/, }."$'\n'
fi
# A plan outlives its item when nothing cites it; the owner deletes it, once the checkpoint has checked what it holds.
# A citation is the path segment plans/<name>, whole: plans/a.md and oldplans/a do not cite plans/a, while
# ~/x/plans/a, plans/a/b and a sentence's "plans/a." do. Names go through ENVIRON, since -v reads backslash escapes.
uncited() {
  PLAN_NAMES="$1" awk -v max=$max_plans 'BEGIN { n = split(ENVIRON["PLAN_NAMES"], name, "\n") }
    { for (k = 1; k <= n; k++) if (!(k in hit) && cites($0, "plans/" name[k])) hit[k] = 1 }
    END { for (k = 1; k <= n; k++) if (!(k in hit) && ++m <= max) printf "plans/%s ", name[k]
          if (m > max) printf "… and %d more ", m - max }
    function cites(s, t,   at, i, b, c) {
      while ((i = index(substr(s, at + 1), t)) > 0) {
        at += i; b = at > 1 ? substr(s, at - 1, 1) : ""; c = substr(s, at + length(t), 2)
        if (b !~ /[[:alnum:]_.-]/ && c !~ /^([[:alnum:]_-]|\.[[:alnum:]_.-])/) return 1
      }
      return 0
    }'
}
if [ -d "$plans" ] && { [ -f "$handoff" ] || [ -f "$cycle" ]; }; then
  stale=$({ cat "$handoff"; echo; cat "$cycle"; } 2>/dev/null \
          | uncited "$(for p in "$plans"/*; do [ -e "$p" ] && printf '%s\n' "${p##*/}"; done)")
  [ -n "$stale" ] && warn+="WARNING: neither CYCLE.md nor HANDOFF.md cites ${stale% }. Once its decisions and the measures an open item needs are in an ADR, an issue or TOOLING.md, give the owner the command that deletes it (checkpoint Skill)."$'\n'
fi
# A branch's review records and captures live in reviews/<branch>/ and captures/<branch>/, deleted here
# once the branch is neither local nor on a remote: a hook is outside auto mode's classifier, which
# refuses an agent's rm there. Without a readable repository nothing is deleted, and with no records git is not run.
# A name below the folder's also keeps it, so a branch deeper than two levels keeps its folder.
alive() {
  local n
  while IFS= read -r n; do case "$n" in "$1" | "$1"/*) return 0 ;; esac; done <<<"$branches"
  return 1
}
gone=""; misfiled=""
if [ -n "$(find "$dir/reviews" "$dir/captures" -mindepth 1 -maxdepth 1 2>/dev/null | head -1)" ] \
   && branches=$(git -C "$repo" for-each-ref --format='%(refname)' refs/heads refs/remotes 2>/dev/null); then
  branches=$(printf '%s\n' "$branches" | sed -e 's|^refs/heads/||' -e 's|^refs/remotes/[^/]*/||')
  for sub in reviews captures; do
    for top in "$dir/$sub"/*; do
      [ -e "$top" ] || continue
      # Branches are <type>/<name> (ADR 0034), so a branch's folder is two levels down; a file above it is warned about alone.
      [ -d "$top" ] || { misfiled+="$sub/${top##*/} "; continue; }
      for b in "$top"/*; do
        [ -e "$b" ] || continue
        [ -d "$b" ] || { misfiled+="$sub/${top##*/}/${b##*/} "; continue; }
        alive "${top##*/}/${b##*/}" || { rm -rf "$b" && gone+="$sub/${top##*/}/${b##*/} "; }
      done
      rmdir "$top" 2>/dev/null
    done
  done
fi
[ -n "$gone" ] && warn+="Deleted, their branch gone (checkpoint Skill): ${gone% }."$'\n'
[ -n "$misfiled" ] && warn+="WARNING: not under a <type>/<name> branch folder, so never deleted: ${misfiled% }. Move or delete each (checkpoint Skill)."$'\n'
owed=$(triage)
size=$((${#warn} + ${#owed} + ${#body} + 2))
[ $size -gt $((cap - 1000)) ] && warn+="WARNING: this hook output is $size chars; above $cap Claude Code shows only a 2,000-char preview."$'\n'

# The triage line goes first, before the warnings, whose length grows with the plans: a 2,000-char preview still shows it.
printf '%s\n%s%s\n' "$owed" "$warn" "$body"
