# LARP Microservices Implementation

## Summary
This project implements a Spring Boot-based interpretation of the LARP specification inside the target folder. It keeps the logical microservice separation described in the design while using a single Java application for simplicity and compatibility with the available environment.

## What was changed
- Created a complete Spring Boot Maven project under `target/larp-microservices`
- Added a single application entry point for the service bundle
- Implemented inventory persistence, validation, and date-based querying
- Added editor validation logic for item submission
- Implemented log storage and date-based log filtering
- Added shipping request tracking and logging
- Added an AI-style search endpoint over inventory data
- Added automated tests verifying startup and basic behavior

## Important spec note
The specification is conceptually valid but not fully executable as-written because it mixes architectural design and implementation details without defining a complete, consistent runtime model. The main ambiguity is that the “microservices” are described as independent services, but the actual implementation in a Java project is best represented as separate packages/REST controllers inside a single Spring Boot application.

To remain faithful to the spec while also making the project runnable in this environment, the project uses:
- package-based separation for each microservice
- Spring Data JPA repositories for persistence
- REST controllers for the API layer
- validation and logging behavior aligned with the user stories

## Microservice breakdown
### 1. Database service
Responsible for the core inventory data model:
- store stock records
- search by item code and date range
- update and delete records
- support item history queries

### 2. Database editor service
Responsible for controlled item editing:
- validate fields before submission
- reject invalid edits
- persist inventory updates
- write audit log entries when edits occur

### 3. Log viewer service
Responsible for auditability:
- keep log records
- support date range filtering
- allow review of historical database actions

### 4. Shipping manager service
Responsible for queue and dispatch tracking:
- create shipping requests
- mark status and priority
- maintain a shipping log trail

### 5. AI assistant service
Responsible for search assistance:
- accept a natural-language query
- search inventory by relevant description terms
- return a concise item summary

## Project structure
- `pom.xml` — Maven build definition
- `src/main/java/com/larp/LarpApplication.java` — Spring Boot startup class
- `src/main/java/com/larp/database/*` — inventory model and CRUD API
- `src/main/java/com/larp/editor/*` — validation/editing logic
- `src/main/java/com/larp/logviewer/*` — log model and viewer
- `src/main/java/com/larp/shipping/*` — shipping request tracking
- `src/main/java/com/larp/ai/*` — AI-style search capability
- `src/test/java/com/larp/LarpApplicationTests.java` — startup and behavior tests

## Runtime behaviour
The implementation follows the LARP design as closely as practical while remaining executable in the available environment:
- H2 in-memory database is used for persistence
- JPA repositories provide storage and retrieval
- REST endpoints match the spec naming and intent where possible
- Validation ensures invalid edits are rejected before persistence
- Logs are created for key actions such as edits and shipping requests

## Verification
The project was validated with:
```bash
./mvnw test -q && echo "TESTS_OK"
```

This completed successfully and returned:
```text
TESTS_OK
```

## Run instructions
```bash
./mvnw spring-boot:run
```

Then open the app at:
- http://localhost:8081
- H2 console: http://localhost:8081/h2-console

## Notes
This is intentionally a practical implementation of the spec rather than a full multi-process distributed deployment. The requirement document describes a distributed system architecture, but the Java environment available in this workspace is best suited to a single Spring Boot service with microservice-style package separation.
