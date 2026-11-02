# V2 (v5.0.0) - School module, visual errors, compass, JSON translation

- **School module** (Admin / Student): slide-out 🎓 panel that pushes the reader left, pop-out Admin Console (students, PDFs, tests, live class, results), student tests, live class, HD education games; Aiven MySQL; `Test<DD-MM-YYYY>.properties` + `.json`.
- **Visual exceptions:** coloured animated cards in the page and full-screen HD error pages (reason, numbered fix, out-of-the-box idea, what you were doing + your selection); JSON errors carry the guide; `/api/error-guides`; the Whitelabel page is gone.
- **Compass:** north-crossing spin bug fixed, sensor API added, manual "where my screen faces" and Sun calibration for PCs, dial ticks.
- **Translation / JSON:** wrong-language answers rejected, three online routes with reasons, *Test translation*, *Translate JSON & speak* (see round 5 below).
- Docs: README, HELPME, DeveloperInstructions (+ PDF) rewritten; `docs/school-schema.sql`; `config/` for the DB password.

# Round 5 changes (Hindi / Marathi / Spanish / French fix + JSON translation)

- **Why those four could look "not translated"** (found by testing every language through the page with a realistic server simulation - the page itself is fine for all nine languages): the local Ollama model (llama3.1) sometimes answers Hindi/Marathi in English or transliteration, or repeats the Spanish/French input, and that answer was shown and spoken as the "translation". Now the answer is checked (right script for Indic/Urdu targets, not an unchanged copy for Spanish/French); a wrong answer is rejected and the free online translator is used instead (or, with the online translator switched off, a clear error - never a fake translation).
- **Online translator hardened**: three routes (Google POST, Google GET in small pieces, MyMemory); when all fail the message lists exactly why each failed (HTTP code / blocked / quota).
- **New "Test translation" button** (next to the engine chip): shows which engine answers for the chosen language, or exactly which route fails and why. Also `GET /api/v1/translate/selftest?target=hi`.
- **New "Translate JSON & speak" button** (JSON preview): translates the document JSON - every readable page, up to 40 per click - into the chosen language, shows the translated JSON (`translatedTo`, `translatedPages`), then reads it aloud with the language's Natural voice (the South Indian voices for Kannada, Tamil, Telugu, Malayalam, Urdu; Swara/Aarohi/Elvira/Denise for Hindi/Marathi/Spanish/French). "Original JSON" switches back.
- Engine chip now names the engine that really answered ("free online translator" or the Ollama model).

# Round 4 changes

- **Translation works without Ollama.** If Ollama is not running, pages are translated by a free online translator
  (keyless public endpoints; internet needed). Ollama is still used first when it is running (`translate.prefer: auto`).
  Orate / Teach modes and accent wording need Ollama; the online translator translates faithfully.
  Privacy: with the online translator the page text is sent to that service. Set `translate.web.enabled: false` to keep everything local.
- A failed translation is retried (after 15 s, on Play, or by clicking the engine chip) instead of sticking on "original (not translated)".
- **Hindi and Marathi added** (text translation + Edge Natural voices Swara/Madhur and Aarohi/Manohar), group "Other Indian Languages".
- **Edge auto-open fixed for Windows 10 Pro**: tries msedge.exe at its real install paths, `start msedge`, the `microsoft-edge:` protocol, then the default browser; Application_run.bat also opens Edge itself once port 8089 answers.
- **Compass**: only north-referenced sensor data is used, tilt and screen rotation are handled. A desktop PC has no magnetometer: the rose stays North-up and a live Sun marker (true Sun direction from clock + browser location) is shown.
- **Layout**: all page content is left-aligned; the right-hand side is an empty `.right-dock` for future use.
- **Stop_Application.bat** stops the app (8089) and games service (8095), and closes only the Edge window this app opened.

# What was wrong in each uploaded zip, and what happened to it

(Found by reading the files. "Not run" = I could not run Maven here; see DeveloperInstructions.md "Verification".)

