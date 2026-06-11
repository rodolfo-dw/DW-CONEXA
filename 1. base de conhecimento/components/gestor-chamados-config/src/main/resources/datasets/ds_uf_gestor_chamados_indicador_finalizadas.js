function createDataset(fields, constraints, sortFields) {
	var dataset = DatasetBuilder.newDataset();
	var dataSource = "/jdbc/AppDS";
	var ic = new javax.naming.InitialContext();
	var ds = ic.lookup(dataSource);
	var created = false;

	var conn = null;
	var stmt = null;
	var rs = null;

	var endDate = null;
	var queryFilter = "";

	var validaPermissoes = true;

	var codDep = "";
	var codCelDep = "";
	var grupoAtd = "";
	var subgrupoAtd = "";
	var codAtv = "";

	if (constraints && constraints.length && constraints.length > 0) {
		for (attr in constraints) {
			var field = constraints[attr]['fieldName'].toUpperCase();
			var iv = constraints[attr]['initialValue'];
			var fv = constraints[attr]['finalValue'];
			if (field == "END_DATE") {
				endDate = " AND PW.END_DATE >= '" + iv + "T00:00:00.000Z' AND PW.END_DATE < '" + fv + "T23:59:59.000Z'";
			}
			if (field == "CODDEPARTAMENTO") {
				codDep = iv;
			}
			if (field == "CODCELULADEPARTAMENTO") {
				codCelDep = iv;
			}
			if (field == "CODGRUPOATENDIMENTO") {
				grupoAtd = iv;
			}
			if (field == "CODSUBGRUPOATENDIMENTO") {
				subgrupoAtd = iv;
			}
			if (field == "CODATIVIDADE") {
				codAtv = iv;
			}
		}
	}
	try {
		validaPermissoesConsulta(validaPermissoes, codDep);

		var WKCompany = getValue("WKCompany");
		var { gestorChamadosML, gestorChamadosMLFull, tblCamposAdicionaisFull, tblHistoricoTransfFull } = getMetaListIds();

		if (endDate == null) throw 'O Preenchimento da constraint DATE é obrigatório';
		queryFilter += endDate;
		
		queryFilter += getFilterMLATEND(codDep, codCelDep, grupoAtd, subgrupoAtd, codAtv);
		
		conn = ds.getConnection();
		stmt = conn.createStatement();
		var dbMeta = conn.getMetaData();
		var dbName = dbMeta.getDatabaseProductName();

		// validar se a query está trazendo os dados corretos.
		
		if (dbName.toLowerCase() == "mysql") {
			var query = "WITH TEMP_ML_IND_FIN AS ( SELECT PW.NUM_PROCES, ML.AVALIACAOATENDIMENTO, CASE WHEN TP.CLOSURE_STATUS = 3 THEN 1 ELSE 0 END AS FORA_PRAZO, CASE WHEN TP.CLOSURE_STATUS = 3 THEN 0 ELSE 1 END AS DENTRO_PRAZO, MLATEND.cod_dep_historic_orig, ML.CODDEPARTAMENTO, MLATEND.cod_cel_historic_orig, ML.CODCELULADEPARTAMENTO, MLATEND.cod_gru_historic_orig, ML.CODGRUPOATENDIMENTO, MLATEND.cod_sgr_historic_orig, ML.CODATIVIDADE, HP.NUM_SEQ_ESTADO FROM PROCES_WORKFLOW PW INNER JOIN " + gestorChamadosMLFull + " ML ON ML.CARDID = PW.NR_DOCUMENTO_CARD_INDEX AND ML.DOCUMENTID = PW.NR_DOCUMENTO_CARD AND ML.COMPANYID = PW.COD_EMPRESA INNER JOIN DOCUMENTO DOC ON DOC.NUM_DOCTO_PROPRIED = PW.NR_DOCUMENTO_CARD_INDEX AND DOC.NR_DOCUMENTO = PW.NR_DOCUMENTO_CARD AND DOC.NR_VERSAO = ML.VERSION AND DOC.COD_EMPRESA = PW.COD_EMPRESA AND DOC.VERSAO_ATIVA = 1 AND DOC.TP_DOCUMENTO = '5' AND DOC.COD_LISTA = " + gestorChamadosML + " INNER JOIN HISTOR_PROCES HP ON HP.NUM_PROCES = PW.NUM_PROCES AND HP.COD_EMPRESA = PW.COD_EMPRESA AND HP.NUM_SEQ_ESTADO IN (20, 21, 22, 23) AND HP.LOG_ATIV = 0 INNER JOIN TAR_PROCES TP ON TP.NUM_PROCES = PW.NUM_PROCES AND TP.COD_EMPRESA = PW.COD_EMPRESA AND TP.NUM_SEQ_MOVTO = HP.NUM_SEQ_MOVTO AND TP.IDI_STATUS = 2 AND TP.IDI_STATUS <> 3 AND TP.LOG_ATIV = 0 LEFT JOIN " + tblHistoricoTransfFull + " MLATEND ON MLATEND.CARDID = PW.NR_DOCUMENTO_CARD_INDEX AND MLATEND.DOCUMENTID = PW.NR_DOCUMENTO_CARD AND MLATEND.VERSION = DOC.NR_VERSAO AND MLATEND.companyid = PW.COD_EMPRESA AND MLATEND.num_seq_movto_historic = TP.NUM_SEQ_MOVTO WHERE PW.COD_DEF_PROCES = 'uf_gestor_chamados' AND PW.STATUS = 2 AND PW.COD_EMPRESA = " + WKCompany + " " + queryFilter + " ) SELECT COUNT(TBAUX1.NUM_PROCES) AS QTD_CHAMADOS, IFNULL ( SUM( CASE WHEN UPPER(TRIM(TBAUX1.AVALIACAOATENDIMENTO)) = 'PÉSSIMO' THEN 1 ELSE 0 END ), 0 ) AS AVAL_PESSIMO, IFNULL ( SUM( CASE WHEN UPPER(TRIM(TBAUX1.AVALIACAOATENDIMENTO)) = 'RUIM' THEN 1 ELSE 0 END ), 0 ) AS AVAL_RUIM, IFNULL ( SUM( CASE WHEN UPPER(TRIM(TBAUX1.AVALIACAOATENDIMENTO)) = 'REGULAR' THEN 1 ELSE 0 END ), 0 ) AS AVAL_REGULAR, IFNULL ( SUM( CASE WHEN UPPER(TRIM(TBAUX1.AVALIACAOATENDIMENTO)) = 'BOM' THEN 1 ELSE 0 END ), 0 ) AS AVAL_BOM, IFNULL ( SUM( CASE WHEN UPPER(TRIM(TBAUX1.AVALIACAOATENDIMENTO)) = 'EXCELENTE' THEN 1 ELSE 0 END ), 0 ) AS AVAL_EXCELENTE, IFNULL ( SUM( CASE WHEN TBAUX1.AVALIACAOATENDIMENTO = '0' OR TBAUX1.AVALIACAOATENDIMENTO = '' THEN 1 ELSE 0 END ), 0 ) AS AVAL_NULL, ( SELECT COUNT(NUM_PROCES) FROM ( SELECT IFNULL (cod_dep_historic_orig, CODDEPARTAMENTO) AS CODDEPARTAMENTO, NUM_PROCES FROM TEMP_ML_IND_FIN ) AS SUB ) AS QTD_ATENDIMENTO, ( SELECT IFNULL (SUM(FORA_PRAZO), 0) FROM ( SELECT IFNULL (cod_dep_historic_orig, CODDEPARTAMENTO) AS CODDEPARTAMENTO, FORA_PRAZO FROM TEMP_ML_IND_FIN ) AS SUB ) AS FORA_PRAZO, ( SELECT IFNULL (SUM(DENTRO_PRAZO), 0) FROM ( SELECT IFNULL (cod_dep_historic_orig, CODDEPARTAMENTO) AS CODDEPARTAMENTO, DENTRO_PRAZO FROM TEMP_ML_IND_FIN ) AS SUB ) AS DENTRO_PRAZO FROM ( SELECT NUM_PROCES, AVALIACAOATENDIMENTO FROM TEMP_ML_IND_FIN GROUP BY NUM_PROCES, AVALIACAOATENDIMENTO ) AS TBAUX1;";
		} else if (dbName.toLowerCase() == "sql") {
			var query = ""
		}
		
		log.dir("query --->")
		log.dir(query)

		rs = stmt.executeQuery(query);
		var meta = rs.getMetaData();
		var columnCount = meta.getColumnCount();
		while (rs.next()) {
			if (!created) {
				for (var i = 1; i <= columnCount; i++) {
					dataset.addColumn(meta.getColumnName(i).toUpperCase());
				}
				created = true;
			}
			var row = [];
			for (var i = 1; i <= columnCount; i++) {
				var colName = meta.getColumnName(i);
				var value = rs.getString(colName);
				row[i - 1] = value !== null ? value : "";
			}
			dataset.addRow(row);
		}
	} catch (e) {
		log.error('Erro ao consultar o dataset ds_uf_gestor_chamados_relatorio_atendimento');
		log.dir(e);

		dataset = DatasetBuilder.newDataset();
		dataset.addColumn("ERROR");
		dataset.addRow(new Array(e.toString()));
	} finally {
		try { if (rs != null) rs.close(); } catch (e) { }
		try { if (stmt != null) stmt.close(); } catch (e) { }
		try { if (conn != null) conn.close(); } catch (e) { }
	}
	return dataset;
}

