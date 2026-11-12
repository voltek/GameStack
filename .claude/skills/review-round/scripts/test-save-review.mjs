#!/usr/bin/env node
// Runs save-review.mjs against transcripts built here, one per shape it must accept or refuse.
// Run: node .claude/skills/review-round/scripts/test-save-review.mjs (also on CI, commits.yml).
import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import fs from "node:fs";
import os from "node:os";
import path from "node:path";

const script = path.join(import.meta.dirname, "save-review.mjs");
const SECRET = "finding text that a notice must never print";
const REVIEW = 'r1: `<details>` on one line & "quoted" \\n literal, ünïcode\n\n2. second finding';

const entry = (id, blocks, stop, extra = {}) => ({
  type: "assistant",
  version: "2.1.322",
  effort: "high",
  message: { id, model: "claude-opus-5-5", stop_reason: stop, content: blocks },
  ...extra,
});
const thinking = { type: "thinking", thinking: SECRET };
const text = (t) => ({ type: "text", text: t });

// A repository on branch docs/a-branch, whose reviews/docs/a-branch/ folder does not exist yet.
function run(entries, { id = "abc123", arg = id, raw, name = "r1.raw.txt", handoff = true, branch = "docs/a-branch", git = true } = {}) {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "save-review-"));
  const dir = path.join(root, "projects", "C--repo", "session-1", "subagents");
  fs.mkdirSync(dir, { recursive: true });
  const body = raw ?? entries.map((e) => JSON.stringify(e)).join("\n") + "\n";
  fs.writeFileSync(path.join(dir, `agent-${id}.jsonl`), body);
  const repo = path.join(root, "Repo");
  if (git) spawnSync("git", ["init", "-q", "-b", branch, repo]);
  else fs.mkdirSync(repo);
  // Unset, the handoff folder is ~/Documents/<main checkout's folder name>, as the hook reads it.
  const home = path.join(root, "home");
  // HOME is what the hook reads; on Windows os.homedir() reads USERPROFILE, set apart here.
  const env = { ...process.env, CLAUDE_CONFIG_DIR: root, HOME: home, USERPROFILE: path.join(root, "profile") };
  if (handoff) env.CLAUDE_HANDOFF_DIR = path.join(root, "handoff");
  else delete env.CLAUDE_HANDOFF_DIR;
  const out = path.join(handoff ? env.CLAUDE_HANDOFF_DIR : path.join(home, "Documents", "Repo"),
    "reviews", ...branch.split("/"), "r1.raw.txt");
  const r = spawnSync(process.execPath, [script, arg, name], { cwd: repo, env, encoding: "utf8" });
  const saved = fs.existsSync(out) ? fs.readFileSync(out, "utf8") : null;
  fs.rmSync(root, { recursive: true, force: true });
  return { ...r, saved };
}

const failsLoudly = (r, found) => {
  assert.equal(r.status, 1, r.stderr);
  assert.equal(r.saved, null);
  assert.match(r.stderr, /Expected: /);
  assert.match(r.stderr, new RegExp(`Found: .*${found}`));
  assert.match(r.stderr, /Claude Code: /);
  assert.ok(!r.stderr.includes(SECRET), "a notice prints shapes, never content");
};

const cases = {
  "saves the last message's text verbatim and prints the reviewer"() {
    const r = run([
      entry("m1", [thinking], "tool_use"),
      entry("m1", [{ type: "tool_use", name: "Bash", input: {} }], "tool_use"),
      { type: "user", message: { content: [text(SECRET)] } },
      entry("m2", [thinking], "end_turn"),
      entry("m2", [text(REVIEW)], "end_turn"),
    ]);
    assert.equal(r.status, 0, r.stderr);
    assert.equal(r.saved, REVIEW);
    assert.match(r.stdout, /^Reviewer: claude-opus-5-5, high$/m);
  },
  "ignores an earlier message's text"() {
    const r = run([entry("m1", [text(SECRET)], "end_turn"), entry("m2", [text(REVIEW)], "end_turn")]);
    assert.equal(r.status, 0, r.stderr);
    assert.equal(r.saved, REVIEW);
  },
  "joins the text blocks of the last message adding nothing between them"() {
    const r = run([entry("m2", [text("see `<det")], "end_turn"), entry("m2", [text("ails>`")], "end_turn")]);
    assert.equal(r.status, 0, r.stderr);
    assert.equal(r.saved, "see `<details>`");
  },
  "counts the saved text in code points"() {
    const r = run([entry("m2", [text("a😀b")], "end_turn")]);
    assert.match(r.stdout, /^Saved 3 characters /m);
  },
  "accepts the id with its agent- prefix"() {
    assert.equal(run([entry("m2", [text(REVIEW)], "end_turn")], { arg: "agent-abc123" }).status, 0);
  },
  "fails on a task id with no transcript, asking for the id before a repair"() {
    const r = run([entry("m2", [text(REVIEW)], "end_turn")], { arg: "missing" });
    failsLoudly(r, "0 matches");
    assert.match(r.stderr, /Check the id against the notification's task-id/);
  },
  "fails on a line that is not JSON"() {
    failsLoudly(run([], { raw: `${JSON.stringify(entry("m2", [text(SECRET)], "end_turn"))}\n{oops\n` }), "line 2");
  },
  "fails when no entry is from the assistant"() {
    failsLoudly(run([{ type: "user", message: { content: [text(SECRET)] } }]), "entry types");
  },
  "fails when the last message did not end its turn"() {
    const toolCall = { type: "tool_use", name: "Bash", input: {} };
    failsLoudly(run([entry("m2", [text(SECRET)], "tool_use"), entry("m2", [toolCall], "tool_use")]), "stop_reason: tool_use");
  },
  "saves a finished review whose entries carry stop_reason null (2.1.296)"() {
    const r = run([entry("m2", [thinking], null), entry("m2", [text(REVIEW)], null)]);
    assert.equal(r.status, 0, r.stderr);
    assert.equal(r.saved, REVIEW);
  },
  "saves under ~/Documents/<checkout> when CLAUDE_HANDOFF_DIR is unset"() {
    const r = run([entry("m2", [text(REVIEW)], "end_turn")], { handoff: false });
    assert.equal(r.status, 0, r.stderr);
    assert.equal(r.saved, REVIEW);
  },
  "refuses a path where the file name goes"() {
    const r = run([entry("m2", [text(REVIEW)], "end_turn")], { name: "elsewhere/r1.raw.txt" });
    assert.equal(r.status, 2);
    assert.equal(r.saved, null);
  },
  "refuses . and .. where the file name goes"() {
    for (const name of [".", ".."]) {
      const r = run([entry("m2", [text(REVIEW)], "end_turn")], { name });
      assert.equal(r.status, 2, `${name}: ${r.stderr}`);
    }
  },
  "exits 2, not a format change, when git cannot name the branch"() {
    const r = run([entry("m2", [text(REVIEW)], "end_turn")], { git: false, handoff: false });
    assert.equal(r.status, 2, r.stderr);
    assert.equal(r.saved, null);
  },
  "fails when the last message holds no text"() {
    failsLoudly(run([entry("m2", [thinking], "end_turn"), entry("m2", [text("  \n")], "end_turn")]), "blocks");
  },
};

let failed = 0;
for (const [name, test] of Object.entries(cases)) {
  try {
    test();
    console.log(`ok   ${name}`);
  } catch (error) {
    failed++;
    console.log(`FAIL ${name}\n     ${error.message.split("\n").join("\n     ")}`);
  }
}
console.log(`${Object.keys(cases).length - failed} passed, ${failed} failed`);
process.exitCode = failed ? 1 : 0;
