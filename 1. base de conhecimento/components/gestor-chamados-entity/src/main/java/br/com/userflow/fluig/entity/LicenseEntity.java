package br.com.userflow.fluig.entity;


import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Entity
@Table(name = "USERFLOW_LICENSE", uniqueConstraints = @UniqueConstraint(columnNames = { "TENANT_ID", "NAME" }, name = "userflow_license_pk"))
@NamedQueries({
		@NamedQuery(
				name = LicenseEntity.FIND_BY_NAME, 
				query = "SELECT le FROM LicenseEntity le WHERE le.tenantId = :tenantId AND LOWER(le.name) LIKE :name ORDER by le.id",
				hints = {@QueryHint(name = "parameters", value = "java.lang.Long tenantId")
		})
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class LicenseEntity implements Serializable {

	private static final long serialVersionUID = 1L;
	public static final String FIND_BY_NAME = "License.findByName";

	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID")
	@Id
	private Long id;

	@Column(name = "NAME", unique = true)
	@NotNull
	private String name;

	@Column(name = "TENANT_ID")
	@NotNull
	private Long tenantId;
	
	@Column(name = "ACTIVATE")
	@NotNull
	private Boolean activate;
}
