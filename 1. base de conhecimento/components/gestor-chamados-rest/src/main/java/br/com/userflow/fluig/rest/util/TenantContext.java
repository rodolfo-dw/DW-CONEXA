package br.com.userflow.fluig.rest.util;

public class TenantContext {

	private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();

	public static void setTenantId(Long tenantId) {
		if (tenantId != null) {
			TENANT_ID.set(tenantId);
		} else {
			TENANT_ID.remove();
		}
	}

	public static Long getTenantId() {
		return TENANT_ID.get();
	}

	public static Long getTenantId(Long defaultTenantId) {
		Long tenantId = TENANT_ID.get();
		return tenantId != null ? tenantId : defaultTenantId;
	}

	public static void clear() {
		TENANT_ID.remove();
	}
}
