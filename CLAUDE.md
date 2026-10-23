# Truvira AI — "Truth Verified."

AI-assisted digital forensics web app: users upload suspicious images, the system records
evidence (SHA-256 hash, metadata, timestamp), runs an AI-generation detector, and produces a
report for HUMAN review. It never declares an image definitively fake or a person guilty.

## HOW TO BEHAVE (most important)

The user is a student preparing for a test next week. They are building this project to
UNDERSTAND the technologies, not just to finish it.

- Act as a patient teacher. Explain every new keyword, annotation, class and function you
  introduce: what it is, what it does, and why it is needed.
- Add teaching comments to new code in the same style as the existing files.
- Work in small slices. One feature at a time, then stop and explain before continuing.
- Before running a command, say what it does in one sentence.
- After each slice, give 3-5 short self-test questions the student could be asked in an exam.
- Never silently fix errors: explain what the error message means and why it happened.
- Do not ask the user to paste secrets (e.g. the MySQL password) into chat. They edit
  application.properties themselves.
- Be honest about uncertainty. The AI detector is the main technical risk; never promise
  perfect deepfake detection.

## Repository

GitHub (private): https://github.com/Allen2255/truvira-ai, branch `main`.
Never commit secrets, evidence images or logs: `.gitignore` excludes `application.properties`,
`backend/uploads/`, `*.log`, `node_modules/`, `.venv/`, `target/`. Settings template:
`backend/src/main/resources/application.example.properties`.

## Architecture

React (Vite, port 5173)  ->  Spring Boot REST API (port 8080)  ->  MySQL (port 3306)
                                       |
                                       +-> (later) Python AI service with a pretrained detector

Backend layers: Controller (HTTP) -> Service (business logic) -> Repository (DB) -> MySQL.

## Tech stack

- Frontend: React + Vite + plain CSS (folder: `frontend/`)
- Backend: Java 17+, Spring Boot 3, Spring Web, Spring Data JPA / Hibernate, Maven (folder: `backend/`)
- Database: MySQL 8, database name `truvira`
- Later: Spring Security, JUnit + Mockito tests, XML report export, Python AI service
- Tools: Git/GitHub, Postman, IntelliJ IDEA

## Project layout

```
truvira-ai/
├── backend/    Spring Boot app, package com.truvira (controller, service, repository, model)
├── frontend/   React app (src/api.js, src/App.jsx, src/components/)
└── ai-service/ (not created yet) Python detector service
```

## How to run

Backend (MySQL service must be running; set username/password in
`backend/src/main/resources/application.properties`):
```
cd backend
mvn spring-boot:run
```
Frontend:
```
cd frontend
npm install
npm run dev        # http://localhost:5173
```
Test without MySQL: add `--spring.profiles.active=h2` (uses in-memory H2 database).

Login (Spring Security, HTTP Basic, stateless): on first start with an empty `app_user` table the
backend creates user `admin`. Its password = `truvira.admin.password` from application.properties if set,
otherwise a random one printed ONCE in the backend log ("Created first user 'admin' with generated password").
Never put passwords in code or chat. The React app keeps the login in memory only (refresh = log in again).

Tests (JUnit 5 + Mockito, no DB/AI needed):
```
cd backend
mvn test
```

