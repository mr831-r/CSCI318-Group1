Read the following API Endpoint table written in markdown. Go through the entire project within the src/ directory and create another markdown table below this one, which has the same format as the one below, with corrected designators, methods, endpoints and request bodies to match the CRUD endpoints. If there are extra entries, make sure you add them to your table. If something doesn't match, fix it in your table. After writing this table, open readme.md and add the API endpoints to the todo section, marked with API endpoints and add the requirements as outlined by the todo comment in that file.

| Designator | Method | Endpoint | Request Body |
|------------|--------|----------|--------------|
| D1 | PUT | /database | DatabaseQueryAdd |
| D2 | PUT | /database/{filter} | DatabaseQueryAdd |
| D3 | DELETE | /database/{filter} | DatabaseQueryRemove |
| D4 | GET | /database/{filter}?start={start}&end={end} | DatabaseQueryRead |
| D5 | POST | /database/{filter} | DatabaseQueryUpdate |
| E1 | PUT | /editor | LogisticsItem |
| E2 | PUT | /editor/prepopulate | LogisticsItem Array |
| E3 | DELETE | /editor | LogisticsItem |
| E4 | GET | /editor | LogisticsItem |
| L1 | PUT | /log/item | LogisticsItem |
| L2 | DELETE | /log/item | LogisticsItem |
| L3 | PUT | /log | LogItem |
| L4 | PUT | /log/shipping | ShippingItem |
| L5 | PUT | /log/ai | AIRequest |
| S1 | PUT | /shipping | ShippingItem |
| A1 | PUT | /ai | AIRequest |

## Verified endpoint table

| Designator | Method | Endpoint | Request Body |
|------------|--------|----------|--------------|
| D1 | PUT | `/database` | `IDatabaseItem` JSON (inventory item, shipping request, or log entry) |
| D2 | PUT | `/database/{filter}` | `IDatabaseItem` JSON |
| D3 | DELETE | `/database/{filter}` | None; `{filter}` is the item code |
| D4 | GET | `/database/{filter}?start={start}&end={end}` | None; date parameters are optional ISO dates |
| D5 | POST | `/database/{filter}` | `IDatabaseItem` JSON (partial fields to update) |
| E1 | PUT | `/editor` | `InventoryItem` JSON |
| E2 | PUT | `/editor/prepopulate` | Optional `InventoryItem[]` JSON; omitted or empty body loads sample items |
| E3 | DELETE | `/editor` | `{"itemCode":"ITEM-1001"}` or `{"filter":"ITEM-1001"}` |
| E4 | DELETE | `/editor/{itemCode}` | None |
| E5 | GET | `/editor` | None |
| E6 | GET | `/editor/statuses` | None; returns `InventoryStatus[]` |
| L1 | GET | `/log?start={start}&end={end}` | None; date parameters are optional ISO dates |
| L2 | PUT | `/log/item` | `InventoryItem` JSON |
| L3 | DELETE | `/log/item` | `InventoryItem` JSON |
| L4 | PUT | `/log` | `{"message":"...","level":"INFO"}` |
| L5 | PUT | `/log/shipping` | `{"itemCode":"ITEM-1001","status":"INFO"}` |
| L6 | PUT | `/log/ai` | `{"query":"...","count":"1"}` |
| L7 | GET | `/log/stream` | None; Server-Sent Events |
| S1 | PUT | `/shipping` | `ShippingRequest` JSON (`itemCode`, `destination`, `quantity`, `requestedDate`) |
| S2 | GET | `/shipping` | None |
| A1 | GET | `/ai` | None |
| A2 | PUT | `/ai` | `AIRequest` JSON |
| A3 | POST | `/ai` | `AIRequest` JSON |
<<<<<<< HEAD
| S3 | PUT | `/shipping/requests` | `ShippingRequest` JSON (`itemCode`, `destination`, `quantity`, optional `requestedDate`) |
| S4 | GET | `/shipping/requests?status={status}` | None; status optional (`PENDING`, `DISPATCHED`, `ARRIVED`) |
| S5 | POST | `/shipping/requests/{id}/dispatch` | `{"courier":"..."}` |
| A4 | POST | `/ai/chat` | `{"sessionId":"optional","message":"..."}` |
| A5 | GET | `/ai/status` | None |
| A6 | GET | `/ai/actions?sessionId={sessionId}` | None; session optional |
| A7 | POST | `/ai/actions/{id}/approve` | None |
| A8 | POST | `/ai/actions/{id}/reject` | None |
| A9 | DELETE | `/ai/sessions/{sessionId}` | None |
=======
>>>>>>> 3aee914b11d4fe54347f4e181b6413330c75b1b7
| H1 | GET | `/health` | None |
| H2 | GET | `/` or `/console` | None; serves the console page |

`GET /database/{filter}` matches a database type name or item code. Database update
and deletion use the latest record for the item code. Inventory status values are
the `InventoryStatus` enum (`AVAILABLE`, `SHIPPING`, `ARRIVED`); shipping request
status values are `ShippingStatus` (`PENDING`, `ARRIVED`). Enum values are stored
and returned in uppercase; incoming enum strings are accepted case-insensitively.

### Kafka event flow

| Topic | Producer | Consumer | Event / effect |
|-------|----------|----------|----------------|
| `item-events` | `EditorService` | `LogService` | `ITEM_ADDED`, `ITEM_UPDATED`, and `ITEM_REMOVED` inventory audit entries |
| `ai-events` | `AiSearchService` | `LogService` | AI search audit entries |
| `stock-events` | `ShippingService` | `LogService` | `STOCK_ARRIVED` audit entry; inventory quantity is updated synchronously by `PUT /shipping` |

