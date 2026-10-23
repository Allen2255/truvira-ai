import { useState } from "react";
import { runDetection } from "../api.js";
import AiResult, { describeScore } from "./AiResult.jsx";
import MetadataDetails from "./MetadataDetails.jsx";
import ReviewForm, { CONCLUSION_LABELS } from "./ReviewForm.jsx";

/**
 * EvidenceList: a table of saved evidence (all, or one case's).
 * Click a row to open the evidence DETAIL panel under the table (Slice 6):
 * metadata, AI check and the human review form for that one item.
 *
 * Its own state is only "UI state": which row is open and which AI check is running.
 * The evidence data itself still comes from the parent (App) as props.
 */
export default function EvidenceList({ items, cases, selectedCase, onChanged }) {
  // Slice 5: which row's AI check is running right now (so only that button says "...").
  const [runningId, setRunningId] = useState(null);
  // Slice 6: which evidence row is open in the detail panel (null = none).
  const [openId, setOpenId] = useState(null);

  // Find the open item in the CURRENT list, so after a reload it shows the fresh data.
  const openItem = items.find((i) => i.id === openId);
  // Evidence in a CLOSED case is frozen (the backend refuses changes too).
  const openItemLocked = cases.find((c) => c.id === openItem?.caseId)?.status === "CLOSED";

  async function handleRun(event, id) {
    // The button sits INSIDE a clickable row. stopPropagation() stops the click from also
    // reaching the row (which would open the detail panel). Events "bubble up" to parents.
    event.stopPropagation();
    setRunningId(id);
    try {
      await runDetection(id);
      onChanged(); // reload the table from the backend
    } catch (err) {
      alert(err.message); // simple pop-up; fine for a table action
    } finally {
      setRunningId(null);
    }
  }

  // One table cell for the AI result: "93% high" or a Run button.
  function aiCell(item) {
    if (item.aiStatus === "COMPLETED") {
      const { level } = describeScore(item.aiScore);
      return <span className={`pill pill-${level}`} title={item.aiModel}>{Math.round(item.aiScore * 100)}% {level}</span>;
    }
    return (
      <button className="small" onClick={(e) => handleRun(e, item.id)} disabled={runningId !== null}>
        {runningId === item.id ? "..." : item.aiStatus === "UNAVAILABLE" ? "Retry" : "Run"}
      </button>
    );
  }

  // Slice 4: the heading says WHICH evidence we're looking at.
  const scope = selectedCase ? `Case #${selectedCase.id}: ${selectedCase.title}` : "all cases";

  if (items.length === 0) {
    return (
      <section className="card">
        <h2>Evidence log, {scope}</h2>
        <p className="hint">Nothing uploaded yet.</p>
      </section>
    );
  }

  return (
    <>
      {/* <> </> is a FRAGMENT: it groups two sections without adding an extra <div> to the page. */}
      <section className="card">
        <div className="card-head">
          <h2>Evidence log, {scope} ({items.length})</h2>
          <span className="hint">Click a row to see details and write a review.</span>
        </div>
        {/* table-wrap: on a small screen the table scrolls sideways inside this box. */}
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Case</th>
                <th>File</th>
                {/* col-hash / col-exif are hidden on medium screens (App.css); the detail panel still shows them */}
                <th className="col-hash">SHA-256</th>
                <th className="col-exif">EXIF</th>
                <th>AI check</th>
                <th>Review</th>
                <th>Received</th>
              </tr>
            </thead>
            <tbody>
              {/* .map() turns each item into a table row.
                  "key" lets React track each row efficiently; it must be unique (the DB id). */}
              {items.map((item) => (
                <tr
                  key={item.id}
                  className={item.id === openId ? "row-open" : ""}
                  // Clicking the open row again closes it (toggle).
                  onClick={() => setOpenId(item.id === openId ? null : item.id)}
                >
                  <td className="num">{item.id}</td>
                  <td className="num">{item.caseId ? `#${item.caseId}` : "-"}</td>
                  {/* Long names are cut with "..." by CSS; hovering shows the full name. */}
                  <td className="file" title={item.originalFileName}>{item.originalFileName}</td>
                  {/* Show only the first 12 characters; hover (title) shows the full hash. */}
                  <td className="mono col-hash" title={item.sha256}>{item.sha256.slice(0, 12)}...</td>
                  <td className="hint col-exif">{exifSummary(item)}</td>
                  <td>{aiCell(item)}</td>
                  <td>
                    {item.reviewConclusion
                      ? <span className="pill pill-review">{CONCLUSION_LABELS[item.reviewConclusion]}</span>
                      : <span className="pill pill-neutral">Not reviewed</span>}
                  </td>
                  {/* Short date + time (e.g. "23/10/26, 11:05"); hover shows the full timestamp. */}
                  <td className="num" title={new Date(item.uploadedAt).toLocaleString()}>
                    {new Date(item.uploadedAt).toLocaleString([], { dateStyle: "short", timeStyle: "short" })}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      {openItem && (
        <section className="card">
          <div className="card-head">
            <h2>Evidence #{openItem.id}: {openItem.originalFileName}</h2>
            <button className="small" onClick={() => setOpenId(null)}>Close</button>
          </div>
          <p className="hint">
            Received {new Date(openItem.uploadedAt).toLocaleString()}
            {openItem.uploadedBy ? ` from ${openItem.uploadedBy}` : ""}
          </p>
          <p className="hint mono">SHA-256 {openItem.sha256}</p>

          {/* Two columns: facts on the left, AI hint + human decision on the right. */}
          <div className="detail-grid">
            <div>
              <MetadataDetails item={openItem} />
            </div>
            <div>
              {/* After a re-run or a saved review, reload everything so the table updates too. */}
              <AiResult item={openItem} onUpdated={onChanged} />
              <ReviewForm item={openItem} locked={openItemLocked} onSaved={onChanged} />
            </div>
          </div>
        </section>
      )}
    </>
  );
}

/**
 * Short text for the table cell. A plain helper function (not a component) because it returns
 * a string, not JSX. It lives outside the component so it isn't re-created on every render.
 */
function exifSummary(item) {
  if (item.exifFound == null) return "not checked";   // "== null" matches both null and undefined
  if (!item.exifFound) return "no EXIF";
  const camera = [item.cameraMake, item.cameraModel].filter(Boolean).join(" ");
  return camera || "EXIF (no camera tag)";
}
