
package org.reactome.server.diagram.converter.input.model;

import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;


/**
 * <p>Java class for anonymous complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element ref="{}org.gk.render.RenderableFeature" maxOccurs="unbounded"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "orgGkRenderRenderableFeature"
})
@XmlRootElement(name = "NodeAttachments")
public class NodeAttachments {

    @XmlElement(name = "org.gk.render.RenderableFeature", required = true)
    protected List<OrgGkRenderRenderableFeature> orgGkRenderRenderableFeature;

    /**
     * Gets the value of the orgGkRenderRenderableFeature property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the orgGkRenderRenderableFeature property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getOrgGkRenderRenderableFeature().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link OrgGkRenderRenderableFeature }
     * 
     * 
     */
    public List<OrgGkRenderRenderableFeature> getOrgGkRenderRenderableFeature() {
        if (orgGkRenderRenderableFeature == null) {
            orgGkRenderRenderableFeature = new ArrayList<OrgGkRenderRenderableFeature>();
        }
        return this.orgGkRenderRenderableFeature;
    }

}
