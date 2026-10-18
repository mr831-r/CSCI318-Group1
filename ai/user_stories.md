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