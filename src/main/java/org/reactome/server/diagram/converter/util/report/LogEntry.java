package org.reactome.server.diagram.converter.util.report;

import java.util.Arrays;

/**
 * @author Kostas Sidiropoulos <ksidiro@ebi.ac.uk>
 */
public class LogEntry {
    private String id;
    private String[] secondaryIds;
    private LogEntryType type;
    private String message;

    public LogEntry(LogEntryType type, String message, String id) {
        this.id = id;
        this.type = type;
        this.message = message;
        this.secondaryIds = null;
    }

    public LogEntry(LogEntryType type, String message, String id, String...secondaryIds) {
        this(type, message, id);
        this.secondaryIds = secondaryIds;
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

    public String[] getSecondaryIds() {
        return secondaryIds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LogEntry logEntry = (LogEntry) o;

        if (!id.equals(logEntry.id)) return false;
        // Probably incorrect - comparing Object[] arrays with Arrays.equals
        if (!Arrays.equals(secondaryIds, logEntry.secondaryIds)) return false;
        return type == logEntry.type;

    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + Arrays.hashCode(secondaryIds);
        result = 31 * result + type.hashCode();
        return result;
    }
}
