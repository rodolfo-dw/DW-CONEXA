importPackage(java.lang);
importPackage(java.io);
importPackage(org.apache.http);
importPackage(org.apache.http.client.methods);
importPackage(org.apache.http.entity);
importPackage(org.apache.http.impl.client);

function createDataset(fields, constraints, sortFields) {
	var dataset = DatasetBuilder.newDataset();

	var codColigada = "";
	var codSistema = "";
	var idLan = "";
	var jsonPayload = "";
	var urlBase = "";

	var client = null;
	var response = null;
	var isr = null;
	var br = null;

	if (constraints != null && constraints.length > 0) {
		for (var i = 0; i < constraints.length; i++) {
			var cFieldName = constraints[i].getFieldName();
			if (!cFieldName) continue;

			var field = String(cFieldName).toUpperCase();
			var iv = String(constraints[i].getInitialValue());

			if (field == "CODCOLIGADA") codColigada = iv;
			if (field == "CODSISTEMA") codSistema = iv;
			if (field == "IDLAN") idLan = iv;
			if (field == "URL_BASE") urlBase = iv;
			if (field == "DATA") jsonPayload = iv;
		}
	}

	try {
		if (codColigada == "") throw "Constraint CODCOLIGADA obrigatoria";
		if (codSistema == "") throw "Constraint CODSISTEMA obrigatoria";
		if (idLan == "") throw "Constraint IDLAN obrigatoria";
		if (jsonPayload == "") throw "Constraint DATA obrigatoria";

		var integration = getPasswordUserIntegration();
		if (urlBase == "") urlBase = integration.URL_RM;
		if (urlBase == "") throw "URL_RM não configurada no ds_dw_getUserPass.";
		urlBase = String(urlBase).replace(/\/+$/, "");

		var endpoint = urlBase + "/rmsrestdataserver/rest/FinLanDataBR/" + codColigada + "$_$" + idLan;

		var token = getToken();

		client = HttpClients.createDefault();

		var patchRequest = new HttpPatch(endpoint);

		patchRequest.addHeader("Accept", "application/json");
		patchRequest.addHeader("Content-Type", "application/json; charset=UTF-8");
		patchRequest.addHeader("Authorization", "Bearer " + String(token).trim());
		patchRequest.addHeader("codcoligada", String(codColigada).trim());
		patchRequest.addHeader("codsistema", String(codSistema).trim());

		var entity = new StringEntity(new java.lang.String(jsonPayload), "UTF-8");
		patchRequest.setEntity(entity);

		response = client.execute(patchRequest);

		var codRetorno = response.getStatusLine().getStatusCode();

		var responseEntity = response.getEntity();
		var outputString = "";

		if (responseEntity != null) {
			isr = new InputStreamReader(responseEntity.getContent(), "UTF-8");
			br = new BufferedReader(isr);
			var responseString = "";
			while ((responseString = br.readLine()) != null) {
				outputString += responseString;
			}
		}

		var jsonResult;
		try {
			jsonResult = JSON.parse(outputString);
		} catch (eParse) {
			throw "Retorno invalido (" + codRetorno + "): " + outputString;
		}

		if (codRetorno < 200 || codRetorno >= 300) {
			if (jsonResult && jsonResult.message) {
				throw "Erro RM: " + jsonResult.message;
			}
			throw "Erro HTTP " + codRetorno + ": " + outputString;
		}

		if (!jsonResult || !jsonResult.data) {
			throw "Nenhum registro encontrado na resposta.";
		}

		var record = jsonResult.data;
		var retCodColigada = record.CODCOLIGADA != null ? String(record.CODCOLIGADA) : "";
		var retIdLan = record.IDLAN != null ? String(record.IDLAN) : "";
		var retStatusLan = record.STATUSLAN != null ? String(record.STATUSLAN) : "";
		var idPendencia = "";
		var codStatusPendencia = "";
		var statusPendencia = "";
		var codEventoRetorno = "";
		var dataRetornoEvento = "";
		var eventoRetornoFinaliz = "";
		var dataFinalizacaoEven = "";
		var idAcordo = "";
		var pendenciaFinalizada = "";

		if (record.FLANCOMPL && record.FLANCOMPL.length > 0) {
			idPendencia = record.FLANCOMPL[0].IDPENDENCIA != null ? String(record.FLANCOMPL[0].IDPENDENCIA) : "";
			codStatusPendencia = record.FLANCOMPL[0].CODSTATUSPENDENCIA != null ? String(record.FLANCOMPL[0].CODSTATUSPENDENCIA) : "";
			statusPendencia = record.FLANCOMPL[0].STATUSPENDENCIA != null ? String(record.FLANCOMPL[0].STATUSPENDENCIA) : "";
			codEventoRetorno = record.FLANCOMPL[0].CODEVENTORETORNO != null ? String(record.FLANCOMPL[0].CODEVENTORETORNO) : "";
			dataRetornoEvento = record.FLANCOMPL[0].DATARETORNOEVENTO != null ? String(record.FLANCOMPL[0].DATARETORNOEVENTO) : "";
			eventoRetornoFinaliz = record.FLANCOMPL[0].EVENTORETORNOFINALIZ != null ? String(record.FLANCOMPL[0].EVENTORETORNOFINALIZ) : "";
			dataFinalizacaoEven = record.FLANCOMPL[0].DATAFINALIZACAOEVEN != null ? String(record.FLANCOMPL[0].DATAFINALIZACAOEVEN) : "";
			idAcordo = record.FLANCOMPL[0].IDACORDO != null ? String(record.FLANCOMPL[0].IDACORDO) : "";
			pendenciaFinalizada = record.FLANCOMPL[0].PENDENCIAFINALIZADA != null ? String(record.FLANCOMPL[0].PENDENCIAFINALIZADA) : "";
		}

		dataset.addColumn("CODCOLIGADA");
		dataset.addColumn("IDLAN");
		dataset.addColumn("STATUSLAN");
		dataset.addColumn("IDPENDENCIA");
		dataset.addColumn("CODSTATUSPENDENCIA");
		dataset.addColumn("STATUSPENDENCIA");
		dataset.addColumn("CODEVENTORETORNO");
		dataset.addColumn("DATARETORNOEVENTO");
		dataset.addColumn("EVENTORETORNOFINALIZ");
		dataset.addColumn("DATAFINALIZACAOEVEN");
		dataset.addColumn("IDACORDO");
		dataset.addColumn("PENDENCIAFINALIZADA");

		dataset.addRow([retCodColigada, retIdLan, retStatusLan, idPendencia, codStatusPendencia, statusPendencia, codEventoRetorno, dataRetornoEvento, eventoRetornoFinaliz, dataFinalizacaoEven, idAcordo, pendenciaFinalizada]);

	} catch (e) {
		var errorMsg = (e && e.message) ? e.message : String(e);

		dataset = DatasetBuilder.newDataset();
		dataset.addColumn("ERROR");
		dataset.addColumn("MESSAGE");
		dataset.addRow(["true", errorMsg]);
	} finally {
		try { if (br != null) br.close(); } catch (eBr) { }
		try { if (isr != null) isr.close(); } catch (eIsr) { }
		try { if (response != null) response.close(); } catch (eResp) { }
		try { if (client != null) client.close(); } catch (eClient) { }
	}

	return dataset;
}

