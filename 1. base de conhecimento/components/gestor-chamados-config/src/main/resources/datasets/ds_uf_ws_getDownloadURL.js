
function createDataset(fields, constraints, sortFields) {
	var dataset = DatasetBuilder.newDataset();
	try {
		dataset.addColumn("data");

		var paramsGlobais = getParametrosGlobais();

		if (!paramsGlobais) throw 'Erro para consultar o formulário Parâmetros Globais';

		var data = {};
		if (constraints && constraints.length && constraints.length > 0) {
			for (attr in constraints) {
				var field = constraints[attr]['fieldName'].toUpperCase();
				var iv = constraints[attr]['initialValue'];
				var fv = constraints[attr]['finalValue'];
				if (field == "DOCUMENTID") {
					if (!isNaN(iv)) {
						var document = fluigAPI.getDocumentService().getActive(parseInt(iv));

						if (!paramsGlobais[document.parentDocumentId]) continue;

						var url = fluigAPI.getDocumentService().getDownloadURL(parseInt(iv));
						data[String(iv)] = String(url);
					}
				}
			}
		}
		dataset.addRow([JSON.stringify(data)]);
	} catch (e) {
		log.error('<<Userflow - Gestor de Processos>> Erro ao consultar o dataset ds_uf_ws_getDownloadURL');
		log.dir(e);

		dataset = DatasetBuilder.newDataset();
		dataset.addColumn("ERROR");
		dataset.addRow(new Array(e.toString()));
	} finally {
		return dataset;
	}
}
function getParametrosGlobais() {
	var cols = ["depFolder", "celDepFolder", "grupoAtdFolder", "subgrupoAtdFolder"];
	var cs = [];
	cs.push(DatasetFactory.createConstraint("companyId", getValue("WKCompany"), getValue("WKCompany"), ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("metadata#active", true, true, ConstraintType.MUST));
	var ds = DatasetFactory.getDataset("ds_uf_form_parametros_globais", cols, cs, null);

	if (!ds || ds.rowsCount == 0) return false;

	var dsMap = ds.getMap().toArray();
	var resultado = {};
	for (i in dsMap) {
		resultado[dsMap[i].get("depFolder")] = true;
		resultado[dsMap[i].get("celDepFolder")] = true;
		resultado[dsMap[i].get("grupoAtdFolder")] = true;
		resultado[dsMap[i].get("subgrupoAtdFolder")] = true;
	}
	return resultado;
}
