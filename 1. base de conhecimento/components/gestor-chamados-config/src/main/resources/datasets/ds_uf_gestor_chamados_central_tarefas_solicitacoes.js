function createDataset(fields, constraints, sortFields) {
	var dataset = DatasetBuilder.newDataset();
	var dataSource = "/jdbc/AppDS";
	var ic = new javax.naming.InitialContext();
	var ds = ic.lookup(dataSource);
	var created = false;

	var conn = null;
	var stmt = null;
	var rs = null;

	var WKCompany = getValue("WKCompany");

	var queryFilter = "";

	var token = "";
	var user = "";
	var type = "";
	var processNumber = "";

	if (constraints && constraints.length && constraints.length > 0) {
		for (attr in constraints) {
			var field = constraints[attr]['fieldName'].toUpperCase();
			var iv = constraints[attr]['initialValue'];
			var fv = constraints[attr]['finalValue'];

			if (field == "TYPE") {
				type = iv;
				continue;
			}
			if (field == "PROCESS_NUMBER") {
				processNumber = iv;
				continue;
			}
			if (field == "TOKEN") {
				token = iv;
				continue;
			}
			if (field == "STATUS") {
				if (iv == "0" || iv == "1" || iv == "2") {
					queryFilter += " AND PW.STATUS = '" + iv + "'";
				} else if (iv == "3") {
					queryFilter += " AND HP.NUM_SEQ_ESTADO IN (12,14,16,18) AND PW.STATUS != '1' AND TP.CD_MATRICULA = 'integration-6585-5698-9865-1253-1'";
				}
				continue;
			}
			if (field == "USER") {
				user = iv;
				queryFilter += " AND ML.idUserCreationExt = '" + iv + "'";
				continue;
			}
		}
	}

	try {
		if (!token || !user) throw "Constraints 'TOKEN' e 'USER' são obrigatórias.";

		validaUserToken(token, user, WKCompany);

		if (type == "observations") {
			if (processNumber == "") throw "Envio do parâmetro 'PROCESS_NUMBER' é obrigatório.";

			validaSolicitacao(processNumber, user);

			var dataset = getObservations(processNumber);
			return dataset;
		}

		var { gestorChamadosML, gestorChamadosMLFull } = getMetaListIds();

		conn = ds.getConnection();
		stmt = conn.createStatement();
		var dbMeta = conn.getMetaData();
		var dbName = dbMeta.getDatabaseProductName();

		if (dbName.toLowerCase() == "mysql") {
			var query = "WITH HISTOR_PROCES_TOP AS ( SELECT HP.NUM_PROCES, HP.COD_EMPRESA, HP.NUM_SEQ_ESTADO, HP.MOVTO_DATE_TIME, ROW_NUMBER() OVER ( PARTITION BY HP.NUM_PROCES, HP.COD_EMPRESA ORDER BY HP.LOG_ATIV DESC, HP.MOVTO_DATE_TIME ASC ) AS RN FROM HISTOR_PROCES HP ), TAR_PROCES_TOP AS ( SELECT TP.NUM_PROCES, TP.COD_EMPRESA, TP.CD_MATRICULA, TP.DEADLINE, TP.NUM_SEQ_MOVTO, ROW_NUMBER() OVER ( PARTITION BY TP.NUM_PROCES, TP.COD_EMPRESA ORDER BY TP.END_DATE ASC ) AS RN FROM TAR_PROCES TP ), PROCESS_OBS_TOP AS ( SELECT PO.NUM_PROCESS, PO.MOV_SEQ, PO.OBSERVATION, ROW_NUMBER() OVER ( PARTITION BY PO.NUM_PROCESS, PO.MOV_SEQ ORDER BY PO.OBSERVATION_ID ASC ) AS RN FROM PROCESS_OBSERVATION PO WHERE PO.OBS_TYPE IS NULL ) SELECT DOC.NR_VERSAO, DOC.NUM_DOCTO_PROPRIED, DOC.NR_DOCUMENTO, PW.NUM_PROCES, IFNULL(DATE_FORMAT(PW.START_DATE, '%d/%m/%Y %H:%i'), '') AS START_DATE, IFNULL(DATE_FORMAT(PW.END_DATE, '%d/%m/%Y %H:%i'), '') AS END_DATE, CASE PW.STATUS WHEN 0 THEN 'Aberta' WHEN 1 THEN 'Cancelada' ELSE 'Finalizada' END AS STATUS, U_REQ.FULL_NAME, CASE WHEN PW.STATUS IN (2) THEN 'FINALIZADA' WHEN PW.STATUS IN (1) THEN 'CANCELADA' ELSE IFNULL(UPPER(EP.NOM_ESTADO), '') END AS TAREFA_ATUAL, HP.NUM_SEQ_ESTADO AS COD_TAREFA_ATUAL, CASE WHEN PW.STATUS IN (1, 2) THEN 'SEM RESPONSÁVEL' ELSE IFNULL(U_RESP.FULL_NAME, 'NÃO ASSUMIDA') END AS RESP_ATUAL, TP.CD_MATRICULA AS MATRICULA_RESP_ATUAL, TP.NUM_SEQ_MOVTO AS NUM_SEQ_MOVTO_ATUAL, DATE_FORMAT(TP.DEADLINE, '%d/%m/%Y %H:%i') AS PRAZO_ATUAL, DATE_FORMAT(HP.MOVTO_DATE_TIME, '%d/%m/%Y %H:%i') AS MOVTO_DATE_TIME, DATEDIFF(IFNULL(PW.END_DATE, NOW()), PW.START_DATE) AS DIAS_ABERTO, IF(PW.STATUS IN (1, 2), NULL, TP.DEADLINE) AS PRAZO_ATIVIDADE, PO.OBSERVATION, IFNULL(ML.qtdCamposComplementares, '0') AS QTDCAMPOSCOMPLEMENTARES, ML.codAtividadesUtilizadas, ML.telefoneContato AS TELEFONECONTATO, ML.codDescDepartamento AS CODDESCDEPARTAMENTO, ML.codDescCelulaDepartamento AS CODDESCCELULADEPARTAMENTO, ML.codDescGrupoAtendimento AS CODDESCGRUPOATENDIMENTO, ML.codDescSubgrupoAtendimento AS CODDESCSUBGRUPOATENDIMENTO, ML.codDescAtividade AS CODDESCATIVIDADE, ML.motivoSolicitacao AS MOTIVOSOLICITACAO FROM PROCES_WORKFLOW PW INNER JOIN " + gestorChamadosMLFull + " ML ON ML.CARDID = PW.NR_DOCUMENTO_CARD_INDEX AND ML.DOCUMENTID = PW.NR_DOCUMENTO_CARD AND ML.COMPANYID = PW.COD_EMPRESA INNER JOIN DOCUMENTO DOC ON DOC.NUM_DOCTO_PROPRIED = PW.NR_DOCUMENTO_CARD_INDEX AND DOC.NR_DOCUMENTO = PW.NR_DOCUMENTO_CARD AND DOC.NR_VERSAO = ML.VERSION AND DOC.VERSAO_ATIVA = 1 AND DOC.TP_DOCUMENTO = '5' AND DOC.COD_LISTA = " + gestorChamadosML + " LEFT JOIN HISTOR_PROCES_TOP HP ON HP.NUM_PROCES = PW.NUM_PROCES AND HP.COD_EMPRESA = PW.COD_EMPRESA AND HP.RN = 1 LEFT JOIN TAR_PROCES_TOP TP ON TP.NUM_PROCES = PW.NUM_PROCES AND TP.COD_EMPRESA = PW.COD_EMPRESA AND TP.RN = 1 LEFT JOIN PROCESS_OBS_TOP PO ON PO.NUM_PROCESS = PW.NUM_PROCES AND PO.MOV_SEQ = TP.NUM_SEQ_MOVTO AND PO.RN = 1 LEFT JOIN FDN_USERTENANT UT_REQ ON UT_REQ.USER_CODE = PW.COD_MATR_REQUISIT LEFT JOIN FDN_USER U_REQ ON U_REQ.USER_ID = UT_REQ.USER_ID LEFT JOIN FDN_USERTENANT UT_RESP ON UT_RESP.USER_CODE = TP.CD_MATRICULA LEFT JOIN FDN_USER U_RESP ON U_RESP.USER_ID = UT_RESP.USER_ID LEFT JOIN ESTADO_PROCES EP ON EP.COD_EMPRESA = PW.COD_EMPRESA AND EP.COD_DEF_PROCES = PW.COD_DEF_PROCES AND EP.NUM_VERS = PW.NUM_VERS AND EP.NUM_SEQ = HP.NUM_SEQ_ESTADO WHERE PW.COD_DEF_PROCES = 'uf_gestor_chamados' AND PW.COD_EMPRESA = " + WKCompany + " " + queryFilter + " ORDER BY PW.NUM_PROCES DESC;"
		}

		rs = stmt.executeQuery(query);
		var meta = rs.getMetaData();
		var columnCount = meta.getColumnCount();
		while (rs.next()) {
			if (!created) {
				for (var i = 1; i <= columnCount; i++) {
					dataset.addColumn(meta.getColumnLabel(i).toUpperCase());
				}
				created = true;
			}

			var row = [];
			for (var i = 1; i <= columnCount; i++) {
				var value = rs.getString(i);
				row[i - 1] = (value !== null) ? String(value) : "";
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
function validaUserToken(token, user, companyId) {
	var cs = [];
	cs.push(DatasetFactory.createConstraint("companyId", companyId, companyId, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("metadata#active", true, true, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("sqlLimit", 1, 1, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("token", token, token, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("login", user, user, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("ativo", "1", "1", ConstraintType.MUST));

	var ds = DatasetFactory.getDataset("ds_uf_form_gerenciamento_token_acesso", null, cs, null);
	if (!ds || ds.rowsCount == 0) throw 'Token inválido ou não corresponde ao usuário.';
}
function validaSolicitacao(processNumber, user) {
	var cs = [];
	cs.push(DatasetFactory.createConstraint("companyId", getValue("WKCompany"), getValue("WKCompany"), ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("sqlLimit", 1, 1, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("metadata#active", true, true, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("processNumber", processNumber, processNumber, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("idUserCreationExt", user, user, ConstraintType.MUST));

	var ds = DatasetFactory.getDataset("ds_uf_form_gestor_chamados", ['processNumber'], cs, null);
	if (!ds || ds.rowsCount == 0) throw "Solicitação " + processNumber + " não pertence ao usuário " + user + " ou não foi encontrada.";
}
function getObservations(processNumber) {
	var dataset = DatasetBuilder.newDataset();
	var cols = ['nomeAutorInteracao', 'descCurrentTaskInteracao', 'dataBRInteracao', 'mailAutorInteracao', 'interacao'];

	for (var i = 0; i < cols.length; i++) {
		dataset.addColumn(cols[i]);
	}

	if (!processNumber) return dataset;

	try {
		var cs = [];
		cs.push(DatasetFactory.createConstraint("companyId", getValue("WKCompany"), getValue("WKCompany"), ConstraintType.MUST));
		cs.push(DatasetFactory.createConstraint("processNumber", processNumber, processNumber, ConstraintType.MUST));
		cs.push(DatasetFactory.createConstraint("metadata#active", true, true, ConstraintType.MUST));
		var ds = DatasetFactory.getDataset("ds_uf_form_gestor_chamados", ["companyId"], cs, null);

		if (!ds || ds.rowsCount == 0) return dataset;

		var documentId = ds.getValue(0, "documentid");
		var version = ds.getValue(0, "metadata#version");

		var cs = [];
		cs.push(DatasetFactory.createConstraint("companyId", getValue("WKCompany"), getValue("WKCompany"), ConstraintType.MUST));
		cs.push(DatasetFactory.createConstraint("tableName", "tblInteracoes", "tblInteracoes", ConstraintType.MUST));
		cs.push(DatasetFactory.createConstraint("metadata#active", true, true, ConstraintType.MUST));
		cs.push(DatasetFactory.createConstraint("documentid", documentId, documentId, ConstraintType.MUST));
		cs.push(DatasetFactory.createConstraint("metadata#version", version, version, ConstraintType.MUST));

		var ds = DatasetFactory.getDataset("ds_uf_form_gestor_chamados", null, cs, null);

		if (!ds || ds.rowsCount == 0) return dataset;

		for (var i = 0; i < ds.rowsCount; i++) {
			var rowArr = [];
			for (var j = 0; j < cols.length; j++) {
				var colName = cols[j];
				try {
					var val = ds.getValue(i, colName);
					rowArr.push(val);
				} catch (err) {
					rowArr.push("");
				}
			}
			dataset.addRow(rowArr);
		}

	} catch (e) {
		log.error("Erro em getObservations: " + e);
	}
	return dataset;
}