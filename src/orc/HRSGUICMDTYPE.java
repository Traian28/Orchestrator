
package orc;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for HRS_GUI_CMD_TYPE.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="HRS_GUI_CMD_TYPE">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="SELECT_ITEM"/>
 *     &lt;enumeration value="GET_INPUT"/>
 *     &lt;enumeration value="DISPLAY_TEXT"/>
 *     &lt;enumeration value="PROVIDE_LOCAL_INFO"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "HRS_GUI_CMD_TYPE")
@XmlEnum
public enum HRSGUICMDTYPE {

    SELECT_ITEM,
    GET_INPUT,
    DISPLAY_TEXT,
    PROVIDE_LOCAL_INFO;

    public String value() {
        return name();
    }

    public static HRSGUICMDTYPE fromValue(String v) {
        return valueOf(v);
    }

}
