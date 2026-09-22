# Setup Guide — Nirikshan AI

This walks through going from a fresh clone to the complete AI pipeline
(Google Vision OCR, Gemini Vision/LLM, Supabase Storage) running against
real services, with no code changes — only credentials.

If you just want to run the app with placeholder credentials to look at
the UI (OCR/Vision calls will fail cleanly, everything else works), you
can skip straight to [Quick local run without real AI credentials](#quick-local-run-without-real-ai-credentials)
at the bottom.

## Prerequisites

- Java 21 (`openjdk@21` — a newer default JDK breaks Lombok's annotation
  processor; see the JAVA_HOME note in step 8)
- Maven (or use the included `mvnw` wrapper if present)
- Node.js 20+ and npm
- A Supabase account (free tier is enough)
- A Google account with billing enabled on Google Cloud (Vision API has a
  free monthly quota, but a project needs billing enabled to use it)
- A Gemini API key (free tier available, no billing required)

---

## 1. Create a Google Cloud project

1. Go to [console.cloud.google.com](https://console.cloud.google.com) and
   sign in.
2. Click the project dropdown (top left, next to "Google Cloud") -> **New
   Project**.
3. Name it something like `nirikshan-ai` and click **Create**.
4. Once created, select it from the project dropdown so it's your active
   project for the next steps.
5. **Enable billing**: Navigation menu -> Billing -> link a billing
   account. Vision API's `DOCUMENT_TEXT_DETECTION` requires billing to be
   enabled even within the free quota.

## 2. Enable the Vision API

1. In the Cloud Console, go to **APIs & Services -> Library**.
2. Search for "Cloud Vision API".
3. Click it, then click **Enable**.

## 3. Create a service account

This is the recommended auth path — the backend supports it directly
(`GoogleCloudCredentialsProvider` in `backend/src/main/java/com/legalmetrology/ocr/config/`).

1. **APIs & Services -> Credentials -> Create Credentials -> Service
   account**.
2. Name it `nirikshan-vision` (or anything memorable).
3. Grant it the role **Cloud Vision AI Service Agent** (or the broader
   **Editor** role if you just want it to work without thinking about
   least-privilege — for a personal/dev project either is fine; use the
   narrower role for anything shared).
4. Click **Done**.

## 4. Download the JSON credentials

1. On the Credentials page, click the service account you just created.
2. Go to the **Keys** tab -> **Add Key** -> **Create new key** -> **JSON**.
3. A `.json` file downloads automatically. This is your only copy of the
   private key — Google doesn't let you re-download it.

## 5. Where to place the JSON file

Put it somewhere **outside the repository** so it can never be accidentally
committed — e.g. `~/secrets/nirikshan-vision-sa.json`. Then reference its
absolute path via the `GOOGLE_APPLICATION_CREDENTIALS` environment
variable in step 9. Do not put it inside `backend/` or `frontend/`.

> If you'd rather skip the service-account flow entirely: **APIs & Services
> -> Credentials -> Create Credentials -> API key**, then use it as
> `GOOGLE_VISION_API_KEY` instead (step 9) and leave
> `GOOGLE_APPLICATION_CREDENTIALS` unset. The backend supports both; the
> service account is what Google recommends for anything beyond quick
> testing, since an API key isn't scoped by IAM role and is easier to
> misuse if leaked.

## 6. Obtain a Gemini API key

1. Go to [aistudio.google.com/apikey](https://aistudio.google.com/apikey).
2. Sign in, click **Create API key**.
3. Choose your Google Cloud project from step 1 (or let it create a new
   one — Gemini API keys don't need to share a project with Vision).
4. Copy the key — this is `GEMINI_API_KEY` in step 9. One key covers Vision
   AI (label understanding), the AI Copilot, report summaries, and
   recommendations — every Gemini-backed capability in the backend.

## 7. Create a Supabase project

1. Go to [supabase.com](https://supabase.com) and sign in.
2. **New Project** -> pick an organization, name it `nirikshan-ai`, set a
   database password (save it — you'll need it for `DB_PASSWORD`), pick a
   region close to you, and create it. Provisioning takes a couple of
   minutes.
3. Once ready, go to **Project Settings -> Database** and copy the
   **Connection string** (Transaction pooler, port 6543) — this becomes
   `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` in step 9.
4. Go to **Project Settings -> API** and copy:
   - **Project URL** -> `SUPABASE_URL`
   - **Publishable key** (older dashboards call this the **anon / public**
     key — same credential) -> `SUPABASE_ANON_KEY` (not currently used by
     this backend — see the comment in `.env.example` — but grab it while
     you're here)
   - **Secret key** (older dashboards: **service_role** key — same
     credential) -> `SUPABASE_SERVICE_ROLE_KEY` (this is the one Storage
     actually authenticates with; never expose it to a frontend)

## 8. Create the Storage buckets

Supabase does **not** auto-create buckets — the backend expects these to
already exist, or every upload/download call to that bucket fails.

1. In the Supabase dashboard, go to **Storage -> New bucket**.
2. Create these three (exact names, both can be **private** — the backend
   generates signed URLs, it never relies on public bucket access). These
   are the only buckets any feature actually uses today:
   - `inspection-images` — raw uploaded label photos
   - `inspection-reports` — generated PDF/DOCX compliance reports
   - `evidence` — cropped/annotated evidence images (kept separate from
     raw photos deliberately, since evidence may need different
     retention/access rules for enforcement purposes)
3. Three more bucket names exist in configuration
   (`SUPABASE_BUCKET_TEMP`, `SUPABASE_BUCKET_EXPORTS`,
   `SUPABASE_BUCKET_TRAINING_DATA`) for planned future features — nothing
   in the current codebase calls them, so you don't need to create them
   yet. Create them later only if/when those features land.

## 9. Configure environment variables

**Backend**: copy `backend/.env.example` to `backend/.env` and fill in
every value gathered above:

```bash
cd backend
cp .env.example .env
```

Edit `.env`:

```
DB_URL=jdbc:postgresql://db.<your-project-ref>.supabase.co:6543/postgres
DB_USERNAME=postgres.<your-project-ref>
DB_PASSWORD=<your database password from step 7>

JWT_SECRET=<openssl rand -base64 64>

SUPABASE_URL=https://<your-project-ref>.supabase.co
SUPABASE_ANON_KEY=<publishable / anon key from step 7>
SUPABASE_SERVICE_ROLE_KEY=<secret / service_role key from step 7>
SUPABASE_BUCKET_IMAGES=inspection-images
SUPABASE_BUCKET_REPORTS=inspection-reports
SUPABASE_BUCKET_EVIDENCE=evidence

GOOGLE_APPLICATION_CREDENTIALS=/absolute/path/to/nirikshan-vision-sa.json
GOOGLE_CLOUD_PROJECT_ID=<your GCP project id from step 1>
# (leave GOOGLE_VISION_API_KEY blank if using the service account above)

GEMINI_API_KEY=<key from step 6>
```

If your shell doesn't auto-load `.env` files, export them before starting
the backend (macOS/Linux):

```bash
set -a; source .env; set +a
```

**Frontend**: copy `frontend/.env.example` to `frontend/.env.local` — no
edits needed unless your backend runs somewhere other than
`localhost:8080`:

```bash
cd frontend
cp .env.example .env.local
```

## 10. Start the backend

Java 21 is required — if `mvn -v` shows a different major version as the
default JDK, point `JAVA_HOME` at a real JDK 21 install explicitly (a newer
JDK breaks Lombok's annotation processor with a cryptic error):

```bash
cd backend
export JAVA_HOME=/path/to/your/jdk-21   # e.g. /opt/homebrew/Cellar/openjdk@21/.../Home on macOS
mvn spring-boot:run
```

On first boot, Flyway runs all migrations against your Supabase database
automatically — watch the log for `Successfully applied N migrations`.
The API is then live at `http://localhost:8080/api/v1`, and Swagger UI at
`http://localhost:8080/swagger-ui.html`.

## 11. Start the frontend

```bash
cd frontend
npm install
npm run dev
```

Open the printed URL (`http://localhost:5173` by default). Sign up for an
account through the UI, or via `curl`:

```bash
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Your Name","email":"you@example.com","password":"YourPassword@123"}'
```

## 12. Verify OCR, Vision AI, and Supabase are actually working

Don't just trust that it looks fine — check the real signal for each
integration:

**Supabase Storage**: upload an image through the UI (New Inspection ->
drag a photo in), then check the Supabase dashboard's **Storage ->
images** bucket — your file should actually appear there within a few
seconds. If it doesn't, or the upload fails, the error message will say
exactly what's missing: `"Supabase Storage is not configured — set
SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY."`

**Google Vision OCR**: after uploading, trigger OCR (the workspace's "Run
AI Pipeline" button, or `POST /api/v1/ocr/images/{imageId}/run`). A
successful response includes real `rawText` extracted from your image and
a non-zero `ocrConfidence`. Check the backend log for
`OCR (google-vision) extracted image ...: N paragraphs, language=...,
confidence=...` — if `N` is 0 or confidence is suspiciously low, try a
clearer, well-lit photo of text before assuming something's misconfigured.
If Vision genuinely isn't configured, the response is a clean
`"Google Vision API key is not configured."` (503) instead of extracted
text — that message means step 9's `GOOGLE_APPLICATION_CREDENTIALS` /
`GOOGLE_VISION_API_KEY` didn't take effect, not that something crashed.

**Gemini (Vision AI + Copilot + summaries)**: trigger Vision analysis
(`POST /api/v1/vision/images/{imageId}/analyze`) and check for real
detected declarations in the response. Separately, open the workspace's
Nirikshak drawer and ask a question — a real answer (not a `"Gemini API
key is not configured."` error) confirms `GEMINI_API_KEY` is live.

**All three together**: run a full inspection through the UI end to end
(upload -> Run AI Pipeline -> generate report) and check that the
Decision Timeline shows every stage — including OCR and Vision Detection
— as genuinely `SUCCESS`, not skipped or failed. That's the real signal
that all three services are wired up correctly, not just individually
reachable.

---

## Quick local run without real AI credentials

If you want to run the app without doing any of the above yet: leave
`GOOGLE_APPLICATION_CREDENTIALS`, `GOOGLE_VISION_API_KEY`, `GEMINI_API_KEY`,
and `SUPABASE_*` blank (or any placeholder non-blank value). The backend
boots normally — auth, inspections, products, and rules all work fully.
OCR, Vision AI, the Copilot, and image storage will each fail with a
clean, specific error (`"<Service> is not configured."` / `"<Service>
API key is not configured."`, HTTP 503) instead of crashing, telling you
exactly which step above to come back and finish.
