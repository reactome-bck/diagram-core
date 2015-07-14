package org.reactome.server.diagram.converter.layout.output;

import org.reactome.server.diagram.converter.input.model.Component;
import org.reactome.server.diagram.converter.input.model.Components;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public class Compartment extends NodeCommon {

    public List<Long> componentIds;

    public Compartment(Object obj) {
        super(obj);
        for (Method method : obj.getClass().getMethods()) {
            switch (method.getName()){
                case "getComponents":
                    this.componentIds = getComponents(method, obj);
                    break;
            }
        }
    }

    private static List<Long> getComponents(Method method, Object object){
        List<Long> rtn = new LinkedList<>();
        try{
            Components components = (Components) method.invoke(object);
            if(components!=null && components.getComponent()!=null){
                for (Object c : components.getComponent()) {
                    Component component = (Component) c;
                    rtn.add(component.getId().longValue());
                }
            }
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return rtn.isEmpty() ? null : rtn;
    }
}
