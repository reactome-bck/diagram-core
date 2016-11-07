package org.reactome.server.diagram.converter.util.report;

/**
 * @author Kostas Sidiropoulos <ksidiro@ebi.ac.uk>
 */
public enum LogEntryType {
    RENDERABLECLASS_MISSMATCH("#DiagramID", "#EntityID"),
    SUBPATHWAY_WITHOUT_PARTICIPANTS("#DiagramID"),
    DIAGRAM_EMPTY("#DiagramID"),
    MISSING_REACTION("#DiagramID", "#EventID"),
    UGLY_NAMES_CORRECTED("#EntityID"),
    DUPLICATE_REACTION_PARTS_CORRECTED("#ReactionID", "#PointingToEntity");

    private String[] columns;

    LogEntryType(String... columns) {
        this.columns = columns;
    }

    public String[] getColumns() {
        return columns;
    }
}
