package com.org.games;

import com.org.games.model.GameInfo;
import com.org.games.service.GameCatalogService;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class GameCatalogServiceTest {

    private final GameCatalogService catalog = new GameCatalogService();

    private static String read(String path) throws Exception {
        try (InputStream in = GameCatalogServiceTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing classpath resource " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void fiveCharacterGamesAndFourVehicleGamesWithUniqueIds() {
        assertEquals(9, catalog.all().size());
        assertEquals(5, catalog.all().stream().filter(g -> g.group().equals(GameCatalogService.HS)).count());
        assertEquals(2, catalog.all().stream().filter(g -> g.type().equals("car")).count());
        assertEquals(2, catalog.all().stream().filter(g -> g.type().equals("bike")).count());
        assertEquals(9, catalog.all().stream().map(GameInfo::id).distinct().count());
    }

    @Test
    void theAttachedProjectsGameIdsSurvive() {
        for (String id : new String[]{"maze", "memory", "runner"}) {
            assertTrue(catalog.byId(id).isPresent(), id);
        }
        assertEquals("Hotstuff Fire Maze", catalog.byId("maze").orElseThrow().name());
    }

    @Test
    void everyGameFileShipsAndUsesTheSharedKit() throws Exception {
        for (GameInfo g : catalog.all()) {
            String html = read("/static/games/" + g.file());
            assertTrue(html.contains("<script src=\"common.js\"></script>"), g.file());
            assertTrue(html.contains("GK.start"), g.file());
        }
        assertTrue(read("/static/games/common.js").contains("close-game"));
    }

    @Test
    void noGamePageLoadsAnythingFromTheInternetAndNoneUsesBlack() throws Exception {      // the attached page loaded three.js from unpkg.com
        for (GameInfo g : catalog.all()) {
            String html = read("/static/games/" + g.file()).replaceAll("xmlns='[^']*'", "");
            assertFalse(html.contains("http://") || html.contains("https://"), g.file());
            assertFalse(html.matches("(?s).*background:\\s*#000[;\"'}].*"), g.file());
        }
    }

    @Test
    void characterFilterKeepsSharedGamesAndDropsOtherCharacters() {
        assertEquals(9, catalog.forCharacter(null).size());
        assertEquals(9, catalog.forCharacter("all").size());
        var hot = catalog.forCharacter("hotstuff");
        assertTrue(hot.stream().anyMatch(g -> g.id().equals("maze")));
        assertTrue(hot.stream().anyMatch(g -> g.id().equals("quiz")), "shared (both) games stay");
        assertTrue(hot.stream().noneMatch(g -> g.id().equals("memory")), "Charmaine's own game is not in Hotstuff's list");
        assertTrue(hot.stream().noneMatch(g -> g.type().equals("car")));
    }
}
