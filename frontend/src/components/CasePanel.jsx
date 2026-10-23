import { useState } from "react";
import { createCase, downloadCaseReport, updateCaseStatus } from "../api.js";

// The three values of the backend's CaseStatus enum, with readable labels.
// An object used as a "lookup table": STATUS_LABELS["UNDER_REVIEW"] -> "Under review".
const STATUS_LABELS = {
  OPEN: "Open",
  UNDER_REVIEW: "Under review",
  CLOSED: "Closed",
};

/**
 * CasePanel: create a case, pick which case you're working on, change its status.
 * It does NOT own the list of cases: App does (lifting state up), because UploadForm and
 * EvidenceList also need to know the selected case. This component reports changes via props.
 */
export default function CasePanel({ cases, selectedCaseId, onSelect, onChanged }) {
  // "Controlled inputs": the input's value lives in state, and every keystroke updates it.
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [error, setError] = useState("");
  const [saving, setSaving] = useState(false);

  // .find() returns the first item that matches, or undefined.
  const selected = cases.find((c) => c.id === selectedCaseId);

  async function handleCreate(event) {
    // A <form> submit normally RELOADS the page. preventDefault() stops that.
    event.preventDefault();
    setSaving(true);
    setError("");
    try {
      const created = await createCase(title, description);
      setTitle("");
      setDescription("");
      onChanged();          // ask App to reload the case list
      onSelect(created.id); // jump straight into the new case
    } catch (err) {
      setError(err.message); // e.g. "Case title is required" from @NotBlank on the backend
    } finally {
      setSaving(false);
    }
  }

  // Slice 6: save the case's XML report as a file.
  async function handleDownload() {
    setError("");
    try {
      await downloadCaseReport(selected.id);
    } catch (err) {
      setError(err.message);
    }
  }

  async function handleStatusChange(event) {
    setError("");
    try {
      await updateCaseStatus(selected.id, event.target.value);
      onChanged();
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <section className="card">
      <h2>Investigation cases</h2>

      <form className="case-form" onSubmit={handleCreate}>
        <input
          type="text"
          placeholder="New case title, e.g. Suspected fake profile photo"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          maxLength={200}
        />
        <input
          type="text"
          placeholder="Description (optional)"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          maxLength={2000}
        />
        {/* No "required" attribute on purpose: the BACKEND validates, so you can see its 400 message. */}
        <button type="submit" disabled={saving}>{saving ? "Creating..." : "Create case"}</button>
      </form>

      {error && <p className="error">{error}</p>}

      <ul className="case-list">
        {/* null = "no case selected" = show all evidence */}
        <li className={selectedCaseId === null ? "active" : ""} onClick={() => onSelect(null)}>
          <span>All evidence</span>
        </li>
        {cases.map((c) => (
          <li key={c.id} className={c.id === selectedCaseId ? "active" : ""} onClick={() => onSelect(c.id)}>
            {/* title= shows the full title on hover if it was cut short with "..." */}
            <span className="case-title" title={c.title}>#{c.id} {c.title}</span>
            <span className="case-meta">
              {c.evidenceCount} item{c.evidenceCount === 1 ? "" : "s"}
              {/* className built from data: "badge badge-OPEN", styled in App.css */}
              <span className={`badge badge-${c.status}`}>{STATUS_LABELS[c.status]}</span>
            </span>
          </li>
        ))}
      </ul>

      {selected && (
        <div className="case-detail section">
          <h3>Case #{selected.id}: {selected.title}</h3>
          {selected.description && <p className="hint">{selected.description}</p>}
          <div className="case-actions">
            <label>
              Status
              {/* A controlled <select>: its value comes from the backend data. */}
              <select value={selected.status} onChange={handleStatusChange}>
                {/* Object.entries turns {OPEN: "Open", ...} into [["OPEN","Open"], ...] */}
                {Object.entries(STATUS_LABELS).map(([value, label]) => (
                  <option key={value} value={value}>{label}</option>
                ))}
              </select>
            </label>
            <button className="small" onClick={handleDownload}>Download XML report</button>
          </div>
        </div>
      )}
    </section>
  );
}
