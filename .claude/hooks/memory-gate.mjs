// PreToolUse hook on Write and Edit: the first write to an agent memory file in a session is
// denied with a question, and the same call sent again goes through (ADR 0066).
// A hook's added context reaches the model only beside the tool's result, after the write,
// so a deny is the one answer that arrives before anything is written.
import fs from "node:fs";
import os from "node:os";
import path from "node:path";

const keepDays = 2;

let input;
try {
  input = JSON.parse(fs.readFileSync(0, "utf8"));
} catch {
  process.exit(0);
}
// Normalized, so a path through `..` that reaches a memory is still matched as one.
const file = path.posix.normalize(String(input?.tool_input?.file_path ?? "").replaceAll("\\", "/"));
// The index only points at memories; asking about it would ask twice for one memory.
const memory = /\/\.claude\/projects\/[^/]+\/memory\/(?!MEMORY\.md$)[^/]+$/i.test(file);
const session = String(input?.session_id ?? "").replace(/[^\w-]/g, "");
if (!memory || !session) process.exit(0);

const stateDir = process.env.CLAUDE_MEMORY_GATE_DIR ?? path.join(os.tmpdir(), "claude-memory-gate");
const state = path.join(stateDir, session);
// One file spelled in another case is still one file on Windows.
const key = file.toLowerCase();
const asked = fs.existsSync(state) ? fs.readFileSync(state, "utf8").split("\n") : [];
if (asked.includes(key)) process.exit(0);

fs.mkdirSync(stateDir, { recursive: true });
const cutoff = Date.now() - keepDays * 86_400_000;
// A crash here would let the write through unasked: a cleanup that fails is skipped.
for (const name of fs.readdirSync(stateDir)) {
  try {
    const p = path.join(stateDir, name);
    if (fs.statSync(p).mtimeMs < cutoff) fs.rmSync(p, { force: true });
  } catch {}
}
fs.appendFileSync(state, key + "\n");

const reason =
  `Agent memory holds only what is personal to the owner or about this one machine (ADR 0066). ` +
  `Before writing ${path.basename(file)}: would it serve a clone of the repository or another person? ` +
  `Then it goes to the repository (CLAUDE.md, a rule, a Skill, TOOLING.md, an ADR or an issue), not here. ` +
  `Does it serve nobody? Then do not write it. Personal or about this machine? Send the same call again; it goes through.`;
process.stdout.write(JSON.stringify({
  hookSpecificOutput: { hookEventName: "PreToolUse", permissionDecision: "deny", permissionDecisionReason: reason },
}));
