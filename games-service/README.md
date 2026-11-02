# games-service (Entertainment Games microservice)

Spring Boot 3.3 / Java 21, port **8095**. `RunGames.bat` or `mvn spring-boot:run`.

* `GET /api/games[?character=all|hotstuff|charmaine]`, `/api/games/{id}` - catalogue (async, CompletableFuture on virtual threads)
* `GET /api/characters`, `/api/characters/hotstuff|charmaine`
* `GET /api/ai/recommend?mood=brave|puzzle|magic|smart|race|bike|fun`
* `GET /api/health`
* `/` lists the games; `/games/<file>.html` are the playable games (offline, no external scripts)

Tests: `mvn test` and `cd js-tests && npm install && npm test` (headless play-through of all nine games).
