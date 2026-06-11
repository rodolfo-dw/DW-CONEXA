
package br.com.userflow.fluig.rest.util.fluig.soap.generated;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * &lt;p&gt;Classe Java de sociableDocumentDto complex type.
 * 
 * &lt;p&gt;O seguinte fragmento do esquema especifica o conteúdo esperado contido dentro desta classe.
 * 
 * &lt;pre&gt;
 * &amp;lt;complexType name="sociableDocumentDto"&amp;gt;
 *   &amp;lt;complexContent&amp;gt;
 *     &amp;lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&amp;gt;
 *       &amp;lt;sequence&amp;gt;
 *         &amp;lt;element name="commented" type="{http://www.w3.org/2001/XMLSchema}boolean"/&amp;gt;
 *         &amp;lt;element name="denounced" type="{http://www.w3.org/2001/XMLSchema}boolean"/&amp;gt;
 *         &amp;lt;element name="documentId" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/&amp;gt;
 *         &amp;lt;element name="following" type="{http://www.w3.org/2001/XMLSchema}boolean"/&amp;gt;
 *         &amp;lt;element name="liked" type="{http://www.w3.org/2001/XMLSchema}boolean"/&amp;gt;
 *         &amp;lt;element name="numberComments" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/&amp;gt;
 *         &amp;lt;element name="numberDenouncements" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/&amp;gt;
 *         &amp;lt;element name="numberFollows" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/&amp;gt;
 *         &amp;lt;element name="numberLikes" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/&amp;gt;
 *         &amp;lt;element name="numberShares" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/&amp;gt;
 *         &amp;lt;element name="shared" type="{http://www.w3.org/2001/XMLSchema}boolean"/&amp;gt;
 *         &amp;lt;element name="sociableId" type="{http://www.w3.org/2001/XMLSchema}long" minOccurs="0"/&amp;gt;
 *         &amp;lt;element name="version" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/&amp;gt;
 *       &amp;lt;/sequence&amp;gt;
 *     &amp;lt;/restriction&amp;gt;
 *   &amp;lt;/complexContent&amp;gt;
 * &amp;lt;/complexType&amp;gt;
 * &lt;/pre&gt;
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "sociableDocumentDto", propOrder = {
    "commented",
    "denounced",
    "documentId",
    "following",
    "liked",
    "numberComments",
    "numberDenouncements",
    "numberFollows",
    "numberLikes",
    "numberShares",
    "shared",
    "sociableId",
    "version"
})
public class SociableDocumentDto {

    protected boolean commented;
    protected boolean denounced;
    protected Integer documentId;
    protected boolean following;
    protected boolean liked;
    protected Integer numberComments;
    protected Integer numberDenouncements;
    protected Integer numberFollows;
    protected Integer numberLikes;
    protected Integer numberShares;
    protected boolean shared;
    protected Long sociableId;
    protected Integer version;

    /**
     * Obtém o valor da propriedade commented.
     * 
     */
    public boolean isCommented() {
        return commented;
    }

    /**
     * Define o valor da propriedade commented.
     * 
     */
    public void setCommented(boolean value) {
        this.commented = value;
    }

    /**
     * Obtém o valor da propriedade denounced.
     * 
     */
    public boolean isDenounced() {
        return denounced;
    }

    /**
     * Define o valor da propriedade denounced.
     * 
     */
    public void setDenounced(boolean value) {
        this.denounced = value;
    }

    /**
     * Obtém o valor da propriedade documentId.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getDocumentId() {
        return documentId;
    }

    /**
     * Define o valor da propriedade documentId.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setDocumentId(Integer value) {
        this.documentId = value;
    }

    /**
     * Obtém o valor da propriedade following.
     * 
     */
    public boolean isFollowing() {
        return following;
    }

    /**
     * Define o valor da propriedade following.
     * 
     */
    public void setFollowing(boolean value) {
        this.following = value;
    }

    /**
     * Obtém o valor da propriedade liked.
     * 
     */
    public boolean isLiked() {
        return liked;
    }

    /**
     * Define o valor da propriedade liked.
     * 
     */
    public void setLiked(boolean value) {
        this.liked = value;
    }

    /**
     * Obtém o valor da propriedade numberComments.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getNumberComments() {
        return numberComments;
    }

    /**
     * Define o valor da propriedade numberComments.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setNumberComments(Integer value) {
        this.numberComments = value;
    }

    /**
     * Obtém o valor da propriedade numberDenouncements.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getNumberDenouncements() {
        return numberDenouncements;
    }

    /**
     * Define o valor da propriedade numberDenouncements.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setNumberDenouncements(Integer value) {
        this.numberDenouncements = value;
    }

    /**
     * Obtém o valor da propriedade numberFollows.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getNumberFollows() {
        return numberFollows;
    }

    /**
     * Define o valor da propriedade numberFollows.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setNumberFollows(Integer value) {
        this.numberFollows = value;
    }

    /**
     * Obtém o valor da propriedade numberLikes.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getNumberLikes() {
        return numberLikes;
    }

    /**
     * Define o valor da propriedade numberLikes.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setNumberLikes(Integer value) {
        this.numberLikes = value;
    }

    /**
     * Obtém o valor da propriedade numberShares.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getNumberShares() {
        return numberShares;
    }

    /**
     * Define o valor da propriedade numberShares.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setNumberShares(Integer value) {
        this.numberShares = value;
    }

    /**
     * Obtém o valor da propriedade shared.
     * 
     */
    public boolean isShared() {
        return shared;
    }

    /**
     * Define o valor da propriedade shared.
     * 
     */
    public void setShared(boolean value) {
        this.shared = value;
    }

    /**
     * Obtém o valor da propriedade sociableId.
     * 
     * @return
     *     possible object is
     *     {@link Long }
     *     
     */
    public Long getSociableId() {
        return sociableId;
    }

    /**
     * Define o valor da propriedade sociableId.
     * 
     * @param value
     *     allowed object is
     *     {@link Long }
     *     
     */
    public void setSociableId(Long value) {
        this.sociableId = value;
    }

    /**
     * Obtém o valor da propriedade version.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getVersion() {
        return version;
    }

    /**
     * Define o valor da propriedade version.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setVersion(Integer value) {
        this.version = value;
    }

}
