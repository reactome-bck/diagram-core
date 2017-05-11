package org.reactome.server.diagram.converter.util.report;

/**
 * @author Kostas Sidiropoulos <ksidiro@ebi.ac.uk>
 */
public enum LogEntryType {
    RENDERABLECLASS_MISMATCH("RenderableClassMismatch", "#Diagram", "#Entity", "#Name"), //Fixed
    SCHEMACLASS_MISSING("SchemaClassMissing", "#Diagram", "#Entity", "#Name", "#RenderableClass"),
    SUBPATHWAY_WITHOUT_PARTICIPANTS("SubpathwayWithoutParticipants", "#Diagram", "#Subpathway", "#SubpathwayName"), //Not Fixed
    DIAGRAM_EMPTY("DiagramEmpty", "#Diagram"), //Not Fixed
    MISSING_REACTION("MissingReaction", "#Diagram", "#Event"),
    VERY_LONG_NAMES("VeryLongNames", "#Entity"), //Not Fixed
    DUPLICATE_REACTION_PARTS("DuplicateReactionParts", "#Diagram", "#Reaction", "#PointingTwiceTo"),
    MISSING_STABLEIDS("MissingStableIds", "#Entity"), //Can be fixed with GHOST
    ISOLATED_GLYPHS("IsolatedGlyphs", "#Diagram", "#Entity", "Name"), //Fixed
    OVERLAPPING_REACTION_SHAPES_ON_REACTOMECURATOR("OverlappingReactionShapesOnReactomeCurator", "#Diagram", "#Reaction"); //Can be fixed?


    private String[] columns;
    private String filename;

    LogEntryType(String filename, String... columns) {
        this.columns = columns;
        this.filename = filename;
    }

    public String[] getColumns() {
        return columns;
    }

    public String getFilename() {
        return filename;
    }
}
