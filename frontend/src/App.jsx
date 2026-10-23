import { useEffect, useState } from "react";
import UploadForm from "./components/UploadForm.jsx";
import EvidenceList from "./components/EvidenceList.jsx";
import CasePanel from "./components/CasePanel.jsx";
import Login from "./components/Login.jsx";
import ChangePassword from "./components/ChangePassword.jsx";
import { fetchCaseEvidence, fetchCases, fetchEvidence, logout, setUnauthorizedHandler } from "./api.js";

/**
 * App = the ROOT COMPONENT. A component is a JavaScript function that returns UI (JSX).
 * App owns the shared data, because several children need it:
 *   - CasePanel creates/selects cases and changes their status
 *   - UploadForm uploads INTO the selected case, then says "a new file was uploaded"
 *   - EvidenceList displays the evidence of the selected case (or all of it)
 * This pattern is called "lifting state up": shared data lives in the nearest common parent.
 *
 * Slice 7: until someone logs in, App shows only the Login form ("conditional rendering").
 */
export default function App() {
  // useState gives a component MEMORY. It returns [currentValue, functionToChangeIt].
  // When the setter is called, React re-renders the component with the new value.
  const [user, setUser] = useState(null);            // logged-in username, or null
  const [loginMessage, setLoginMessage] = useState("");
  const [evidence, setEvidence] = useState([]);
  const [cases, setCases] = useState([]);
  const [selectedCaseId, setSelectedCaseId] = useState(null); // null = "All evidence"
  const [loadError, setLoadError] = useState("");
  const [showPasswordForm, setShowPasswordForm] = useState(false);
  const [notice, setNotice] = useState(""); // short success message, e.g. "Password changed"

  const selectedCase = cases.find((c) => c.id === selectedCaseId) || null;

  // If ANY call gets 401 (e.g. the backend restarted with a new password), go back to the login form.
  useEffect(() => {
    setUnauthorizedHandler(() => {
      setUser(null);
      setLoginMessage("Your login is no longer valid. Please log in again.");
    });
  }, []);

  // Ask the backend for the evidence to show: one case's, or everything.
  async function loadEvidence() {
    try {
      const data = selectedCaseId === null ? await fetchEvidence() : await fetchCaseEvidence(selectedCaseId);
      setEvidence(data);
      setLoadError("");
    } catch (err) {
      setLoadError(err.message === "Failed to fetch"
        ? "Cannot reach the backend. Is Spring Boot running on port 8080?"
        : err.message);
    }
  }

  async function loadCases() {
    try {
      setCases(await fetchCases());
    } catch (err) {
      setLoadError("Cannot reach the backend. Is Spring Boot running on port 8080?");
    }
  }

  // After an upload, BOTH change: a new evidence row, and the case's evidenceCount.
  function refreshAll() {
    loadEvidence();
    loadCases();
  }

  function handleLogout() {
    logout();
    setUser(null);
    setLoginMessage("You have logged out.");
    setEvidence([]);
    setCases([]);
    setSelectedCaseId(null);
  }

  // Load the cases once someone is logged in. [user] = re-run whenever the user changes.
  // (Hooks must always run in the same order, so we check "user" INSIDE the effect, not around it.)
  useEffect(() => {
    if (user) loadCases();
  }, [user]);

  // DEPENDENCY ARRAY: re-run when the selected case OR the user changes.
  useEffect(() => {
    if (user) loadEvidence();
  }, [selectedCaseId, user]);

  return (
    <div className="page">
      <header className="header">
        <div className="brand">
          {/* aria-hidden: screen readers skip this decorative "T" square */}
          <div className="logo" aria-hidden="true">T</div>
          <div>
            <h1>Truvira AI</h1>
            <p className="tagline">Truth Verified.</p>
          </div>
        </div>
        {user && (
          <div className="user-bar">
            <span>Logged in as</span> <strong>{user}</strong>
            <button className="small" onClick={() => setShowPasswordForm(true)}>Change password</button>
            <button className="small" onClick={handleLogout}>Log out</button>
          </div>
        )}
      </header>

      <main>
        {/* CONDITIONAL RENDERING: condition ? (show this) : (show that) */}
        {!user ? (
          <div className="auth-wrap">
            <Login onLogin={(name) => { setUser(name); setLoginMessage(""); }} message={loginMessage} />
          </div>
        ) : (
          <>
            {notice && <p className="notice">{notice}</p>}
            {showPasswordForm && (
              <ChangePassword onDone={(message) => { setShowPasswordForm(false); setNotice(message); }} />
            )}

            {/* Two columns on wide screens (see .workspace in App.css), stacked on phones. */}
            <div className="workspace">
              <CasePanel
                cases={cases}
                selectedCaseId={selectedCaseId}
                onSelect={setSelectedCaseId}
                onChanged={loadCases}
              />

              {/* We pass functions and data as PROPS (inputs to a component). */}
              <UploadForm selectedCase={selectedCase} onUploaded={refreshAll} />
            </div>

            {loadError && <p className="error">{loadError}</p>}

            <EvidenceList items={evidence} cases={cases} selectedCase={selectedCase} onChanged={refreshAll} />
          </>
        )}
      </main>
    </div>
  );
}
