package br.com.userflow.fluig.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.QueryHint;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "USERFLOW_APPLICATION", uniqueConstraints = @UniqueConstraint(columnNames = {"DEVELOPER", "TENANT_ID", "NAME"}, name = "userflow_application_pk"))
@NamedQueries({
		@NamedQuery(
				name = ApplicationEntity.FIND_BY_NAME_DEV, 
				query = "SELECT ua FROM ApplicationEntity ua WHERE ua.tenantId = :tenantId"
						+ " AND (LOWER(ua.name) LIKE :text OR LOWER(ua.developer) LIKE :text)",
				hints = {@QueryHint(name = "parameters", value = "java.lang.Long tenantId")
		}),
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ApplicationEntity implements Serializable {
	
	private static final long serialVersionUID = 1L;

	public static final String FIND_BY_NAME_DEV = "Application.findAll";	
	
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID")
	@Id private Long id;

	@Column(name = "TENANT_ID")
	@NotNull private Long tenantId;
		
	@Column(name = "NAME", unique = true)
	@NotNull private String name;

	@Column(name = "DEVELOPER")	
	@NotNull private String developer;
	
	@Column(name = "SITE")	
	@NotNull private String site;
	
	@Column(name = "EMAIL")	
	@NotNull private String email;
	
	@Column(name = "PHONE")	
	@NotNull private String phone;
	
	
	public ApplicationEntity(String name, String developer, String site, String email, String phone) {
		this.name = name;
		this.developer = developer;
		this.site = site;
		this.email = email;
		this.phone = phone;
	}
}