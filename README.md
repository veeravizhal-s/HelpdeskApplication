# Helpdesk Application

Console-based Helpdesk system in Java that logs support tickets, assigns agents,
tracks status/priority and prints dashboards. Built for
**23CSR303 Object Oriented Programming with Java - Assignment II (Question 13)**.

**Author:** VEERAVIZHAL S - 717825F357 - II IT, Sem III, Section C, Karpagam College of Engineering

## Classes (package `com.helpdesk`)

| Class | Role |
|---|---|
| `User` | id, name, email, role (Customer/Agent) |
| `Agent` | extends `User`; overrides workload limit and assignment rules |
| `Ticket` | ticketId, subject, description, priority, status, assignee; guarded status workflow |
| `UrgentTicket` | extends `Ticket`; overrides `display()` for HIGH/CRITICAL tickets |
| `Category` | id, name, SLA hours, escalation rules |
| `HelpdeskService` | open/assign/reassign tickets, change status/priority, SLA checks, reports |
| `HelpdeskAppMain` | demo / test driver |

## OOP concepts demonstrated

- **Inheritance** - `Agent extends User` (overrides `getWorkloadLimit()`, `getAssignmentBlocker()`); `UrgentTicket extends Ticket`
- **Method overloading** - `openTicket()` with/without category and attachment; `listByPriority()` with/without a priority filter
- **Method overriding** - `Ticket.display()` vs `UrgentTicket.display()`
- **Polymorphism** - customers and agents are handled through `User` references
- **Encapsulation** - no `setStatus()`; `changeStatus()` enforces NEW > OPEN > PENDING > RESOLVED > CLOSED; SLA and workload counters have no public setters

## Run

Requires JDK 11 or later.

```bash
javac -d bin src/com/helpdesk/*.java
java -cp bin com.helpdesk.HelpdeskAppMain
```

In Eclipse: **File > Import > Existing Projects into Workspace**, then run `HelpdeskAppMain`.
Expected output is in `docs/sample_output.txt`.
