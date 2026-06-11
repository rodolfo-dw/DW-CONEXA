
package br.com.userflow.fluig.rest.util.fluig.soap.generated;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * &lt;p&gt;Classe Java de processDefinitionVersionDto complex type.
 * 
 * &lt;p&gt;O seguinte fragmento do esquema especifica o conteúdo esperado contido dentro desta classe.
 * 
 * &lt;pre&gt;
 * &amp;lt;complexType name="processDefinitionVersionDto"&amp;gt;
 *   &amp;lt;complexContent&amp;gt;
 *     &amp;lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&amp;gt;
 *       &amp;lt;sequence&amp;gt;
 *         &amp;lt;element name="categoryStructure" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 *         &amp;lt;element name="companyId" type="{http://www.w3.org/2001/XMLSchema}long"/&amp;gt;
 *         &amp;lt;element name="counterSign" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&amp;gt;
 *         &amp;lt;element name="favorite" type="{http://www.w3.org/2001/XMLSchema}boolean"/&amp;gt;
 *         &amp;lt;element name="formId" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/&amp;gt;
 *         &amp;lt;element name="formVersion" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/&amp;gt;
 *         &amp;lt;element name="fullCategoryStructure" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 *         &amp;lt;element name="initialProcessState" type="{http://ws.workflow.ecm.technology.totvs.com/}processStateDto" minOccurs="0"/&amp;gt;
 *         &amp;lt;element name="mobileReady" type="{http://www.w3.org/2001/XMLSchema}boolean"/&amp;gt;
 *         &amp;lt;element name="processDescription" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 *         &amp;lt;element name="processId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 *         &amp;lt;element name="relatedDatasets" type="{http://www.w3.org/2001/XMLSchema}string" maxOccurs="unbounded" minOccurs="0"/&amp;gt;
 *         &amp;lt;element name="rowId" type="{http://www.w3.org/2001/XMLSchema}int"/&amp;gt;
 *         &amp;lt;element name="version" type="{http://www.w3.org/2001/XMLSchema}int"/&amp;gt;
 *         &amp;lt;element name="versionDescription" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 *       &amp;lt;/sequence&amp;gt;
 *     &amp;lt;/restriction&amp;gt;
 *   &amp;lt;/complexContent&amp;gt;
 * &amp;lt;/complexType&amp;gt;
 * &lt;/pre&gt;
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "processDefinitionVersionDto", propOrder = {
    "categoryStructure",
    "companyId",
    "counterSign",
    "favorite",
    "formId",
    "formVersion",
    "fullCategoryStructure",
    "initialProcessState",
    "mobileReady",
    "processDescription",
    "processId",
    "relatedDatasets",
    "rowId",
    "version",
    "versionDescription"
})
public class ProcessDefinitionVersionDto {

    protected String categoryStructure;
    protected long companyId;
    protected Boolean counterSign;
    protected boolean favorite;
    protected Integer formId;
    protected Integer formVersion;
    protected String fullCategoryStructure;
    protected ProcessStateDto initialProcessState;
    protected boolean mobileReady;
    protected String processDescription;
    protected String processId;
    @XmlElement(nillable = true)
    protected List<String> relatedDatasets;
    protected int rowId;
    protected int version;
    protected String versionDescription;

    /**
     * Obtém o valor da propriedade categoryStructure.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCategoryStructure() {
        return categoryStructure;
    }

    /**
     * Define o valor da propriedade categoryStructure.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCategoryStructure(String value) {
        this.categoryStructure = value;
    }

    /**
     * Obtém o valor da propriedade companyId.
     * 
     */
    public long getCompanyId() {
        return companyId;
    }

    /**
     * Define o valor da propriedade companyId.
     * 
     */
    public void setCompanyId(long value) {
        this.companyId = value;
    }

    /**
     * Obtém o valor da propriedade counterSign.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isCounterSign() {
        return counterSign;
    }

    /**
     * Define o valor da propriedade counterSign.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setCounterSign(Boolean value) {
        this.counterSign = value;
    }

    /**
     * Obtém o valor da propriedade favorite.
     * 
     */
    public boolean isFavorite() {
        return favorite;
    }

