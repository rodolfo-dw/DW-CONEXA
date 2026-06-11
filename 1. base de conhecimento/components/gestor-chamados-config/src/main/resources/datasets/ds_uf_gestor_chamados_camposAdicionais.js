function createDataset(fields, constraints, sortFields) {

	var dataset = DatasetBuilder.newDataset();

	try {
		var cols = getCols();
		for (var i = 0; i < cols.length; i++) {
			dataset.addColumn(cols[i]);
		}
		var codes = null;
		var processNumber = null;
		var type = null;
		var token = null;
		var user = null;

		for (var attr in constraints) {
			var field = constraints[attr]['fieldName'].toUpperCase();
			var value = constraints[attr]['initialValue'];
			if (field == "CODES") {
				codes = value;
				continue;
			}
			if (field == "TOKEN") {
				token = value;
				continue;
			}
			if (field == "USER") {
				user = value;
				continue;
			}
			if (field == "PROCESS_NUMBER") {
				processNumber = value;
				continue;
			}
			if (field == "TYPE") {
				type = value;
				continue;
			}
		}

		if (processNumber != null) {
			if (token == null || user == null) throw "Constraints 'TOKEN' e 'USER' são obrigatórias.";
			
			validaUserToken(token, user);
			validaSolicitacao(processNumber, user);

			if (type == "pxf") {
				dataset = getValuesFromProcessStartedPXF(processNumber);
			} else {
				dataset = getValuesFromProcessStarted(processNumber);
			}

			return dataset;
		}

		if (!codes) throw 'É obrigatório o envio da constraint codes. Exemplo de envio: 1,1,1,1 (Departamento,Célula do Departamento, Grupo de Atendimento, Atividade)';

		var recordsCabecalho = getRecordsCabecalho();
		if (!recordsCabecalho) throw 'Não foi encontrado nenhum registro do formulário de campos complementares';

		codes = codes.split(';');

		var added = new java.util.ArrayList();

		for (i in codes) {
			var code = codes[i].split(',');

			var codDepartamento = code[0] == "0" ? "" : code[0];
			var codCelulaDepartamento = code[1] == "0" ? "" : code[1];
			var codGrupoAtendimento = code[2] == "0" ? "" : code[2];
			var codSubgrupoAtendimento = code[3] == "0" ? "" : code[3];
			var codAtividade = code[4] == "0" ? "" : code[4];

			var recordCabecalho = recordsCabecalho.toArray().filter(function(o, i) {
				if (
					o.get('codDepartamento') == codDepartamento &&
					o.get('codCelulaDepartamento') == codCelulaDepartamento &&
					o.get('codGrupoAtendimento') == codGrupoAtendimento &&
					o.get('codSubgrupoAtendimento') == codSubgrupoAtendimento &&
					o.get('codAtividade') == codAtividade
				) {
					return true;
				}
			});
			recordCabecalho = !recordCabecalho.length ? false : recordCabecalho[0];


			if (recordCabecalho) {
				var documentId = recordCabecalho.get('documentid');
				var version = recordCabecalho.get('version');

				var records = getCamposFilho(documentId, version).toArray();

				for (i in records) {
					var record = records[i];
					if (added.contains(record.get('codCampoAdicional'))) continue;

					var arrTmp = [];
					for (var y = 0; y < cols.length; y++) {
						arrTmp.push(record.get(cols[y]));
					}
					added.add(record.get('codCampoAdicional'));

					dataset.addRow(arrTmp);
				}
			}

			var recordCabecalho = recordsCabecalho.toArray().filter(function(o, i) {
				if (
					o.get('codDepartamento') == codDepartamento &&
					o.get('codCelulaDepartamento') == codCelulaDepartamento &&
					o.get('codGrupoAtendimento') == codGrupoAtendimento &&
					o.get('codSubgrupoAtendimento') == codSubgrupoAtendimento &&
					o.get('codAtividade') == ''
				) {
					return true;
				}
			});
			recordCabecalho = !recordCabecalho.length ? false : recordCabecalho[0];

			if (recordCabecalho) {
				var documentId = recordCabecalho.get('documentid');
				var version = recordCabecalho.get('version');

				var records = getCamposFilho(documentId, version).toArray();

				for (i in records) {
					var record = records[i];
					if (added.contains(record.get('codCampoAdicional'))) continue;

					var arrTmp = [];
					for (var y = 0; y < cols.length; y++) {
						arrTmp.push(record.get(cols[y]));
					}
					added.add(record.get('codCampoAdicional'));

					dataset.addRow(arrTmp);
				}
			}

			var recordCabecalho = recordsCabecalho.toArray().filter(function(o, i) {
				if (
					o.get('codDepartamento') == codDepartamento &&
					o.get('codCelulaDepartamento') == codCelulaDepartamento &&
					o.get('codGrupoAtendimento') == codGrupoAtendimento &&
					o.get('codSubgrupoAtendimento') == '' &&
					o.get('codAtividade') == ''
				) {
					return true;
				}
			});
			recordCabecalho = !recordCabecalho.length ? false : recordCabecalho[0];

			if (recordCabecalho) {
				var documentId = recordCabecalho.get('documentid');
				var version = recordCabecalho.get('version');

				var records = getCamposFilho(documentId, version).toArray();

				for (i in records) {
					var record = records[i];
					if (added.contains(record.get('codCampoAdicional'))) continue;

					var arrTmp = [];
					for (var y = 0; y < cols.length; y++) {
						arrTmp.push(record.get(cols[y]));
					}
					added.add(record.get('codCampoAdicional'));

					dataset.addRow(arrTmp);
				}
			}

			var recordCabecalho = recordsCabecalho.toArray().filter(function(o, i) {
				if (
					o.get('codDepartamento') == codDepartamento &&
					o.get('codCelulaDepartamento') == codCelulaDepartamento &&
					o.get('codGrupoAtendimento') == '' &&
					o.get('codSubgrupoAtendimento') == '' &&
					o.get('codAtividade') == ''
				) {
					return true;
				}
			});
			recordCabecalho = !recordCabecalho.length ? false : recordCabecalho[0];

			if (recordCabecalho) {
				var documentId = recordCabecalho.get('documentid');
				var version = recordCabecalho.get('version');

				var records = getCamposFilho(documentId, version).toArray();

				for (i in records) {
					var record = records[i];
					if (added.contains(record.get('codCampoAdicional'))) continue;

					var arrTmp = [];
					for (var y = 0; y < cols.length; y++) {
						arrTmp.push(record.get(cols[y]));
					}
					added.add(record.get('codCampoAdicional'));

					dataset.addRow(arrTmp);
				}
			}

			var recordCabecalho = recordsCabecalho.toArray().filter(function(o, i) {
				if (
					o.get('codDepartamento') == codDepartamento &&
					o.get('codCelulaDepartamento') == '' &&
					o.get('codGrupoAtendimento') == '' &&
					o.get('codSubgrupoAtendimento') == '' &&
					o.get('codAtividade') == ''
				) {
					return true;
				}
			});
			recordCabecalho = !recordCabecalho.length ? false : recordCabecalho[0];

			if (recordCabecalho) {
				var documentId = recordCabecalho.get('documentid');
				var version = recordCabecalho.get('version');

				var records = getCamposFilho(documentId, version).toArray();

				for (i in records) {
					var record = records[i];
					if (added.contains(record.get('codCampoAdicional'))) continue;

					var arrTmp = [];
					for (var y = 0; y < cols.length; y++) {
						arrTmp.push(record.get(cols[y]));
					}
					added.add(record.get('codCampoAdicional'));

					dataset.addRow(arrTmp);
				}
			}
		}
	} catch (e) {
		log.error('Erro ao consultar o dataset ds_uf_gestor_chamados_camposAdicionais');
		log.dir(e);

		dataset = DatasetBuilder.newDataset();
		dataset.addColumn("ERROR");
		dataset.addRow(['Erro ao executar o dataset ds_uf_gestor_chamados_camposAdicionais: ' + e]);
	} finally {
		return dataset;
	}
}

