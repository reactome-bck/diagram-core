package org.reactome.server.diagram.converter.layout;

/**
 * @author Kostas Sidiropoulos <ksidiro@ebi.ac.uk>
 */
public class DuplicateIdException extends Exception {
    public DuplicateIdException() { super(); }
    public DuplicateIdException(String message) { super(message); }
    public DuplicateIdException(String message, Throwable cause) { super(message, cause); }
    public DuplicateIdException(Throwable cause) { super(cause); }
}
