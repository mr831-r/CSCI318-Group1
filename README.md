# LARP Microservices

This project implements a simplified Spring Boot version of the LARP specification using a single application with logically separated microservice-style packages.

## Microservices included

- Database service: generic persistence for inventory, log, and shipping records
- Database editor: validation and update workflow
- Log viewer: audit trail recording and date-range queries
- Shipping manager: stock arrivals, a queue of shipping requests from camps, and dispatch with a courier
- AI assistant: an LLM agent that answers questions with tools, learns terminology, reacts to stock events and
  proposes changes for a person to approve; plus a fast keyword search that uses the learned terminology

## Run the project

This project requires Java 21, docker and docker-compose.
To run the application, run the following depending on your system:

Windows:
```bash
docker compose up -d
<<<<<<< HEAD
=======
./mvnw spring-boot:run
```

Linux:
Ensure docker has started with `sudo systemctl start docker`. Then run:
```bash
sudo docker compose up -d
>>>>>>> 3aee914b11d4fe54347f4e181b6413330c75b1b7
./mvnw spring-boot:run
```

Linux:
Ensure docker has started with `sudo systemctl start docker`. Then run:
```bash
sudo docker compose up -d
./mvnw spring-boot:run
```

To use the AI agent, set an LLM API key first (a Google AI Studio Gemini key by default). Without a key everything
else, including keyword search at `/ai`, still works.

```powershell
# Windows PowerShell
$env:AI_API_KEY = "your-key"
# optional: $env:AI_PROVIDER = "openai"   # gemini (default) | openai | anthropic
```
```bash
# Linux/macOS
export AI_API_KEY=your-key
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
<<<<<<< HEAD
- `stock-events` - `STOCK_ARRIVED`, `STOCK_DISPATCHED` and `SHIPPING_REQUESTED`; consumed by the Log Viewer and by the
  AI agent's stock reviewer (consumer group `larp-agent`)
- `agent-events` - every AI agent decision (chats, proposals, approvals, learned terms, stock reviews); consumed by
  the Log Viewer and logged with level `AGENT`
=======
- `stock-events` - shipping stock arrivals; consumed by the Log Viewer
>>>>>>> 3aee914b11d4fe54347f4e181b6413330c75b1b7

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
<<<<<<< HEAD
| `PUT`, `GET` | `/shipping` | Confirm arrivals; list all shipping records |
| `PUT` | `/shipping/requests` | A camp requests stock (status `PENDING`) |
| `GET` | `/shipping/requests?status=` | Request queue, oldest first (default: everything not `ARRIVED`) |
| `POST` | `/shipping/requests/{id}/dispatch` | `{"courier":"..."}` takes the stock and marks the request `DISPATCHED` (409 if not enough stock) |
| `GET`, `PUT`, `POST` | `/ai` | List inventory or keyword search it (uses learned terminology) |
| `POST` | `/ai/chat` | `{"sessionId":"optional","message":"..."}` ask the AI agent; returns its reply, the tools it called and pending proposals |
| `GET` | `/ai/status` | LLM provider/model and whether a key is configured |
| `GET` | `/ai/actions?sessionId=` | Changes the agent has proposed |
| `POST` | `/ai/actions/{id}/approve`, `/ai/actions/{id}/reject` | Approve (runs it through the Editor/Shipping service) or reject a proposal |
| `DELETE` | `/ai/sessions/{sessionId}` | Forget a conversation |
=======
| `PUT`, `GET` | `/shipping` | Confirm and list shipping arrivals |
| `GET`, `PUT`, `POST` | `/ai` | List inventory or search it |
>>>>>>> 3aee914b11d4fe54347f4e181b6413330c75b1b7
| `GET` | `/health` | Check service health |

Inventory statuses are `AVAILABLE`, `SHIPPING`, and `ARRIVED`. The console loads
these options from `/editor/statuses`; status strings in requests are accepted
<<<<<<< HEAD
case-insensitively. Shipping record statuses are `PENDING` (requested by a camp),
`DISPATCHED` (sent with a courier) and `ARRIVED` (incoming stock received).
=======
case-insensitively. Shipping request statuses are `PENDING` and `ARRIVED`.
>>>>>>> 3aee914b11d4fe54347f4e181b6413330c75b1b7

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
<<<<<<< HEAD

Request stock for a camp, then dispatch it:

```http
PUT /shipping/requests
Content-Type: application/json