| Service | Designator | Input | Output |
|---------|------------|-------|--------|
| DatabaseHostService | D1 | Create Database | Return creation status, create list of generics inheriting from IDatabaseItem internally. Create new endpoint. |
| DatabaseHostService | D2 | Database Query for Add Item | Add item to database at date index (with filter from IDatabaseItem). |
| DatabaseHostService | D3 | Database Query for Remove Item | Search for item with filter within IDatabaseItem (most recent entry first), and remove from database. |
| DatabaseHostService | D4 | Database Query for Read Item (Between two dates) | Search for item with filter within IDatabaseItem and corresponding end date first, then return all items until start date reached. |
| DatabaseHostService | D5 | Database Query for Update Item | Search for item with filter within IDatabaseItem, and update/hydrate its contents. |
| DatabaseEditService | E1 | User Add Logistics Item | Add item to database (via DatabaseHostService), broadcast item added signal for logging. |
| DatabaseEditService | E2 | Prepopulate Logistics Database | Add list of items to database (via DatabaseHostService). |
| DatabaseEditService | E3 | User Remove Logistics Item | Remove item from database (via DatabaseHostService), broadcast item removed signal for logging. |
| DatabaseEditService | E4 | User request item search | Read item from DatabaseHostService using supplied information. |
| DatabaseLogService | L1 | Logistics Item Added (From event-driven signal) | Add log item to database via DatabaseHostService. |
| DatabaseLogService | L2 | Logistics Item Removed (From event-driven signal) | Add log item to database via DatabaseHostService. |
| DatabaseLogService | L3 | Log sent to Database | Display log on command prompt for user. |
| DatabaseLogService | L4 | Shipping Stock Arrived | Add log item to database via DatabaseHostService. |
| DatabaseLogService | L5 | AI Search Query Made | Add log item to database via DatabaseHostService. |
| ShippingService | S1 | Shipping API states item arrived | Mark item as ARRIVED in database via DatabaseHostService. |
| AIService | A1 | AI Search Request | Search database using AI and DatabaseHostService, and give a response to the user. |

## Verified service operations

| Service | Designator | Input | Output |
|---------|------------|-------|--------|
| `DatabaseService` | D1 | `PUT /database` with an `IDatabaseItem` | Persist and return the created item |
| `DatabaseService` | D2 | `PUT /database/{filter}` with an `IDatabaseItem` | Persist and return the created item |
| `DatabaseService` | D3 | `DELETE /database/{filter}` | Delete the latest item matching the item code |
| `DatabaseService` | D4 | `GET /database/{filter}` with optional `start` and `end` dates | Return matching records in reverse date order |
| `DatabaseService` | D5 | `POST /database/{filter}` with partial item fields | Update and return the latest matching item |
| `EditorService` | E1 | `PUT /editor` with an `InventoryItem` | Validate, persist, publish an item event, and return the item |
| `EditorService` | E2 | `PUT /editor/prepopulate` with optional inventory list | Persist the supplied items or seed sample inventory |
| `EditorService` | E3 | `DELETE /editor` or `/editor/{itemCode}` | Delete latest matching inventory item and publish an item event |
| `EditorService` | E4 | `GET /editor` | Return inventory items |
| `EditorService` | E5 | `GET /editor/statuses` | Return all `InventoryStatus` enum values for the console |
| `LogService` | L1 | `GET /log` with optional date range | Return matching log entries |
| `LogService` | L2 | Item add/update/remove Kafka events | Persist audit entries through `PUT /database/logs` |
| `LogService` | L3 | `PUT /log`, `/log/item`, `/log/shipping`, or `/log/ai` | Persist and display a log entry; notify SSE subscribers |
| `LogService` | L4 | `GET /log/stream` | Stream new log entries as Server-Sent Events |
| `ShippingService` | S1 | `PUT /shipping` with item code and received quantity | Increase inventory quantity immediately, set inventory status to `ARRIVED`, persist the shipping request, publish `STOCK_ARRIVED` |
| `ShippingService` | S2 | `GET /shipping` | Return persisted shipping requests |
<<<<<<< HEAD
| `ShippingService` | S3 | `PUT /shipping/requests` | Save a `PENDING` request; publish `SHIPPING_REQUESTED` |
| `ShippingService` | S4 | `GET /shipping/requests` | Return the request queue, oldest first |
| `ShippingService` | S5 | `POST /shipping/requests/{id}/dispatch` | Take stock, assign courier, mark `DISPATCHED`; publish `STOCK_DISPATCHED` (409 if not enough stock) |
| `AgentService` | A4 | `POST /ai/chat` | LLM agent answers using tools; returns reply, tool calls and pending proposals |
| `AgentService` | A5-A9 | `/ai/status`, `/ai/actions`, `/ai/sessions` | Agent status, approve/reject proposals, forget a conversation |
| `StockEventAgent` | K4 | `stock-events` | Match stock to `PENDING` requests; LLM review may propose dispatches |
| `LogService` | K5 | `agent-events` | Log every AI agent decision with level `AGENT` |
| `AiSearchService` | A1 | `GET /ai` | Return available inventory items |
| `AiSearchService` | A2 | `PUT /ai` or `POST /ai` with an `AIRequest` | Keyword search using learned terminology, publish an AI event, and return matching items |
=======
| `AiSearchService` | A1 | `GET /ai` | Return available inventory items |
| `AiSearchService` | A2 | `PUT /ai` or `POST /ai` with an `AIRequest` | Search inventory, publish an AI event, and return matching items |
>>>>>>> 3aee914b11d4fe54347f4e181b6413330c75b1b7
| `LogService` | K1 | `item-events` | Consume inventory add/update/remove events and write audit entries |
| `LogService` | K2 | `ai-events` | Consume AI search events and write audit entries |
| `LogService` | K3 | `stock-events` | Consume stock arrival events and write audit entries |