package com.org.pdfplatform.exception;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The single catalogue behind every visual error: the colour theme, the animated scene, WHY it happens, the steps that
 * fix it and an "out of the box" idea. The server pages, the JSON error body and the browser notices all read it
 * (the browser gets it from GET /api/error-guides), so a failure looks and reads the same everywhere.
 */
public final class ErrorGuide {

    /** category: FILE NETWORK TRANSLATION VOICE SECURITY DATABASE INPUT NOTFOUND SERVER SCHOOL INFO */
    public record Guide(String code, String category, String emoji, String headline, String reason, List<String> steps, String tip,
                        String color1, String color2) { }

    private static final Map<String, String[]> THEME = new LinkedHashMap<>();
    private static final Map<String, Guide> CATALOG = new LinkedHashMap<>();

    static {
        THEME.put("FILE", new String[]{"#6a11cb", "#2575fc"});
        THEME.put("NETWORK", new String[]{"#0f9b8e", "#1fd1f9"});
        THEME.put("TRANSLATION", new String[]{"#b4207a", "#7b2ff7"});
        THEME.put("VOICE", new String[]{"#f7971e", "#ff512f"});
        THEME.put("SECURITY", new String[]{"#cb2d3e", "#ef473a"});
        THEME.put("DATABASE", new String[]{"#1d4ed8", "#06b6d4"});
        THEME.put("INPUT", new String[]{"#e58a1f", "#e8c13a"});
        THEME.put("NOTFOUND", new String[]{"#7f00ff", "#e100ff"});
        THEME.put("SERVER", new String[]{"#b91d47", "#ff5f6d"});
        THEME.put("SCHOOL", new String[]{"#11998e", "#38ef7d"});
        THEME.put("INFO", new String[]{"#2193b0", "#6dd5ed"});

        g("FILE_TOO_LARGE", "FILE", "\uD83D\uDCE6", "That PDF is too heavy", "The file is bigger than the 50 MB upload limit, so the server refused it before reading a single page.",
          "Open the PDF in Microsoft Edge, press Ctrl+P, choose \"Microsoft Print to PDF\" and print only the chapters you need.",
          "Most of a big PDF is pictures. A text-only export is often 20 times smaller - and it is the text this app reads aloud anyway.",
          "Split the PDF into parts (for example pages 1-50, 51-100).", "Press Analyze for one part at a time - the voice continues where you stop.");
        g("TIMEOUT", "FILE", "\u23F1\uFE0F", "Reading took too long", "The PDF needed more time than the server allows. Very long documents and scanned pages are the usual reason.",
          "Try a smaller part of the document.", "A scan is a photograph of text. If the page cannot be selected with the mouse, it needs OCR first (Google Drive: upload, then Open with Google Docs).",
          "Wait a moment, then press Analyze again.", "If it repeats, split the PDF.");
        g("EMPTY_FILE", "FILE", "\uD83D\uDCED", "The file is empty", "The file you chose has 0 bytes - often a download that was interrupted.",
          "Download or export the PDF again.", "Right-click the file in Explorer and check Properties: a real PDF shows a size above 0 KB.",
          "Choose a different file.", "Download the PDF again, then press Analyze.");
        g("INVALID_PDF", "FILE", "\uD83D\uDCC4", "This is not a readable PDF", "The file does not start like a PDF. A renamed Word, picture or web page is the usual cause.",
          "Open the file in Microsoft Edge. If Edge cannot show it, it is not a PDF.", "Never rename .docx to .pdf. In the original program use Print, then \"Microsoft Print to PDF\" - that makes a true PDF.",
          "Open the original program.", "Use File, Save as PDF (or Print to PDF).", "Choose the new PDF and press Analyze.");
        g("PASSWORD_PROTECTED", "SECURITY", "\uD83D\uDD12", "The PDF is locked", "The document is password protected, so its text cannot be read.",
          "Open it in Edge, type the password, then Print and choose \"Microsoft Print to PDF\" to make an unlocked copy.", "Only unlock documents you are allowed to read aloud.",
          "Open the PDF in Edge and enter its password.", "Print it to a new PDF.", "Analyze the new copy.");
        g("TOO_MANY_PAGES", "FILE", "\uD83D\uDCDA", "Too many pages at once", "The document has more pages than one run can read.",
          "Read it in parts: pages 1-200 first, then the next part.", "A good study trick: split by chapter, so each audio session ends at a natural pause.",
          "Split the PDF by chapter.", "Analyze one part at a time.");
        g("MISSING_FILE", "INPUT", "\uD83D\uDCCE", "No PDF chosen yet", "You pressed Analyze but no file is attached.",
          "Press \"Choose file\", pick a PDF, then press Analyze.", "Tip: you can also drop the PDF file onto the Choose file button.",
          "Click the Choose file button.", "Select a PDF with selectable text.", "Press Analyze.");
        g("NO_TEXT", "FILE", "\uD83D\uDDBC\uFE0F", "No text to read", "Every page in this PDF is a picture or scan, so there is no text to speak.",
          "Use a PDF whose text can be selected with the mouse.", "Free OCR: upload the PDF to Google Drive, right-click, Open with Google Docs - it types out the scanned text.",
          "Try to select a word on the page with the mouse.", "If you cannot, run OCR (see the idea below).", "Analyze the OCR result.");
        g("NON_TEXT_CONTENT_SKIPPED", "INFO", "\uD83D\uDDBC\uFE0F", "Some pages were skipped", "Pages that are only pictures cannot be read aloud, so they were left out.",
          "Nothing is broken - the text pages are read normally.", "Pictures with captions: the caption is text and will be read.", "Continue with Play.");
        g("INTERNAL_ERROR", "SERVER", "\uD83D\uDEE0\uFE0F", "Something broke on the server", "The server hit a problem it did not expect. Nothing you did caused it.",
          "Try the same action again.", "Quote the reference code below if you report it - it points to the exact line in the server window.",
          "Press the button again.", "Try a different PDF to see whether this one is the trigger.", "Look at the black server window for the red lines.");
        g("TRANSLATION_ENGINE_OFFLINE", "TRANSLATION", "\uD83D\uDCA4", "No translation engine answered", "Neither the local Ollama model nor the free online translator could be reached.",
          "Press \"Test translation\" - it shows which route fails and why.", "On a slow line the online translator times out; Ollama works fully offline: install it once and run \"ollama serve\".",
          "Check that this computer is online.", "Press the engine chip to retry.", "Or start Ollama: ollama serve, then ollama pull llama3.1.");
        g("TRANSLATION_TIMEOUT", "TRANSLATION", "\u231B", "The translation is slow today", "The translator did not answer in time.",
          "Translate one page at a time.", "A smaller local model (ollama pull llama3.2) answers faster on a modest PC.", "Press Play again.", "Try a smaller page.");
        g("TRANSLATION_FAILED", "TRANSLATION", "\uD83C\uDF10", "This page could not be translated", "The translator refused or returned the wrong language, so the original is shown instead of a fake translation.",
          "Press \"Test translation\" for the exact failing route.", "Short pages with only numbers or names have nothing to translate - that is normal.", "Press Test translation.", "Retry with another page.");
        g("TRANSLATION_OK", "INFO", "\u2705", "Translation works", "The test translation succeeded.", "Nothing to fix.", "You can now press Play or \"Translate JSON & speak\".");
        g("INVALID_LANGUAGE", "INPUT", "\uD83D\uDDE3\uFE0F", "Unknown language", "The chosen language is not one this app can translate.",
          "Pick a language from the list: Kannada, Tamil, Telugu, Malayalam, Urdu, Hindi, Marathi, English, Spanish or French.", "Choose \"Original language\" to read without translating.",
          "Open the Read in list.", "Pick one of the listed languages.");
        g("CHOOSE_LANGUAGE", "INPUT", "\uD83D\uDDE3\uFE0F", "Choose a language first", "This button translates into the language selected under \"Read in\", and none is selected.",
          "Pick a language in the Read in list, then press the button again.", "Marathi and Hindi use the same script - pick the one your listeners speak.", "Open the Read in list.", "Choose a language.");
        g("EMPTY_TEXT", "INPUT", "\uD83D\uDCDD", "Nothing to translate", "This page has no readable text (it is a picture or scan).",
          "Go to a page with text.", "Use the page links under the progress bar - dimmed numbers are picture pages.", "Choose another page.");
        g("TEXT_TOO_LONG", "INPUT", "\uD83D\uDCCF", "That page is too long to translate in one go", "The text is longer than one translation request may carry.",
          "Translate one page at a time.", "Use \"Translate JSON & speak\": it translates page by page for you.", "Go to a shorter page.");
        g("NETWORK", "NETWORK", "\uD83D\uDD0C", "The app cannot be reached", "The page could not talk to the server - it may have stopped, or the window was closed.",
          "Start it again with Application_run.bat.", "Look for the black window titled \"PDF Voice And Games (8089)\". If it shows red text, copy the first red line - that is the real reason.",
          "Check that the black server window is still open.", "If not, double-click Application_run.bat.", "Reload this page when it says started.");
        g("NO_VOICE", "VOICE", "\uD83D\uDD08", "No voice could speak", "The browser has no voice for this language, or the chosen Natural voice is not installed.",
          "Open the app in Microsoft Edge - it carries the free Natural voices - and press \"Voice check\".", "Edge: Settings, Languages, add the language, then restart Edge - Natural voices appear after the first online use.",
          "Open the page in Microsoft Edge.", "Press Voice check.", "Choose another voice in the list.");
        g("EDGE", "VOICE", "\uD83C\uDF10", "Use Microsoft Edge for the best voices", "This browser may not have the Kannada, Tamil, Telugu, Malayalam, Urdu, Hindi or Marathi Natural voices.",
          "Open http://localhost:8089 in Microsoft Edge.", "Press the Windows key, type Edge, Enter, then paste the address.", "Copy the address.", "Open Edge and paste it.");
        g("INFO", "INFO", "\u2139\uFE0F", "For your information", "A note about what just happened.", "No action is needed.", "");
        // web / security
        g("BAD_REQUEST", "INPUT", "\uD83E\uDDE9", "The request was not understood", "The server received something it could not read - a missing or wrongly typed value.",
          "Go back and check the fields you filled in.", "If you typed a web address by hand, compare it with the link on the page.", "Go back.", "Check every field.", "Try again.");
        g("NOT_FOUND", "NOTFOUND", "\uD83D\uDD0D", "That page does not exist", "There is nothing at this address - a typing mistake, an old bookmark or a removed page.",
          "Go to the home page and use the buttons there.", "Addresses that start with /api only answer to buttons, not to the address bar.", "Check the spelling of the address.", "Open the home page.");
        g("METHOD_NOT_ALLOWED", "NOTFOUND", "\uD83D\uDEAB", "This address works only from a button", "You opened an address that accepts data from a button or form (POST) by typing it in the browser (GET).",
          "Use the button on the page instead of the address bar.", "Developers: this address answers POST only.", "Go to the home page.", "Press the button that does this job.");
        g("UNSUPPORTED_MEDIA", "INPUT", "\uD83D\uDCE4", "That kind of file is not accepted here", "The server expected a different format.", "Upload a PDF (or the format named on the page).", "Check the file extension.", "Choose the right file type.");
        g("SERVICE_UNAVAILABLE", "SERVER", "\uD83D\uDEA7", "Service temporarily unavailable", "A part of the app is not ready or is switched off.",
          "Wait a few seconds and retry.", "Check the black server window for a line that says why.", "Retry in a moment.");
        g("AUTH_REQUIRED", "SECURITY", "\uD83D\uDD11", "Please log in first", "This screen is for logged-in school users and your session has ended or never started.",
          "Log in again from the School panel.", "Sessions end after a long idle time - this protects student data on shared computers.", "Open the School panel.", "Choose Admin or Student login.");
        g("LOGIN_FAILED", "SECURITY", "\uD83D\uDD10", "Login did not work", "The user name or password is not correct.",
          "Check the user name, the Caps Lock key and that you chose the right portal (Admin or Student).", "Forgot the password? The admin can create a new one in Students, Reset password.", "Retype slowly.", "Ask the admin for a new password.");
        g("WRONG_PORTAL", "SECURITY", "\uD83D\uDEAA", "Wrong door", "This account belongs to the other portal (Admin accounts use Admin login, Students use Student login).",
          "Close this window and open the other login.", "Teachers who are admins: use Admin login.", "Open the School panel.", "Choose the other login.");
        g("ACCOUNT_DISABLED", "SECURITY", "\uD83D\uDEAB", "This account is switched off", "The admin has deactivated this login.", "Ask the school admin to activate it.", "Nothing is deleted - results are kept.", "Contact the admin.");
        g("ACCESS_DENIED", "SECURITY", "\u26D4", "You are not allowed here", "Your account does not have permission for this screen or file.",
          "Go back to your own screen.", "Students see only their class PDFs and the PDFs sent to them personally.", "Go back.", "Ask the admin if you should have access.");
        g("TOO_MANY_ATTEMPTS", "SECURITY", "\u23F3", "Too many tries", "Too many wrong passwords in a short time. The login pauses to protect the account.",
          "Wait 10 minutes, then try again.", "The admin can reset the password at once.", "Wait, then retry carefully.");
        g("SCHOOL_DB_OFFLINE", "DATABASE", "\uD83D\uDDC4\uFE0F", "The school database is not reachable", "The cloud database (Aiven MySQL) did not answer. The PDF reader keeps working - only school features pause.",
          "Check this computer's internet connection.", "Free Aiven databases fall asleep: open console.aiven.io, find the service and press Power on. Also check that port 11591 is not blocked by a firewall.",
          "Open console.aiven.io and power the service on.", "Check config/db-secret.properties has the right password.", "Press retry.");
        g("VALIDATION", "INPUT", "\u270D\uFE0F", "Please check what you entered", "One of the values is missing or not allowed.", "Read the reason above and correct that field.", "Class must be 1 to 10; passwords need at least 6 characters.", "Fix the field.", "Save again.");
        g("TEST_NOT_OPEN", "SCHOOL", "\uD83D\uDCC5", "This test is not open today", "Tests open automatically on their test date, for the students the teacher selected.",
          "Come back on the test date.", "The Student screen lists upcoming tests with their dates.", "Open My tests.", "Check the date.");
        g("TEST_TIME_UP", "SCHOOL", "\u23F0", "Time is up", "The test timer ended, so the answers were submitted automatically.", "Open Results to see your marks.", "Next time, answer the sure questions first.", "Open Results.");
        // clean, non-repeating step lists (the g(...) calls above give the wording of reason and tip)
        fix("TIMEOUT", "Try a smaller part of the document (split the PDF).", "Wait a moment, then press Analyze again.", "If it repeats, the file may be a scan - see the idea below.");
        fix("EMPTY_FILE", "Download or export the PDF again from where you got it.", "In Explorer, right-click the file, Properties: the size must be above 0 KB.", "Choose the file again and press Analyze.");
        fix("INVALID_PDF", "Open the file in Microsoft Edge - if Edge cannot show it, it is not a PDF.", "Open the original program (Word, PowerPoint, photo...).", "Use Print, then \"Microsoft Print to PDF\" to make a true PDF.", "Choose the new PDF and press Analyze.");
        fix("PASSWORD_PROTECTED", "Open the PDF in Edge and type its password.", "Press Ctrl+P and print to \"Microsoft Print to PDF\" to make an unlocked copy.", "Analyze the new copy.");
        fix("TOO_MANY_PAGES", "Split the PDF by chapter or in blocks of about 200 pages.", "Analyze one part at a time.");
        fix("MISSING_FILE", "Click the Choose file button.", "Select a PDF whose text can be selected with the mouse.", "Press Analyze.");
        fix("NO_TEXT", "Try to select a word on the page with the mouse - if you cannot, the page is a picture.", "Run OCR on the PDF (see the idea below).", "Analyze the OCR result.");
        fix("NON_TEXT_CONTENT_SKIPPED", "Nothing is broken - the text pages are read normally.", "Press Play to continue.");
        fix("INTERNAL_ERROR", "Press the same button again.", "Try a different PDF to see whether this one is the trigger.", "Look at the black server window for red lines and quote the reference code.");
        fix("TRANSLATION_ENGINE_OFFLINE", "Press \"Test translation\" - it shows which route fails and why.", "Check that this computer is online, then click the engine chip to retry.", "Or start Ollama: ollama serve, then ollama pull llama3.1.");
        fix("TRANSLATION_TIMEOUT", "Press Play again.", "Translate one page at a time (use a shorter page).");
        fix("TRANSLATION_FAILED", "Press \"Test translation\" for the exact failing route.", "Retry with another page.");
        fix("INVALID_LANGUAGE", "Open the Read in list.", "Pick one of the listed languages (Kannada, Tamil, Telugu, Malayalam, Urdu, Hindi, Marathi, English, Spanish, French).");
        fix("CHOOSE_LANGUAGE", "Open the Read in list.", "Pick a language, then press the button again.");
        fix("EMPTY_TEXT", "Go to a page that has text.", "Dimmed page numbers under the progress bar are picture pages.");
        fix("TEXT_TOO_LONG", "Translate one page at a time.", "Or press \"Translate JSON & speak\" - it goes page by page for you.");
        fix("NETWORK", "Check that the black server window \"PDF Voice And Games (8089)\" is still open.", "If not, double-click Application_run.bat.", "Reload this page when the window says it has started.");
        fix("NO_VOICE", "Open the app in Microsoft Edge - it carries the free Natural voices.", "Press \"Voice check\" to see what is installed.", "Choose another voice in the Voice list.");
        fix("EDGE", "Copy the address http://localhost:8089.", "Press the Windows key, type Edge, press Enter.", "Paste the address in Edge.");
        fix("BAD_REQUEST", "Go back and check the fields you filled in.", "Correct any empty or odd value.", "Try again.");
        fix("NOT_FOUND", "Check the spelling of the address.", "Open the home page and use the buttons there.");
        fix("METHOD_NOT_ALLOWED", "Go to the home page.", "Press the button that does this job instead of typing the address.");
        fix("AUTH_REQUIRED", "Open the School panel.", "Choose Admin login or Student login.");
        fix("LOGIN_FAILED", "Check the user name and that Caps Lock is off.", "Make sure you chose the right portal (Admin or Student).", "Ask the admin for a new password if you forgot it.");
        fix("WRONG_PORTAL", "Close this window.", "Open the School panel and choose the other login.");
        fix("ACCOUNT_DISABLED", "Ask the school admin to activate the account.");
        fix("ACCESS_DENIED", "Go back to your own screen.", "Ask the admin if you should have access.");
        fix("TOO_MANY_ATTEMPTS", "Wait 10 minutes, then try again carefully.", "Or ask the admin to reset the password.");
        fix("SCHOOL_DB_OFFLINE", "Check this computer's internet connection.", "Open console.aiven.io and power the database service on.", "Check config/db-secret.properties has the right password.", "Press retry.");
        fix("VALIDATION", "Read the exact message and correct that field.", "Save again.");
        fix("TEST_NOT_OPEN", "Come back on the test date.", "Open My tests to see the dates.");
        fix("TEST_TIME_UP", "Open Results to see your marks.");
        fix("SERVICE_UNAVAILABLE", "Wait a few seconds, then retry.");
        fix("UNSUPPORTED_MEDIA", "Choose the file type named on the page (usually a PDF).");
    }