    /**
     * Define o valor da propriedade favorite.
     * 
     */
    public void setFavorite(boolean value) {
        this.favorite = value;
    }

    /**
     * Obtém o valor da propriedade formId.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getFormId() {
        return formId;
    }

    /**
     * Define o valor da propriedade formId.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setFormId(Integer value) {
        this.formId = value;
    }

    /**
     * Obtém o valor da propriedade formVersion.
     * 
     * @return
     *     possible object is
     *     {@link Integer }
     *     
     */
    public Integer getFormVersion() {
        return formVersion;
    }

    /**
     * Define o valor da propriedade formVersion.
     * 
     * @param value
     *     allowed object is
     *     {@link Integer }
     *     
     */
    public void setFormVersion(Integer value) {
        this.formVersion = value;
    }

    /**
     * Obtém o valor da propriedade fullCategoryStructure.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getFullCategoryStructure() {
        return fullCategoryStructure;
    }

    /**
     * Define o valor da propriedade fullCategoryStructure.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setFullCategoryStructure(String value) {
        this.fullCategoryStructure = value;
    }

    /**
     * Obtém o valor da propriedade initialProcessState.
     * 
     * @return
     *     possible object is
     *     {@link ProcessStateDto }
     *     
     */
    public ProcessStateDto getInitialProcessState() {
        return initialProcessState;
    }

    /**
     * Define o valor da propriedade initialProcessState.
     * 
     * @param value
     *     allowed object is
     *     {@link ProcessStateDto }
     *     
     */
    public void setInitialProcessState(ProcessStateDto value) {
        this.initialProcessState = value;
    }

    /**
     * Obtém o valor da propriedade mobileReady.
     * 
     */
    public boolean isMobileReady() {
        return mobileReady;
    }

    /**
     * Define o valor da propriedade mobileReady.
     * 
     */
    public void setMobileReady(boolean value) {
        this.mobileReady = value;
    }

    /**
     * Obtém o valor da propriedade processDescription.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getProcessDescription() {
        return processDescription;
    }

    /**
     * Define o valor da propriedade processDescription.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setProcessDescription(String value) {
        this.processDescription = value;
    }

    /**
     * Obtém o valor da propriedade processId.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getProcessId() {
        return processId;
    }

    /**
     * Define o valor da propriedade processId.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setProcessId(String value) {
        this.processId = value;
    }

    /**
     * Gets the value of the relatedDatasets property.
     * 
     * &lt;p&gt;
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a &lt;CODE&gt;set&lt;/CODE&gt; method for the relatedDatasets property.
     * 
     * &lt;p&gt;
     * For example, to add a new item, do as follows:
     * &lt;pre&gt;
     *    getRelatedDatasets().add(newItem);
     * &lt;/pre&gt;
     * 
     * 
     * &lt;p&gt;
     * Objects of the following type(s) are allowed in the list
     * {@link String }
     * 
     * 
     */
    public List<String> getRelatedDatasets() {
        if (relatedDatasets == null) {
            relatedDatasets = new ArrayList<String>();
        }
        return this.relatedDatasets;
    }

    /**
     * Obtém o valor da propriedade rowId.
     * 
     */
    public int getRowId() {
        return rowId;
    }

    /**
     * Define o valor da propriedade rowId.
     * 
     */
    public void setRowId(int value) {
        this.rowId = value;
    }

    /**
     * Obtém o valor da propriedade version.
     * 
     */
    public int getVersion() {
        return version;
    }

    /**
     * Define o valor da propriedade version.
     * 
     */
    public void setVersion(int value) {
        this.version = value;
    }

    /**
     * Obtém o valor da propriedade versionDescription.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getVersionDescription() {
        return versionDescription;
    }

    /**
     * Define o valor da propriedade versionDescription.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setVersionDescription(String value) {
        this.versionDescription = value;
    }

}
