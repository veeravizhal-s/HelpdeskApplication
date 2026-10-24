package com.helpdesk;

import java.time.LocalDateTime;
import java.util.List;

public class HelpdeskAppMain {

    private static final int WIDTH = 80;
    private static final String LINE = "=".repeat(WIDTH);
    private static final String SUB_LINE = "-".repeat(WIDTH);

    private static final HelpdeskService service = new HelpdeskService(
            "Chennai Tech Solutions Pvt Ltd", LocalDateTime.of(2026, 10, 24, 9, 0));

    private static int stepNo = 0;
    private static int checksRun = 0;
    private static int checksPassed = 0;

    private HelpdeskAppMain() { }

    public static void main(String[] args) {
        printBanner();
        registerUsersAndCategories();
        createTickets();
        assignAndReassign();
        changePriorities();
        runLifecycle();
        printDashboards();
        demonstrateOopConcepts();
        runValidationTests();
        runSelfChecks();
        printFooter();
    }

    private static void registerUsersAndCategories() {
        printHeading("REGISTER USERS AND CATEGORIES");
        User[] people = {
            new User("C101", "Anita Rao", "anita.rao@gmail.com", User.Role.CUSTOMER),
            new User("C102", "Vikram Singh", "vikram.singh@gmail.com", User.Role.CUSTOMER),
            new User("C103", "Sneha Pillai", "sneha.pillai@gmail.com", User.Role.CUSTOMER),
            new Agent("A201", "Ravi Kumar", "ravi.kumar@helpdesk.in", "Network",
                    Agent.Level.SENIOR, 4),
            new Agent("A202", "Divya Menon", "divya.menon@helpdesk.in", "Billing",
                    Agent.Level.SENIOR, 2),
            new Agent("A203", "Arjun Das", "arjun.das@helpdesk.in", "Software",
                    Agent.Level.JUNIOR, 1)
        };
        for (User p : people) {
            service.registerUser(p);
        }
        System.out.println("Company : " + service.getCompanyName());
        System.out.printf("  %-9s %-5s %-14s %s%n", "Role", "ID", "Name", "Email");
        for (User u : service.getUsers()) {
            u.displayDetails();
        }
        System.out.println("Registered " + User.getUserCount() + " users ("
                + (User.getUserCount() - Agent.getAgentCount()) + " customers, "
                + Agent.getAgentCount() + " agents).");

        printSubHeading("Categories, SLA hours and escalation rules");
        Category net = new Category("C01", "Network", 8);
        net.addEscalationRule(Ticket.Priority.CRITICAL, "Network Team Lead, immediately");
        net.addEscalationRule(Ticket.Priority.HIGH, "Network Team Lead after 2 h");
        Category bill = new Category("C02", "Billing", 24);
        bill.addEscalationRule(Ticket.Priority.HIGH, "Finance Manager after 6 h");
        Category soft = new Category("C03", "Software", 12);
        soft.addEscalationRule(Ticket.Priority.CRITICAL, "Engineering Head, immediately");
        soft.addEscalationRule(Ticket.Priority.HIGH, "Senior Developer after 3 h");
        service.addCategory(net);
        service.addCategory(bill);
        service.addCategory(soft);
        for (Category c : service.getCategories()) {
            c.display();
        }
    }

    private static void createTickets() {
        printHeading("OPEN TICKETS (OVERLOADED openTicket() METHODS)");
        User anita = service.getUser("C101");
        User vikram = service.getUser("C102");
        User sneha = service.getUser("C103");

        printSubHeading("Overload 1: openTicket(customer, subject, desc, priority)");
        show(service.openTicket(anita, "Request for new keyboard",
                "Keys on my keyboard are worn out", Ticket.Priority.LOW));
        service.advanceMinutes(10);
        show(service.openTicket(vikram, "Printer toner replacement",
                "Toner is empty on floor 2", Ticket.Priority.LOW));
        service.advanceMinutes(10);

        printSubHeading("Overload 2: openTicket(..., categoryId)");
        show(service.openTicket(anita, "VPN not connecting",
                "VPN fails after the latest update", Ticket.Priority.HIGH, "C01"));
        service.advanceMinutes(10);
        show(service.openTicket(vikram, "Wi-Fi drops in meeting room",
                "Connection drops every 10 minutes", Ticket.Priority.MEDIUM, "C01"));
        service.advanceMinutes(10);
        show(service.openTicket(sneha, "Refund status enquiry",
                "Refund not received after 5 days", Ticket.Priority.LOW, "C02"));
        service.advanceMinutes(10);

        printSubHeading("Overload 3: openTicket(..., categoryId, attachment)");
        show(service.openTicket(vikram, "Invoice charged twice",
                "Card charged two times for one invoice", Ticket.Priority.MEDIUM,
                "C02", "invoice_scan.png"));
        service.advanceMinutes(10);
        show(service.openTicket(sneha, "App crashes on login",
                "App closes right after entering password", Ticket.Priority.CRITICAL,
                "C03", "crash_log.txt"));
        System.out.println();
        System.out.println("Total tickets opened: " + service.getTicketCount());
    }

