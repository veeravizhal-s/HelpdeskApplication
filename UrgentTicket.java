package com.helpdesk;

public class UrgentTicket extends Ticket {

    public UrgentTicket(String ticketId, User requester, String subject, String description,
                        Priority priority, Category category, String attachment,
                        java.time.LocalDateTime createdAt) {
        super(ticketId, requester, subject, description, priority, category,
                attachment, createdAt);
        if (!priority.isUrgent()) {
            throw new IllegalArgumentException("UrgentTicket needs HIGH or CRITICAL priority.");
        }
    }

    UrgentTicket(Ticket other) {
        super(other);
    }

    @Override
    protected String statusNote() {
        return "URGENT - " + super.statusNote();
    }

    @Override
    public void display() {
        System.out.println("  *** URGENT " + getPriority() + " TICKET - SLA "
                + getEffectiveSlaHours() + " h, due " + getDueAt().format(STAMP) + " ***");
        super.display();
        System.out.println("  Escalation  : " + getCategory().getEscalationRule(getPriority()));
    }

    @Override
    public String summaryLine() {
        return super.summaryLine() + "  (!)";
    }
}
