package org.reactome.server.diagram.converter.util.report;

/**
 * @author Kostas Sidiropoulos <ksidiro@ebi.ac.uk>
 */
public class LogEntry {
    private String id;
    private String secondaryId;
    private LogEntryType type;
    private String message;

    public LogEntry(LogEntryType type, String id, String message) {
        this.id = id;
        this.type = type;
        this.message = message;
        this.secondaryId = null;
    }

    public LogEntry(LogEntryType type, String id, String secondaryId, String message) {
        this(type, id, message);
        this.secondaryId = secondaryId;
    }

    public String getId() {
        return id;
    }

    public LogEntryType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public String getSecondaryId() {
        return secondaryId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LogEntry logEntry = (LogEntry) o;

        if (!id.equals(logEntry.id)) return false;
        if (secondaryId != null ? !secondaryId.equals(logEntry.secondaryId) : logEntry.secondaryId != null)
            return false;
        if (type != logEntry.type) return false;
        return message != null ? message.equals(logEntry.message) : logEntry.message == null;

    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + (secondaryId != null ? secondaryId.hashCode() : 0);
        result = 31 * result + type.hashCode();
        result = 31 * result + (message != null ? message.hashCode() : 0);
        return result;
    }
}