    private static void assignAndReassign() {
        printHeading("ASSIGN AND REASSIGN AGENTS (WORKLOAD AND LEVEL RULES)");
        User ravi = service.getUser("A201");
        User divya = service.getUser("A202");
        User arjun = service.getUser("A203");
        User anita = service.getUser("C101");

        attemptAssign("TCK-003", ravi);
        attemptAssign("TCK-007", arjun);
        attemptAssign("TCK-007", ravi);
        attemptAssign("TCK-006", divya);
        attemptAssign("TCK-001", arjun);
        attemptAssign("TCK-004", arjun);
        attemptAssign("TCK-004", divya);
        attemptAssign("TCK-005", divya);
        attemptAssign("TCK-005", ravi);
        attemptAssign("TCK-002", anita);
        printSubHeading("Reassignment");
        attemptReassign("TCK-001", ravi);
        attemptReassign("TCK-001", arjun);
        attemptReassign("TCK-002", arjun);
        attemptReassign("TCK-005", arjun);
        service.advanceMinutes(10);
    }

    private static void changePriorities() {
        printHeading("CHANGE PRIORITY");
        System.out.println("> TCK-001 (keyboard) raised from LOW to HIGH");
        Ticket t = service.changePriority("TCK-001", Ticket.Priority.HIGH);
        printMessage(service.getLastMessage());
        System.out.println("  Object type is now: " + t.getClass().getSimpleName());
        System.out.println("> TCK-002 (toner) raised from LOW to MEDIUM");
        t = service.changePriority("TCK-002", Ticket.Priority.MEDIUM);
        printMessage(service.getLastMessage());
        System.out.println("  Object type stays : " + t.getClass().getSimpleName());
    }

    private static void runLifecycle() {
        printHeading("TICKET LIFECYCLE: NEW > OPEN > PENDING > RESOLVED > CLOSED");
        service.advanceMinutes(10);
        printSubHeading("Illegal moves (must be blocked)");
        attempt("TCK-002 NEW -> OPEN with no assignee",
                () -> service.changeStatus("TCK-002", Ticket.Status.OPEN));
        attempt("TCK-003 NEW -> RESOLVED (skipping steps)",
                () -> service.changeStatus("TCK-003", Ticket.Status.RESOLVED));
        attempt("TCK-003 NEW -> CLOSED (skipping steps)",
                () -> service.changeStatus("TCK-003", Ticket.Status.CLOSED));

        printSubHeading("TCK-003 VPN issue: full lifecycle");
        move("TCK-003", Ticket.Status.OPEN);
        service.advanceMinutes(30);
        move("TCK-003", Ticket.Status.PENDING);
        service.advanceHours(4);
        move("TCK-003", Ticket.Status.RESOLVED);
        service.advanceMinutes(15);
        move("TCK-003", Ticket.Status.CLOSED);

        printSubHeading("Other tickets move part of the way");
        move("TCK-007", Ticket.Status.OPEN);
        move("TCK-004", Ticket.Status.OPEN);
        move("TCK-004", Ticket.Status.PENDING);
        move("TCK-006", Ticket.Status.OPEN);
        move("TCK-006", Ticket.Status.RESOLVED);
        move("TCK-001", Ticket.Status.OPEN);
        move("TCK-005", Ticket.Status.OPEN);

        printSubHeading("Moves after CLOSED (must be blocked)");
        attempt("TCK-003 CLOSED -> OPEN (reopen)",
                () -> service.changeStatus("TCK-003", Ticket.Status.OPEN));
        attempt("TCK-003 priority change after CLOSED",
                () -> service.changePriority("TCK-003", Ticket.Priority.LOW));
    }

    private static void printDashboards() {
        printHeading("DASHBOARDS");
        service.advanceMinutes(285);
        int found = service.checkSlaBreaches();
        System.out.println("SLA sweep at " + service.getNow().format(Ticket.STAMP) + ": "
                + found + " new breach(es) found.");

        printSubHeading("Dashboard 1: tickets by agent");
        service.printTicketsByAgent();
        printSubHeading("Dashboard 2: tickets by category");
        service.printTicketsByCategory();
        printSubHeading("Dashboard 3: SLA breaches");
        service.printSlaReport();
        printSubHeading("listByPriority(): highest priority first");
        service.printTicketList("  All tickets", service.listByPriority());
        service.printTicketList("  Only CRITICAL/HIGH",
                merge(service.listByPriority(Ticket.Priority.CRITICAL),
                      service.listByPriority(Ticket.Priority.HIGH)));
        printSubHeading("listByStatus()");
        service.printByStatus();
    }

