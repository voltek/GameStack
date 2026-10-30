#!/bin/sh
# The mechanical half of ADR 0034's message and branch rules, and ADR 0033's
# trailer forms; the judgement half stays in ship-a-branch § 2. Reports every
# failure, not only the first, then exits 1. Usage: the case at the end of this
# file; cases: test-rules.sh.

LC_ALL=C
export LC_ALL

TYPES='feat fix refactor docs chore test'
FORM="^($(printf '%s' "$TYPES" | tr ' ' '|'))(\([a-z]+\))?: [^ ]"
KNOWN_TRAILERS='drift bypassed co-authored-by claude-session signed-off-by'

# The closed scope list (ADR 0034). A new scope joins it in the commit that
# first uses it.
SCOPES='search      the Search feature
home        the Home feature
library     the Library feature
detail      the Detail feature
auth        Twitch auth: AuthRepository, AuthInterceptor
nav         navigation graphs, routes, the bottom bar
theme       ui/theme
core        core/ packages shared by two or more features
build       Gradle build files, dependency versions, the wrapper
governance  CLAUDE.md rules and bounds, ADRs, drift, the drift checklist
skills      implementation Skills: new-*, write-tests, verify-on-device, discovery-feature
ship        branch to merge: review rounds, PR template, commit messages, PR titles, branch names
gate        CI workflows
agent       .claude/ settings and agent hooks
repo        .gitattributes, .gitignore, git hooks
spec        PRODUCT.md, openspec/ and its generated .claude/skills/openspec-*
design      DESIGN.md and design/ mockups
tooling     the TOOLING documents
readme      README.md'

LIMIT=72
BODY_LIMIT=12
HOOK=0
MERGE=0
status=0

fail() {
    if [ -n "$GITHUB_ACTIONS" ]; then
        printf '::error::%s\n' "$1" >&2
    else
        printf '%s\n' "$1" >&2
    fi
    printf '  Saw: %s\n  Fix: %s\n' "$2" "$3" >&2
    status=1
}

report_each() {
    [ -z "$1" ] && return
    while IFS= read -r line; do fail "$2" "$line" "$3"; done <<LINES
$1
LINES
}

code_points() {
    printf '%s' "$1" | tr -d '\200-\277' | wc -c | tr -d ' '
}

print_scopes() {
    printf '  Scopes (scope optional only when repo-wide):\n' >&2
    printf '%s\n' "$SCOPES" | sed 's/^/    /' >&2
}

has_form() {
    printf '%s\n' "$1" | grep -Eq "$FORM"
}

exempt() {
    case $1 in
        'Revert "'* | 'Reapply "'* | 'Squashed commit of the following:') return 0 ;;
        'Merge '*) is_merge "$1" ;;
        *) return 1 ;;
    esac
}

# A Merge subject is git's only on a merge commit: one CI marks, one being made
# (MERGE_HEAD), or one being amended (HEAD is a merge with that subject).
is_merge() {
    [ "$MERGE" = 1 ] && return 0
    [ "$HOOK" = 1 ] || return 1
    git rev-parse -q --verify MERGE_HEAD >/dev/null && return 0
    git rev-parse -q --verify HEAD^2 >/dev/null && [ "$(git log -1 --format=%s HEAD)" = "$1" ]
}

# git strips comments only when it opened an editor, and runs commit hooks with
# GIT_EDITOR=: when it did not (githooks(5)). "auto" is read as "#", git's
# choice unless the message already opens a line with it.
strip_comments() {
    prefix=$(git config core.commentString || git config core.commentChar)
    awk -v p="$prefix" '
        BEGIN { if (p == "" || p == "auto") p = "#" }
        $0 == p " ------------------------ >8 ------------------------" { exit }
        index($0, p) != 1' "$1"
}

check_subject() {
    subject=$1
    if ! has_form "$subject"; then
        fail "Subject is not type(scope): Summary (ADR 0034)." "$subject" \
            "Write type(scope): Summary with a type from: $TYPES."
        print_scopes
        return
    fi

    scope=$(printf '%s\n' "$subject" | sed -n 's/^[a-z]*(\([a-z]*\)):.*/\1/p')
    if [ -n "$scope" ] && ! printf '%s\n' "$SCOPES" | grep -q "^$scope "; then
        fail "Scope '$scope' is not in the closed list (ADR 0034)." "$subject" \
            "Pick a scope below, or add one to SCOPES in .githooks/rules.sh in this same commit."
        print_scopes
    fi

    summary=${subject#*: }
    case $summary in
        [A-Z]*) ;;
        *) fail "The summary opens with a capitalised verb (ADR 0034)." "$summary" \
            "Capitalise the first letter; identifiers later in the line keep their spelling." ;;
    esac
    case $summary in
        *.) fail "A subject has no trailing period (ADR 0034)." "$subject" "Drop the final period." ;;
    esac

    length=$(code_points "$subject")
    if [ "$length" -gt "$LIMIT" ]; then
        fail "Subject is $length characters, the limit is $LIMIT, counted in code points (ADR 0034)." \
            "$subject" "If it lists two changes, split the commit; otherwise shorten it."
    fi
}

