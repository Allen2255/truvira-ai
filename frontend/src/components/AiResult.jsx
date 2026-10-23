import { useState } from "react";
import { runDetection } from "../api.js";

/**
 * Turns the score (0..1) into careful words. Three bands, with a wide "Inconclusive" middle,
 * because a score near 50% means the model itself is unsure.
 * Exported so EvidenceList can reuse the same wording in the table.
 */
export function describeScore(score) {
  if (score < 0.3) return { text: "Low likelihood of AI generation", level: "low" };
  if (score <= 0.7) return { text: "Inconclusive", level: "mid" };
  return { text: "High likelihood of AI generation", level: "high" };
}

/**
 * AiResult: shows the detector's answer for ONE evidence record, plus a button to (re)run it.
 * Props: item = the evidence record, onUpdated = tell the parent the record changed.
 */
export default function AiResult({ item, onUpdated }) {
  const [running, setRunning] = useState(false);
  const [error, setError] = useState("");

  async function handleRun() {
    setRunning(true);
    setError("");
    try {
      const updated = await runDetection(item.id);
      onUpdated(updated); // give the parent the fresh record
    } catch (err) {
      setError(err.message);
    } finally {
      setRunning(false);
    }
  }

  const runButton = (
    <button className="small" onClick={handleRun} disabled={running}>
      {running ? "Checking..." : item.aiStatus ? "Run AI check again" : "Run AI check"}
    </button>
  );

  // Never checked (uploaded before Slice 5), or the detector was off / failed.
  if (item.aiStatus !== "COMPLETED") {
    const reason = {
      UNAVAILABLE: "AI check not available: the detector service was not running.",
      FAILED: "The AI detector could not analyse this image.",
    }[item.aiStatus] ?? "AI check has not been run for this image."; // ?? = fallback when the lookup gives undefined
    return (
      <div className="ai-result section">
        <h3>AI-generation check</h3>
        <p className="hint">{reason}</p>
        {runButton}
        {error && <p className="error">{error}</p>}
      </div>
    );
  }

  const percent = Math.round(item.aiScore * 100);
  const { text, level } = describeScore(item.aiScore); // "destructuring": pull two fields out of the object

  return (
    <div className="ai-result section">
      <h3>AI-generation check</h3>
      <p>
        <strong>{percent}%</strong> estimated probability of AI generation{" "}
        <span className={`pill pill-${level}`}>{text}</span>
      </p>
      {/* A simple bar: the inner div's width is the percentage (an inline style object in JSX). */}
      <div className="ai-bar"><div className={`ai-fill ai-fill-${level}`} style={{ width: `${percent}%` }} /></div>
      <p className="hint">
        Model: {item.aiModel}, checked {new Date(item.aiCheckedAt).toLocaleString()}.
        This is a model's estimate, <strong>not proof</strong>. Detectors make mistakes, especially on
        compressed images (e.g. sent through WhatsApp) and on images from newer AI generators.
        A human reviewer must weigh it with the other evidence.
      </p>
      {runButton}
      {error && <p className="error">{error}</p>}
    </div>
  );
}