    private static void demonstrateOopConcepts() {
        printHeading("OOP CONCEPT DEMONSTRATIONS");
        printSubHeading("1. Inheritance: Agent extends User");
        Agent ravi = (Agent) service.getUser("A201");
        System.out.println("  Ravi is an Agent: " + (ravi instanceof Agent));
        System.out.println("  Ravi is a User  : " + (ravi instanceof User));
        System.out.println("  Inherited getName()  : " + ravi.getName());
        System.out.println("  Inherited getEmail() : " + ravi.getEmail());
        System.out.println("  Overridden getWorkloadLimit(): " + ravi.getWorkloadLimit());

        printSubHeading("2. Overriding: Ticket.display() vs UrgentTicket.display()");
        System.out.println("> TCK-002 is a plain Ticket (MEDIUM, NEW):");
        service.findById("TCK-002").display();
        System.out.println("> TCK-007 is an UrgentTicket (CRITICAL, OPEN):");
        service.findById("TCK-007").display();

        printSubHeading("3. Polymorphism: User references, different behaviour");
        Ticket probe = service.findById("TCK-007");
        System.out.println("  Who can be assigned " + probe + "?");
        for (User u : service.getUsers()) {
            String blocker = u.getAssignmentBlocker(probe);
            printWrapped(String.format("  %-9s %-13s -> ", u.getRole().getLabel(), u.getName()),
                    " ".repeat(30), blocker == null ? "YES" : "NO. " + blocker);
        }

        printSubHeading("4. Overloading: same method name, different parameters");
        System.out.println("  listByPriority()         -> " + service.listByPriority().size()
                + " tickets (all)");
        System.out.println("  listByPriority(HIGH)     -> "
                + service.listByPriority(Ticket.Priority.HIGH).size() + " ticket(s)");
        System.out.println("  listByPriority(LOW)      -> "
                + service.listByPriority(Ticket.Priority.LOW).size() + " ticket(s)");
        System.out.println("  (openTicket() overloads were shown in the open-ticket step)");

        printSubHeading("5. Encapsulation: status and SLA counters are guarded");
        System.out.println("  Ticket has no setStatus(); changeStatus() enforces the workflow.");
        System.out.println("  Agent has no setters for resolvedCount / slaBreaches.");
        attempt("setSubject() on CLOSED TCK-003",
                () -> service.findById("TCK-003").setSubject("Edited after close"));
        attempt("Agent with workload limit 0",
                () -> new Agent("A999", "Test", "t@x.in", "IT", Agent.Level.JUNIOR, 0));
        System.out.println("  Resolved/SLA counters are read-only: Ravi resolved "
                + ravi.getResolvedCount() + ", SLA breaches " + ravi.getSlaBreaches());
    }

    private static void runValidationTests() {
        printHeading("VALIDATION TESTS (INVALID INPUT MUST BE BLOCKED)");
        User anita = service.getUser("C101");
        User ravi = service.getUser("A201");
        attempt("Blank subject",
                () -> service.openTicket(anita, " ", "x", Ticket.Priority.LOW));
        attempt("Null priority",
                () -> service.openTicket(anita, "No priority", "x", null));
        attempt("Unknown category C99",
                () -> service.openTicket(anita, "Bad category", "x", Ticket.Priority.LOW, "C99"));
        attempt("Agent raising a ticket",
                () -> service.openTicket(ravi, "Agent ticket", "x", Ticket.Priority.LOW));
        attempt("Unknown ticket TCK-999",
                () -> service.changeStatus("TCK-999", Ticket.Status.OPEN));
        attempt("Duplicate user ID C101",
                () -> service.registerUser(new User("C101", "Copy", "c@x.in", User.Role.CUSTOMER)));
        attempt("Invalid email",
                () -> new User("C500", "Bad Mail", "not-an-email", User.Role.CUSTOMER));
        attempt("Category with SLA 0 hours",
                () -> new Category("C77", "Broken", 0));
        System.out.println();
        System.out.println("Tickets stored after all invalid attempts: "
                + service.getTicketCount() + " (unchanged)");
    }

