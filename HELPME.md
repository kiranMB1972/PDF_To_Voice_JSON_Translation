# HELPME - PDF Voice And Games + School

## Start
1. Double-click `Build_all.bat` once, then `Application_run.bat` (JDK 21 + Maven 3.9). Edge opens `http://localhost:8089`.
2. Choose a PDF, press **Analyze**, press **Play**. Only the main app is needed for reading.
3. Stop everything with `Stop_Application.bat` (it closes only the Edge window this app opened).

## When something fails
A coloured card appears: **why it happened**, **how to fix it** (numbered), an **out-of-the-box idea**, and **what you were doing** (the button and your language/voice choice). In the address bar a failing page shows a full-screen error page with a reference code and a *Copy details* button.
Typical cases: *No voice could speak* -> use Edge, press **Voice check**. *Translation failed* -> press **Test translation**. *The app cannot be reached* -> the black server window was closed; run `Application_run.bat`. *School database not reachable* -> power the Aiven service on (console.aiven.io).

## "No sound" checklist
1. Use **Microsoft Edge**; stay online (Natural voices are online voices).
2. **Voice check** lists the voices Edge has for the language; **Hear sample** tests the voice.
3. If the sample speaks but the PDF does not, the page is a picture/scan - the left panel says so.
4. Windows volume, and the Edge tab must not be muted.

## Languages, translation, JSON
Kannada, Tamil, Telugu, Malayalam, Urdu (South Indian accents Mysuru, Bengaluru, Chennai, Madurai ...), Hindi, Marathi, English, Spanish, French.
Translation: local **Ollama** if it is running and answers in the right language, otherwise the free online translator (three routes). A wrong-language answer is never shown as a translation. To keep text on this PC set `translate.web.enabled: false` (then Ollama is required).
**Translate JSON & speak** (JSON panel): translates up to 40 readable pages into the chosen language, shows the translated JSON and reads it aloud. **Original JSON** switches back.

## Compass and clock
The clock is the PC's time. On a phone/tablet the compass follows the sensor (iPhone: tap it once). On a desktop PC there is no sensor: click the compass, pick where your screen faces (N, NE ...) or tap where the Sun is - the dial then shows true directions and the Sun marker moves live.

## School
* Click **🎓 School** (right edge). **Admin login** / **Student login**. Passwords are shown once when created; forgotten? Admin -> Students -> New password.
* **Admin Console** (pop-out): Students, PDFs, Tests, Live class, Results. Tests: choose class, subject, date (default today), minutes, students, write questions, tick every correct answer, **Save** -> `Test<DD-MM-YYYY>.properties` and `.json` (download from the list; copies also in the `tests/` folder).
* **Student:** *Start* a test on its day (timer; answers autosave; submit or auto-submit at 0). In a test press 🔊 to hear a question in another language. Games and the live class open in their own windows.
* First admin login is in `config/FIRST_ADMIN_LOGIN.txt` - change the password, delete the file.

## Games
Top-right **Entertainment Games** (Hotstuff / Charmaine / car and bike). School games (Math Sprint, Memory Match, Word Unscramble, Quiz Blitz) are in the student panel; scores go to the school leaderboard.

## Tests
Java: `mvn test`. Browser tests: `cd js-tests && npm install && npm test` (73 tests: speech, page, compass, visual errors, games).
