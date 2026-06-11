
package br.com.userflow.fluig.rest.util.fluig.soap.generated;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.XMLGregorianCalendar;


/**
 * &lt;p&gt;Classe Java de processTaskDto complex type.
 * 
 * &lt;p&gt;O seguinte fragmento do esquema especifica o conteúdo esperado contido dentro desta classe.
 * 
 * &lt;pre&gt;
 * &amp;lt;complexType name="processTaskDto"&amp;gt;
 *   &amp;lt;complexContent&amp;gt;
 *     &amp;lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&amp;gt;
 *       &amp;lt;sequence&amp;gt;
 *         &amp;lt;element name="active" type="{http://www.w3.org/2001/XMLSchema}boolean"/&amp;gt;
 *         &amp;lt;element name="canCurrentUserTakeTask" type="{http://www.w3.org/2001/XMLSchema}boolean"/&amp;gt;
 *         &amp;lt;element name="choosedColleagueId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 *         &amp;lt;element name="choosedSequence" type="{http://www.w3.org/2001/XMLSchema}int"/&amp;gt;
 *         &amp;lt;element name="colleagueId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 *         &amp;lt;element name="colleagueName" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 *         &amp;lt;element name="companyId" type="{http://www.w3.org/2001/XMLSchema}long"/&amp;gt;
 *         &amp;lt;element name="complement" type="{http://www.w3.org/2001/XMLSchema}boolean"/&amp;gt;
 *         &amp;lt;element name="completeColleagueId" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 *         &amp;lt;element name="completeType" type="{http://www.w3.org/2001/XMLSchema}int"/&amp;gt;
 *         &amp;lt;element name="deadlineText" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 *         &amp;lt;element name="historCompleteColleague" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 *         &amp;lt;element name="historTaskObservation" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 *         &amp;lt;element name="movementSequence" type="{http://www.w3.org/2001/XMLSchema}int"/&amp;gt;
 *         &amp;lt;element name="processInstanceId" type="{http://www.w3.org/2001/XMLSchema}int"/&amp;gt;
 *         &amp;lt;element name="status" type="{http://www.w3.org/2001/XMLSchema}int"/&amp;gt;
 *         &amp;lt;element name="statusConsult" type="{http://www.w3.org/2001/XMLSchema}int"/&amp;gt;
 *         &amp;lt;element name="taskCompletionDate" type="{http://www.w3.org/2001/XMLSchema}dateTime" minOccurs="0"/&amp;gt;
 *         &amp;lt;element name="taskCompletionHour" type="{http://www.w3.org/2001/XMLSchema}int"/&amp;gt;
 *         &amp;lt;element name="taskObservation" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&amp;gt;
 *         &amp;lt;element name="taskSigned" type="{http://www.w3.org/2001/XMLSchema}boolean"/&amp;gt;
 *         &amp;lt;element name="transferredSequence" type="{http://www.w3.org/2001/XMLSchema}int"/&amp;gt;
 *       &amp;lt;/sequence&amp;gt;
 *     &amp;lt;/restriction&amp;gt;
 *   &amp;lt;/complexContent&amp;gt;
 * &amp;lt;/complexType&amp;gt;
 * &lt;/pre&gt;
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "processTaskDto", propOrder = {
    "active",
    "canCurrentUserTakeTask",
    "choosedColleagueId",
    "choosedSequence",
    "colleagueId",
    "colleagueName",
    "companyId",
    "complement",
    "completeColleagueId",
    "completeType",
    "deadlineText",
    "historCompleteColleague",
    "historTaskObservation",
    "movementSequence",
    "processInstanceId",
    "status",
    "statusConsult",
    "taskCompletionDate",
    "taskCompletionHour",
    "taskObservation",
    "taskSigned",
    "transferredSequence"
})
public class ProcessTaskDto {

    protected boolean active;
    protected boolean canCurrentUserTakeTask;
    protected String choosedColleagueId;
    protected int choosedSequence;
    protected String colleagueId;
    protected String colleagueName;
    protected long companyId;
    protected boolean complement;
    protected String completeColleagueId;
    protected int completeType;
    protected String deadlineText;
    protected String historCompleteColleague;
    protected String historTaskObservation;
    protected int movementSequence;
    protected int processInstanceId;
    protected int status;
    protected int statusConsult;
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar taskCompletionDate;
    protected int taskCompletionHour;
    protected String taskObservation;
    protected boolean taskSigned;
    protected int transferredSequence;

    /**
     * Obtém o valor da propriedade active.
     * 
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Define o valor da propriedade active.
     * 
     */
    public void setActive(boolean value) {
        this.active = value;
    }

    /**
     * Obtém o valor da propriedade canCurrentUserTakeTask.
     * 
     */
    public boolean isCanCurrentUserTakeTask() {
        return canCurrentUserTakeTask;
    }

    /**
     * Define o valor da propriedade canCurrentUserTakeTask.
     * 
     */
    public void setCanCurrentUserTakeTask(boolean value) {
        this.canCurrentUserTakeTask = value;
    }

    /**
     * Obtém o valor da propriedade choosedColleagueId.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getChoosedColleagueId() {
        return choosedColleagueId;
    }

    /**
     * Define o valor da propriedade choosedColleagueId.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setChoosedColleagueId(String value) {
        this.choosedColleagueId = value;
    }

    /**
     * Obtém o valor da propriedade choosedSequence.
     * 
     */
    public int getChoosedSequence() {
        return choosedSequence;
    }

    /**
     * Define o valor da propriedade choosedSequence.
     * 
     */
    public void setChoosedSequence(int value) {
        this.choosedSequence = value;
    }

    /**
     * Obtém o valor da propriedade colleagueId.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getColleagueId() {
        return colleagueId;
    }

    /**
     * Define o valor da propriedade colleagueId.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setColleagueId(String value) {
        this.colleagueId = value;
    }

    /**
     * Obtém o valor da propriedade colleagueName.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getColleagueName() {
        return colleagueName;
    }

    /**
     * Define o valor da propriedade colleagueName.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setColleagueName(String value) {
        this.colleagueName = value;
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
     * Obtém o valor da propriedade complement.
     * 
     */
    public boolean isComplement() {
        return complement;
    }

    /**
     * Define o valor da propriedade complement.
     * 
     */
    public void setComplement(boolean value) {
        this.complement = value;
    }

    /**
     * Obtém o valor da propriedade completeColleagueId.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getCompleteColleagueId() {
        return completeColleagueId;
    }

    /**
     * Define o valor da propriedade completeColleagueId.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setCompleteColleagueId(String value) {
        this.completeColleagueId = value;
    }

    /**
     * Obtém o valor da propriedade completeType.
     * 
     */
    public int getCompleteType() {
        return completeType;
    }

    /**
     * Define o valor da propriedade completeType.
     * 
     */
    public void setCompleteType(int value) {
        this.completeType = value;
    }

    /**
     * Obtém o valor da propriedade deadlineText.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDeadlineText() {
        return deadlineText;
    }

    /**
     * Define o valor da propriedade deadlineText.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDeadlineText(String value) {
        this.deadlineText = value;
    }

    /**
     * Obtém o valor da propriedade historCompleteColleague.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getHistorCompleteColleague() {
        return historCompleteColleague;
    }

    /**
     * Define o valor da propriedade historCompleteColleague.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setHistorCompleteColleague(String value) {
        this.historCompleteColleague = value;
    }

    /**
     * Obtém o valor da propriedade historTaskObservation.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getHistorTaskObservation() {
        return historTaskObservation;
    }

    /**
     * Define o valor da propriedade historTaskObservation.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setHistorTaskObservation(String value) {
        this.historTaskObservation = value;
    }

    /**
     * Obtém o valor da propriedade movementSequence.
     * 
     */
    public int getMovementSequence() {
        return movementSequence;
    }

    /**
     * Define o valor da propriedade movementSequence.
     * 
     */
    public void setMovementSequence(int value) {
        this.movementSequence = value;
    }

    /**
     * Obtém o valor da propriedade processInstanceId.
     * 
     */
    public int getProcessInstanceId() {
        return processInstanceId;
    }

    /**
     * Define o valor da propriedade processInstanceId.
     * 
     */
    public void setProcessInstanceId(int value) {
        this.processInstanceId = value;
    }

    /**
     * Obtém o valor da propriedade status.
     * 
     */
    public int getStatus() {
        return status;
    }

    /**
     * Define o valor da propriedade status.
     * 
     */
    public void setStatus(int value) {
        this.status = value;
    }

    /**
     * Obtém o valor da propriedade statusConsult.
     * 
     */
    public int getStatusConsult() {
        return statusConsult;
    }

    /**
     * Define o valor da propriedade statusConsult.
     * 
     */
    public void setStatusConsult(int value) {
        this.statusConsult = value;
    }

    /**
     * Obtém o valor da propriedade taskCompletionDate.
     * 
     * @return
     *     possible object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public XMLGregorianCalendar getTaskCompletionDate() {
        return taskCompletionDate;
    }

    /**
     * Define o valor da propriedade taskCompletionDate.
     * 
     * @param value
     *     allowed object is
     *     {@link XMLGregorianCalendar }
     *     
     */
    public void setTaskCompletionDate(XMLGregorianCalendar value) {
        this.taskCompletionDate = value;
    }

    /**
     * Obtém o valor da propriedade taskCompletionHour.
     * 
     */
    public int getTaskCompletionHour() {
        return taskCompletionHour;
    }

    /**
     * Define o valor da propriedade taskCompletionHour.
     * 
     */
    public void setTaskCompletionHour(int value) {
        this.taskCompletionHour = value;
    }

    /**
     * Obtém o valor da propriedade taskObservation.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTaskObservation() {
        return taskObservation;
    }

    /**
     * Define o valor da propriedade taskObservation.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTaskObservation(String value) {
        this.taskObservation = value;
    }

    /**
     * Obtém o valor da propriedade taskSigned.
     * 
     */
    public boolean isTaskSigned() {
        return taskSigned;
    }

    /**
     * Define o valor da propriedade taskSigned.
     * 
     */
    public void setTaskSigned(boolean value) {
        this.taskSigned = value;
    }

    /**
     * Obtém o valor da propriedade transferredSequence.
     * 
     */
    public int getTransferredSequence() {
        return transferredSequence;
    }

    /**
     * Define o valor da propriedade transferredSequence.
     * 
     */
    public void setTransferredSequence(int value) {
        this.transferredSequence = value;
    }

}
