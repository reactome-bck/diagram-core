package org.reactome.server.diagram.converter.util;

import org.reactome.server.diagram.converter.layout.output.Shadow;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class ShadowsUtil {

    private Set<Shadow> shadows = new HashSet<>();

    public ShadowsUtil(Set<Shadow> shadows) {
        this.shadows = shadows;
    }

    public Set<Shadow> getShadows() {
        process();
        return shadows;
    }

    private void process(){

    }
}
