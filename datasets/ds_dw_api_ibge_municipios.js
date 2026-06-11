importPackage(java.lang);
importPackage(java.io);
importPackage(org.apache.http);
importPackage(org.apache.http.client.methods);
importPackage(org.apache.http.impl.client);

/**
 * ds_dw_api_ibge_municipios
 *
 * Espelha a tabela de municípios do IBGE para o de-para código IBGE -> nome
 * (DANFSe). O JSON completo da API oficial (~2,4 MB) é quebrado em linhas de
 * 2.000 caracteres - o menor limite de coluna TEXT entre os bancos do Fluig é
 * o Oracle (4.000 BYTES, e acentos em UTF-8 ocupam 2 bytes; 2.000 chars cabem
 * em qualquer um). Quem consome lê todas as linhas, concatena na ordem da
 * coluna CODIGO (0001, 0002...) até a quantidade da linha TOTAL e faz o parse.
 *
 * Sincronização jornalizada: agendar a tarefa "Sincronização de dataset" no
 * Painel de Controle (sugestão: 1x por dia).
 */

var URL_IBGE = "https://servicodados.ibge.gov.br/api/v1/localidades/municipios";
var TAMANHO_LINHA = 2000;

function defineStructure() {
	addColumn("CODIGO");
	addColumn("JSON", DatasetFieldType.TEXT);

	setKey(["CODIGO"]);
}

function onSync(lastSyncDate) {
	var dataset = DatasetBuilder.newDataset();

	try {
		var partes = getMunicipiosPartes();
		for (var i = 0; i < partes.length; i++) {
			dataset.addOrUpdateRow([String("0000" + (i + 1)).slice(-4), partes[i]]);
		}
		dataset.addOrUpdateRow(["TOTAL", String(partes.length)]);
	} catch (e) {
		var errorMsg = (e && e.message) ? e.message : String(e);
		log.error("** DATASET ds_dw_api_ibge_municipios ** - Erro no onSync (tabela mantém a última sincronização): " + errorMsg);
	}

	return dataset;
}

function createDataset(fields, constraints, sortFields) {
	var dataset = DatasetBuilder.newDataset();

	try {
		dataset.addColumn("CODIGO");
		dataset.addColumn("JSON");

		var partes = getMunicipiosPartes();
		for (var i = 0; i < partes.length; i++) {
			dataset.addRow([String("0000" + (i + 1)).slice(-4), partes[i]]);
		}
		dataset.addRow(["TOTAL", String(partes.length)]);
	} catch (e) {
		var errorMsg = (e && e.message) ? e.message : String(e);

		dataset = DatasetBuilder.newDataset();
		dataset.addColumn("ERROR");
		dataset.addColumn("MESSAGE");
		dataset.addRow(["true", errorMsg]);
	}

	return dataset;
}

function getMunicipiosPartes() {
	var client = null;
	var response = null;
	var isr = null;
	var br = null;

	try {
		client = HttpClients.createDefault();

		var getRequest = new HttpGet(URL_IBGE);
		getRequest.addHeader("Accept", "application/json");

		response = client.execute(getRequest);

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

		if (codRetorno < 200 || codRetorno >= 300) {
			throw "Erro HTTP " + codRetorno + " na API do IBGE: " + outputString.substring(0, 200);
		}

		var jsonResult;
		try {
			jsonResult = JSON.parse(outputString);
		} catch (eParse) {
			throw "Retorno invalido da API do IBGE (" + codRetorno + "): " + outputString.substring(0, 200);
		}

		if (!jsonResult || jsonResult.length == 0) {
			throw "Nenhum município retornado pela API do IBGE.";
		}

		var partes = [];
		for (var i = 0; i < outputString.length; i += TAMANHO_LINHA) {
			partes.push(outputString.substring(i, i + TAMANHO_LINHA));
		}
		return partes;

	} finally {
		try { if (br != null) br.close(); } catch (eBr) { }
		try { if (isr != null) isr.close(); } catch (eIsr) { }
		try { if (response != null) response.close(); } catch (eResp) { }
		try { if (client != null) client.close(); } catch (eClient) { }
	}
}
