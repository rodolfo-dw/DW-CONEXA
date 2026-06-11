package br.com.datawer.danfse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** PDF gerado acompanhado da trilha de auditoria dos campos impressos. */
public final class DanfseGenerationResult {

	private final byte[] pdf;
	private final List<DanfseAuditEntry> audit;

	DanfseGenerationResult(byte[] pdf, List<DanfseAuditEntry> audit) {
		this.pdf = pdf;
		this.audit = Collections.unmodifiableList(new ArrayList<>(audit));
	}

	public byte[] getPdf() {
		return pdf;
	}

	public List<DanfseAuditEntry> getAudit() {
		return audit;
	}
}
