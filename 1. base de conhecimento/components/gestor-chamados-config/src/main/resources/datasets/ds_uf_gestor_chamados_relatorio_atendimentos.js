function createDataset(fields, constraints, sortFields) {
	var dataset = DatasetBuilder.newDataset();
	var dataSource = "/jdbc/AppDS";
	var ic = new javax.naming.InitialContext();
	var ds = ic.lookup(dataSource);
	var created = false;

	var conn = null;
	var stmt = null;
	var rs = null;

	var startDate = null;
	var status = [];
	var processNumber = null;
	var requester = null;
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
			if (field == "START_DATE") {
				startDate = " AND PW.START_DATE >= '" + iv + "T00:00:00.000Z' AND PW.START_DATE < '" + fv + "T23:59:59.000Z'";
			}
			if (field == "STATUS") {
				status.push(iv);
			}
			if (field == "PROCESS_NUMBER") {
				processNumber = " AND PW.NUM_PROCES = '" + iv + "'";
			}
			if (field == "FILTER_REQUESTER" && iv) {
				data.requester = " AND PW.COD_MATR_REQUISIT = '" + getValue("WKUser") + "'";
				validaPermissoes = false;
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
		var { gestorChamadosML, gestorChamadosMLFull, tblCamposAdicionaisFull, tblHistoricoTransfFull} = getMetaListIds();
		
		if (processNumber == null) {

			if (startDate == null) throw 'O Preenchimento da constraint DATE é obrigatório';
			queryFilter += startDate;

			if (!status.length) throw 'O Preenchimento da constraint STATUS é obrigatório';
			queryFilter += " AND PW.STATUS IN (" + status.join(',') + ")";

			if (requester != null) queryFilter += data.requester;

			if (codDep != "") queryFilter += " AND ML.CODDEPARTAMENTO = '" + codDep + "'";
			if (codCelDep != "") queryFilter += " AND ML.CODCELULADEPARTAMENTO = '" + codCelDep + "'";
			if (grupoAtd != "") queryFilter += " AND ML.CODGRUPOATENDIMENTO = '" + grupoAtd + "'";
			if (subgrupoAtd != "") queryFilter += " AND ML.CODSUBGRUPOATENDIMENTO = '" + subgrupoAtd + "'";
			if (codAtv != "") queryFilter += " AND ML.CODATIVIDADE = '" + codAtv + "'";

		} else {
			queryFilter += data.processNumber;
		}

		conn = ds.getConnection();
		stmt = conn.createStatement();
		var dbMeta = conn.getMetaData();
		var dbName = dbMeta.getDatabaseProductName();

		if (dbName.toLowerCase() == "mysql") {
			var query = "WITH PROCESS_OBS_TOP AS ( SELECT PO.NUM_PROCESS, PO.MOV_SEQ, PO.OBSERVATION, ROW_NUMBER() OVER ( PARTITION BY PO.NUM_PROCESS, PO.MOV_SEQ ORDER BY PO.OBSERVATION_ID DESC ) AS RN FROM PROCESS_OBSERVATION PO WHERE PO.OBS_TYPE IS NULL ), CAMPOS_ADICIONAIS_AGG AS ( SELECT MLADC.documentid, MLADC.companyid, IF( SUBSTRING_INDEX(@@version, '.', 1) = '5' AND SUBSTRING_INDEX(SUBSTRING_INDEX(@@version, '.', 2), '.', -1) < '7', CONCAT( '[', GROUP_CONCAT( CONCAT( '{ID:', IFNULL(MLADC.ID, 'null'), ',', 'tipoCampoAdicional:''', IFNULL(MLADC.tipoCampoAdicional, ''), ''',', 'tabelaPxfCampoAdicional:''', IFNULL(MLADC.tabelaPxfCampoAdicional, ''), ''',', 'codCampoAdicional:''', IFNULL(MLADC.codCampoAdicional, ''), ''',', 'descCampoAdicional:''', IFNULL(MLADC.descCampoAdicional, ''), ''',', 'descSelecionadoCampoAdicional:''', IFNULL(MLADC.descSelecionadoCampoAdicional, ''), ''',', 'valorSelecionadoCampoAdicional:''', IFNULL(MLADC.valorSelecionadoCampoAdicional, ''), '''}' ) ), ']' ), JSON_ARRAYAGG( JSON_OBJECT( 'ID', MLADC.ID, 'tipoCampoAdicional', MLADC.tipoCampoAdicional, 'tabelaPxfCampoAdicional', MLADC.tabelaPxfCampoAdicional, 'codCampoAdicional', MLADC.codCampoAdicional, 'descCampoAdicional', MLADC.descCampoAdicional, 'descSelecionadoCampoAdicional', MLADC.descSelecionadoCampoAdicional, 'valorSelecionadoCampoAdicional', MLADC.valorSelecionadoCampoAdicional ) ) ) AS campos_adicionais FROM " + tblCamposAdicionaisFull + " MLADC GROUP BY MLADC.documentid, MLADC.companyid ) SELECT DOC.NR_VERSAO, DOC.NUM_DOCTO_PROPRIED, DOC.NR_DOCUMENTO, PW.NUM_PROCES, IFNULL(DATE_FORMAT(PW.START_DATE, '%d/%m/%Y %H:%i'), '') AS START_DATE, IFNULL(DATE_FORMAT(PW.END_DATE, '%d/%m/%Y %H:%i'), '') AS END_DATE, CASE PW.STATUS WHEN 0 THEN 'Aberta' WHEN 1 THEN 'Cancelada' ELSE 'Finalizada' END AS STATUS, U_REQ.FULL_NAME, UPPER(EP1.NOM_ESTADO) AS TAREFA_ATUAL, DATEDIFF(IFNULL(PW.END_DATE, NOW()), PW.START_DATE) AS DIAS_ABERTO, IFNULL(U_RESP.FULL_NAME,'') AS FULL_NAME_ATEND, IFNULL(DATE_FORMAT(TP.START_DATE, '%d/%m/%Y %H:%i'), '') AS START_DATE_ATEND, IFNULL(DATE_FORMAT(TP.ASSIGN_START_DATE, '%d/%m/%Y %H:%i'), '') AS ASSIGN_DATE_ATEND, IFNULL(DATE_FORMAT(TP.DEADLINE, '%d/%m/%Y %H:%i'), '') AS DEADLINE_ATEND, IFNULL(DATE_FORMAT(TP.END_DATE, '%d/%m/%Y %H:%i'), '') AS END_DATE_ATEND, IFNULL(IFNULL(MLATEND.runtime_historic, TP.TOTAL_RUNTIME),0) AS TOTAL_RUNTIME_ATEND_SEC, IFNULL(CONCAT(FLOOR(IFNULL(MLATEND.runtime_historic, TP.TOTAL_RUNTIME) / 86400),'D:',LPAD(SEC_TO_TIME(MOD(IFNULL(MLATEND.runtime_historic, TP.TOTAL_RUNTIME), 86400)),8,'0')),0) AS TOTAL_RUNTIME_ATEND, CASE TP.CLOSURE_STATUS WHEN 0 THEN 'EM ABERTO' WHEN 1 THEN 'FINALIZADA NO PRAZO' WHEN 2 THEN 'AVISO DE EXPIRAÇÃO' WHEN 3 THEN 'EXPIRADA' END AS CLOSURE_STATUS_ATEND, CASE UPPER(EP.NOM_ESTADO) WHEN 'APROVAÇÃO' THEN 'TRANSFERÊNCIA DE DEPARTAMENTO' ELSE UPPER(EP.NOM_ESTADO) END AS NOM_ESTADO_DEST, IFNULL(MLATEND.coddesc_dep_historic_orig, ML.codDescDepartamento) AS CODDESCDEPARTAMENTO, IFNULL(MLATEND.cod_dep_historic_orig, ML.codDepartamento) AS CODDEPARTAMENTO, IFNULL(MLATEND.coddesc_cel_historic_orig, ML.codDescCelulaDepartamento) AS CODDESCCELULADEPARTAMENTO, IFNULL(MLATEND.cod_cel_historic_orig, ML.codCelulaDepartamento) AS CODCELULADEPARTAMENTO, IFNULL(MLATEND.coddesc_gru_historic_orig, ML.codDescGrupoAtendimento) AS CODDESCGRUPOATENDIMENTO, IFNULL(MLATEND.cod_gru_historic_orig, ML.codGrupoAtendimento) AS CODGRUPOATENDIMENTO, IFNULL(MLATEND.coddesc_sgr_historic_orig, ML.codDescSubgrupoAtendimento) AS CODDESCSUBGRUPOATENDIMENTO, IFNULL(MLATEND.cod_sgr_historic_orig, ML.codSubgrupoAtendimento) AS CODSUBGRUPOATENDIMENTO, IFNULL(MLATEND.coddesc_ati_historic_orig, ML.codDescAtividade) AS CODDESCATIVIDADE, IFNULL(MLATEND.cod_ati_historic_orig, ML.codAtividade) AS CODATIVIDADE, ML.telefoneContato AS TELEFONECONTATO, ML.motivoSolicitacao AS MOTIVOSOLICITACAO, IFNULL(PO.OBSERVATION,'') AS OBSERVATION, IFNULL(ML.qtdCamposComplementares, '0') AS QTDCAMPOSCOMPLEMENTARES, IFNULL(CA.campos_adicionais, '[]') AS CAMPOS_ADICIONAIS FROM PROCES_WORKFLOW PW INNER JOIN " + gestorChamadosMLFull + " ML ON ML.CARDID = PW.NR_DOCUMENTO_CARD_INDEX AND ML.DOCUMENTID = PW.NR_DOCUMENTO_CARD AND ML.COMPANYID = PW.COD_EMPRESA INNER JOIN DOCUMENTO DOC ON DOC.NUM_DOCTO_PROPRIED = PW.NR_DOCUMENTO_CARD_INDEX AND DOC.NR_DOCUMENTO = PW.NR_DOCUMENTO_CARD AND DOC.NR_VERSAO = ML.VERSION AND DOC.VERSAO_ATIVA = 1 AND DOC.TP_DOCUMENTO = '5' AND DOC.COD_LISTA = " + gestorChamadosML + " LEFT JOIN CAMPOS_ADICIONAIS_AGG CA ON CA.documentid = PW.NR_DOCUMENTO_CARD AND CA.companyid = PW.COD_EMPRESA INNER JOIN HISTOR_PROCES HP ON HP.NUM_PROCES = PW.NUM_PROCES AND HP.COD_EMPRESA = PW.COD_EMPRESA AND HP.NUM_SEQ_ESTADO IN (20, 21, 22, 23) INNER JOIN TAR_PROCES TP ON TP.NUM_PROCES = PW.NUM_PROCES AND TP.COD_EMPRESA = PW.COD_EMPRESA AND TP.NUM_SEQ_MOVTO = HP.NUM_SEQ_MOVTO AND TP.IDI_STATUS NOT IN (0,3) LEFT JOIN " + tblHistoricoTransfFull + " MLATEND ON MLATEND.CARDID = PW.NR_DOCUMENTO_CARD_INDEX AND MLATEND.DOCUMENTID = PW.NR_DOCUMENTO_CARD AND MLATEND.VERSION = DOC.NR_VERSAO AND MLATEND.NUM_SEQ_MOVTO_HISTORIC = TP.NUM_SEQ_MOVTO LEFT JOIN PROCESS_OBS_TOP PO ON PO.NUM_PROCESS = PW.NUM_PROCES AND PO.MOV_SEQ = TP.NUM_SEQ_MOVTO AND PO.RN = 1 LEFT JOIN FDN_USERTENANT UT_REQ ON UT_REQ.USER_CODE = PW.COD_MATR_REQUISIT LEFT JOIN FDN_USER U_REQ ON U_REQ.USER_ID = UT_REQ.USER_ID LEFT JOIN FDN_USERTENANT UT_RESP ON UT_RESP.USER_CODE = ( CASE WHEN TP.CD_MATRICULA_CONCLUS = '' THEN TP.CD_MATRICULA ELSE TP.CD_MATRICULA_CONCLUS END ) LEFT JOIN FDN_USER U_RESP ON U_RESP.USER_ID = UT_RESP.USER_ID LEFT JOIN ESTADO_PROCES EP1 ON EP1.COD_EMPRESA = PW.COD_EMPRESA AND EP1.COD_DEF_PROCES = PW.COD_DEF_PROCES AND EP1.NUM_VERS = PW.NUM_VERS AND EP1.NUM_SEQ = HP.NUM_SEQ_ESTADO LEFT JOIN ESTADO_PROCES EP ON EP.COD_EMPRESA = PW.COD_EMPRESA AND EP.COD_DEF_PROCES = PW.COD_DEF_PROCES AND EP.NUM_VERS = PW.NUM_VERS AND EP.NUM_SEQ = TP.NUM_SEQ_ESCOLHID WHERE PW.COD_DEF_PROCES = 'uf_gestor_chamados' AND PW.COD_EMPRESA = " + WKCompany + " " + queryFilter + " ORDER BY PW.NUM_PROCES DESC;"
		}
		
		var qtdCamposComplementares = 0;

		rs = stmt.executeQuery(query);
		var meta = rs.getMetaData();
		var columnCount = meta.getColumnCount();
		while (rs.next()) {
			if (!created) {
				for (var i = 1; i <= columnCount; i++) {
					dataset.addColumn(meta.getColumnName(i).toUpperCase());
				}
				dataset.addColumn('QTD_MAX_CAMPOS_ADC');
				created = true;
			}

			var row = [];
			for (var i = 1; i <= columnCount; i++) {
				var colName = meta.getColumnName(i);
				var value = rs.getString(colName);
				row[i - 1] = value !== null ? value : "";

				if (colName.toLowerCase() == "qtdcamposcomplementares") {
					if (Number(value) > Number(qtdCamposComplementares)) {
						qtdCamposComplementares = Number(value);
					}
				}
			}
			row.push(String(qtdCamposComplementares));


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