    private ErrorGuide() { }

    private static void fix(String code, String... steps) {
        Guide o = CATALOG.get(code);
        CATALOG.put(code, new Guide(o.code(), o.category(), o.emoji(), o.headline(), o.reason(), List.of(steps), o.tip(), o.color1(), o.color2()));
    }

    /** g(code, category, emoji, headline, reason, firstStepOrIdea, tip, moreSteps...)  - see the usages above. */
    private static void g(String code, String category, String emoji, String headline, String reason, String step1, String tip, String... more) {
        String[] colors = THEME.get(category);
        List<String> steps = new java.util.ArrayList<>();
        steps.add(step1);
        steps.addAll(List.of(more));
        CATALOG.put(code, new Guide(code, category, emoji, headline, reason, List.copyOf(steps), tip, colors[0], colors[1]));
    }

    public static Guide forCode(String code) {
        Guide g = code == null ? null : CATALOG.get(code);
        return g != null ? g : CATALOG.get("INTERNAL_ERROR");
    }

    public static Guide forCode(ErrorCodeModel code) {
        return forCode(code == null ? null : code.name());
    }

    public static ErrorCodeModel codeForStatus(int status) {
        return switch (status) {
            case 400 -> ErrorCodeModel.BAD_REQUEST;
            case 401 -> ErrorCodeModel.AUTH_REQUIRED;
            case 403 -> ErrorCodeModel.ACCESS_DENIED;
            case 404 -> ErrorCodeModel.NOT_FOUND;
            case 405 -> ErrorCodeModel.METHOD_NOT_ALLOWED;
            case 408 -> ErrorCodeModel.TIMEOUT;
            case 413 -> ErrorCodeModel.FILE_TOO_LARGE;
            case 415 -> ErrorCodeModel.UNSUPPORTED_MEDIA;
            case 429 -> ErrorCodeModel.TOO_MANY_ATTEMPTS;
            case 503 -> ErrorCodeModel.SERVICE_UNAVAILABLE;
            default -> status >= 500 ? ErrorCodeModel.INTERNAL_ERROR : ErrorCodeModel.BAD_REQUEST;
        };
    }

