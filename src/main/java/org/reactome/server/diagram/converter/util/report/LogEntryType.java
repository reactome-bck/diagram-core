package org.reactome.server.diagram.converter.util.report;

import org.apache.commons.lang.ArrayUtils;

/**
 * @author Kostas Sidiropoulos <ksidiro@ebi.ac.uk>
 */
public enum LogEntryType {
    RENDERABLECLASS_MISMATCH("RenderableClassMismatch", 1, "#Diagram", "#Entity", "#Name", "#Is","#ShouldBe"), //Fixed
    SCHEMACLASS_MISSING("SchemaClassMissing", 1, "#Diagram", "#Entity", "#Name", "#RenderableClass"),
    OBJECT_NOT_LONGER_IN_DATABASE("ObjectNotLongerInDatabase", 1, "#Diagram", "#Entity"),
    SUBPATHWAY_WITHOUT_PARTICIPANTS("SubpathwayWithoutParticipants", 1, "#Diagram", "#Subpathway", "#SubpathwayName"), //Not Fixed
    DIAGRAM_EMPTY("DiagramEmpty", 0, "#Diagram"), //Not Fixed
    MISSING_REACTION("MissingReaction", 1, "#Diagram", "#Event"),
    VERY_LONG_NAMES("VeryLongNames", 0, "#Entity"), //Not Fixed
    DUPLICATE_REACTION_PARTS("DuplicateReactionParts", 1, "#Diagram", "#Reaction", "#PointingTwiceTo"),
    MISSING_STABLEIDS("MissingStableIds", 0, "#Entity"), //Can be fixed with GHOST
    ISOLATED_GLYPHS("IsolatedGlyphs", 1, "#Diagram", "#Entity", "Name"), //Fixed
    OVERLAPPING_REACTION_SHAPES_ON_REACTOMECURATOR("OverlappingReactionShapesOnReactomeCurator", 1, "#Diagram", "#Reaction"); //Can be fixed?


    private String[] columns;
    private String filename;
    private int id;

    LogEntryType(String filename, int id, String... columns) {
        this.columns = columns;
        this.filename = filename;
        this.id = id;
    }

    public String[] getColumns() {
        return (String[]) ArrayUtils.addAll(columns, new String[]{"#Created","#Modified"});
    }

    public String getFilename() {
        return filename;
    }

    public int getId() {
        return id;
    }
}
