
package br.com.userflow.fluig.rest.util.fluig.soap.generated;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * &lt;p&gt;Classe Java de availableUsersDto complex type.
 * 
 * &lt;p&gt;O seguinte fragmento do esquema especifica o conteúdo esperado contido dentro desta classe.
 * 
 * &lt;pre&gt;
 * &amp;lt;complexType name="availableUsersDto"&amp;gt;
 *   &amp;lt;complexContent&amp;gt;
 *     &amp;lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&amp;gt;
 *       &amp;lt;sequence&amp;gt;
 *         &amp;lt;element name="isCollectiveTask" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&amp;gt;
 *         &amp;lt;element name="users" type="{http://ws.workflow.ecm.technology.totvs.com/}colleagueDto" maxOccurs="unbounded" minOccurs="0"/&amp;gt;
 *         &amp;lt;element name="willShowUsers" type="{http://www.w3.org/2001/XMLSchema}boolean" minOccurs="0"/&amp;gt;
 *       &amp;lt;/sequence&amp;gt;
 *     &amp;lt;/restriction&amp;gt;
 *   &amp;lt;/complexContent&amp;gt;
 * &amp;lt;/complexType&amp;gt;
 * &lt;/pre&gt;
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "availableUsersDto", propOrder = {
    "isCollectiveTask",
    "users",
    "willShowUsers"
})
public class AvailableUsersDto {

    protected Boolean isCollectiveTask;
    @XmlElement(nillable = true)
    protected List<ColleagueDto> users;
    protected Boolean willShowUsers;

    /**
     * Obtém o valor da propriedade isCollectiveTask.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isIsCollectiveTask() {
        return isCollectiveTask;
    }

    /**
     * Define o valor da propriedade isCollectiveTask.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setIsCollectiveTask(Boolean value) {
        this.isCollectiveTask = value;
    }

    /**
     * Gets the value of the users property.
     * 
     * &lt;p&gt;
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a &lt;CODE&gt;set&lt;/CODE&gt; method for the users property.
     * 
     * &lt;p&gt;
     * For example, to add a new item, do as follows:
     * &lt;pre&gt;
     *    getUsers().add(newItem);
     * &lt;/pre&gt;
     * 
     * 
     * &lt;p&gt;
     * Objects of the following type(s) are allowed in the list
     * {@link ColleagueDto }
     * 
     * 
     */
    public List<ColleagueDto> getUsers() {
        if (users == null) {
            users = new ArrayList<ColleagueDto>();
        }
        return this.users;
    }

    /**
     * Obtém o valor da propriedade willShowUsers.
     * 
     * @return
     *     possible object is
     *     {@link Boolean }
     *     
     */
    public Boolean isWillShowUsers() {
        return willShowUsers;
    }

    /**
     * Define o valor da propriedade willShowUsers.
     * 
     * @param value
     *     allowed object is
     *     {@link Boolean }
     *     
     */
    public void setWillShowUsers(Boolean value) {
        this.willShowUsers = value;
    }

}
