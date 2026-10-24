package com.helpdesk;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HelpdeskService {

    private static int ticketSequence = 0;

    private final String companyName;
    private final Map<String, User> users = new LinkedHashMap<>();
    private final Map<String, Category> categories = new LinkedHashMap<>();
    private final List<Ticket> tickets = new ArrayList<>();
    private LocalDateTime clock;
    private int slaBreachCount = 0;
    private String lastMessage = "";

    public HelpdeskService(String companyName, LocalDateTime startTime) {
        if (companyName == null || companyName.trim().isEmpty()) {
            throw new IllegalArgumentException("Company name must not be blank.");
        }
        if (startTime == null) {
            throw new IllegalArgumentException("Start time must not be null.");
        }
        this.companyName = companyName.trim();
        this.clock = startTime;
        addCategory(new Category("C00", "General", 48));
    }

    public String getCompanyName() { return companyName; }
    public LocalDateTime getNow() { return clock; }
    public int getTicketCount() { return tickets.size(); }
    public int getSlaBreachCount() { return slaBreachCount; }
    public String getLastMessage() { return lastMessage; }
    public static int getTicketSequence() { return ticketSequence; }
    public List<User> getUsers() { return new ArrayList<>(users.values()); }
    public List<Category> getCategories() { return new ArrayList<>(categories.values()); }

    public void advanceMinutes(long minutes) {
        if (minutes < 0) {
            throw new IllegalArgumentException("Cannot move the clock backwards.");
        }
        clock = clock.plusMinutes(minutes);
    }

    public void advanceHours(long hours) { advanceMinutes(hours * 60); }

    public void registerUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("User must not be null.");
        }
        if (users.containsKey(user.getId())) {
            throw new IllegalArgumentException("Duplicate user ID: " + user.getId());
        }
        users.put(user.getId(), user);
    }

    public User getUser(String id) {
        User user = (id == null) ? null : users.get(id.trim());
        if (user == null) {
            throw new IllegalArgumentException("No user found with ID " + id + ".");
        }
        return user;
    }

    public void addCategory(Category category) {
        if (category == null) {
            throw new IllegalArgumentException("Category must not be null.");
        }
        if (categories.containsKey(category.getId())) {
            throw new IllegalArgumentException("Duplicate category ID: " + category.getId());
        }
        categories.put(category.getId(), category);
    }

    public Category getCategory(String id) {
        Category c = (id == null) ? null : categories.get(id.trim());
        if (c == null) {
            throw new IllegalArgumentException("No category found with ID " + id + ".");
        }
        return c;
    }

    public Ticket openTicket(User customer, String subject, String description,
                             Ticket.Priority priority) {
        return openTicket(customer, subject, description, priority, "C00", null);
    }

    public Ticket openTicket(User customer, String subject, String description,
                             Ticket.Priority priority, String categoryId) {
        return openTicket(customer, subject, description, priority, categoryId, null);
    }

    public Ticket openTicket(User customer, String subject, String description,
                             Ticket.Priority priority, String categoryId, String attachment) {
        if (customer == null || customer.getRole() != User.Role.CUSTOMER) {
            throw new IllegalArgumentException("Only a registered CUSTOMER can raise a ticket.");
        }
        Category category = getCategory(categoryId);
        String id = String.format("TCK-%03d", ticketSequence + 1);
        Ticket ticket = (priority != null && priority.isUrgent())
                ? new UrgentTicket(id, customer, subject, description, priority,
                        category, attachment, clock)
                : new Ticket(id, customer, subject, description, priority,
                        category, attachment, clock);
        ticketSequence++;
        tickets.add(ticket);
        category.recordTicket();
        customer.recordRaised();
        return ticket;
    }

    public Ticket findById(String ticketId) {
        if (ticketId == null || ticketId.trim().isEmpty()) {
            throw new IllegalArgumentException("Ticket ID must not be blank.");
        }
        for (Ticket t : tickets) {
            if (t.getTicketId().equalsIgnoreCase(ticketId.trim())) {
                return t;
            }
        }
        throw new IllegalArgumentException("No ticket found with ID " + ticketId.trim() + ".");
    }

    public boolean assignAgent(String ticketId, User assignee) {
        Ticket ticket = findById(ticketId);
        if (assignee == null) {
            throw new IllegalArgumentException("Assignee must not be null.");
        }
        String blocker = assignee.getAssignmentBlocker(ticket);
        if (blocker != null) {
            lastMessage = "[DENIED] " + blocker;
            return false;
        }
        if (ticket.getAssignee() != null) {
            lastMessage = "[DENIED] " + ticket.getTicketId() + " is already assigned to "
                    + ticket.getAssignee().getName() + ". Use reassignAgent().";
            return false;
        }
        assignee.takeTicket();
        ticket.assignTo(assignee);
        lastMessage = "[ASSIGNED] " + ticket.getTicketId() + " -> " + assignee.getName()
                + " (" + assignee.getActiveTickets() + "/" + assignee.getWorkloadLimit()
                + " active).";
        return true;
    }

    public boolean reassignAgent(String ticketId, User newAssignee) {
        Ticket ticket = findById(ticketId);
        if (newAssignee == null) {
            throw new IllegalArgumentException("Assignee must not be null.");
        }
        User previous = ticket.getAssignee();
        if (previous == null) {
            lastMessage = "[DENIED] " + ticket.getTicketId()
                    + " has no current assignee; use assignAgent() first.";
            return false;
        }
        String blocker = newAssignee.getAssignmentBlocker(ticket);
        if (blocker != null) {
            lastMessage = "[DENIED] " + blocker;
            return false;
        }
        previous.releaseTicket();
        newAssignee.takeTicket();
        ticket.assignTo(newAssignee);
        lastMessage = "[REASSIGNED] " + ticket.getTicketId() + ": " + previous.getName()
                + " -> " + newAssignee.getName() + ".";
        return true;
    }

    public Ticket changeStatus(String ticketId, Ticket.Status next) {
        Ticket ticket = findById(ticketId);
        Ticket.Status before = ticket.getStatus();
        ticket.changeStatus(next, clock);
        User owner = ticket.getAssignee();
        if (next == Ticket.Status.RESOLVED) {
            owner.releaseTicket();
            owner.recordResolved();
            if (ticket.isOverSla(clock) && !ticket.isSlaBreached()) {
                registerBreach(ticket);
            }
        }
        lastMessage = "[STATUS] " + ticket.getTicketId() + ": " + before + " -> " + next
                + " at " + clock.format(Ticket.STAMP) + ".";
        return ticket;
    }

    public Ticket changePriority(String ticketId, Ticket.Priority newPriority) {
        Ticket ticket = findById(ticketId);
        Ticket.Priority before = ticket.getPriority();
        ticket.changePriority(newPriority);
        if (newPriority.isUrgent() && !(ticket instanceof UrgentTicket)) {
            Ticket urgent = new UrgentTicket(ticket);
            tickets.set(tickets.indexOf(ticket), urgent);
            ticket = urgent;
        }
        lastMessage = "[PRIORITY] " + ticket.getTicketId() + ": " + before + " -> "
                + newPriority + " (SLA now " + ticket.getEffectiveSlaHours() + " h).";
        return ticket;
    }

    public int checkSlaBreaches() {
        int found = 0;
        for (Ticket t : tickets) {
            boolean live = t.getStatus() != Ticket.Status.RESOLVED
                    && t.getStatus() != Ticket.Status.CLOSED;
            if (live && t.isOverSla(clock) && !t.isSlaBreached()) {
                registerBreach(t);
                found++;
            }
        }
        return found;
    }

    private void registerBreach(Ticket ticket) {
        ticket.markSlaBreached();
        slaBreachCount++;
        if (ticket.getAssignee() != null) {
            ticket.getAssignee().recordSlaBreach();
        }
    }

    public List<Ticket> listByPriority() {
        List<Ticket> sorted = new ArrayList<>(tickets);
        Collections.sort(sorted, new Comparator<Ticket>() {
            @Override
            public int compare(Ticket a, Ticket b) {
                int byRank = b.getPriority().getRank() - a.getPriority().getRank();
                return byRank != 0 ? byRank : a.getTicketId().compareTo(b.getTicketId());
            }
        });
        return sorted;
    }

    public List<Ticket> listByPriority(Ticket.Priority priority) {
        List<Ticket> result = new ArrayList<>();
        for (Ticket t : listByPriority()) {
            if (t.getPriority() == priority) {
                result.add(t);
            }
        }
        return result;
    }

    public List<Ticket> listByStatus(Ticket.Status status) {
        List<Ticket> result = new ArrayList<>();
        for (Ticket t : tickets) {
            if (t.getStatus() == status) {
                result.add(t);
            }
        }
        return result;
    }

    public List<Ticket> listByAgent(User agent) {
        List<Ticket> result = new ArrayList<>();
        for (Ticket t : tickets) {
            if (t.getAssignee() != null && t.getAssignee().getId().equals(agent.getId())) {
                result.add(t);
            }
        }
        return result;
    }

    public List<Ticket> listByCategory(Category category) {
        List<Ticket> result = new ArrayList<>();
        for (Ticket t : tickets) {
            if (t.getCategory().getId().equals(category.getId())) {
                result.add(t);
            }
        }
        return result;
    }

    public void printTicketList(String title, List<Ticket> list) {
        System.out.println(title + " (" + list.size() + ")");
        if (list.isEmpty()) {
            System.out.println("    (none)");
            return;
        }
        System.out.println("    " + Ticket.summaryHeader());
        for (Ticket t : list) {
            System.out.println("    " + t.summaryLine());
        }
    }

    public void printByStatus() {
        for (Ticket.Status s : Ticket.Status.values()) {
            printTicketList("  Status " + s, listByStatus(s));
        }
    }

    public void printTicketsByAgent() {
        System.out.println("Clock: " + clock.format(Ticket.STAMP));
        for (User u : users.values()) {
            if (u.getRole() != User.Role.AGENT) {
                continue;
            }
            Agent a = (Agent) u;
            System.out.println();
            System.out.println("  " + a.getName() + " [" + a.getLevel().getLabel() + ", "
                    + a.getTeam() + "] active " + a.getActiveTickets() + "/"
                    + a.getWorkloadLimit() + ", resolved " + a.getResolvedCount()
                    + ", SLA breaches " + a.getSlaBreaches());
            List<Ticket> mine = listByAgent(a);
            if (mine.isEmpty()) {
                System.out.println("    (no tickets)");
            }
            for (Ticket t : mine) {
                System.out.println("    " + t.summaryLine());
            }
        }
        List<Ticket> unassigned = new ArrayList<>();
        for (Ticket t : tickets) {
            if (t.getAssignee() == null) {
                unassigned.add(t);
            }
        }
        System.out.println();
        System.out.println("  Unassigned tickets: " + unassigned.size());
        for (Ticket t : unassigned) {
            System.out.println("    " + t.summaryLine());
        }
    }

    public void printTicketsByCategory() {
        System.out.printf("  %-9s %7s %5s %9s %8s%n",
                "Category", "Tickets", "Open", "Resolved", "Breached");
        int all = 0;
        for (Category c : categories.values()) {
            int open = 0;
            int done = 0;
            int breached = 0;
            for (Ticket t : listByCategory(c)) {
                if (t.getStatus() == Ticket.Status.RESOLVED
                        || t.getStatus() == Ticket.Status.CLOSED) {
                    done++;
                } else {
                    open++;
                }
                if (t.isSlaBreached()) {
                    breached++;
                }
            }
            all += c.getTicketCount();
            System.out.printf("  %-9s %7d %5d %9d %8d%n", c.getName(),
                    c.getTicketCount(), open, done, breached);
        }
        System.out.printf("  %-9s %7d%n", "TOTAL", all);
    }

    public void printSlaReport() {
        System.out.println("  (SLA hours depend on category and priority; elapsed = hours to resolve,");
        System.out.println("   or hours so far if still unresolved)");
        System.out.printf("  %-8s %-9s %-9s %5s %8s  %-9s %s%n",
                "ID", "Category", "Priority", "SLA", "Elapsed", "Status", "Result");
        for (Ticket t : tickets) {
            System.out.printf("  %-8s %-9s %-9s %4dh %7.1fh  %-9s %s%n", t.getTicketId(),
                    t.getCategory().getName(), t.getPriority(), t.getEffectiveSlaHours(),
                    t.hoursElapsed(clock), t.getStatus(),
                    t.isSlaBreached() ? "BREACHED" : "within SLA");
        }
        System.out.println("  Total SLA breaches: " + slaBreachCount);
    }
}
