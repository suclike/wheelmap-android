package wheelmap.org.domain.node;

import java.math.BigInteger;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name = "category")
public class Category {

	@Element(required = true)
    protected BigInteger id;
	@Element(required = false)
    protected String identifier;
	
	/**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link BigInteger }
     *     
     */
    public BigInteger getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link BigInteger }
     *     
     */
    public void setId(BigInteger value) {
        this.id = value;
    }
    
    /**
     * Gets the value of the identifier property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Sets the value of the identifier property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setIdentifier(String value) {
        this.identifier = value;
    }
    
    public String toString() {
    	return "id=" + id + ", identifier=" + identifier;
    }
}
