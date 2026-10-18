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
- `stock-events` - shipping stock arrivals; consumed by the Log Viewer

The Log Viewer persists consumed events by calling `PUT /database/logs`; Kafka consumers do not write directly to H2. Stock event records are keyed by item code. The shipping endpoint updates inventory quantity synchronously before publishing its `STOCK_ARRIVED` audit event, so the stock change is immediately visible and does not depend on Kafka Streams processing.

## API endpoints and examples

| Method | Endpoint | Purpose |
|--------|----------|---------|
| `PUT` | `/database` or `/database/{filter}` | Create a database item |
| `GET` | `/database/{filter}?start={date}&end={date}` | Read matching records; date bounds are optional |
| `POST` | `/database/{itemCode}` | Update the latest matching item |
| `DELETE` | `/database/{itemCode}` | Delete the latest matching item |
| `PUT`, `GET`, `DELETE` | `/editor`, `/editor/prepopulate`, `/editor/{itemCode}` | Add/list/prepopulate/remove inventory |
| `GET` | `/editor/statuses` | Get inventory statuses used by the console |
| `GET`, `PUT`, `DELETE` | `/log`, `/log/item`, `/log/shipping`, `/log/ai` | Read and write logs |
| `GET` | `/log/stream` | Subscribe to log updates using Server-Sent Events |
| `PUT`, `GET` | `/shipping` | Confirm and list shipping arrivals |
| `GET`, `PUT`, `POST` | `/ai` | List inventory or search it |
| `GET` | `/health` | Check service health |

Inventory statuses are `AVAILABLE`, `SHIPPING`, and `ARRIVED`. The console loads
these options from `/editor/statuses`; status strings in requests are accepted
case-insensitively. Shipping request statuses are `PENDING` and `ARRIVED`.

### Examples

Create or update inventory through the editor:

```http
PUT /editor
Content-Type: application/json

{
  "itemCode": "ITEM-3001",
  "name": "Steel spear",
  "description": "Steel spear details",
  "quantity": 7,
  "status": "AVAILABLE",
  "date": "2026-09-09",
  "payslipReference": "manual-entry"
}
```

Confirm an arrival. The received quantity is added to the current inventory
quantity immediately; the inventory status becomes `ARRIVED`. A `STOCK_ARRIVED`
event is also published to Kafka for audit logging.

```http
PUT /shipping
Content-Type: application/json

{
  "itemCode": "ITEM-3001",
  "destination": "North Camp",
  "quantity": 3,
  "requestedDate": "2026-09-09"
}
```

Read an item's records in a date range:

```http
GET /database/ITEM-3001?start=2026-09-01&end=2026-09-30
```

## Workflow overview

### Typical usage flow
1. Seed data by sending an empty body to `PUT /editor/prepopulate`, or bulk load records via the same endpoint.
2. Query inventory using `GET /database/{filter}` or the AI endpoint `PUT /ai`.
3. Update or validate inventory through `PUT /editor`.
4. Confirm stock arrival with `PUT /shipping`.
5. Inspect event history using `GET /log` and date-range filtering.
6. Review service status with `GET /health` and the root status endpoint `GET /`.

## Notes

The app exposes a root endpoint at `/` and a health endpoint at `/health` so the default localhost page does not return a Whitelabel 404 when the service is running.
