package com.org.pdfplatform.launcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Opens the app in Microsoft Edge ONCE after start-up. Windows 10 Pro fix: the earlier build only tried
 * "cmd /c start msedge", which silently did nothing on PCs where msedge is not on the App Paths list. Now it tries,
 * in order: msedge.exe at its real install paths, "start msedge", the microsoft-edge: protocol, the default browser.
 * The first one that works wins; every failure is logged. Switch off with app.auto-open-browser=false
 * (Application_run.bat does that itself and opens Edge once the port answers).
 *
 * With app.browser-isolated-profile=true Edge starts with its own small profile folder, so Stop_Application.bat can
 * close exactly this window without touching the user's other Edge windows.
 */
@Component
public class EdgeLauncher {

    private static final Logger log = LoggerFactory.getLogger(EdgeLauncher.class);
    static final String PROFILE_MARK = "PdfVoiceAndGames";

    private final int port;
    private final boolean autoOpen;
    private final boolean isolated;
    private final String profileDir;
    private boolean launched;

    public EdgeLauncher(@Value("${server.port:8089}") int port,
                        @Value("${app.auto-open-browser:true}") boolean autoOpen,
                        @Value("${app.browser-isolated-profile:true}") boolean isolated,
                        @Value("${app.browser-profile-dir:}") String profileDir) {
        this.port = port;
        this.autoOpen = autoOpen;
        this.isolated = isolated;
        this.profileDir = profileDir == null ? "" : profileDir.trim();
    }

    @EventListener(ApplicationReadyEvent.class)
    public synchronized void onReady() {
        String url = "http://localhost:" + port;
        log.info("PDF Voice And Games is ready: {}   (use Microsoft Edge for the free Natural voices)", url);
        if (!autoOpen || launched) {
            return;
        }
        launched = true;
        Thread t = new Thread(() -> open(url), "edge-launcher");      // never delays start-up
        t.setDaemon(true);
        t.start();
    }

    private void open(String url) {
        String os = System.getProperty("os.name", "");
        String profile = isolated ? resolveProfileDir(os) : null;
        for (List<String> cmd : commands(os, url, profile, System.getenv())) {
            try {
                Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
                boolean ended = p.waitFor(8, TimeUnit.SECONDS);
                if (!ended || p.exitValue() == 0) {
                    log.info("Opened Microsoft Edge with: {}", cmd.get(0));
                    return;
                }
                log.warn("Browser start '{}' exited with code {}; trying the next way", cmd, p.exitValue());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.warn("Browser start '{}' failed: {}; trying the next way", cmd.get(0), e.getMessage());
            }
        }
        try {
            if (java.awt.Desktop.isDesktopSupported() && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(URI.create(url));
                log.info("Opened the default browser");
                return;
            }
        } catch (Exception e) {
            log.warn("Default browser could not be opened: {}", e.getMessage());
        }
        log.warn("Could not open a browser automatically. Open {} in Microsoft Edge yourself.", url);
    }

    private String resolveProfileDir(String os) {
        if (!profileDir.isEmpty()) {
            return profileDir;
        }
        String base = System.getenv("LOCALAPPDATA");
        if (base == null || base.isBlank()) {
            base = System.getProperty("user.home") + File.separator + (os.toLowerCase().contains("win") ? "AppData" + File.separator + "Local" : ".config");
        }
        return base + File.separator + PROFILE_MARK + File.separator + "edge-profile";
    }

    /** Package-visible for tests: the attempts, best first. */
    static List<List<String>> commands(String osName, String url, String profileDir, java.util.Map<String, String> env) {
        String os = osName.toLowerCase();
        List<String> flags = new ArrayList<>();
        if (profileDir != null) {
            flags.add("--user-data-dir=" + profileDir);
            flags.add("--no-first-run");
            flags.add("--no-default-browser-check");
        }
        flags.add("--new-window");
        List<List<String>> out = new ArrayList<>();
        if (os.contains("win")) {
            for (String exe : windowsEdgePaths(env)) {
                if (new File(exe).isFile()) {
                    List<String> c = new ArrayList<>();
                    c.add(exe);
                    c.addAll(flags);
                    c.add(url);
                    out.add(c);
                }
            }
            List<String> viaStart = new ArrayList<>(List.of("cmd", "/c", "start", "", "msedge"));     // "" = the window-title argument of START
            viaStart.addAll(flags);
            viaStart.add(url);
            out.add(viaStart);
            out.add(List.of("cmd", "/c", "start", "", "microsoft-edge:" + url));
            out.add(List.of("rundll32", "url.dll,FileProtocolHandler", url));
        } else if (os.contains("mac")) {
            List<String> c = new ArrayList<>(List.of("open", "-na", "Microsoft Edge", "--args"));
            c.addAll(flags);
            c.add(url);
            out.add(c);
        } else {
            List<String> c = new ArrayList<>(List.of("microsoft-edge"));
            c.addAll(flags);
            c.add(url);
            out.add(c);
        }
        return out;
    }

    static List<String> windowsEdgePaths(java.util.Map<String, String> env) {
        List<String> paths = new ArrayList<>();
        for (String[] k : new String[][]{{"ProgramFiles(x86)", "C:\\Program Files (x86)"}, {"ProgramFiles", "C:\\Program Files"}}) {
            String base = env.getOrDefault(k[0], k[1]);
            paths.add(base + "\\Microsoft\\Edge\\Application\\msedge.exe");
        }
        String local = env.get("LOCALAPPDATA");
        if (local != null && !local.isBlank()) {
            paths.add(local + "\\Microsoft\\Edge\\Application\\msedge.exe");
        }
        return paths;
    }
}