    private static void runSelfChecks() {
        printHeading("AUTOMATIC SELF-CHECKS");
        check(service.getTicketCount() == 7, "7 tickets stored; none added by invalid attempts");
        check(service.getUsers().size() == 6 && Agent.getAgentCount() == 3,
                "6 users created, 3 of them agents");
        check(Ticket.getTotalCreated() == 7 && HelpdeskService.getTicketSequence() == 7,
                "static counters match the 7 valid tickets");
        check(service.findById("TCK-003").getStatus() == Ticket.Status.CLOSED,
                "TCK-003 completed NEW > OPEN > PENDING > RESOLVED > CLOSED");
        check(service.findById("TCK-002").getStatus() == Ticket.Status.NEW,
                "unassigned TCK-002 stayed NEW");
        check(service.findById("TCK-001") instanceof UrgentTicket,
                "TCK-001 became an UrgentTicket after priority raise");
        check(!(service.findById("TCK-002") instanceof UrgentTicket),
                "TCK-002 (MEDIUM) stayed a plain Ticket");
        check(service.getUser("A203").getActiveTickets() == 1
                && service.getUser("A202").getActiveTickets() == 1,
                "workloads: Arjun 1, Divya 1");
        check(service.getUser("A201").getActiveTickets() == 2,
                "workload: Ravi 2 (TCK-003 resolved, so it no longer counts)");
        check(service.getSlaBreachCount() == 3, "3 SLA breaches counted (TCK-003, TCK-004, TCK-007)");
        check(service.listByStatus(Ticket.Status.OPEN).size() == 3
                && service.listByStatus(Ticket.Status.PENDING).size() == 1
                && service.listByStatus(Ticket.Status.RESOLVED).size() == 1
                && service.listByStatus(Ticket.Status.CLOSED).size() == 1
                && service.listByStatus(Ticket.Status.NEW).size() == 1,
                "status counts: NEW 1, OPEN 3, PENDING 1, RESOLVED 1, CLOSED 1");
        check(service.listByPriority().get(0).getPriority() == Ticket.Priority.CRITICAL,
                "listByPriority() puts CRITICAL first");
        System.out.println();
        System.out.println("Self-checks passed: " + checksPassed + " / " + checksRun);
    }

    private static List<Ticket> merge(List<Ticket> a, List<Ticket> b) {
        List<Ticket> all = new java.util.ArrayList<>(a);
        all.addAll(b);
        return all;
    }

    private static void attemptAssign(String ticketId, User user) {
        System.out.println("> assign " + ticketId + " to " + user.getName()
                + " (" + user.getRole().getLabel() + ")");
        service.assignAgent(ticketId, user);
        printMessage(service.getLastMessage());
    }

    private static void attemptReassign(String ticketId, User user) {
        System.out.println("> reassign " + ticketId + " to " + user.getName());
        service.reassignAgent(ticketId, user);
        printMessage(service.getLastMessage());
    }

    private static void move(String ticketId, Ticket.Status next) {
        service.changeStatus(ticketId, next);
        printMessage(service.getLastMessage());
    }

    private static void show(Ticket t) {
        System.out.println("[OPENED] " + t.getTicketId() + " | " + t.getPriority() + " | "
                + t.getCategory().getName() + " | " + t.getStatus() + " | "
                + t.getRequester().getName() + " | " + t.getClass().getSimpleName());
        System.out.println("         Attachment: " + (t.getAttachment() == null
                ? "none" : t.getAttachment()) + " | SLA " + t.getEffectiveSlaHours() + " h");
    }

    private static void attempt(String label, Runnable action) {
        try {
            action.run();
            System.out.println("  [UNEXPECTED] " + label + " was NOT blocked!");
            checksRun++;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            System.out.println("  [BLOCKED] " + label);
            printWrapped("            -> ", "               ", ex.getMessage());
        }
    }

    private static void printMessage(String message) {
        printWrapped("  ", " ".repeat(13), message);
    }

    private static void printWrapped(String firstPrefix, String nextPrefix, String text) {
        StringBuilder line = new StringBuilder(firstPrefix);
        boolean empty = true;
        for (String word : text.split(" ")) {
            if (!empty && line.length() + 1 + word.length() > WIDTH) {
                System.out.println(line);
                line = new StringBuilder(nextPrefix);
                empty = true;
            }
            if (!empty) {
                line.append(' ');
            }
            line.append(word);
            empty = false;
        }
        System.out.println(line);
    }

    private static void check(boolean condition, String label) {
        checksRun++;
        if (condition) {
            checksPassed++;
        }
        System.out.println("  [" + (condition ? "PASS" : "FAIL") + "] " + label);
    }

    private static void printHeading(String title) {
        stepNo++;
        System.out.println();
        System.out.println(LINE);
        System.out.println("STEP " + stepNo + ": " + title);
        System.out.println(LINE);
    }

    private static void printSubHeading(String title) {
        System.out.println();
        System.out.println("--- " + title);
    }

    private static void printBanner() {
        System.out.println(LINE);
        System.out.println("  HELPDESK SYSTEM - 23CSR303 OOP WITH JAVA - ASSIGNMENT 2");
        System.out.println("  Log ticket -> Assign agent -> Track status -> Report");
        System.out.println(LINE);
    }

    private static void printFooter() {
        System.out.println();
        System.out.println(SUB_LINE);
        System.out.println("Program finished. All steps ran with built-in sample data.");
        System.out.println(SUB_LINE);
    }
}
