package org.reactome.server.diagram.converter.util.report;

/**
 * @author Kostas Sidiropoulos <ksidiro@ebi.ac.uk>
 */
public enum LogEntryType {
    RENDERABLECLASS_MISSMATCH("#Diagram", "#Entity"),
    SUBPATHWAY_WITHOUT_PARTICIPANTS("#Diagram"),
    DIAGRAM_EMPTY("#Diagram"),
    MISSING_REACTION("#Diagram", "#Event"),
    VERY_LONG_NAMES("#Entity"),
    DUPLICATE_REACTION_PARTS("#Diagram", "#Reaction", "#PointingTwiceTo"),
    MISSING_STABLEIDS("#Entity"),
    ISOLATED_GLYPHS("#Diagram", "#Entity"),
    OVERLAPPING_REACTION_SHAPES("#Diagram", "#Reaction");


    private String[] columns;

    LogEntryType(String... columns) {
        this.columns = columns;
    }

    public String[] getColumns() {
        return columns;
    }
}
