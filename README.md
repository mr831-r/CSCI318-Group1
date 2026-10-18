# LARP Microservices

This project implements a simplified Spring Boot version of the LARP specification using a single application with logically separated microservice-style packages.

## Microservices included

- Database service: generic persistence for inventory, log, and shipping records
- Database editor: validation and update workflow
- Log viewer: audit trail recording and date-range queries
- Shipping manager: queue tracking and dispatch updates
- AI assistant: keyword-based stock lookup and explanation endpoint

## Run the project

This project requires Java 21, docker and docker-compose.
To run the application, run the following depending on your system:

Windows:
```bash
docker compose up -d
./mvnw spring-boot:run
```

Linux:
Ensure docker has started with `sudo systemctl start docker`. Then run:
```bash
sudo docker compose up -d
./mvnw spring-boot:run
```

Then open:

- http://localhost:48484/ for the service status page
- http://localhost:48484/console for the interactive UI
- http://localhost:48484/health for the health endpoint
- http://localhost:48484/h2-console for the in-memory database inspection page

### H2 console credentials

The app uses an in-memory H2 database with the following settings:

- JDBC URL: `jdbc:h2:mem:larpdb`
- Username: `sa`
- Password: empty

This is configured in `application.properties`, and the console servlet is registered explicitly for the Spring Boot 4 / Jakarta servlet stack so the page resolves correctly.

### Kafka

The broker defaults to `localhost:9092`; override it with `KAFKA_BROKERS` when needed. The Database REST base URL used by Kafka consumers defaults to `http://localhost:48484` and can be overridden with `DATABASE_SERVICE_URL`.

Events are published after their corresponding existing operation succeeds:

- `item-events` - editor item additions, updates, and removals; consumed by the Log Viewer
- `ai-events` - AI searches; consumed by the Log Viewer
- `stock-events` - shipping stock arrivals; consumed by the Log Viewer and stock-level processor

The Log Viewer persists consumed events by calling `PUT /database/logs`; Kafka consumers do not write directly to H2. Stock event records are keyed by item code. The Kafka Streams stock-level processor aggregates arrivals and dispatches and updates inventory through the existing `POST /database/{filter}` endpoint.

## TODO: Add endpoints and examples

## Workflow overview

### Typical usage flow
1. Seed data by sending an empty body to `PUT /editor/prepopulate`, or bulk load records via the same endpoint.
2. Query inventory using `GET /database/{filter}` or the AI endpoint `PUT /ai`.
3. Update or validate stock through `PUT /editor`.
4. Confirm stock arrival with `PUT /shipping`.
5. Inspect event history using `GET /log` and date-range filtering.
6. Review service status with `GET /health` and the root status endpoint `GET /`.

## Notes

The app exposes a root endpoint at `/` and a health endpoint at `/health` so the default localhost page does not return a Whitelabel 404 when the service is running.
