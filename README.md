# Backend

This is a Spring Boot 3.3.x application using Java 17 and MongoDB.

## Stable Branch

- The stable branch is `stable_branch`.

## Requirements

- Java `17`
- MongoDB running locally or remotely
- Maven (or use the Maven Wrapper)

## Configuration

Set the following properties (prefer environment variables to avoid committing secrets):

- Server: `server.port=8080`
- MongoDB: `spring.data.mongodb.uri`, `spring.data.mongodb.database`
- JWT: `jwt.secret`, `jwt.expiration`
- Mail (SMTP): `spring.mail.host`, `spring.mail.port`, `spring.mail.username`, `spring.mail.password`
- Frontend URL: `app.frontend.url`
- CORS: `cors.allowed-origins` (comma-separated origins)
- Twilio (optional): `twilio.account.sid`, `twilio.auth.token`, `twilio.phone.number`

Example local `application.properties` (do not commit real secrets):

```
server.port=8080
spring.data.mongodb.uri=mongodb://localhost:27017/event_db
spring.data.mongodb.database=event_db

jwt.secret=<your-256-bit-secret>
jwt.expiration=86400000

spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=<your-email>
spring.mail.password=<your-app-password>

app.frontend.url=http://localhost:4200
cors.allowed-origins=http://localhost:3000,http://localhost:4200

twilio.account.sid=<your-sid>
twilio.auth.token=<your-token>
twilio.phone.number=<your-phone>
```

## Running Locally

- Windows: `./mvnw.cmd spring-boot:run`
- macOS/Linux: `./mvnw spring-boot:run`
- Maven: `mvn spring-boot:run`

The API serves on `http://localhost:8080/` by default.

## API Docs

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`

## Notes

- Keep secrets out of version control; use environment variables or a local, uncommitted `application.properties`.
- Update `cors.allowed-origins` to include your frontend origin.
