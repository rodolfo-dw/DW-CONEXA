package br.com.datawer.danfse;

import java.util.LinkedHashMap;
import java.util.Map;

/** Registro auditável de um campo efetivamente impresso no DANFSe. */
public final class DanfseAuditEntry {

	private final String bloco;
	private final String labelImpresso;
	private final String valorImpresso;
	private final String caminhoXmlOrigem;
	private final String regraFormatacaoAplicada;
	private final boolean usouTracoPorAusencia;
	private final boolean usouDescricaoConvertidaPorTabela;
	private final boolean usouFraseEspecialDeBloco;

	DanfseAuditEntry(String bloco, String labelImpresso, String valorImpresso,
			String caminhoXmlOrigem, String regraFormatacaoAplicada,
			boolean usouTracoPorAusencia, boolean usouDescricaoConvertidaPorTabela,
			boolean usouFraseEspecialDeBloco) {
		this.bloco = bloco;
		this.labelImpresso = labelImpresso;
		this.valorImpresso = valorImpresso;
		this.caminhoXmlOrigem = caminhoXmlOrigem;
		this.regraFormatacaoAplicada = regraFormatacaoAplicada;
		this.usouTracoPorAusencia = usouTracoPorAusencia;
		this.usouDescricaoConvertidaPorTabela = usouDescricaoConvertidaPorTabela;
		this.usouFraseEspecialDeBloco = usouFraseEspecialDeBloco;
	}

	public String getBloco() {
		return bloco;
	}

	public String getLabelImpresso() {
		return labelImpresso;
	}

	public String getValorImpresso() {
		return valorImpresso;
	}

	public String getCaminhoXmlOrigem() {
		return caminhoXmlOrigem;
	}

	public String getRegraFormatacaoAplicada() {
		return regraFormatacaoAplicada;
	}

	public boolean isUsouTracoPorAusencia() {
		return usouTracoPorAusencia;
	}

	public boolean isUsouDescricaoConvertidaPorTabela() {
		return usouDescricaoConvertidaPorTabela;
	}

	public boolean isUsouFraseEspecialDeBloco() {
		return usouFraseEspecialDeBloco;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> item = new LinkedHashMap<>();
		item.put("bloco", bloco);
		item.put("labelImpresso", labelImpresso);
		item.put("valorImpresso", valorImpresso);
		item.put("caminhoXmlOrigem", caminhoXmlOrigem);
		item.put("regraFormatacaoAplicada", regraFormatacaoAplicada);
		item.put("usouTracoPorAusencia", usouTracoPorAusencia);
		item.put("usouDescricaoConvertidaPorTabela", usouDescricaoConvertidaPorTabela);
		item.put("usouFraseEspecialDeBloco", usouFraseEspecialDeBloco);
		return item;
	}
}
