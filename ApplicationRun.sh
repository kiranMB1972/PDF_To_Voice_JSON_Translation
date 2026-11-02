#!/bin/sh
# Starts the Games microservice (8095) in the background, then the main app (8089); the app opens Microsoft Edge.
cd "$(dirname "$0")"
(cd games-service && mvn -q spring-boot:run) &
mvn -q spring-boot:run
