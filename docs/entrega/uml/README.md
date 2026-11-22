# UML and database diagrams

Each row includes an editable PlantUML source and a locally rendered PNG. The figures model the Java application and MySQL migrations V1–V4. English labels explain operations. Identifiers match the code. The original PlantUML sets and the corrected ZIP were consulted separately and preserved in their original locations.

| Figure | Diagram | Source | Render |
| --- | --- | --- | --- |
| A1 | Implemented use cases | [PlantUML](01-use-cases.puml) | [PNG](01-use-cases.png) |
| A2 | Domain classes | [PlantUML](02-domain-classes.puml) | [PNG](02-domain-classes.png) |
| A3 | Sign-in | [PlantUML](03-sign-in.puml) | [PNG](03-sign-in.png) |
| A4 | Guest registration | [PlantUML](04-guest-registration.puml) | [PNG](04-guest-registration.png) |
| A5 | Availability | [PlantUML](05-availability.puml) | [PNG](05-availability.png) |
| A6 | Reservation creation | [PlantUML](06-create-reservation.puml) | [PNG](06-create-reservation.png) |
| A7 | Reservation modification | [PlantUML](07-modify-reservation.puml) | [PNG](07-modify-reservation.png) |
| A8 | Reservation cancellation | [PlantUML](08-cancel-reservation.puml) | [PNG](08-cancel-reservation.png) |
| A9 | Simulated payment registration | [PlantUML](09-register-payment.puml) | [PNG](09-register-payment.png) |
| A10 | Simulated payment validation | [PlantUML](10-review-payment.puml) | [PNG](10-review-payment.png) |
| A11 | Check-in | [PlantUML](11-check-in.puml) | [PNG](11-check-in.png) |
| A12 | Check-out | [PlantUML](12-check-out.puml) | [PNG](12-check-out.png) |
| A13 | Reservation states | [PlantUML](13-reservation-states.puml) | [PNG](13-reservation-states.png) |
| A14 | Payment states | [PlantUML](14-payment-states.puml) | [PNG](14-payment-states.png) |
| A15 | Operational room states | [PlantUML](15-room-states.puml) | [PNG](15-room-states.png) |
| A16 | MySQL entity relationships | [PlantUML](16-mysql-entity-relationships.puml) | [PNG](16-mysql-entity-relationships.png) |

## Render locally

With a PlantUML JAR and Java, open the repository root and run:

```powershell
java -jar path/to/plantuml.jar -charset UTF-8 -failfast2 -tpng docs/entrega/uml
```

The PNGs were generated locally with PlantUML 1.2025.4. Diagrams that use a graph layout select Smetana and can render without a separate Graphviz executable. The [technical report](../documentacion_tecnica.md#appendix-a-uml-diagrams) describes the design differences and includes all 16 renders.