function getToken() {
	var key = "fd270aa97904091fe40407003721d4101ad8b0eb57f97d60d389396f39ac7d6e";
	var cs = [];
	cs.push(DatasetFactory.createConstraint("KEY", key, key, ConstraintType.MUST));
	var ds = DatasetFactory.getDataset("ds_dw_api_rm_createToken", null, cs, null);

	if (ds && ds.rowsCount > 0) {
		var erro = ds.getValue(0, "ERROR");
		if (erro == "true") {
			throw ds.getValue(0, "MESSAGE");
		}
		return ds.getValue(0, "TOKEN");
	}
	throw "Falha ao obter token.";
}

function getPasswordUserIntegration() {
	var key = "fd270aa97904091fe40407003721d4101ad8b0eb57f97d60d389396f39ac7d6e";
	var cs = [];
	cs.push(DatasetFactory.createConstraint("KEY", key, key, ConstraintType.MUST));
	var ds = DatasetFactory.getDataset("ds_dw_getUserPass", null, cs, null);

	if (ds && ds.rowsCount > 0) {
		var erro = ds.getValue(0, "ERROR");
		if (erro == "true") {
			throw ds.getValue(0, "MESSAGE");
		}

		var urlRm = ds.getValue(0, "URL_RM");
		if (!urlRm) {
			throw "URL_RM não configurada no ds_dw_getUserPass.";
		}

		return {
			URL_RM: urlRm
		};
	}

	throw "ds_dw_getUserPass - Não foi encontrado nenhum registro no dataset de integração para o RM.";
}
