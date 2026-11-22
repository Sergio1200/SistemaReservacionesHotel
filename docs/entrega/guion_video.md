# Demonstration video script

Plan a **7 minute 30 second** screen recording with English narration. Use a local, isolated demo instance. Record the actual application and use existing images only when they are identified as previously captured evidence. Keep the application's labels in Spanish.

The recording is pending. This document contains the recording plan and narration.

## Recording setup

1. Set the browser to 100% zoom at a readable desktop resolution. Capture the browser window at 1920 × 1080 and 30 frames per second when your recorder supports it. Close unrelated tabs and disable desktop notifications.
2. Follow the [installation manual](manual_instalacion.md). Start the application with the `demo` profile in an isolated instance. Prepare separate authenticated browser contexts for administration, guest and reception. Enter passwords before recording or pause capture while typing.
3. Use fictional test data. Keep a public availability tab, the report and the diagram images ready. Read the hotel date from the availability form. Use one future search and one live demonstration reservation from today through tomorrow.
4. Rehearse the payment and two stay transitions on a disposable data set. For the recording, use a fresh room number and guest email if the rehearsal data remains. Confirm that the full amount is approved before the check-in segment.
5. Record with a screen recorder available on your workstation. Enable microphone capture, make a ten-second audio test, then choose a browser-window source. Review the final export for readable Spanish labels, clear English narration and the absence of credential prompts or unrelated personal information.

## Recording order and narration

| Segment | Duration | Screen and action | Suggested narration |
| --- | ---: | --- | --- |
| 1. Project introduction | 0:35 | Show the English report cover and the home page. | “I am Sergio Garduño Arriaga. This project supports hotel reservations, room management, simulated payments and guest stays.” |
| 2. Design and data | 0:55 | Show the use cases, one service sequence and the ER diagram. | “The three roles use different screens. Controllers receive web requests, services apply rules and repositories access MySQL. A reservation links one guest and one room and records its historical cost.” |
| 3. Inventory | 0:45 | Show the administrator's room type and room forms. Save the fictional type and room. | “The administrator sets capacity through a room type, then creates a room with its price and initial status.” |
| 4. Guest and availability | 0:55 | Show guest registration and a future availability search for two people. Sign in to the prepared guest context. | “The public search checks both dates and capacity. The guest can view only reservations associated with their own account.” |
| 5. Reservation and simulated payment | 1:05 | Reserve the room from today to tomorrow. Show **Pendiente**, then submit the exact simulated amount. | “This reservation covers one night. It starts as pending. The guest enters a simulated payment amount and method. The app creates an attempt without processing a bank transaction.” |
| 6. Reception review | 0:55 | Locate the reservation in reception, process the attempt and approve the full amount. | “Reception checks the attempt and approves it. The reservation becomes confirmed when the approved payments cover the total.” |
| 7. Stay operations | 0:55 | Record today's check-in and check-out. Show occupied and available states and both actual times. | “The check-in and check-out each update the reservation and room in one transaction. The historical price and approved payment are preserved.” |
| 8. Validation and verification | 0:55 | Show equal dates rejected and the evidence for a forbidden request. End on the results table. | “Input validation and role checks run on the server. The isolated MySQL tests also covered concurrent operations and verified that a conflicting request does not leave partial changes.” |
| 9. Closing | 0:30 | Show conclusions and the evidence index. | “The project demonstrates the full academic workflow. Performance under load and the review of legacy records remain open. Thank you.” |

**Total running time: 7:30.** Export and watch the full recording before submission. Verify the voice track, the state transitions and the business messages at full size.
