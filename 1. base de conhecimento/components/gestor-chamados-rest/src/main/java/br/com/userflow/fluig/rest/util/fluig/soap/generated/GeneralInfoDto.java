
package br.com.userflow.fluig.rest.util.fluig.soap.generated;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * &lt;p&gt;Classe Java de generalInfoDto complex type.
 * 
 * &lt;p&gt;O seguinte fragmento do esquema especifica o conteúdo esperado contido dentro desta classe.
 * 
 * &lt;pre&gt;
 * &amp;lt;complexType name="generalInfoDto"&amp;gt;
 *   &amp;lt;complexContent&amp;gt;
 *     &amp;lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&amp;gt;
 *       &amp;lt;sequence&amp;gt;
 *         &amp;lt;element name="versionOption" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 *       &amp;lt;/sequence&amp;gt;
 *     &amp;lt;/restriction&amp;gt;
 *   &amp;lt;/complexContent&amp;gt;
 * &amp;lt;/complexType&amp;gt;
 * &lt;/pre&gt;
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "generalInfoDto", propOrder = {
    "versionOption"
})
public class GeneralInfoDto {

    protected String versionOption;

    /**
     * Obtém o valor da propriedade versionOption.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVersionOption() {
        return versionOption;
    }

    /**
     * Define o valor da propriedade versionOption.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVersionOption(String value) {
        this.versionOption = value;
    }

}
