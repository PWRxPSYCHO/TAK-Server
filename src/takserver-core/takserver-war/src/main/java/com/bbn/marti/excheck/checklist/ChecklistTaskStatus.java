//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.8-b130911.1802
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a>
// Any modifications to this file will be lost upon recompilation of the source schema.
// Generated on: 2017.10.03 at 02:27:05 PM EDT
//


package com.bbn.marti.excheck.checklist;

import jakarta.xml.bind.annotation.XmlEnum;
import jakarta.xml.bind.annotation.XmlEnumValue;
import jakarta.xml.bind.annotation.XmlType;


/**
 * <p>Java class for checklistTaskStatus.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 * <p>
 * <pre>
 * {@code
 * &lt;simpleType name="checklistTaskStatus">
 *   &lt;restriction base="{http://www.w3.org/2001/XMLSchema}string">
 *     &lt;enumeration value="Pending"/>
 *     &lt;enumeration value="Complete"/>
 *     &lt;enumeration value="Complete (late)"/>
 *     &lt;enumeration value="Late"/>
 *   &lt;/restriction>
 * &lt;/simpleType>
 * }
 * </pre>
 *
 */
@XmlType(name = "checklistTaskStatus")
@XmlEnum
public enum ChecklistTaskStatus {

    @XmlEnumValue("Pending")
    PENDING("Pending"),
    @XmlEnumValue("Complete")
    COMPLETE("Complete"),
    @XmlEnumValue("Complete (late)")
    COMPLETE_LATE("Complete (late)"),
    @XmlEnumValue("Late")
    LATE("Late");
    private final String value;

    ChecklistTaskStatus(String v) {
        value = v;
    }

    public String value() {
        return value;
    }

    public static ChecklistTaskStatus fromValue(String v) {
        for (ChecklistTaskStatus c: ChecklistTaskStatus.values()) {
            if (c.value.equals(v)) {
                return c;
            }
        }
        throw new IllegalArgumentException(v);
    }

}
