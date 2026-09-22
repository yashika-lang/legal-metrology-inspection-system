# Setup Checklist — Nirikshan AI

A scannable companion to [SETUP.md](./SETUP.md) — use that for the
step-by-step walkthrough of anything unchecked here.

## External services

- [ ] Google Cloud project created, billing enabled (SETUP.md §1)
- [ ] Cloud Vision API enabled on that project (§2)
- [ ] Service account created with a Vision-capable role (§3)
- [ ] Service account JSON key downloaded (§4)
- [ ] JSON key file saved **outside the repository** (§5) — e.g.
      `~/secrets/nirikshan-vision-sa.json`, never inside `backend/` or
      `frontend/`
- [ ] Gemini API key generated at [aistudio.google.com/apikey](https://aistudio.google.com/apikey) (§6)
- [ ] Supabase project created (§7)
- [ ] Supabase database password saved somewhere safe (§7)
- [ ] Supabase Project URL copied (§7)
- [ ] Supabase Publishable/anon key copied (§7)
- [ ] Supabase Secret/service_role key copied (§7) — **never** put this in
      a frontend `.env` file
- [ ] Storage bucket `inspection-images` created (§8)
- [ ] Storage bucket `inspection-reports` created (§8)
- [ ] Storage bucket `evidence` created (§8)

## Local configuration

- [ ] `backend/.env` created from `backend/.env.example`
- [ ] `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` filled in from Supabase's
      connection string (Transaction pooler, port 6543)
- [ ] `JWT_SECRET` set to a real random value ≥ 32 bytes
      (`openssl rand -base64 64`) — the backend refuses to boot below that
- [ ] `SUPABASE_URL` filled in
- [ ] `SUPABASE_SERVICE_ROLE_KEY` filled in (the Secret key)
- [ ] `SUPABASE_ANON_KEY` filled in (the Publishable key — optional today,
      but fill it in while you have it)
- [ ] `SUPABASE_BUCKET_IMAGES` / `_REPORTS` / `_EVIDENCE` match the three
      bucket names you actually created
- [ ] Either `GOOGLE_APPLICATION_CREDENTIALS` (absolute path to the JSON
      key) **or** `GOOGLE_VISION_API_KEY` is set — not required to set both
- [ ] `GEMINI_API_KEY` filled in
- [ ] `frontend/.env.local` created from `frontend/.env.example`
      (no edits needed unless the backend isn't on `localhost:8080`)
- [ ] No real secret committed to git — `git status` shows no `.env` or
      `.env.local` staged (they're git-ignored by default; verify, don't
      just assume)

## Running it

- [ ] `JAVA_HOME` points at a real JDK 21 install (a newer default JDK
      breaks Lombok's annotation processor)
- [ ] Backend starts cleanly: `cd backend && mvn spring-boot:run` — log
      shows `Successfully applied N migrations` and `Started
      LegalMetrologyApplication`
- [ ] Frontend starts cleanly: `cd frontend && npm install && npm run dev`
      — open the printed `localhost:5173` URL
- [ ] Signed up / logged in through the UI (or via `curl` — see SETUP.md §11)

## Verifying each integration is genuinely live

Don't just check "no error" — check the specific real signal:

- [ ] **Supabase Storage**: uploaded an image through the UI, then saw the
      actual file appear in the Supabase dashboard's **Storage ->
      inspection-images** bucket
- [ ] **Google Vision OCR**: ran OCR on an uploaded image and got back
      real, non-empty `rawText` with a non-zero `ocrConfidence` — not a
      `"Google Vision API key is not configured."` error
- [ ] **Gemini Vision AI**: ran Vision analysis and got back real detected
      declarations
- [ ] **Gemini Copilot**: asked Nirikshak a question in the workspace
      drawer and got a real generated answer (`generatedByAi: true`,
      `provider: "gemini"`) — not a `"Gemini API key is not configured."`
      error
- [ ] **Evidence generation**: ran violations through the pipeline and
      confirmed evidence images land in the **evidence** bucket, not
      **inspection-images**
- [ ] **End to end**: ran a full inspection through the UI and the
      Decision Timeline showed every stage — including OCR and Vision
      Detection — as `SUCCESS`, not skipped or failed

## If something's still not working

Every missing/blank credential fails with a specific message, not a
crash — read it, it tells you exactly what to fix:

| Message | Fix |
|---|---|
| `"Google Vision API key is not configured."` | Set `GOOGLE_APPLICATION_CREDENTIALS` or `GOOGLE_VISION_API_KEY` |
| `"Gemini API key is not configured."` | Set `GEMINI_API_KEY` |
| `"Claude API key is not configured."` | Only relevant if you set `AI_VISION_PROVIDER`/`AI_LLM_PROVIDER` to `claude` — set `ANTHROPIC_API_KEY` |
| `"Supabase Storage is not configured — set SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY."` | Set both of those |
| `"All OCR providers failed. Primary error: ..."` | The real cause is in the message — if it mentions Tesseract/`libtesseract`, that's the offline fallback failing too; fix the primary (Vision) cause first |
| Backend won't start, mentions `JWT_SECRET` | Needs to be ≥ 32 bytes |
| Backend won't start, mentions Lombok / `TypeTag` | Wrong JDK — export `JAVA_HOME` to a real JDK 21 |
