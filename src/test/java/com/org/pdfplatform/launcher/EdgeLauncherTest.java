package com.org.pdfplatform.launcher;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EdgeLauncherTest {

    private static final String URL = "http://localhost:8089";

    @Test
    void windowsTriesStartMsedgeThenTheProtocolThenTheDefaultBrowser() {
        List<List<String>> c = EdgeLauncher.commands("Windows 10", URL, null, Map.of());
        assertTrue(c.contains(List.of("cmd", "/c", "start", "", "msedge", "--new-window", URL)));   // "" = START's title argument
        assertTrue(c.contains(List.of("cmd", "/c", "start", "", "microsoft-edge:" + URL)));
        assertEquals(List.of("rundll32", "url.dll,FileProtocolHandler", URL), c.get(c.size() - 1));
    }

    @Test
    void isolatedProfileAddsItsOwnFolderAndNoFirstRunFlags() {
        List<List<String>> c = EdgeLauncher.commands("Windows 10", URL, "C:\\x\\PdfVoiceAndGames\\edge-profile", Map.of());
        List<String> viaStart = c.stream().filter(l -> l.contains("msedge")).findFirst().orElseThrow();
        assertTrue(viaStart.contains("--user-data-dir=C:\\x\\PdfVoiceAndGames\\edge-profile"));
        assertTrue(viaStart.contains("--no-first-run"));
        assertEquals(URL, viaStart.get(viaStart.size() - 1));
    }

    @Test
    void edgeInstallPathsComeFromTheEnvironment() {
        List<String> p = EdgeLauncher.windowsEdgePaths(Map.of("LOCALAPPDATA", "C:\\Users\\a\\AppData\\Local"));
        assertTrue(p.contains("C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe"));
        assertTrue(p.contains("C:\\Users\\a\\AppData\\Local\\Microsoft\\Edge\\Application\\msedge.exe"));
    }

    @Test
    void macAndLinuxCommands() {
        assertEquals(List.of("open", "-na", "Microsoft Edge", "--args", "--new-window", URL), EdgeLauncher.commands("Mac OS X", URL, null, Map.of()).get(0));
        assertEquals(List.of("microsoft-edge", "--new-window", URL), EdgeLauncher.commands("Linux", URL, null, Map.of()).get(0));
    }

    @Test
    void launchesNothingWhenSwitchedOff() {
        EdgeLauncher off = new EdgeLauncher(8089, false, true, "");
        off.onReady();
        off.onReady();                                                      // autoOpen=false: nothing is started, nothing throws
    }
}