## The five pdf-voice-* zips
| Zip | Problem found | Now |
|-----|---------------|-----|
| pdf-voice-SARVAM-8091-FIXED-PDFBOX | `pom.xml` is not valid XML (a `</plugin>` is missing, line 13) so Maven cannot even read it; no `main` class | one valid pom, one `PdfPlatformApplication` |
| pdf-voice-FINAL-v2_0-SARVAM-AZURE | pom has **no dependencies and no build plugin**; no `main` class; `application.yml` needs `${AZURE_SPEECH_KEY}` so start-up fails when it is not set | keys optional (`${AZURE_SPEECH_KEY:}`) |
| pdf-voice-SARVAM-8091-FINAL-CLEAN-NILA | voice descriptors only; no PDF library | PDF + voices in one app |
| pdf-voice-SARVAM-8091-FULL-FINAL-BUILD-PASS-NILA | same voice-only skeleton; Nila SSML built from raw, unescaped text | text is XML-escaped |
| pdf-voice-merged-HD-ACCENT-GOLD | no `main` class (cannot start); `application.yml` mis-indented so the upload limits were ignored; JTE templates without JTE dev/precompile setup (page cannot render); Tika + POI unused; read **only the first 20 pages** while reporting the real page count; spoke the literal text "[Scanned]"; 5-minute timeout class never used; every male request returned the Kannada voice; the play button never paused and never set a voice | fixed each one (static gold page, all pages, picture pages skipped, timeout enforced, voice per language) |

Kept: gold look, 3D gold heading, upload / page slider / JSON preview / gold highlight, `/api/v1/documents/analyze-sync`,
`/api/v1/voice/health|sarvam|nila-private|ssml`, the Nila private style, port-free defaults.

## BharathaVani-FINAL-NO-FEMALE-HUMAN-GENERIC-PERFECT
| Problem found | Now |
|---------------|-----|
| **Audio:** the chosen voice was never set on the utterance (only `lang`), text cut to 600/800 characters, voice list read once before Edge had loaded it, no error handling, and the "sample" spoke placeholder text ("Translation ... Human generic ...") | new `speech.js` engine: voice set, chunked, waits for voices, watchdog + fallback voice, clear messages; 19 simulated-Edge tests + 11 page tests |
| Voice names were invented ("... Live - Human"), so no real voice could ever match | real Edge Natural voice ids (30 voices, 10 languages) |
| A blanket find-and-replace had turned "Female"/"Male" into "Human" everywhere (README before/after lines are identical, voice gender = "Human") | screens are gender-neutral as you asked (voice names only, no gender words); the catalogue keeps a hidden gender field so voices can be selected by type |
| Empty PDF pages were **filled with made-up sentences** ("Marusinchana ENGLISH 2026-2027 ...") which were then translated and read aloud | nothing is ever invented; picture pages are marked and skipped |
| When Ollama was down, the "translation" was a label + the first 280 English characters | engine offline = the original text is read, with a clear notice |
| Prompts hard-coded "10th Standard Marusinchana"; whole document translated at once and then split by paragraph count (pages misaligned) | generic prompts; translation per page, next page pre-translated while reading |
| Unknown language code silently became Telugu; `nativeScript` held the English name; Gulf accents produced tags like `ur-QA` that match no voice (browser fell back to a default voice); accents API returned bare enums | strict lookup, native names, valid voice locales, DTOs |
| Edge opened twice (CommandLineRunner **and** ApplicationReadyEvent) | opens once |
| Spring AI 1.0.0-M6 milestone dependency (needs a milestone repository; API changes between milestones) | plain REST call to the same local Ollama |
Not carried over (they only returned fixed text): OCR placeholder, translation-library and language-game stubs, "AI thinking" lines, "ignored images" tab, "next actions" box.

## New
Games corner with four original games (no internet needed), shared speech engine, honest translation status, tests.

## Round 3 (this version)
1. **Compass + clock** top-left (`HH:MM:SS DD/Mon/Year`); **dark-sky-blue** background, no black (also in every game page).
2. **South Indian accents only.** Removed Gulf, Mumbai/Pune, Mexican, Argentine, Quebec, Belgian, Spain/France accents and the Pakistani Urdu, Hindi and Marathi voices. Added Mangaluru, Hubballi-Dharwad, Coimbatore, Tirunelveli, Warangal, Kozhikode.
3. **Languages:** Kannada, Tamil, Telugu, Urdu, Malayalam + English (Indian PDF text to English) + International Languages Spanish and French. A Hindi/Marathi PDF can still be read and translated to English; it simply has no voice.
4. **Entertainment Games** drop-down top-right, served by the separate `games-service` microservice (port 8095) - my four car/bike games plus the attached Hotstuff / Princess Charmaine project.
5. **From the attached project** (Hotstuff_PrincessCharmaine_Games_1): the Maven poms had no `spring-boot-maven-plugin` (`mvn spring-boot:run` and the jar could not start); the AI service reassigned a lambda parameter (does not compile); four identical copies of the page; the 3D background loaded from unpkg.com (needs internet); the run script opened Chrome (not Edge); the maze had no win condition and the memory game no match detection. All games were rewritten on the offline game kit as real, scoreable games (maze, memory, runner + quiz and star catcher added).
6. Fixed my own failing test (`VoiceAndGamesControllerTest`, JsonPath filter returns a list).
