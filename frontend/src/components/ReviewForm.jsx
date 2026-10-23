import { useEffect, useState } from "react";
import { saveReview } from "../api.js";

// Same names as the backend's ReviewConclusion enum, with readable labels.
// Exported so the table can show the same words.
export const CONCLUSION_LABELS = {
  LIKELY_AUTHENTIC: "Likely authentic",
  LIKELY_AI_GENERATED: "Likely AI-generated",
  LIKELY_MANIPULATED: "Likely manipulated",
  INCONCLUSIVE: "Inconclusive",
};

/**
 * ReviewForm: the HUMAN reviewer writes their conclusion for one piece of evidence.
 * The AI score is shown above this form as a hint; the decision here is the person's own.
 */
export default function ReviewForm({ item, locked, onSaved }) {
  // Start the boxes with the existing review (if any). "|| ''" turns null into an empty box.
  const [conclusion, setConclusion] = useState(item.reviewConclusion || "");
  const [note, setNote] = useState(item.reviewNote || "");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  // When the user opens a DIFFERENT evidence item, refill the boxes from that item.
  useEffect(() => {
    setConclusion(item.reviewConclusion || "");
    setNote(item.reviewNote || "");
    setError("");
  }, [item.id]);

  async function handleSubmit(event) {
    event.preventDefault(); // stop the page reload a <form> normally does
    setSaving(true);
    setError("");
    try {
      // conclusion "" -> null, so the backend's @NotNull gives a clear "Choose a conclusion" message
      // Slice 7: no reviewer name sent: the backend takes it from the login.
      const updated = await saveReview(item.id, { conclusion: conclusion || null, note });
      onSaved(updated);
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <form className="review-form section" onSubmit={handleSubmit}>
      <h3>Human review</h3>
      {item.reviewedAt && (
        <p className="hint">
          Last review: <strong>{CONCLUSION_LABELS[item.reviewConclusion]}</strong> by {item.reviewedBy},{" "}
          {new Date(item.reviewedAt).toLocaleString()}
        </p>
      )}
      {locked && <p className="error">This case is closed. Reopen it to change the review.</p>}

      {/* fieldset disabled = disables every input inside it at once */}
      <fieldset disabled={locked || saving}>
        <label>
          Conclusion
          <select value={conclusion} onChange={(e) => setConclusion(e.target.value)}>
            <option value="">Choose...</option>
            {Object.entries(CONCLUSION_LABELS).map(([value, label]) => (
              <option key={value} value={value}>{label}</option>
            ))}
          </select>
        </label>
        <label>
          Reasoning
          <textarea
            rows={3}
            maxLength={2000}
            placeholder="What did you base this on? e.g. AI score 98%, no EXIF, lighting inconsistent on the face"
            value={note}
            onChange={(e) => setNote(e.target.value)}
          />
        </label>
        <p className="hint">Saved under your login name.</p>
        <button type="submit">{saving ? "Saving..." : "Save review"}</button>
      </fieldset>
      {error && <p className="error">{error}</p>}
    </form>
  );
}