function getRecordsCabecalho() {
	try {
		var cols = ['codDepartamento', 'codCelulaDepartamento', 'codGrupoAtendimento', 'codSubgrupoAtendimento', 'codAtividade'];
		var cs = [];
		cs.push(DatasetFactory.createConstraint("metadata#active", true, true, ConstraintType.MUST));
		cs.push(DatasetFactory.createConstraint("ativoGeral", "1", "1", ConstraintType.MUST));
		cs.push(DatasetFactory.createConstraint("companyId", getValue("WKCompany"), getValue("WKCompany"), ConstraintType.MUST));
		var ds = DatasetFactory.getDataset("ds_uf_form_amarracao_campos_complementares", cols, cs, null);

		if (!ds || ds.rowsCount == 0) return false;

		return ds.getMap();
	} catch (e) {
		return false;
	}
}

function getCamposFilho(documentId, version) {
	try {
		var cs = [];

		cs.push(DatasetFactory.createConstraint("tableName", "tblCamposAdicionais", "tblCamposAdicionais", ConstraintType.MUST));
		cs.push(DatasetFactory.createConstraint("documentid", documentId, documentId, ConstraintType.MUST));
		cs.push(DatasetFactory.createConstraint("companyId", getValue("WKCompany"), getValue("WKCompany"), ConstraintType.MUST));
		cs.push(DatasetFactory.createConstraint("ativo", "1", "1", ConstraintType.MUST));
		cs.push(DatasetFactory.createConstraint("metadata#active", true, true, ConstraintType.MUST));
		cs.push(DatasetFactory.createConstraint("metadata#version", Number(version), Number(version), ConstraintType.MUST));

		var cols = getCols();

		var ds = DatasetFactory.getDataset("ds_uf_form_amarracao_campos_complementares", cols, cs, null);

		if (!ds || ds.rowsCount == 0) return new java.util.ArrayList();

		return ds.getMap();
	} catch (e) {
		throw 'Houve um erro na função getCamposFilho(): ' + e;
	}
}

