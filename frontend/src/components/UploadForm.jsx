import { useEffect, useState } from "react";
import { uploadEvidence } from "../api.js";
import MetadataDetails from "./MetadataDetails.jsx";
import AiResult from "./AiResult.jsx";

/**
 * UploadForm: pick an image, preview it, send it to the backend, show the result.
 * "props" = the inputs a parent passes in. Here we receive { onUploaded } (a function)
 * and, since Slice 4, { selectedCase } (the case object to upload into, or null).
 */
export default function UploadForm({ selectedCase, onUploaded }) {
  // A closed case accepts no new evidence (the backend enforces this too; this just explains it early).
  const caseClosed = selectedCase?.status === "CLOSED"; // "?." = optional chaining: undefined if selectedCase is null
  // Each useState is one piece of memory for this component.
  const [file, setFile] = useState(null);          // the chosen File object
  const [previewUrl, setPreviewUrl] = useState(""); // temporary URL so we can show the image
  const [uploading, setUploading] = useState(false);
  const [result, setResult] = useState(null);       // the saved record from the backend
  const [error, setError] = useState("");

  // Create/clean up the preview URL whenever the chosen file changes.
  useEffect(() => {
    if (!file) {
      setPreviewUrl("");
      return;
    }
    // createObjectURL makes a temporary "blob:" address pointing at the file in memory.
    const url = URL.createObjectURL(file);
    setPreviewUrl(url);
    // The function we RETURN from useEffect is the cleanup: it runs when the file changes
    // again or the component disappears. It frees the memory.
    return () => URL.revokeObjectURL(url);
  }, [file]); // [file] = re-run this effect whenever "file" changes

  // Runs when the user picks a file in the <input type="file">.
  function handleFileChange(event) {
    // event.target.files is a list; we take the first.
    setFile(event.target.files[0] || null);
    setResult(null);
    setError("");
  }

  // Runs when the user clicks "Upload".
  async function handleUpload() {
    if (!file) return;
    setUploading(true);
    setError("");
    try {
      const saved = await uploadEvidence(file, selectedCase?.id); // calls the backend
      setResult(saved);                         // show the SHA-256 etc.
      onUploaded();                             // tell App to refresh the list
    } catch (err) {
      // Network down or backend returned an error message.
      setError(err.message === "Failed to fetch"
        ? "Cannot reach the backend. Is Spring Boot running on port 8080?"
        : err.message);
    } finally {
      // finally runs whether it worked or failed.
      setUploading(false);
    }
  }

  return (
    <section className="card">
      <h2>Upload evidence</h2>
      <p className="hint">
        {selectedCase
          ? <>Uploading into <strong>Case #{selectedCase.id}: {selectedCase.title}</strong></>
          : "Not assigned to a case. Select a case on the left to file evidence into it."}
      </p>
      {caseClosed && <p className="error">This case is closed. Reopen it to add evidence.</p>}

      {/* A <label> wrapped around a hidden file input: clicking anywhere on the label opens the file picker.
          accept= limits the picker; the backend STILL validates (never trust the browser). */}
      <label className="dropzone">
        <input type="file" accept="image/jpeg,image/png,image/webp" onChange={handleFileChange} />
        <strong>{file ? file.name : "Click to choose an image"}</strong>
        <span>JPEG, PNG or WEBP, max 10 MB</span>
      </label>

      <div className="upload-row">
        {/* "cond && <thing/>" renders the thing only when cond is truthy. */}
        {previewUrl && <img className="preview" src={previewUrl} alt="Selected evidence preview" />}
        <button onClick={handleUpload} disabled={!file || uploading || caseClosed}>
          {uploading ? "Uploading and analysing..." : "Upload & analyse"}
        </button>
      </div>

      {error && <p className="error">{error}</p>}

      {result && (
        <div className="section">
          <h3>Saved as evidence #{result.id}</h3>
          <dl>
            <dt>File</dt><dd>{result.originalFileName}</dd>
            <dt>Case</dt><dd>{result.caseId ? `#${result.caseId}` : "Unassigned"}</dd>
            <dt>Size</dt><dd>{result.fileSize} bytes</dd>
            <dt>SHA-256</dt><dd className="mono">{result.sha256}</dd>
            <dt>Received</dt><dd>{new Date(result.uploadedAt).toLocaleString()}</dd>
          </dl>
          {/* Slice 3: the same saved record also carries the extracted metadata. */}
          <MetadataDetails item={result} />
          {/* Slice 5: the detector's answer. Re-running it updates this result AND the table. */}
          <AiResult item={result} onUpdated={(updated) => { setResult(updated); onUploaded(); }} />
        </div>
      )}
    </section>
  );
}