# Trailing whitespace goes as git's cleanup removes it, so a line of spaces is
# blank. An exempt subject skips the form checks; its trailers are still read.
check_message() {
    if [ "$HOOK" = 1 ] && [ "$GIT_EDITOR" != : ]; then
        message=$(strip_comments "$1" | sed -e 's/[[:space:]]*$//' -e '/./,$!d')
    else
        message=$(sed -e 's/[[:space:]]*$//' -e '/./,$!d' "$1")
    fi
    [ -z "$message" ] && return
    exempt "$(printf '%s\n' "$message" | sed -n 1p)" || check_form "$message"
    check_trailers "$message"
}

check_form() {
    message=$1
    check_subject "$(printf '%s\n' "$message" | sed -n 1p)"

    second=$(printf '%s\n' "$message" | sed -n 2p)
    if [ -n "$second" ]; then
        fail "The subject is one line, followed by a blank line." "$second" \
            "Insert a blank line after the subject."
    fi

    # Non-blank body lines. In a final paragraph shaped as trailers, only lines
    # with a known key, and their continuations, are left out of the count.
    body_lines=$(printf '%s\n' "$message" | awk -v known=" $KNOWN_TRAILERS " '
        NR > 1 { line[++n] = $0 }
        END {
            last = n; while (last > 0 && line[last] == "") last--
            first = last; while (first > 0 && line[first] != "") first--
            trailers = last > first
            for (i = first + 1; i <= last; i++)
                if (line[i] !~ /^[A-Za-z][A-Za-z0-9-]*: / && line[i] !~ /^[ \t]/) trailers = 0
            end = trailers ? first : last
            for (i = 1; i <= end; i++) if (line[i] != "") count++
            for (i = first + 1; trailers && i <= last; i++) {
                if (line[i] !~ /^[ \t]/) {
                    key = tolower(line[i]); sub(/:.*/, "", key)
                    known_key = index(known, " " key " ") > 0
                }
                if (!known_key) count++
            }
            print count + 0
        }')
    if [ "$body_lines" -gt "$BODY_LIMIT" ]; then
        fail "Body is $body_lines lines, the limit is $BODY_LIMIT, trailers and blank lines excluded (CLAUDE.md § Git workflow)." \
            "$body_lines non-blank lines" \
            "Keep the diagnosis the diff cannot show; point at a document instead of restating it."
    fi
}

check_trailers() {
    message=$1
    # Every line, not only trailers: ADR 0033 reads with git log --grep='^Drift:'.
    report_each "$(printf '%s\n' "$message" | grep -i '^drift:' |
        grep -Ev '^Drift: (code fixed|doc updated|both)$')" \
        "A Drift: trailer holds one of three values, spelled as shown (ADR 0033)." \
        "Write Drift: code fixed, Drift: doc updated or Drift: both."
    report_each "$(printf '%s\n' "$message" | grep -i '^bypassed:' |
        grep -v '^Bypassed: enforce_admins — .')" \
        "A Bypassed: trailer reads enforce_admins — <reason> (ADR 0033)." \
        "Write Bypassed: enforce_admins — <why the emergency exit was needed>."
}

check_title() {
    exempt "$1" && return
    check_subject "$1"
    if printf '%s\n' "$1" | grep -Eiq '(^|[^[:alnum:]_])and([^[:alnum:]_]|$)'; then
        fail "A PR title states one claim, so it has no \"and\" (ADR 0034)." "$1" \
            "Split the PR, or rephrase if the \"and\" belongs to one name: gh pr edit --title."
    fi
}

check_branch() {
    name=$1
    [ $# -ge 2 ] && exempt "$2" && return
    # GitHub's Revert button names its branch revert-<PR number>-<head branch>.
    reverted=$(printf '%s\n' "$name" | sed -E 's/^revert-[0-9]+-//')
    prefixes=$(printf '%s\n' $TYPES | sed 's/^feat$/feature/' | paste -sd ' ' -)
    prefix_alt=$(printf '%s' "$prefixes" | tr ' ' '|')
    if ! printf '%s\n' "$reverted" | grep -Eq "^($prefix_alt)/[a-z0-9]+(-[a-z0-9]+)*\$"; then
        fail "A branch is {type}/{kebab-name} (ADR 0034)." "$name" \
            "Rename it to one of $prefixes, a slash, then lowercase-kebab: git branch -m <new-name>."
        return
    fi
    # A malformed title is the title check's to report, not a prefix mismatch.
    [ $# -lt 2 ] && return
    has_form "$2" || return
    type=${2%%[(:]*}
    [ "$type" = feat ] && type=feature
    if [ "${reverted%%/*}" != "$type" ]; then
        fail "A branch's prefix is its PR title's type, feature/ for feat (ADR 0034)." "$name, title type ${2%%[(:]*}" \
            "Retitle the PR, or open a new PR from a renamed branch: renaming an open PR's head closes it."
    fi
}

case $1 in
    commit-msg) HOOK=1; check_message "$2" ;;
    message) [ "$3" = merge ] && MERGE=1; check_message "$2" ;;
    title) check_title "$2" ;;
    branch) shift; check_branch "$@" ;;
    *) printf 'Usage: rules.sh commit-msg <file> | message <file> [merge] | title <title> | branch <name> [<title>]\n' >&2; exit 2 ;;
esac
exit $status
