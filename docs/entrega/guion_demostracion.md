# Fifteen minute presentation and live demonstration

## Preparation

Use the isolated installation described in the [manual](manual_instalacion.md). Start the app with the `demo` profile and create the fictitious guest during the presentation or rehearse once on a disposable instance. Use a clean data set for the live run. Open three browser profiles or private session contexts for administration, the guest and reception. Keep the console, password prompts and database connection settings outside the shared screen.

Prepare two sets of dates using the hotel's local date, normally `America/Mexico_City`. Query a future interval for the public demonstration. Create the live one-night reservation with arrival **today** and departure **tomorrow**, and perform check-in the same day. The configured clock and normal validation remain active.

Use a fictional double-room type named **Demo Double**, capacity **2**, room **LIVE-201**, floor **2**, and nightly price **850.75 MXN**. If the room number is already taken in the rehearsal instance, choose a fresh number. For the guest use a fictitious name such as **Demo Guest**, `demo.guest@example.test`, and a placeholder telephone number such as `000-000-0000`. Prepare local passwords and enter them off-screen.

## First five minutes

| Time | On screen | Suggested English narration |
| --- | --- | --- |
| 0:00–0:45 | Report cover and application home page. | “Good morning. My name is Sergio Garduño Arriaga. This project is a hotel reservation management system. The course project began with a practical problem: staff need a reliable way to check whether a room is available for specific dates.” |
| 0:45–1:35 | Problem, purpose and three roles. | “The main goal is to keep reservations, room occupancy and payments consistent. Guests manage their own reservations, reception staff handle guest records and stays, and administrators manage room inventory.” |
| 1:35–2:35 | UML use cases and domain classes. | “The use cases show what each role can do. In the class model, a reservation links one guest and one room. It can have several payment attempts, and each room has one room type.” |
| 2:35–3:40 | Architecture and a sequence diagram. | “Spring MVC handles the request and Thymeleaf renders the page. The controller validates inputs and calls a service. The service enforces business rules and interacts with MySQL through Spring Data JPA repositories.” |
| 3:40–5:00 | MySQL ER diagram, Flyway versions and test summary. | “Flyway applies four versioned migrations. A reservation stores its original nightly price and total. Payments are simulated, and the system uses transactions to avoid overlapping bookings or repeated charges.” |

## Next ten minutes

| Time | Actions | Suggested English narration |
| --- | --- | --- |
| 5:00–6:40 | Sign in with the demo administrator. Open **Tipos de habitación** and **Habitaciones**. Create **Demo Double**, then **LIVE-201** as available. Show edit and confirm the inventory row. | “The administrator maintains room types and rooms. A room type defines capacity. The room stores its own price and initial operational state.” |
| 6:40–7:45 | Sign out. Open **Registro de huéspedes** for the fictitious guest. In the separate public tab, use **Consultar disponibilidad** with future dates and two guests. | “Availability is public and uses the date range and guest count. An estimated price is calculated from the current rate and the number of nights.” |
| 7:45–9:00 | Sign in as the guest. Select the same room for today through tomorrow and create the reservation. Open its detail. | “The system checks the dates and capacity again before saving. This one-night reservation is pending and stores its agreed price of 850.75 MXN.” |
| 9:00–10:10 | Open **Pagos simulados**. Enter **850.75** and choose **Efectivo**. Submit the attempt. | “This is an academic simulation. Submitting an amount creates a pending payment attempt. The detail page shows what is already approved and what is still available to pay.” |
| 10:10–11:45 | Switch to reception. Open **Huéspedes** to show the fictitious record, then **Pagos**. Find the reservation by ID. Choose **Procesar** and **Aprobar**. | “Reception staff review each attempt. Once approved payments cover the total, the reservation becomes confirmed. The other roles cannot validate this payment.” |
| 11:45–13:20 | Open **Check-in y check-out**. Find the reservation by ID. Record check-in, then check-out. Show both actual times and the room state. | “Check-in records the hotel-local time and changes the stay to in progress. Check-out records the actual departure and releases the room when no other active stay blocks it. Neither operation changes the agreed price or payment history.” |
| 13:20–14:25 | In the public availability tab, send equal arrival and departure dates. Show the validation message. If time remains, show the HTTP 403 evidence. | “The server rejects an invalid date range and also enforces permissions. Guests can open only their own reservations and payments.” |
| 14:25–15:00 | Show the test summary and conclusion. | “The H2 verification passed 181 tests, and the isolated MySQL suite passed 177, including 16 concurrent cases. The project demonstrates the full workflow. Performance under load remains open, and the payments remain simulated.” |

If a live operation cannot continue, identify the step and open the corresponding verified image from the [evidence matrix](../pruebas/matriz_pruebas.md). Explain what the earlier run demonstrates and which action could not be shown live.
