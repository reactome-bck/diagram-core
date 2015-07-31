package org.reactome.server.diagram.converter.graph.output;

import org.gk.model.GKInstance;
import org.gk.model.ReactomeJavaConstants;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class EventNode extends GraphNode {

    private Set<Long> preceding = new HashSet<>();
    private Set<Long> following = new HashSet<>();

    public Set<Long> inputs = new HashSet<>();
    public Set<Long> outputs = new HashSet<>();
    public Set<Long> catalysts = new HashSet<>();
    public Set<Long> inhibitors = new HashSet<>();
    public Set<Long> activators = new HashSet<>();
    public Set<Long> requirements = new HashSet<>();

    public Set<Long> diagramIds = new HashSet<>();

    public EventNode(GKInstance event) {
        super(event);

        this.inputs = this.getItemsIds(event, ReactomeJavaConstants.input);
        this.outputs = this.getItemsIds(event, ReactomeJavaConstants.output);

        //Catalysts contains catalystActivity's PhysicalEntity and entityFunctionalStatus's PhysicalEntity
        //because the diagram is pointing to this dbId rather than the container itself
        this.catalysts = this.getRegulators(event, ReactomeJavaConstants.catalystActivity);
        this.catalysts.addAll(this.getRegulators(event, ReactomeJavaConstants.entityFunctionalStatus));

        this.setActivatorsInhibitorsRequirements(event);

        this.preceding = this.getItemsIds(event, ReactomeJavaConstants.precedingEvent);
    }

    public void addDiagramId(Long diagramId){
        this.diagramIds.add(diagramId);
    }

    public Long getDbId() {
        return dbId;
    }

    public String getStId() {
        return stId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void addFollowingEvent(Long id){
        this.following.add(id);
    }


    public Set<Long> getPreceding() {
        if(preceding.isEmpty()) return null;
        return preceding;
    }

    public Set<Long> getFollowing() {
        if(following.isEmpty()) return null;
        return following;
    }

    public Set<Long> getInputs() {
        if(inputs.isEmpty()) return null;
        return inputs;
    }

    public Set<Long> getOutputs() {
        if(outputs.isEmpty()) return null;
        return outputs;
    }

    public Set<Long> getCatalysts() {
        if(catalysts.isEmpty()) return null;
        return catalysts;
    }

    public Set<Long> getInhibitors() {
        if(inhibitors.isEmpty()) return null;
        return inhibitors;
    }

    public Set<Long> getActivators() {
        if(activators.isEmpty()) return null;
        return activators;
    }

    public Set<Long> getRequirements() {
        if(requirements.isEmpty()) return null;
        return requirements;
    }

    private Set<Long> getItemsIds(GKInstance instance, String attr){
        Set<Long> rtn = new HashSet<>();
        try{
            Collection items = instance.getAttributeValuesList(attr);
            if(items!=null) {
                for (Object item : items) {
                    rtn.add(((GKInstance) item).getDBID());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rtn;
    }

    private Set<Long> getRegulators(GKInstance instance, String attr){
        Set<Long> rtn = new HashSet<>();
        try{
            Collection items = instance.getAttributeValuesList(attr);
            if(items!=null) {
                for (Object item : items) {
                    GKInstance reg = (GKInstance) item;
                    try {
                        GKInstance pe = (GKInstance) reg.getAttributeValue(ReactomeJavaConstants.physicalEntity);
                        if(pe!=null){
                            rtn.add(pe.getDBID());
                        }
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rtn;
    }


    private void setActivatorsInhibitorsRequirements(GKInstance event) {
        Collection<GKInstance> regulations = null;
        try {
            //noinspection unchecked
            regulations = event.getReferers(ReactomeJavaConstants.regulatedEntity);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (regulations == null || regulations.size() == 0)
            return;

        for (GKInstance regulation : regulations) {
            try {
                GKInstance regulator = (GKInstance) regulation.getAttributeValue(ReactomeJavaConstants.regulator);
                if (regulator == null)
                    continue; // Just in case. This should not happen usually

                // Have to check Requirement first since it is a subclass to PositiveRegulation
                if (regulation.getSchemClass().isa(ReactomeJavaConstants.Requirement)) {
                    this.requirements.add(regulator.getDBID());
                }
                else if (regulation.getSchemClass().isa(ReactomeJavaConstants.PositiveRegulation)) {
                    this.activators.add(regulator.getDBID());

                }
                else if (regulation.getSchemClass().isa(ReactomeJavaConstants.NegativeRegulation)) {
                    this.inhibitors.add(regulator.getDBID());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