function getMetaListIds() {
	var ds = DatasetFactory.getDataset("ds_uf_gestor_chamados_metaListIds", null, null, null);

	if (!ds || ds == null || !ds.rowsCount) throw 'Não foi possível recuperar o código ML da tabela do Gestor de Chamados.';
	if (ds.getColumnName(0) == "ERROR") throw ds.getValue(0, "ERROR");

	return JSON.parse(ds.getValue(0, "METALISTIDS"));
}
function validaPermissoesConsulta(execute, codDep) {
	if (!execute) return;

	if (!codDep || codDep == '' || codDep == null || codDep == undefined) {
		var cs = [];
		cs.push(DatasetFactory.createConstraint("colleagueGroupPK.companyId", getValue("WKCompany"), getValue("WKCompany"), ConstraintType.MUST));
		cs.push(DatasetFactory.createConstraint("colleagueGroupPK.groupId", "uf_gc_gestores", "uf_gc_gestores", ConstraintType.MUST));
		cs.push(DatasetFactory.createConstraint("colleagueGroupPK.colleagueId", getValue("WKUser"), getValue("WKUser"), ConstraintType.MUST));
		var ds = DatasetFactory.getDataset("colleagueGroup", null, cs, null);

		if (!ds || !ds.rowsCount || ds.rowsCount == 0) throw 'O preenchimento do campo Departamento é obrigatório';

		return;
	}

	var cs = [];
	cs.push(DatasetFactory.createConstraint("colleagueGroupPK.companyId", getValue("WKCompany"), getValue("WKCompany"), ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("colleagueGroupPK.colleagueId", getValue("WKUser"), getValue("WKUser"), ConstraintType.MUST));
	var c1 = DatasetFactory.createConstraint("colleagueGroupPK.groupId", "uf_gc_atend_dp_" + codDep + '_%', null, ConstraintType.SHOULD);
	var c2 = DatasetFactory.createConstraint("colleagueGroupPK.groupId", "uf_gc_atend_cd_" + codDep + '_%', null, ConstraintType.SHOULD);
	var c3 = DatasetFactory.createConstraint("colleagueGroupPK.groupId", "uf_gc_atend_ga_" + codDep + '_%', null, ConstraintType.SHOULD);
	var c4 = DatasetFactory.createConstraint("colleagueGroupPK.groupId", "uf_gc_atend_sg_" + codDep + '_%', null, ConstraintType.SHOULD);
	var c5 = DatasetFactory.createConstraint("colleagueGroupPK.groupId", "uf_gc_gestores_dp_" + codDep + '_%', null, ConstraintType.SHOULD);
	var c6 = DatasetFactory.createConstraint("colleagueGroupPK.groupId", "uf_gc_gestores_cd_" + codDep + '_%', null, ConstraintType.SHOULD);

	c1.setLikeSearch(true);
	c2.setLikeSearch(true);
	c3.setLikeSearch(true);
	c4.setLikeSearch(true);
	c5.setLikeSearch(true);
	c6.setLikeSearch(true);

	cs.push(c1, c2, c3, c4, c5, c6);

	var ds = DatasetFactory.getDataset("colleagueGroup", null, cs, null);

	if (!ds || !ds.rowsCount || ds.rowsCount == 0) {
		throw 'Você não tem permissão para consultar esse departamento';
	}
}
function getFilterMLATEND(codDep, codCelDep, grupAtd, subgrupAtd, codAtv) {
	var filter = '';
	if (codDep != "") filter += " AND (MLATEND.COD_DEP_HISTORIC_ORIG = '" + codDep + "' OR ML.CODDEPARTAMENTO = '" + codDep + "')";
	if (codCelDep != "") filter += " AND (MLATEND.COD_CEL_HISTORIC_ORIG = '" + codCelDep + "' OR ML.CODCELULADEPARTAMENTO = '" + codCelDep + "')";
	if (grupAtd != "") filter += " AND (MLATEND.COD_GRU_HISTORIC_ORIG = '" + grupAtd + "' OR ML.CODGRUPOATENDIMENTO = '" + grupAtd + "')";
	if (subgrupAtd != "") filter += " AND (MLATEND.COD_SGR_HISTORIC_ORIG = '" + grupAtd + "' OR ML.CODSUBGRUPOATENDIMENTO = '" + grupAtd + "')";
	if (codAtv != "") filter += " AND (MLATEND.COD_ATI_HISTORIC_ORIG = '" + codAtv + "' OR ML.CODATIVIDADE = '" + codAtv + "')";
	return filter;
}