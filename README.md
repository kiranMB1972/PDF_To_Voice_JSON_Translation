# PDF Voice And Games + School  (V2 / v5.0.0)

Spring Boot (Java 21, virtual threads) PDF reader with South Indian voices, translation, JSON, games - and a **School module** (Admin / Student).

| Folder | Port | What |
|--------|------|------|
| `/` (main app) | **8089** | PDF -> JSON, text preview, translation, the free **Microsoft Edge Natural voices**, **School panel**, visual error pages |
| `games-service/` | **8095** | the **Entertainment Games** microservice (Hotstuff / Charmaine / car and bike games) |
| `kong/` | 8000 | optional Kong gateway config |
| `docs/school-schema.sql` | | the School tables (created automatically; the file is for DBeaver) |

## Run
```
Build_all.bat          builds both parts (JDK 21 + Maven 3.9)
Application_run.bat    starts both services, waits for port 8089, opens Microsoft Edge once
Stop_Application.bat   stops both services and closes only the Edge window this app opened
```
Use **Microsoft Edge** and stay online. Translation works without installing anything (free online translator); **Ollama** (`StartTranslationEngine.bat`) is optional and works offline.

## What is on the screen
* **Top-left:** live clock and a **live compass** (phones / tablets / 2-in-1 PCs follow the magnetic sensor; a desktop PC has no sensor - click the compass to tell it where your screen faces, or calibrate with the Sun).
* **Left:** all the PDF tools (upload, text preview, translation, voice, JSON). **Right:** the **🎓 School** tab slides a panel in and pushes the reader to the left.
* **Languages:** Kannada, Tamil, Telugu, Malayalam, Urdu (South Indian voices), Hindi, Marathi, English, Spanish, French.
* **JSON:** *Translate JSON & speak* translates the document JSON into the chosen language and reads it with that language's voice; *Test translation* shows which engine answers or exactly why it fails.
* **Errors:** every failure is a coloured, animated card (reason, steps, out-of-the-box idea, what you were doing) - and in the browser a full-screen HD error page.

## School module
* **Admin login** (one admin, created on first start - see `config/FIRST_ADMIN_LOGIN.txt`): school name (**editable only by the admin**), students of classes 1-10 (one by one or pasted lists), subject PDFs for a whole class or one student, tests, live class, results (+ CSV).
* **Tests:** the Tests screen builds multiple-choice (check box) questions and saves **`Test<DD-MM-YYYY>.properties`** (+ a `.json` twin) - the student's live test screen is built from that file. The date is set automatically (today), the test opens by itself on its date for the students the teacher selected.
* **Student login:** today's tests (timer, check boxes, auto-submit, marks), PDFs (open, download, or *Listen & translate* in the reader), **live class** (teacher moves the page, students follow and ask), results, and four HD education games (Math Sprint, Memory Match, Word Unscramble, Quiz Blitz) for classes 1-10.
* **Cloud DB:** Aiven MySQL (`config/db-secret.properties`). If the database is asleep the PDF reader keeps working and the School shows an error card that explains how to wake it.

See `HELPME.md` (users), `DeveloperInstructions.md` / `.pdf` (developers), `CHANGES.md`.
