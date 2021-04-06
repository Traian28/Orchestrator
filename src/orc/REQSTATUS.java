
package orc;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for REQSTATUS.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * &lt;simpleType name="REQSTATUS">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="OK"/>
 *     &lt;enumeration value="TO_RETRY"/>
 *     &lt;enumeration value="FAILED"/>
 *     &lt;enumeration value="TURNED_OFF"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * </pre>
 * 
 */
@XmlType(name = "REQSTATUS")
@XmlEnum
public enum REQSTATUS {

    OK,
    TO_RETRY,
    FAILED,
    TURNED_OFF;

    public String value() {
        return name();
    }

    public static REQSTATUS fromValue(String v) {
        return valueOf(v);
    }

}
