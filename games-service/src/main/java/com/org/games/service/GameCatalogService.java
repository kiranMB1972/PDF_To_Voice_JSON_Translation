package com.org.games.service;

import com.org.games.model.GameInfo;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class GameCatalogService {

    public static final String HS = "Hotstuff & Princess Charmaine";
    public static final String VEHICLES = "Cars & Bikes";

    private static final List<GameInfo> GAMES = List.of(
            new GameInfo("maze", "Hotstuff Fire Maze", HS, "hotstuff", "maze", "Easy", "\uD83D\uDD25", "hotstuff-fire-maze.html",
                    "Guide Hotstuff through fresh mazes to the diamond. Grab Charmaine's crown for bonus time.",
                    "Arrows / WASD or the touch pad"),
            new GameInfo("memory", "Charmaine Memory Flip", HS, "charmaine", "memory", "Medium", "\uD83D\uDC51", "charmaine-memory-flip.html",
                    "Flip the cards and find every matching pair - fewer moves, more points.",
                    "Tap or click a card"),
            new GameInfo("runner", "Hotstuff Fire Runner", HS, "hotstuff", "runner", "Medium", "\uD83C\uDFC3", "hotstuff-fire-runner.html",
                    "Run, jump the ice blocks and collect diamonds.",
                    "Space / Up / tap to jump (hold = higher)"),
            new GameInfo("quiz", "Hotstuff & Charmaine Quiz", HS, "both", "quiz", "Easy", "\u2753", "hotstuff-charmaine-quiz.html",
                    "Ten friendly questions - answer fast for more points.",
                    "Tap an answer, or keys 1 / 2 / 3"),
            new GameInfo("stars", "Charmaine Star Catcher", HS, "charmaine", "catcher", "Easy", "\u2B50", "charmaine-star-catcher.html",
                    "Slide the princess to catch falling stars and hearts. Dodge the embers!",
                    "Left / Right or drag"),
            new GameInfo("highway-racer", "Gold Highway Racer", VEHICLES, "none", "car", "Medium", "\uD83D\uDE97", "highway-racer.html",
                    "Weave through traffic, grab gold coins, burn nitro for speed.",
                    "Left/Right or A/D steer - Space = nitro - Up/Down = gas/brake"),
            new GameInfo("neon-circuit", "Neon Circuit", VEHICLES, "none", "car", "Hard", "\uD83C\uDFCE\uFE0F", "neon-circuit.html",
                    "120-second time attack: drift round the circuit for the most laps.",
                    "Arrows/WASD drive - Space = handbrake drift"),
            new GameInfo("moto-dash", "Moto Dash", VEHICLES, "none", "bike", "Medium", "\uD83C\uDFCD\uFE0F", "moto-dash.html",
                    "Endless bike run: jump cones, barrels and road gaps.",
                    "Space / Up / tap = jump (hold for height) - Down = fast fall"),
            new GameInfo("hill-climb-bike", "Hill Climb Bike", VEHICLES, "none", "bike", "Hard", "\uD83D\uDEB5", "hill-climb-bike.html",
                    "Climb rolling hills, catch air, land flips and keep fuel up.",
                    "Right/Up = gas - Left/Down = brake - in the air they tilt the bike"));

    public List<GameInfo> all() { return GAMES; }

    public Optional<GameInfo> byId(String id) {
        return GAMES.stream().filter(g -> g.id().equalsIgnoreCase(id)).findFirst();
    }

    /** character: all (or blank) = everything; hotstuff / charmaine = that character's games plus the shared ones. */
    public List<GameInfo> forCharacter(String character) {
        if (character == null || character.isBlank() || character.equalsIgnoreCase("all")) {
            return GAMES;
        }
        String c = character.toLowerCase();
        return GAMES.stream().filter(g -> g.character().equals(c) || g.character().equals("both")).toList();
    }
}
