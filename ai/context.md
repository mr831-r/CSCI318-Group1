--- PROJECT SPECIFICATIONS FOR AGENTIC CODE GENERATION ---
# Preface
We are a company creating a logistical assistance software package with multiple independent microservices (Called LARP).
LARP aims to integrate the following capabilities:
- A visually clean stock management system
- A fault-tolerant stock database
- Integrate paysheets into stock management
- AI Integration to search through stock and paysheets
- A queue management system to track what items are on their way, and what items are overdue
- A persistent logging system to see what modifications are made to the stock database and downtime messages

# Functional requirements
| Functional Requirement | Relevance |
|------------------------|-----------|
| Organize and maintain a structured item data model. | This requirement is a business need, because stock needs to be easily located when an order is placed and ultimately distributed. |
| Query items in the database. | Checking stock availability is another business need because stock must be updated continually during operation to allow for full transparency for both the customer and the company dealing the product(s). |
| Retrieve database records between two specified dates. | Database records need to be compared day-to-day to identify any unexpected changes and ensure the stock total is correct. |
| Add and edit items in the database. | This is a stakeholder requirement as new items may become available or unavailable depending on the products the LARP uses. |
| Attach a payslip confirmation file to a stock entry.
| Provides authentication in the form of the item/date/quantity/description and can be easily tracked back if the specific order is missing or returned.  |
| Log every database action for auditability. | Database auditability is required for meeting standards and financial/tax purposes, which is required for the stakeholders. |
| Edit item status/quantity through a dedicated editor interface. | This requirement is the fundamental task of the database editor, allowing the business to manually adjust item status if the item in question is lost or faulty. |
| Validate item edits before submission (reject invalid data). | Requirement to mitigate human error by authenticating any Database editor changes. |
| View historical edit logs filtered by date range | Required so staff can audit what changed and when (.g. investigating a discrepancy without manually cross-referencing every edit). |
| Display previous edits made to an item on request | Support accountability lets a stakeholder trace a specific item's full history rather than just the latest state. |
| Continuously update available stock levels as items are dispatched | A business need; outdated stock figures risk overselling or misallocating shipments; this is a real-time/stream-processing requirement. |
| Continuously match available stock to outstanding shipping requests as new stock arrives or requests come in | A business need; this is a stream-processing requirement which ensures that the correct stock is assigned to incoming orders. |
| Sort and prioritise shipping request by location/urgency | Business need, as this ensures urgent or geographically efficient shipments aren't delayed behind lower-priority ones. |
| Assign a courier and initiate delivery | Stakeholder requirement, couriers/logistics staff need a clear handoff point once stock is allocated to a request |
| Recognise and learn unfamiliar item terminology to improve future searches. | Improves system accuracy over time as inventory/terminology evolves, without manual retraining each time. |

# Realtime Requirements
Real-time/streaming analysis means the system is continuously watching a flow of events (via Kafka) and computing something as each event arrives, without anyone having to ask. The "analysis" happens on the stream itself. Requirements 11 and 12 both fit this criteria. 

Requirement 11. Continuously update available stock levels as items are dispatched.

This event needs to be classified as a real-time event as the stock levels change continuously as dispatches occur throughout the day. A standard query would only reflect stock at the moment it's run, risking staff working from stale figures. By processing dispatch events as a stream, the available stock count is always current without requiring a manual refresh. This is implemented via a Kafka topic (stock-events) that a Spring Cloud Stream processor consumes continuously, recalculating stock levels as each event arrives. Every delivery dispatch event is continuously logged by Kafka. 

Requirement 12. Continuously match available stock to outstanding shipping requests as new stock arrives or requests come in. 

# Layered Architecture
| Name | Presentation Layer | Domain Layer | Service Layer | Infrastructure Layer |
|------|--------------------|--------------|---------------|----------------------|
| Database | API Endpoints (implemented as CRUD) | Database Host Entity, Database Item Interface | Implementation of API endpoint functionality | Nil. Optional for integration with external databases such as PostgreSQL |
| Database Editor | Command line user input | User Request Edit Object, Database Editor Host Entity | Verify user input is valid, send entry to database, send log entry | Nil. |
| Log Viewer | Real-time streaming of logs to command line, take filter command from user, logging API endpoint to add to database | Log Filter Object, Log Service Entity, Log Item Object (Implementation of Database Item Interface) | Implement API endpoint and add log item to database. Verify user input and filter new real-time events accordingly to push to the presentation layer. | Nil. |
| Shipping Manager | Shipping arrival confirmation input | Shipping Item Object (Implementation of Database Item Interface) | Confirm shipment arrival, update item status to ARRIVED, log the event | Package Tracking external API |
| AI Assistant | Command line user input | *UNKNOWN* | Coordinate external API calls, gather data from database microservice | OpenAI external API |

# Interfaces
The DatabaseHostService stores entries whose domain classes implement the shared IDatabaseItem interface. Database operations accept and return IDatabaseItem rather than declaring a separate method-level generic type for each operation. Implementations provide a filter name and item code so external services can query, update, and remove the appropriate entries.

# Data Ownership
| Microservice | Data Entities | Value Objects |
|--------------|---------------|---------------|
| Database | DatabaseHostService, IDatabaseItem (Interface) | Database, DatabaseQueryAdd, DatabaseQueryDelete, DatabaseQueryRead, DatabaseQueryUpdate |
| Database Editor | DatabaseEditService | LogisticsItem > IDatabaseItem, PayslipItem > IDatabaseItem |
| Log Viewer | DatabaseLogService | LogItem > IDatabaseItem |
| Shipping Manager | ShippingService | ShippingItem > IDatabaseItem |
| AI Assistant | AIService | AIRequest, AIResponse |

Note: A “>” symbol indicates that an object implements an interface

# User Stories
| **Service** | **Designator** | **Input** | **Output** |
| --- | --- | --- | --- |
| DatabaseHostService | D1 | Create Database | Return creation status; create list of generics inheriting from IDatabaseItem internally; create new endpoint |
| DatabaseHostService | D2 | Database Query for Add Item | Add item to database at date index (with filter) |
| DatabaseHostService | D3 | Database Query for Remove Item | Search for item with filter (most recent entry first) and remove from database |
| DatabaseHostService | D4 | Database Query for Read Item (Between two dates) | Search for item with filter and end date first; return all items until start date reached |
| DatabaseHostService | D5 | Database Query for Update Item | Search for item with filter and update/hydrate its contents |
| DatabaseEditService | E1 | User Add Logistics Item | Add item to database; broadcast item‑added signal for logging |
| DatabaseEditService | E2 | Prepopulate Logistics Database | Add list of items to database |
| DatabaseEditService | E3 | User Remove Logistics Item | Remove item from database; broadcast item‑removed signal for logging |
| DatabaseEditService | E4 | User Request Item Search | Read item from database using supplied information |
| DatabaseLogService | L1 | Logistics Item Added (event‑driven) | Add log item to database |
| DatabaseLogService | L2 | Logistics Item Removed (event‑driven) | Add log item to database |
| DatabaseLogService | L3 | Log Sent to Database | Display log on command prompt for user |
| DatabaseLogService | L4 | Shipping Stock Arrived | Add log item to database |
| DatabaseLogService | L5 | AI Search Query Made | Add log item to database |
| ShippingService | S1 | Shipping API states item arrived | Confirm shipment received, mark item as ARRIVED in the database, and log the arrival |
| AIService | A1 | AI Search Request | Search database using AI and return response to user |

# API Endpoints
| **Designator** | **Method** | **Endpoint** | **Request Body** |
| --- | --- | --- | --- |
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