AI detector service (optional; without it uploads still work and show "AI check not available"):
```
cd ai-service
py -3.13 -m venv .venv                     # first time only
.venv\Scripts\python -m pip install torch --index-url https://download.pytorch.org/whl/cpu
.venv\Scripts\python -m pip install -r requirements.txt
.venv\Scripts\python -m uvicorn app:app --port 8000
```
Model: `haywoodsloan/ai-image-detector-deploy` (Hugging Face, Apache-2.0, ~745 MB, cached after first run).
Change with env var `TRUVIRA_MODEL`. Spring reads `truvira.detector.url` (default http://localhost:8000).

## API (current)

- `POST /api/evidence/upload`  multipart form-data, key `file` (JPEG/PNG/WEBP, max 10 MB), optional `caseId` -> 201 + saved record
- `GET  /api/evidence`         list all evidence, newest first
- `GET  /api/evidence/{id}`    one record (404 if missing)
- All `/api/**` endpoints need HTTP Basic login (401 `{"error": ...}` otherwise); `GET /api/auth/me` -> `{username}`
- `PUT  /api/auth/password` JSON `{currentPassword, newPassword}` (8-72 chars) -> change your own password
- `PUT  /api/evidence/{id}/review` JSON `{conclusion, note}` (reviewer = logged-in user) (conclusion: LIKELY_AUTHENTIC |
  LIKELY_AI_GENERATED | LIKELY_MANIPULATED | INCONCLUSIVE); refused (400) when the case is CLOSED
- `GET  /api/cases/{id}/report` XML forensic report (JAXB), downloaded as `truvira-case-{id}-report.xml`
- `POST /api/evidence/{id}/detect` re-run the AI check (aiStatus COMPLETED | UNAVAILABLE | FAILED, aiScore 0..1)
- Python service: `POST :8000/detect` (multipart `file`) -> `{aiScore, model, labels, elapsedMs}`; `GET :8000/health`
- `POST /api/cases`            JSON `{title, description}` -> 201 + CaseSummary
- `GET  /api/cases`            list cases with evidenceCount, newest first
- `GET  /api/cases/{id}`       one case
- `PATCH /api/cases/{id}/status`  JSON `{status: OPEN|UNDER_REVIEW|CLOSED}`
- `GET  /api/cases/{id}/evidence` evidence in that case
- Errors: always `{"error": "..."}`; 400 bad input, 404 not found (GlobalExceptionHandler)

## Conventions

- Controllers stay thin; business logic goes in services.
- Never trust uploaded filenames: files are stored under random UUID names.
- Validate on the backend even if the frontend also validates.
- Frontend talks to the backend only through `frontend/src/api.js`.
- CORS origin = setting `truvira.frontend-origin` (default http://localhost:5173), used by every controller's
  @CrossOrigin. Frontend API address = env `VITE_API_URL` (default http://localhost:8080/api). Set both when deploying.
- Styling lives in `frontend/src/App.css` (colour variables at the top; responsive rules at the bottom).
- Error-to-HTTP mapping lives only in `controller/GlobalExceptionHandler` (@RestControllerAdvice).
- API request/response shapes that are not plain entities go in `dto/` as records (validated with @Valid).
- A CLOSED case accepts no new evidence (enforced in EvidenceService).

## Status

- [x] Slice 1: backend upload -> SHA-256 -> MySQL (verified working by the user via Postman)
- [x] Slice 2: React upload page + evidence table (verified running on user's PC, 2026-10-22)
- [x] Slice 3: image metadata (EXIF) extraction (metadata-extractor lib, MetadataService; tested on a
      WhatsApp JPEG = no EXIF + dimensions; camera/GPS path still to test with a real phone photo)
- [x] Slice 4: investigation cases (InvestigationCase entity, @ManyToOne case_id FK, CaseStatus enum,
      DTOs, CaseController, React CasePanel; verified 2026-10-23)
- [x] Slice 5: AI detector service (FastAPI + transformers, DetectorClient with RestClient, UNAVAILABLE
      fallback, AiResult UI). Tested 2026-10-23: 3 real photos 13-17%, 2 Gemini PNGs 98%; a Gemini image
      shrunk to 400px at JPEG q30 dropped to 30% (detector fooled by heavy compression - known limitation).
- [x] Slice 6: human review (ReviewConclusion enum, PUT review, frozen when case CLOSED) and XML report
      (JAXB ForensicReport + ReportService, download via blob in api.js); evidence detail panel in React.
      Verified 2026-10-23. Reviewer name is typed for now; Slice 7 login should fill it.
- [x] Slice 7: Spring Security (SecurityConfig, AppUser + BCrypt, AdminUserInitializer, AuthController),
      uploadedBy/reviewedBy from the login, React Login screen; 9 unit tests (EvidenceServiceTest,
      CaseServiceTest) all passing. Verified 2026-10-23. Not done: user management UI, change password.
- [ ] Deploy (needs HTTPS because of HTTP Basic; hosted MySQL; Python service hosting)

## Forensics notes (for explanations)

- SHA-256 identifies the exact file: changing one pixel changes the whole hash.
- Missing EXIF metadata is common (social apps strip it) and is NOT proof of manipulation.
- Detector output is a probability with limitations, never a verdict.
