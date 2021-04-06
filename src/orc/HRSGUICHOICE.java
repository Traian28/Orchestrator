
package orc;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for HRS_GUI_CHOICE complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="HRS_GUI_CHOICE">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;all>
 *         &lt;element name="sItemValue" type="{ORC}HRS_SITEM_VALUE"/>
 *         &lt;element name="gInputValue" type="{ORC}HRS_GINPUT_VALUE"/>
 *       &lt;/all>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "HRS_GUI_CHOICE", propOrder = {

})
public class HRSGUICHOICE {

    @XmlElement(required = true, nillable = true)
    protected HRSSITEMVALUE sItemValue;
    @XmlElement(required = true, nillable = true)
    protected HRSGINPUTVALUE gInputValue;

    /**
     * Gets the value of the sItemValue property.
     * 
     * @return
     *     possible object is
     *     {@link HRSSITEMVALUE }
     *     
     */
    public HRSSITEMVALUE getSItemValue() {
        return sItemValue;
    }

    /**
     * Sets the value of the sItemValue property.
     * 
     * @param value
     *     allowed object is
     *     {@link HRSSITEMVALUE }
     *     
     */
    public void setSItemValue(HRSSITEMVALUE value) {
        this.sItemValue = value;
    }

    /**
     * Gets the value of the gInputValue property.
     * 
     * @return
     *     possible object is
     *     {@link HRSGINPUTVALUE }
     *     
     */
    public HRSGINPUTVALUE getGInputValue() {
        return gInputValue;
    }

    /**
     * Sets the value of the gInputValue property.
     * 
     * @param value
     *     allowed object is
     *     {@link HRSGINPUTVALUE }
     *     
     */
    public void setGInputValue(HRSGINPUTVALUE value) {
        this.gInputValue = value;
    }

}
