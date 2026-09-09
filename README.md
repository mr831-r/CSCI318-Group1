# LARP Microservices

This project implements a simplified Spring Boot version of the LARP specification using a single application with logically separated microservice-style packages.

## Microservices included

- Database service: inventory storage and search
- Database editor: validation and update workflow
- Log viewer: audit trail recording and date-range queries
- Shipping manager: queue tracking and dispatch updates
- AI assistant: keyword-based stock lookup and explanation endpoint

## Run the project

From the project root, run:

```bash
./mvnw spring-boot:run
```

Then open:

- http://localhost:8081/ for the service status page
- http://localhost:8081/health for the health endpoint
- http://localhost:8081/h2-console for the in-memory database inspection page

## Main endpoints

### Database
- `PUT /database` - add item
- `GET /database/{filter}` - query item by item code or description
- `GET /database/{filter}?start=2026-01-01&end=2026-12-31` - history by date range
- `POST /database/{filter}` - update item
- `DELETE /database/{filter}` - remove item

### Editor
- `PUT /editor` - add or update an inventory item
- `GET /editor` - fetch all items
- `DELETE /editor/{itemCode}` - delete item

### Logs
- `GET /log` - list log entries
- `GET /log?start=...&end=...` - filter by date range

### Shipping
- `PUT /shipping` - create shipping request
- `GET /shipping` - list requests

### AI assistant
- `PUT /ai` - search stock based on natural language input

## Notes

The app now exposes a root endpoint at `/` and a health endpoint at `/health` so the default localhost page does not return a Whitelabel 404 when the service is running.

This implementation keeps the structure close to the provided logical architecture while using Spring Boot features that are available in the project, such as JPA repositories, REST controllers, and validation.
