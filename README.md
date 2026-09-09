# LARP Microservices

This project implements a simplified Spring Boot version of the LARP specification using a single application with logically separated microservice-style packages.

## Microservices included

- Database service: inventory storage and search
- Database editor: validation and update workflow
- Log viewer: audit trail recording and date-range queries
- Shipping manager: queue tracking and dispatch updates
- AI assistant: keyword-based stock lookup and explanation endpoint

## Run the project

This project requires Java 21. Set it explicitly before running Maven:

```bash
./mvnw spring-boot:run
```

Then open:

- http://localhost:48484/ for the service status page
- http://localhost:48484/console for the interactive AI, editor, and log console
- http://localhost:48484/health for the health endpoint
- http://localhost:48484/h2-console for the in-memory database inspection page

### H2 console credentials

The app uses an in-memory H2 database with the following settings:

- JDBC URL: `jdbc:h2:mem:larpdb`
- Username: `sa`
- Password: empty

This is configured in `application.properties`, and the console servlet is registered explicitly for the Spring Boot 4 / Jakarta servlet stack so the page resolves correctly.

## API endpoints and descriptions

### Root and health
- `GET /` - redirects to the console
- `GET /health` - health check endpoint returning service status
- `GET /console` - browser-based console for AI, editor, and log interaction
- `GET /h2-console` - H2 in-memory database console

### Database service
- `PUT /database` - create a new inventory item record
  - Request body: `InventoryItem` with `itemCode`, `description`, `quantity`, `status`, `date`, and optional `payslipReference`
- `GET /database/{filter}` - read items matching the supplied filter (item code or description keyword)
- `GET /database/{filter}?start=YYYY-MM-DD&end=YYYY-MM-DD` - return historical inventory records within a date range
- `POST /database/{filter}` - update the most recent matching inventory record
- `DELETE /database/{filter}` - remove the most recent matching inventory record

### Database editor service
- `PUT /editor` - add or update a single inventory item after validation
  - Validates required fields: item code, description, quantity, status, and date
- `PUT /editor/prepopulate` - bulk insert inventory records; if empty body is sent, it seeds realistic sample data automatically
- `GET /editor` - fetch all current inventory items
- `POST /editor/seed` - create a small set of fake inventory items for demos and testing
- `DELETE /editor` - delete an item using a JSON body such as `{ "itemCode": "ITEM-1001" }`
- `DELETE /editor/{itemCode}` - delete an item by its item code path value

### Log viewer service
- `GET /log` - list all log entries
- `GET /log?start=YYYY-MM-DD&end=YYYY-MM-DD` - filter log entries by date range
- `PUT /log` - create a generic log event
  - Request body example: `{ "message": "System event", "level": "INFO" }`
- `PUT /log/item` - record a database item action event
- `DELETE /log/item` - record an item removal event
- `PUT /log/shipping` - record a shipping-related event
  - Request body example: `{ "itemCode": "ITEM-1001", "status": "ARRIVED" }`
- `PUT /log/ai` - record an AI search event
  - Request body example: `{ "query": "iron sword" }`

### Shipping manager service
- `PUT /shipping` - confirm that a shipment has arrived and update the item status to `ARRIVED`
  - Request body includes item code, shipping firm/origin, quantity received, status, and arrival date
  - This endpoint matches the original specification requirement: the shipping API reports that stock has arrived and the database item should reflect the arrival state
- `GET /shipping` - list all shipping arrival confirmations

### AI assistant service
- `GET /ai` - return all inventory items as a basic AI-style stock view
- `PUT /ai` - perform an AI-style database search using a natural language request body and optional date bounds
  - Request body example: `{ "query": "sword", "startDate": "2026-09-01", "endDate": "2026-09-10" }`
- `POST /ai` - same as above, used for search-style interaction with the AI endpoint

## Database design note

The specification in `ai/context.md` describes an abstract `IDatabaseItem` contract for database segregation and filtering. In a production-grade multi-service design, that would typically be implemented as a common interface or shared domain model across service-specific entities.

This project keeps a simplified single-application architecture, so the same concept is represented as a shared `DatabaseItem` interface implemented by the main persisted types used across the app:

- `InventoryItem` implements `DatabaseItem` and exposes the logistics filter name
- `LogEntry` implements `DatabaseItem` and exposes the log filter name
- `ShippingRequest` implements `DatabaseItem` and exposes the shipping filter name

This preserves the specification’s intent of common filtering and segregation semantics without forcing a full multiple-table inheritance hierarchy into a compact Spring Boot service that uses JPA repositories directly.

## Workflow overview

### Typical usage flow
1. Seed data using `POST /editor/seed` or bulk load via `PUT /editor/prepopulate`.
2. Query inventory using `GET /database/{filter}` or the AI endpoint `PUT /ai`.
3. Update or validate stock through `PUT /editor`.
4. Confirm stock arrival with `PUT /shipping`.
5. Inspect event history using `GET /log` and date-range filtering.
6. Review service status with `GET /health` and the root status endpoint `GET /`.

## Notes

The app exposes a root endpoint at `/` and a health endpoint at `/health` so the default localhost page does not return a Whitelabel 404 when the service is running.

The project intentionally keeps the architecture close to the provided logical design while remaining practical for a single Spring Boot application: JPA repositories, REST controllers, validation, Thymeleaf UI, and an H2 in-memory database.
