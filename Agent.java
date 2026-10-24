package com.helpdesk;

public class Agent extends User {

    public enum Level {
        JUNIOR("Junior"), SENIOR("Senior");

        private final String label;

        Level(String label) { this.label = label; }

        public String getLabel() { return label; }
    }

    private static int agentCount = 0;

    private final String team;
    private final Level level;
    private final int maxWorkload;
    private int activeTickets = 0;
    private int resolvedCount = 0;
    private int slaBreaches = 0;

    public Agent(String id, String name, String email, String team,
                 Level level, int maxWorkload) {
        super(id, name, email, Role.AGENT);
        if (team == null || team.trim().isEmpty()) {
            throw new IllegalArgumentException("Team must not be blank.");
        }
        if (level == null) {
            throw new IllegalArgumentException("Agent level must not be null.");
        }
        if (maxWorkload <= 0) {
            throw new IllegalArgumentException("Workload limit must be a positive number.");
        }
        this.team = team.trim();
        this.level = level;
        this.maxWorkload = maxWorkload;
        agentCount++;
    }

    public String getTeam() { return team; }
    public Level getLevel() { return level; }
    public int getResolvedCount() { return resolvedCount; }
    public int getSlaBreaches() { return slaBreaches; }
    public static int getAgentCount() { return agentCount; }

    @Override
    public int getWorkloadLimit() { return maxWorkload; }

    @Override
    public int getActiveTickets() { return activeTickets; }

    @Override
    public String getAssignmentBlocker(Ticket ticket) {
        if (ticket == null) {
            return "No ticket was supplied.";
        }
        String id = ticket.getTicketId();
        if (ticket.getStatus() == Ticket.Status.RESOLVED
                || ticket.getStatus() == Ticket.Status.CLOSED) {
            return "Ticket " + id + " is already " + ticket.getStatus()
                    + " and cannot be assigned.";
        }
        if (ticket.getAssignee() != null && ticket.getAssignee().getId().equals(getId())) {
            return id + " is already assigned to " + getName() + ".";
        }
        if (activeTickets >= maxWorkload) {
            return "Workload limit: " + getName() + " already holds " + activeTickets
                    + " of " + maxWorkload + " active tickets.";
        }
        if (level == Level.JUNIOR && ticket.getPriority() == Ticket.Priority.CRITICAL) {
            return "Level rule: Junior agents cannot take CRITICAL tickets. "
                    + id + " needs a SENIOR agent.";
        }
        return null;
    }

    @Override
    public void displayDetails() {
        System.out.printf("  %-9s %-5s %-14s %s%n", getRole().getLabel(), getId(),
                getName(), getEmail());
        System.out.println("            -> Team: " + team + " | Level: " + level.getLabel()
                + " | Workload limit: " + maxWorkload);
    }

    @Override
    void takeTicket() {
        if (activeTickets >= maxWorkload) {
            throw new IllegalStateException(getName() + " has reached the workload limit.");
        }
        activeTickets++;
    }

    @Override
    void releaseTicket() {
        if (activeTickets > 0) {
            activeTickets--;
        }
    }

    @Override
    void recordResolved() { resolvedCount++; }

    @Override
    void recordSlaBreach() { slaBreaches++; }
}
