package org.reactome.server.diagram.converter.util.report;

/**
 * @author Kostas Sidiropoulos <ksidiro@ebi.ac.uk>
 */
public enum LogEntryType {
    RENDERABLECLASS_MISSMATCH("#Diagram", "#Entity"),
    SUBPATHWAY_WITHOUT_PARTICIPANTS("#Diagram"),
    DIAGRAM_EMPTY("#Diagram"),
    MISSING_REACTION("#Diagram", "#Event"),
    UGLY_NAMES_CORRECTED("#Entity"),
    DUPLICATE_REACTION_PARTS_CORRECTED("#Diagram", "#Reaction", "#PointingTwiceTo");

    private String[] columns;

    LogEntryType(String... columns) {
        this.columns = columns;
    }

    public String[] getColumns() {
        return columns;
    }
}