function getCols() {
	return [
		'tipoCampoAdicional',
		'codCampoAdicional',
		'descCampoAdicional',
		'obrigatorioCampoAdicional',
		'scriptCampoAdicional',
		'tamanhoCampoAdicional',
		'mascaraCampoAdicional',
		'limiteCaracteresCampoAdicional',
		'valorOpcaoCampoAdicional',
		'descOpcaoCampoAdicional',
		'valorPadraoCampoAdicional',
		'atributosCampoAdicional',
		'classCampoAdicional',
		'tabelaPxfCampoAdicional',
	];
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
function validaUserToken(token, user) {
	var cs = [];
	cs.push(DatasetFactory.createConstraint("companyId", getValue("WKCompany"), getValue("WKCompany"), ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("metadata#active", true, true, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("token", token, token, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("login", user, user, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("ativo", "1", "1", ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("sqlLimit", 1, 1, ConstraintType.MUST));

	var ds = DatasetFactory.getDataset("ds_uf_form_gerenciamento_token_acesso", null, cs, null);
	if (!ds || ds.rowsCount == 0) throw 'Token inválido ou não corresponde ao usuário.';
}
function getValuesFromProcessStarted(processNumber) {
	var dataset = DatasetBuilder.newDataset();
	var cols = getCols();

	for (var i = 0; i < cols.length; i++) {
		dataset.addColumn(cols[i]);
	}

	cols.push("valorSelecionadoCampoAdicional", "descSelecionadoCampoAdicional");
	dataset.addColumn('valorSelecionadoCampoAdicional');
	dataset.addColumn('descSelecionadoCampoAdicional');
	dataset.addColumn('row');

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
		cs.push(DatasetFactory.createConstraint("tableName", "tblCamposAdicionais", "tblCamposAdicionais", ConstraintType.MUST));
		cs.push(DatasetFactory.createConstraint("metadata#active", true, true, ConstraintType.MUST));
		cs.push(DatasetFactory.createConstraint("documentid", documentId, documentId, ConstraintType.MUST));
		cs.push(DatasetFactory.createConstraint("metadata#version", version, version, ConstraintType.MUST));

		var ds = DatasetFactory.getDataset("ds_uf_form_gestor_chamados", null, cs, null);

		if (!ds || ds.rowsCount == 0) return dataset;

		var row = 1;
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
			rowArr.push(String(row));
			row = row + 1;
			dataset.addRow(rowArr);
		}

	} catch (e) {
		log.error("Erro em getValuesFromProcessStarted: " + e);
	}
	return dataset;
}
function getValuesFromProcessStartedPXF(processNumber) {
	var dataset = DatasetBuilder.newDataset();

	dataset.addColumn('jsonTabelasCamposAdc');

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
		cs.push(DatasetFactory.createConstraint("tableName", "tblCamposAdicionaisPXF", "tblCamposAdicionaisPXF", ConstraintType.MUST));
		cs.push(DatasetFactory.createConstraint("metadata#active", true, true, ConstraintType.MUST));
		cs.push(DatasetFactory.createConstraint("documentid", documentId, documentId, ConstraintType.MUST));
		cs.push(DatasetFactory.createConstraint("metadata#version", version, version, ConstraintType.MUST));

		var ds = DatasetFactory.getDataset("ds_uf_form_gestor_chamados", null, cs, null);

		if (!ds || ds.rowsCount == 0) return dataset;

		for (var i = 0; i < ds.rowsCount; i++) {
			var rowArr = [];
			try {
				var val = ds.getValue(i, "jsonTabelasCamposAdc");
				rowArr.push(val);
			} catch (err) {
				rowArr.push("");
			}
			dataset.addRow(rowArr);
		}

	} catch (e) {
		log.error("Erro em getValuesFromProcessStartedPXF: " + e);
	}
	return dataset;
}