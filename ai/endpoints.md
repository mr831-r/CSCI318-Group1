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