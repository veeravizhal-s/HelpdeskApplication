package com.helpdesk;

import java.util.EnumMap;
import java.util.Map;

public class Category {

    private static int categoryCount = 0;

    private final String id;
    private String name;
    private int slaHours;
    private final Map<Ticket.Priority, String> escalationRules =
            new EnumMap<>(Ticket.Priority.class);
    private int ticketCount = 0;

    public Category(String id, String name, int slaHours) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Category ID must not be blank.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name must not be blank.");
        }
        this.id = id.trim();
        this.name = name.trim();
        this.slaHours = validateSla(slaHours);
        categoryCount++;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getSlaHours() { return slaHours; }
    public int getTicketCount() { return ticketCount; }
    public static int getCategoryCount() { return categoryCount; }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Category name must not be blank.");
        }
        this.name = name.trim();
    }

    public void setSlaHours(int slaHours) { this.slaHours = validateSla(slaHours); }

    public void addEscalationRule(Ticket.Priority priority, String rule) {
        if (priority == null || rule == null || rule.trim().isEmpty()) {
            throw new IllegalArgumentException("Escalation rule needs a priority and text.");
        }
        escalationRules.put(priority, rule.trim());
    }

    public String getEscalationRule(Ticket.Priority priority) {
        String rule = escalationRules.get(priority);
        return rule == null ? "No escalation; handled by the assigned agent" : rule;
    }

    public int getEffectiveSlaHours(Ticket.Priority priority) {
        switch (priority) {
            case CRITICAL: return Math.max(1, slaHours / 4);
            case HIGH:     return Math.max(1, slaHours / 2);
            case MEDIUM:   return slaHours;
            default:       return slaHours * 2;
        }
    }

    void recordTicket() { ticketCount++; }

    public void display() {
        System.out.printf("  %-4s %-9s base SLA %3d h  (CRIT %d h, HIGH %d h, MED %d h, LOW %d h)%n",
                id, name, slaHours,
                getEffectiveSlaHours(Ticket.Priority.CRITICAL),
                getEffectiveSlaHours(Ticket.Priority.HIGH),
                getEffectiveSlaHours(Ticket.Priority.MEDIUM),
                getEffectiveSlaHours(Ticket.Priority.LOW));
        for (Map.Entry<Ticket.Priority, String> e : escalationRules.entrySet()) {
            System.out.println("         escalate " + e.getKey() + ": " + e.getValue());
        }
    }

    @Override
    public String toString() { return name + " (" + id + ")"; }

    private static int validateSla(int hours) {
        if (hours <= 0) {
            throw new IllegalArgumentException("SLA hours must be a positive number.");
        }
        return hours;
    }
}
