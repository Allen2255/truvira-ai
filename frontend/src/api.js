// api.js: ALL communication with the Spring Boot backend lives here.
// Keeping it in one file means components don't care HOW data is fetched (same idea as the
// service layer in the backend).

// The base address of our backend API. Each resource adds its own path (/evidence, /cases).
// import.meta.env.VITE_API_URL = a setting Vite reads from the environment (or a .env file) when it starts.
// Online we'll set it to the real server address; locally it's empty, so the default (port 8080) is used.
const API_ROOT = import.meta.env.VITE_API_URL || "http://localhost:8080/api";

// ---------------- LOGIN (Slice 7) ----------------
// The login lives ONLY in this variable (memory). Refreshing the page forgets it on purpose:
// we never save the password in the browser (localStorage could be read by other scripts).
let authHeader = null;
let currentUsername = null; // needed to rebuild the header after a password change
// App gives us a function to call when the backend says 401 (login wrong/expired) -> show the login form.
let onUnauthorized = () => {};

export function setUnauthorizedHandler(handler) {
  onUnauthorized = handler;
}

/**
 * Try a username + password. HTTP Basic = "Basic " + base64("username:password").
 * btoa() = base64-encode. Base64 is ENCODING, not encryption: anyone can decode it,
 * so online this must only travel over HTTPS.
 */
export async function login(username, password) {
  authHeader = "Basic " + btoa(`${username}:${password}`);
  try {
    const me = await request("/auth/me"); // 200 {"username": ...} = correct login
    currentUsername = me.username;
    return me;
  } catch (err) {
    authHeader = null; // wrong login: forget it
    throw err;
  }
}

export function logout() {
  authHeader = null;
  currentUsername = null;
}

/**
 * Change the logged-in user's own password (PUT /api/auth/password).
 * Afterwards the OLD password stops working, so we rebuild the login header with the new one
 * to stay logged in.
 */
export async function changePassword(currentPassword, newPassword) {
  const result = await request("/auth/password", jsonBody("PUT", { currentPassword, newPassword }));
  authHeader = "Basic " + btoa(`${currentUsername}:${newPassword}`);
  return result;
}

/** Headers every call needs: the login, plus any extra ones (e.g. Content-Type). */
function withAuth(headers = {}) {
  return authHeader ? { ...headers, Authorization: authHeader } : headers; // "..." copies the object
}

/**
 * Shared helper for every JSON call (Slice 4). It sends the request, reads the JSON answer,
 * and turns a backend error {"error": "..."} into a JavaScript Error the components can show.
 * "options" is the same object fetch() takes: { method, headers, body }.
 */
async function request(path, options = {}) {
  const response = await fetch(`${API_ROOT}${path}`, { ...options, headers: withAuth(options.headers) });
  // 401 on a normal call = we're not (or no longer) logged in. (Not for the login check itself.)
  if (response.status === 401 && path !== "/auth/me") {
    authHeader = null;
    onUnauthorized();
  }
  // response.json() reads the body and converts the JSON text into a JavaScript object.
  const data = await response.json();
  // response.ok is true for status codes 200-299. For 400/404/500 it is false.
  if (!response.ok) {
    throw new Error(data.error || `Request failed (${response.status})`);
  }
  return data;
}

/** Helper for sending a JS object as JSON (used by POST/PATCH). */
function jsonBody(method, value) {
  return {
    method,
    // Here we MUST say the body is JSON, otherwise Spring's @RequestBody can't read it.
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(value), // object -> JSON text
  };
}

// ---------------- EVIDENCE ----------------

/**
 * Upload one image file, optionally into a case.
 * "async" = this function does slow work (network) and returns a Promise.
 * "await" = pause here until the slow work finishes, then continue.
 */
export async function uploadEvidence(file, caseId) {
  // FormData builds a multipart/form-data body, the same thing Postman's "form-data" tab did.
  const formData = new FormData();
  // The key "file" MUST match @RequestParam("file") in EvidenceController.
  formData.append("file", file);
  // Slice 4: matches @RequestParam(value = "caseId", required = false). Left out = no case.
  if (caseId != null) formData.append("caseId", caseId);

  // IMPORTANT: we do NOT set a Content-Type header ourselves for FormData. The browser adds
  // "multipart/form-data; boundary=..." automatically, and setting it by hand breaks it.
  return request("/evidence/upload", { method: "POST", body: formData });
}

/** Slice 5: POST /api/evidence/{id}/detect -> re-run the AI check, returns the updated record */
export function runDetection(evidenceId) {
  return request(`/evidence/${evidenceId}/detect`, { method: "POST" });
}

/** Slice 6: PUT /api/evidence/{id}/review with {conclusion, note, reviewer} -> the updated record */
export function saveReview(evidenceId, review) {
  return request(`/evidence/${evidenceId}/review`, jsonBody("PUT", review));
}

/** Get the list of all saved evidence (GET /api/evidence). fetch defaults to GET. */
export function fetchEvidence() {
  return request("/evidence");
}

// ---------------- CASES (Slice 4) ----------------

/** GET /api/cases -> [{id, title, status, evidenceCount, ...}] */
export function fetchCases() {
  return request("/cases");
}

/** POST /api/cases with {title, description} -> the created case */
export function createCase(title, description) {
  return request("/cases", jsonBody("POST", { title, description }));
}

/** PATCH /api/cases/{id}/status with {status} -> the updated case */
export function updateCaseStatus(caseId, status) {
  return request(`/cases/${caseId}/status`, jsonBody("PATCH", { status }));
}

/**
 * Slice 6: download the XML report of a case and make the browser save it as a file.
 * The answer is XML, not JSON, so we can't use request() (it calls response.json()).
 * A BLOB ("binary large object") = raw file data held in memory.
 * We give it a temporary "blob:" URL, click a hidden link to it, then free the memory.
 */
export async function downloadCaseReport(caseId) {
  const response = await fetch(`${API_ROOT}/cases/${caseId}/report`, { headers: withAuth() });
  if (!response.ok) {
    throw new Error(`Report failed (${response.status})`);
  }
  const blob = await response.blob();
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `truvira-case-${caseId}-report.xml`; // "download" = save, don't open
  link.click();
  URL.revokeObjectURL(url);
}

/** GET /api/cases/{id}/evidence -> only that case's evidence */
export function fetchCaseEvidence(caseId) {
  return request(`/cases/${caseId}/evidence`);
}
