Read the following user stories table written in markdown. Go through the entire project within the src/ directory and create another markdown table below this one, which has the same format as the one below, with corrected service names, designators, inputs and outputs. If there are extra entries, make sure you add them to your table. If something doesn't match, fix it in your table. Stick to the main CRUD endpoints within the project and do not deviate. Be sure to be wary of Kafka events. These should also be added to the table.

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

## Verified service operations and event flow

| Service | Designator | Input | Output |
|---------|------------|-------|--------|
| `DatabaseService` | D1 | Create one `IDatabaseItem` using `PUT /database` | Persist and return the item |
| `DatabaseService` | D2 | Add an `IDatabaseItem` using `PUT /database/{filter}` | Persist and return the item |
| `DatabaseService` | D3 | Delete by item code using `DELETE /database/{filter}` | Remove the latest matching item |
| `DatabaseService` | D4 | Read by filter and optional start/end dates using `GET /database/{filter}` | Return matching items in descending date order |
| `DatabaseService` | D5 | Update latest item using `POST /database/{filter}` and partial item fields | Return the updated item |
| `EditorService` | E1 | Add or update inventory using `PUT /editor` | Validate and persist item; publish `ITEM_ADDED` or `ITEM_UPDATED` on `item-events` |
| `EditorService` | E2 | Prepopulate using `PUT /editor/prepopulate` with an optional list | Persist supplied items or sample inventory |
| `EditorService` | E3 | Remove inventory using `DELETE /editor` or `DELETE /editor/{itemCode}` | Delete latest item; publish `ITEM_REMOVED` on `item-events` |
| `EditorService` | E4 | List inventory using `GET /editor` | Return inventory items |
| `LogService` | L1 | Receive editor item events from `item-events` | Persist audit logs via the database endpoint |
| `LogService` | L2 | Receive AI search events from `ai-events` | Persist AI search audit logs |
| `LogService` | L3 | Receive stock arrival events from `stock-events` | Persist stock arrival audit logs |
| `LogService` | L4 | Write a log using `PUT /log` or the specialized log endpoints | Persist and print log entry; notify SSE subscribers |
| `LogService` | L5 | Read logs using `GET /log` or subscribe using `GET /log/stream` | Return date-filtered entries or stream new entries |
| `ShippingService` | S1 | Confirm received stock using `PUT /shipping` | Immediately increase inventory quantity, mark inventory `ARRIVED`, save request, and publish `STOCK_ARRIVED` to `stock-events` |
| `ShippingService` | S2 | List arrivals using `GET /shipping` | Return saved shipping requests |
| `AiSearchService` | A1 | Search with `PUT /ai` or `POST /ai` and an `AIRequest` | Return ranked inventory matches and publish a search event to `ai-events` |
| `AiSearchService` | A2 | List inventory using `GET /ai` | Return available inventory |
| `LogService` | K1 | Consume `item-events` produced by `EditorService` | Log inventory add, update, and remove events |
| `LogService` | K2 | Consume `ai-events` produced by `AiSearchService` | Log AI query events |
| `LogService` | K3 | Consume `stock-events` produced by `ShippingService` | Log stock arrival events; this consumer does not apply the quantity change |