{ "itemCode": "ITEM-1002", "destination": "South Camp", "quantity": 5 }
```
```http
POST /shipping/requests/1/dispatch
Content-Type: application/json

{ "courier": "Fast Horse Couriers" }
```

Ask the AI agent:

```http
POST /ai/chat
Content-Type: application/json

{ "message": "Which pending requests can we send now?" }
```

## AI agent

The AI Assistant (`com.larp.ai`) is built with [LangChain4j](https://docs.langchain4j.dev). On each message the
LLM decides which tools to call, chains them (up to 10 calls), and answers from the results. Each conversation
(`sessionId`) has its own memory.

| Tool | Uses | Effect |
|------|------|--------|
| `searchInventory`, `getItem`, `findLowStock` | `AiSearchService`, `DatabaseService` | Read |
| `getItemHistory`, `readLogs` | `LogService` | Read |
| `listShipments` | `ShippingService` | Read |
| `listLearnedTerms`, `learnTerm` | `TerminologyService` (`TermAlias`, filter `term-alias`) | `learnTerm` saves a term |
| `proposeQuantityChange`, `proposeStatusChange`, `proposeStockArrival`, `proposeDispatch` | `AgentActionService` | Proposal only |

- **Learning terminology:** when a search fails because of an unfamiliar word, the agent works out what it means
  and calls `learnTerm`. Every later search, including keyword search at `/ai`, then understands the word.
- **Human in the loop:** the agent cannot change stock. Proposals wait in `/ai/actions` (and the console) until a
  person approves them. Approved proposals run through `EditorService.validateAndSave`,
  `ShippingService.confirmArrival` or `ShippingService.dispatch`, so their validation, logs and events apply.
  Identical open proposals are not repeated, and approving one closes other proposals for the same change.
  The dispatch tool refuses when there is not enough stock.
- **Reacting to events:** `StockEventAgent` consumes `stock-events`. For each arrival, dispatch or new request it
  matches stock to `PENDING` requests (oldest first) and logs the result. If a key is configured, the agent then
  reviews the item with its tools and may propose dispatches (session `autonomous`). Set
  `larp.ai.autonomous-review: false` to turn the LLM review off.
- **Audit:** every chat, proposal, approval and learned term is published to `agent-events` and shown in the Log Viewer.

| Variable | Default | Purpose |
|----------|---------|---------|
| `AI_PROVIDER` | `gemini` | `gemini`, `openai` or `anthropic` |
| `AI_API_KEY` | none | API key. Falls back to `GEMINI_API_KEY`, `OPENAI_API_KEY` or `ANTHROPIC_API_KEY` |
| `AI_MODEL` | `gemini-2.5-flash` / `gpt-4.1-mini` / `claude-haiku-5-5` | Model name |
| `AI_BASE_URL` | none | For OpenAI-compatible servers, e.g. a local Ollama at `http://localhost:11434/v1` |

The agent's tests (`src/test/java/com/larp/ai`) use `ScriptedChatModel`, a fake LLM that plays back tool calls,
so `./mvnw test` needs no API key or network.
=======
>>>>>>> 3aee914b11d4fe54347f4e181b6413330c75b1b7

## Workflow overview

### Typical usage flow
1. Seed data by sending an empty body to `PUT /editor/prepopulate`, or bulk load records via the same endpoint.
2. Query inventory using `GET /database/{filter}` or the AI endpoint `PUT /ai`.
3. Update or validate inventory through `PUT /editor`.
<<<<<<< HEAD
4. Confirm stock arrival with `PUT /shipping`; camps request stock with `PUT /shipping/requests`, and requests are
   sent with `POST /shipping/requests/{id}/dispatch`.
5. Ask the AI agent questions at `POST /ai/chat` (or in the console) and approve its proposals.
6. Inspect event history using `GET /log` and date-range filtering.
7. Review service status with `GET /health` and the root status endpoint `GET /`.
=======
4. Confirm stock arrival with `PUT /shipping`.
5. Inspect event history using `GET /log` and date-range filtering.
6. Review service status with `GET /health` and the root status endpoint `GET /`.
>>>>>>> 3aee914b11d4fe54347f4e181b6413330c75b1b7

## Notes

The app exposes a root endpoint at `/` and a health endpoint at `/health` so the default localhost page does not return a Whitelabel 404 when the service is running.
