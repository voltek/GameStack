#!/usr/bin/env node
// Saves a /code-review subagent's final text verbatim, from its transcript (review-round § The sequence).
// The transcript format is undocumented: every unexpected shape fails loudly with a repair notice,
// because a record saved wrong in silence is worse than none.
import fs from "node:fs";
import os from "node:os";
import path from "node:path";

class Unexpected extends Error {
  constructor(expected, found, version = "unknown", kind = "format") {
    super(expected);
    Object.assign(this, { expected, found, version, kind });
  }
}

// Walks every project on purpose: it costs milliseconds, and the project folder's name follows an
// undocumented rule that a worktree or a subfolder launch also breaks (#189 weighs the session's folder).
function findTranscript(root, id) {
  const projects = path.join(root, "projects");
  const name = `agent-${id}.jsonl`;
  const hits = [];
  let sessions = 0;
  for (const project of fs.existsSync(projects) ? fs.readdirSync(projects) : []) {
    const projectDir = path.join(projects, project);
    if (!fs.statSync(projectDir).isDirectory()) continue;
    for (const session of fs.readdirSync(projectDir)) {
      const file = path.join(projectDir, session, "subagents", name);
      if (fs.existsSync(path.join(projectDir, session, "subagents"))) sessions++;
      if (fs.existsSync(file)) hits.push(file);
    }
  }
  if (hits.length === 1) return hits[0];
  throw new Unexpected(
    `exactly one ${name} under ${projects}/<project>/<session>/subagents/`,
    `${hits.length} matches across ${sessions} session folders with subagents`,
    "unknown",
    hits.length === 0 ? "not-found" : "format",
  );
}

function shape(entry) {
  const blocks = Array.isArray(entry?.message?.content) ? entry.message.content.map((b) => b?.type) : "no content array";
  return `{type: ${entry?.type}, stop_reason: ${entry?.message?.stop_reason}, blocks: ${JSON.stringify(blocks)}}`;
}

function extract(lines) {
  const entries = lines.map((line, i) => {
    try {
      return JSON.parse(line);
    } catch {
      throw new Unexpected("one JSON object per line", `line ${i + 1} is not JSON`);
    }
  });
  const assistant = entries.filter((e) => e?.type === "assistant");
  const version = assistant.at(-1)?.version ?? entries.find((e) => e?.version)?.version;
  if (assistant.length === 0) {
    const types = [...new Set(entries.map((e) => e?.type))];
    throw new Unexpected("entries with type \"assistant\"", `entry types ${JSON.stringify(types)}`, version);
  }
  // One API message is written as several entries, one per content block, sharing message.id.
  const lastId = assistant.at(-1).message?.id;
  const last = lastId === undefined ? [assistant.at(-1)] : assistant.filter((e) => e.message?.id === lastId);
  const final = last.at(-1);
  // 2.1.296 wrote stop_reason null on every entry; later versions write end_turn on a finished turn.
  const calledATool = last.some((e) => e.message?.content?.some?.((b) => b?.type === "tool_use"));
  if (calledATool || ![null, undefined, "end_turn"].includes(final.message?.stop_reason)) {
    throw new Unexpected(
      "a last assistant message with no tool_use block and stop_reason \"end_turn\" or null",
      last.map(shape).join(", "),
      version,
    );
  }
  const text = last
    .flatMap((e) => (Array.isArray(e.message?.content) ? e.message.content : []))
    .filter((b) => b?.type === "text" && typeof b.text === "string")
    .map((b) => b.text)
    .join("");
  if (text.trim() === "") {
    throw new Unexpected("text blocks with content in the last assistant message", last.map(shape).join(", "), version);
  }
  return { text, model: final.message?.model ?? "unknown", effort: final.effort ?? "unknown" };
}

function notice(error, script) {
  const advice =
    error.kind === "not-found"
      ? [
          "Check the id against the notification's task-id: a wrong id is not a format change.",
          `If it matches, the folder layout changed: repair findTranscript in ${script} as below.`,
        ]
      : ["If the review had not finished, wait for its notification and run this again."];
  return [
    "save-review: the review text was not saved.",
    `Expected: ${error.expected}`,
    `Found: ${error.found}`,
    `Claude Code: ${error.version}`,
    ...advice,
    `Repair, on a branch of its own: change the selector in ${script} that expects the above, add a`,
    "fixture of the new shape to test-save-review.mjs, and see that test fail before the fix.",
    "Now: save the record from the notification (review-round references/extractor-failed.md).",
  ].join("\n");
}

function main([taskId, out]) {
  if (!taskId || !out) {
    console.error("Usage: save-review.mjs <task-id> <output file>");
    return 2;
  }
  const root = process.env.CLAUDE_CONFIG_DIR || path.join(os.homedir(), ".claude");
  try {
    const transcript = findTranscript(root, taskId.replace(/^agent-/, ""));
    const lines = fs.readFileSync(transcript, "utf8").split("\n").filter((l) => l.trim() !== "");
    const { text, model, effort } = extract(lines);
    fs.writeFileSync(out, text);
    console.log(`Reviewer: ${model}, ${effort}`);
    console.log(`Saved ${[...text].length} characters to ${out}`);
    return 0;
  } catch (error) {
    if (!(error instanceof Unexpected)) throw error;
    console.error(notice(error, path.basename(import.meta.filename)));
    return 1;
  }
}

process.exitCode = main(process.argv.slice(2));
