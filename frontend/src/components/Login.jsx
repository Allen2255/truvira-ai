import { useState } from "react";
import { login } from "../api.js";

/**
 * Login: shown instead of the app until the user is logged in.
 * onLogin(username) tells App "we're in".
 */
export default function Login({ onLogin, message }) {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    try {
      const me = await login(username, password);
      setPassword(""); // don't keep the password in this component's memory longer than needed
      onLogin(me.username);
    } catch (err) {
      setError(err.message === "Failed to fetch"
        ? "Cannot reach the backend. Is Spring Boot running on port 8080?"
        : "Wrong username or password.");
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="card login">
      <h2>Log in</h2>
      {message && <p className="hint">{message}</p>}
      <form onSubmit={handleSubmit}>
        <label>
          Username
          {/* autoComplete helps password managers fill these in correctly */}
          <input value={username} onChange={(e) => setUsername(e.target.value)} autoComplete="username" />
        </label>
        <label>
          Password
          {/* type="password" hides the characters as you type */}
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)}
                 autoComplete="current-password" />
        </label>
        <button type="submit" disabled={busy || !username || !password}>{busy ? "Checking..." : "Log in"}</button>
      </form>
      {error && <p className="error">{error}</p>}
    </section>
  );
}
