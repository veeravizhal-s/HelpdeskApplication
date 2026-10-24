package com.helpdesk;

public class User {

    public enum Role {
        CUSTOMER("Customer"), AGENT("Agent");

        private final String label;

        Role(String label) { this.label = label; }

        public String getLabel() { return label; }
    }

    private static int userCount = 0;

    private final String id;
    private String name;
    private String email;
    private final Role role;
    private int ticketsRaised = 0;

    public User(String id, String name, String email, Role role) {
        this.id = requireText(id, "User ID");
        this.name = requireText(name, "Name");
        this.email = requireEmail(email);
        if (role == null) {
            throw new IllegalArgumentException("Role must not be null.");
        }
        this.role = role;
        userCount++;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public int getTicketsRaised() { return ticketsRaised; }
    public static int getUserCount() { return userCount; }

    public void setName(String name) { this.name = requireText(name, "Name"); }
    public void setEmail(String email) { this.email = requireEmail(email); }

    public int getWorkloadLimit() { return 0; }

    public int getActiveTickets() { return 0; }

    public String getAssignmentBlocker(Ticket ticket) {
        return name + " (" + role.getLabel() + ") cannot be assigned tickets.";
    }

    public boolean canHandle(Ticket ticket) {
        return getAssignmentBlocker(ticket) == null;
    }

    public void displayDetails() {
        System.out.printf("  %-9s %-5s %-14s %s%n", role.getLabel(), id, name, email);
        System.out.println("            -> Raises tickets; no workload (limit 0)");
    }

    void recordRaised() { ticketsRaised++; }

    void takeTicket() { }

    void releaseTicket() { }

    void recordResolved() { }

    void recordSlaBreach() { }

    @Override
    public String toString() { return name + " (" + id + ")"; }

    private static String requireText(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank.");
        }
        return value.trim();
    }

    private static String requireEmail(String value) {
        String text = requireText(value, "Email");
        int at = text.indexOf('@');
        if (at < 1 || text.indexOf('.', at) < 0 || text.contains(" ")) {
            throw new IllegalArgumentException("Invalid email address: " + text);
        }
        return text;
    }
}
