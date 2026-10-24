package com.helpdesk;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Ticket {

    public enum Priority {
        LOW(1), MEDIUM(2), HIGH(3), CRITICAL(4);

        private final int rank;

        Priority(int rank) { this.rank = rank; }

        public int getRank() { return rank; }

        public boolean isUrgent() { return rank >= HIGH.rank; }
    }

    public enum Status {
        NEW, OPEN, PENDING, RESOLVED, CLOSED;

        public boolean canMoveTo(Status next) {
            switch (this) {
                case NEW:     return next == OPEN;
                case OPEN:    return next == PENDING || next == RESOLVED;
                case PENDING: return next == OPEN || next == RESOLVED;
                case RESOLVED: return next == CLOSED;
                default:      return false;
            }
        }
    }

    static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("dd-MMM HH:mm", Locale.ENGLISH);

    private static int totalCreated = 0;

    private final String ticketId;
    private String subject;
    private String description;
    private Priority priority;
    private Status status;
    private User assignee;
    private final User requester;
    private final Category category;
    private final String attachment;
    private final LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private boolean slaBreached;

    public Ticket(String ticketId, User requester, String subject, String description,
                  Priority priority, Category category, String attachment,
                  LocalDateTime createdAt) {
        if (ticketId == null || ticketId.trim().isEmpty()) {
            throw new IllegalArgumentException("Ticket ID must not be blank.");
        }
        if (requester == null) {
            throw new IllegalArgumentException("Requester must not be null.");
        }
        if (subject == null || subject.trim().isEmpty()) {
            throw new IllegalArgumentException("Subject must not be blank.");
        }
        if (priority == null) {
            throw new IllegalArgumentException("Priority must not be null.");
        }
        if (category == null || createdAt == null) {
            throw new IllegalArgumentException("Category and creation time are required.");
        }
        this.ticketId = ticketId.trim();
        this.requester = requester;
        this.subject = subject.trim();
        this.description = (description == null) ? "" : description.trim();
        this.priority = priority;
        this.category = category;
        this.attachment = (attachment == null || attachment.trim().isEmpty())
                ? null : attachment.trim();
        this.createdAt = createdAt;
        this.status = Status.NEW;
        totalCreated++;
    }

    protected Ticket(Ticket other) {
        this.ticketId = other.ticketId;
        this.subject = other.subject;
        this.description = other.description;
        this.priority = other.priority;
        this.status = other.status;
        this.assignee = other.assignee;
        this.requester = other.requester;
        this.category = other.category;
        this.attachment = other.attachment;
        this.createdAt = other.createdAt;
        this.resolvedAt = other.resolvedAt;
        this.slaBreached = other.slaBreached;
    }

    public String getTicketId() { return ticketId; }
    public String getSubject() { return subject; }
    public String getDescription() { return description; }
    public Priority getPriority() { return priority; }
    public Status getStatus() { return status; }
    public User getAssignee() { return assignee; }
    public User getRequester() { return requester; }
    public Category getCategory() { return category; }
    public String getAttachment() { return attachment; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public boolean isSlaBreached() { return slaBreached; }
    public static int getTotalCreated() { return totalCreated; }

    public void setSubject(String subject) {
        if (status == Status.RESOLVED || status == Status.CLOSED) {
            throw new IllegalStateException("Ticket " + ticketId + " is " + status
                    + "; its subject can no longer be edited.");
        }
        if (subject == null || subject.trim().isEmpty()) {
            throw new IllegalArgumentException("Subject must not be blank.");
        }
        this.subject = subject.trim();
    }

    public int getEffectiveSlaHours() { return category.getEffectiveSlaHours(priority); }

    public LocalDateTime getDueAt() { return createdAt.plusHours(getEffectiveSlaHours()); }

    public double hoursElapsed(LocalDateTime now) {
        LocalDateTime end = (resolvedAt != null) ? resolvedAt : now;
        return Duration.between(createdAt, end).toMinutes() / 60.0;
    }

    public boolean isOverSla(LocalDateTime now) {
        return hoursElapsed(now) > getEffectiveSlaHours();
    }

    void assignTo(User user) { this.assignee = user; }

    void changePriority(Priority newPriority) {
        if (newPriority == null) {
            throw new IllegalArgumentException("Priority must not be null.");
        }
        if (status == Status.RESOLVED || status == Status.CLOSED) {
            throw new IllegalStateException("Priority of ticket " + ticketId
                    + " cannot be changed because it is " + status + ".");
        }
        this.priority = newPriority;
    }

    void changeStatus(Status next, LocalDateTime when) {
        if (next == null) {
            throw new IllegalArgumentException("New status must not be null.");
        }
        if (!status.canMoveTo(next)) {
            throw new IllegalStateException("Invalid transition for " + ticketId + ": "
                    + status + " -> " + next + " is not allowed.");
        }
        if ((next == Status.OPEN || next == Status.RESOLVED) && assignee == null) {
            throw new IllegalStateException("Ticket " + ticketId + " must be assigned to an "
                    + "agent before it can move to " + next + ".");
        }
        status = next;
        if (next == Status.RESOLVED) {
            resolvedAt = when;
        }
    }

    void markSlaBreached() { slaBreached = true; }

    protected String statusNote() {
        switch (status) {
            case NEW:      return "Waiting to be assigned and opened";
            case OPEN:     return "Being worked on by the agent";
            case PENDING:  return "Waiting for customer / third-party reply";
            case RESOLVED: return "Fix delivered at " + resolvedAt.format(STAMP);
            default:       return "Closed; no further action";
        }
    }

    public void display() {
        System.out.println("  Ticket ID   : " + ticketId);
        System.out.println("  Subject     : " + subject);
        System.out.println("  Customer    : " + requester);
        System.out.println("  Category    : " + category.getName());
        System.out.println("  Priority    : " + priority);
        System.out.println("  Status      : " + status + " (" + statusNote() + ")");
        System.out.println("  Assignee    : " + (assignee == null ? "unassigned" : assignee.getName()));
        System.out.println("  Attachment  : " + (attachment == null ? "none" : attachment));
    }

    public static String summaryHeader() {
        return String.format("%-8s %-9s %-9s %-9s %-12s %s",
                "ID", "Priority", "Status", "Category", "Assignee", "Subject");
    }

    public String summaryLine() {
        String shortSubject = subject.length() > 24 ? subject.substring(0, 23) + "~" : subject;
        return String.format("%-8s %-9s %-9s %-9s %-12s %s", ticketId, priority, status,
                category.getName(), assignee == null ? "-" : assignee.getName(), shortSubject);
    }

    @Override
    public String toString() {
        return ticketId + " [" + priority + ", " + status + "]";
    }
}
