import { useState } from "react";
import { changePassword } from "../api.js";

/**
 * ChangePassword: the logged-in user sets their OWN new password.
 * They type it here themselves, so it never appears in code, logs or chat.
 */
export default function ChangePassword({ onDone }) {
  const [current, setCurrent] = useState("");
  const [next, setNext] = useState("");
  const [repeat, setRepeat] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError("");
    // Checked in the browser for a quick message; the BACKEND checks the length and the current password too.
    if (next !== repeat) {
      setError("The two new passwords don't match.");
      return;
    }
    setBusy(true);
    try {
      await changePassword(current, next);
      setCurrent("");
      setNext("");
      setRepeat("");
      onDone("Password changed. Use the new one next time you log in.");
    } catch (err) {
      setError(err.message); // e.g. "Current password is wrong" or "New password must be 8 to 72 characters"
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="card login">
      <h2>Change password</h2>
      <form onSubmit={handleSubmit}>
        <label>
          Current password
          <input type="password" value={current} onChange={(e) => setCurrent(e.target.value)}
                 autoComplete="current-password" />
        </label>
        <label>
          New password (at least 8 characters)
          {/* autoComplete="new-password" lets password managers suggest a strong one */}
          <input type="password" value={next} onChange={(e) => setNext(e.target.value)}
                 autoComplete="new-password" />
        </label>
        <label>
          New password again
          <input type="password" value={repeat} onChange={(e) => setRepeat(e.target.value)}
                 autoComplete="new-password" />
        </label>
        <div className="form-actions">
          <button type="submit" disabled={busy || !current || !next || !repeat}>
            {busy ? "Saving..." : "Save new password"}
          </button>
          <button type="button" className="small" onClick={() => onDone("")}>Cancel</button>
        </div>
      </form>
      {error && <p className="error">{error}</p>}
    </section>
  );
}