    public static Map<String, Guide> all() {
        return java.util.Collections.unmodifiableMap(CATALOG);
    }

    /** What the person was doing, in words, from the request address. */
    public static String operationFor(String method, String path) {
        if (path == null) {
            return "Opening a page";
        }
        String[][] map = {
            {"/api/v1/documents/analyze", "Analyze PDF"}, {"/api/v1/translate/selftest", "Test translation"}, {"/api/v1/translate/status", "Check the translation engine"},
            {"/api/v1/translate", "Translate text"}, {"/api/v1/voice", "Load voices"}, {"/api/languages", "Load languages"}, {"/api/accents", "Load accents"},
            {"/school/api/auth/login", "Log in"}, {"/school/api/auth", "Account"}, {"/school/api/admin/students", "Manage students"},
            {"/school/api/admin/materials", "Upload or download PDFs"}, {"/school/api/admin/tests", "Create or edit a test"}, {"/school/api/admin/live", "Run a live class"},
            {"/school/api/admin", "Admin screen"}, {"/school/api/student/tests", "Take a test"}, {"/school/api/student/live", "Live class"},
            {"/school/api/student/games", "Games"}, {"/school/api/student", "Student screen"}, {"/school/files", "Open a PDF"}};
        for (String[] m : map) {
            if (path.startsWith(m[0])) {
                return m[1];
            }
        }
        return "GET".equalsIgnoreCase(method) ? "Opening " + path : (method == null ? "" : method) + " " + path;
    }
}
