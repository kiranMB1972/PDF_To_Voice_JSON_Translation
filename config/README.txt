config/db-secret.properties     the Aiven MySQL password (read at start-up; or set the environment variable SPRING_DATASOURCE_PASSWORD instead)
config/FIRST_ADMIN_LOGIN.txt    created on the very first School start: user "admin" and a random password - change it at the first login, then delete the file
Host, port, database and user are in src/main/resources/application.yml (spring.datasource.url / username); override with DB_HOST, DB_PORT, DB_NAME, DB_USER.
