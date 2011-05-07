//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vhudson-jaxb-ri-2.1-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2011.05.05 at 02:26:05 PM PDT 
//


package org.apache.geronimo.openejb.deployment.model;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for web-service-bindingType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="web-service-bindingType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="ejb-name" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="web-service-address" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="web-service-virtual-host" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="web-service-security" type="{http://geronimo.apache.org/xml/ns/j2ee/ejb/openejb-2.0}web-service-securityType" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "web-service-bindingType", propOrder = {
    "ejbName",
    "webServiceAddress",
    "webServiceVirtualHost",
    "webServiceSecurity"
})
public class WebServiceBindingType {

    @XmlElement(name = "ejb-name", required = true)
    protected String ejbName;
    @XmlElement(name = "web-service-address")
    protected String webServiceAddress;
    @XmlElement(name = "web-service-virtual-host")
    protected List<String> webServiceVirtualHost;
    @XmlElement(name = "web-service-security")
    protected WebServiceSecurityType webServiceSecurity;

    /**
     * Gets the value of the ejbName property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getEjbName() {
        return ejbName;
    }

    /**
     * Sets the value of the ejbName property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setEjbName(String value) {
        this.ejbName = value;
    }

    /**
     * Gets the value of the webServiceAddress property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getWebServiceAddress() {
        return webServiceAddress;
    }

    /**
     * Sets the value of the webServiceAddress property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setWebServiceAddress(String value) {
        this.webServiceAddress = value;
    }

    /**
     * Gets the value of the webServiceVirtualHost property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the webServiceVirtualHost property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getWebServiceVirtualHost().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getWebServiceVirtualHost() {
        if (webServiceVirtualHost == null) {
            webServiceVirtualHost = new ArrayList<String>();
        }
        return this.webServiceVirtualHost;
    }

    /**
     * Gets the value of the webServiceSecurity property.
     * 
     * @return
     *     possible object is
     *     {@link WebServiceSecurityType }
     *     
     */
    public WebServiceSecurityType getWebServiceSecurity() {
        return webServiceSecurity;
    }

    /**
     * Sets the value of the webServiceSecurity property.
     * 
     * @param value
     *     allowed object is
     *     {@link WebServiceSecurityType }
     *     
     */
    public void setWebServiceSecurity(WebServiceSecurityType value) {
        this.webServiceSecurity = value;
    }

}
