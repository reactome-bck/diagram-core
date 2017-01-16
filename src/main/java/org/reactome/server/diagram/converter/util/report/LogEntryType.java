package org.reactome.server.diagram.converter.util.report;

/**
 * @author Kostas Sidiropoulos <ksidiro@ebi.ac.uk>
 */
public enum LogEntryType {
    RENDERABLECLASS_MISSMATCH("#Diagram", "#Entity"), //Fixed
    SUBPATHWAY_WITHOUT_PARTICIPANTS("#Diagram"), //Not Fixed
    DIAGRAM_EMPTY("#Diagram"), //Not Fixed
    MISSING_REACTION("#Diagram", "#Event"),
    VERY_LONG_NAMES("#Entity"), //Not Fixed
    DUPLICATE_REACTION_PARTS("#Diagram", "#Reaction", "#PointingTwiceTo"),
    MISSING_STABLEIDS("#Entity"), //Can be fixed with GHOST
    ISOLATED_GLYPHS("#Diagram", "#Entity"), //Fixed
    OVERLAPPING_REACTION_SHAPES("#Diagram", "#Reaction"); //Can be fixed?


    private String[] columns;

    LogEntryType(String... columns) {
        this.columns = columns;
    }

    public String[] getColumns() {
        return columns;
    }
}
