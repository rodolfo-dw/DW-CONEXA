
function createDataset(fields, constraints, sortFields) {
	var dataset = DatasetBuilder.newDataset();
	var token = null;
	var user = null;
	var type = null;
	var codDepartamento = null;
	var codCelulaDepartamento = null;
	var codGrupoAtendimento = null;
	var codSubgrupoAtendimento = null;

	if (constraints && constraints.length && constraints.length > 0) {
		for (attr in constraints) {
			var field = constraints[attr]['fieldName'].toUpperCase();
			var iv = constraints[attr]['initialValue'];
			var fv = constraints[attr]['finalValue'];
			if (field == "TOKEN") {
				token = iv;
			}
			if (field == "USER") {
				user = iv;
			}
			if (field == "TYPE") {
				type = iv;
			}
			if (field == "CODDEPARTAMENTO") {
				codDepartamento = iv;
			}
			if (field == "CODCELULADEPARTAMENTO") {
				codCelulaDepartamento = iv;
			}
			if (field == "CODGRUPOATENDIMENTO") {
				codGrupoAtendimento = iv;
			}
			if (field == "CODSUBGRUPOATENDIMENTO") {
				codSubgrupoAtendimento = iv;
			}
		}
	}
	try {
		if (token == null) throw "O envio da constraint token é obrigatório";
		if (user == null) throw "O envio da constraint user é obrigatório";
		if (type == null) throw "O envio da constraint type é obrigatório";

		try {
			validaUserToken(token, user);
		} catch (e) {
			dataset = DatasetBuilder.newDataset();
			dataset.addColumn("ERROR");
			dataset.addColumn("STATUS_CODE");
			dataset.addRow(new Array(e.toString(), "401"));
			return dataset;
		}

		var ds = false;
		var permissionsData = getDatasetUsuarioExterno(user);

		if (type == "departamento") {
			ds = getDepartamento(permissionsData);
		} else if (type == "celulaDepartamento") {
			ds = getCelulaDepartamento(permissionsData, codDepartamento);
		} else if (type == "grupoAtendimento") {
			ds = getGrupoAtendimento(permissionsData, codDepartamento, codCelulaDepartamento);
		} else if (type == "subgrupoAtendimento") {
			ds = getSubgrupoAtendimento(permissionsData, codDepartamento, codCelulaDepartamento, codGrupoAtendimento);
		} else if (type == "atividade") {
			ds = getAtividade(permissionsData, codDepartamento, codCelulaDepartamento, codGrupoAtendimento, codSubgrupoAtendimento);
		}

		if (!ds || ds.rowsCount == 0) {
			log.dir("<<Userflow - Gestor de Processos>> Ao consultar o dataset ds_uf_gestor_chamados_central_tarefas, não retornou nenhum valor, dataset: " + type + ". Verificar o formulário '00.3. Cadastro de Usuário Externo' e validar se o cadastro está corretamente preenchido.");
			log.dir("<<Userflow - Gestor de Processos>> Importante verifiar se a amarração dos cadastros estão corretas.");

			dataset = DatasetBuilder.newDataset();
			dataset.addColumn("ERROR");
			dataset.addColumn("STATUS_CODE");
			dataset.addRow(new Array(
				'Não foi localizado nenhum registro associado ao formulário "' + type + '", verifique também se existe alguma restrição cadastro associado ao seu usuário no formulário "00.3. Cadastro de Usuário Externo" e validar se o cadastro está corretamente preenchido.',
				"200"
			));
			return dataset;
		}

		var cols = ds.getColumnsName();

		for (var c = 0; c < cols.length; c++) {
			dataset.addColumn(String(cols[c]));
		}

		for (var i = 0; i < ds.rowsCount; i++) {
			var row = [];
			for (var c2 = 0; c2 < cols.length; c2++) {
				var colName = String(cols[c2]);
				row.push(ds.getValue(i, colName));
			}
			dataset.addRow(row);
		}


	} catch (e) {
		log.error('<<Userflow - Gestor de Processos>> Erro ao consultar o dataset ds_uf_gestor_chamados_central_tarefas');
		log.dir(e);

		dataset = DatasetBuilder.newDataset();
		dataset.addColumn("ERROR");
		dataset.addRow(new Array(e.toString()));
	} finally {
		return dataset;
	}
}
function validaUserToken(token, user) {
	var cs = [];
	cs.push(DatasetFactory.createConstraint("companyId", getValue("WKCompany"), getValue("WKCompany"), ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("metadata#active", true, true, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("token", token, token, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("login", user, user, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("ativo", "1", "1", ConstraintType.MUST));
	var ds = DatasetFactory.getDataset("ds_uf_form_gerenciamento_token_acesso", null, cs, null);

	if (!ds || ds.rowsCount == 0) throw 'Você não possui permissão para realizar esta ação. O token informado não corresponde ao usuário enviado.';
}

function getDepartamento(permissionsData) {
	var fields = ['CODDEPARTAMENTO_DEP', 'DESCDEPARTAMENTO_DEP', 'IMGDOCUMENTID_DEP', 'IMGDOCUMENTVERSION_DEP', 'ORDEMEXIBICAO_DEP'];

	var cs = [];

	if (permissionsData) {
		for (var i = 0; i < permissionsData.rowsCount; i++) {
			var codDep = permissionsData.getValue(i, "codDepartamento");

			if (!codDep || codDep == "") continue;
			cs.push(DatasetFactory.createConstraint("CODDEPARTAMENTO_DEP", Number(codDep), Number(codDep), ConstraintType.SHOULD));
		}
	}
	cs.push(DatasetFactory.createConstraint("DUPLICATED_DEP", 0, 0, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("ATIVO_DEP", 1, 1, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("ATIVOGERAL_AMA", 1, 1, ConstraintType.MUST));
	var ds = DatasetFactory.getDataset("ds_uf_gestor_chamados_atividades", fields, cs, ['ORDEMEXIBICAO_DEP']);

	return ds;
}
function getCelulaDepartamento(permissionsData, codDepartamento) {
	var fields = ['CODCELULADEPARTAMENTO_CEL', 'DESCCELULADEPARTAMENTO_CEL', 'IMGDOCUMENTID_CEL', 'IMGDOCUMENTVERSION_CEL', 'OBSCELULADEPARTAMENTO_CEL'];

	var cs = [];

	if (permissionsData) {
		for (var i = 0; i < permissionsData.rowsCount; i++) {
			var codCelDep = permissionsData.getValue(i, "codCelulaDepartamento");

			if (!codCelDep || codCelDep == "") continue;
			cs.push(DatasetFactory.createConstraint("CODCELULADEPARTAMENTO_CEL", Number(codCelDep), Number(codCelDep), ConstraintType.SHOULD));
		}
	}
	cs.push(DatasetFactory.createConstraint("DUPLICATED_CEL", 0, 0, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("ATIVO_CEL", 1, 1, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("CODDEPARTAMENTO_DEP", Number(codDepartamento), Number(codDepartamento), ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("ATIVOGERAL_AMA", 1, 1, ConstraintType.MUST));

	var ds = DatasetFactory.getDataset("ds_uf_gestor_chamados_atividades", fields, cs, null);

	return ds;
}
function getGrupoAtendimento(permissionsData, codDepartamento, codCelulaDepartamento) {
	var fields = ['CODGRUPOATENDIMENTO_GRU', 'DESCGRUPOATENDIMENTO_GRU', 'IMGDOCUMENTID_GRU', 'IMGDOCUMENTVERSION_GRU', 'OBSGRUPOATENDIMENTO_GRU'];

	var cs = [];

	if (permissionsData) {
		for (var i = 0; i < permissionsData.rowsCount; i++) {
			var codGruAtd = permissionsData.getValue(i, "codGrupoAtendimento");

			if (!codGruAtd || codGruAtd == "") continue;
			cs.push(DatasetFactory.createConstraint("CODGRUPOATENDIMENTO_GRU", Number(codGruAtd), Number(codGruAtd), ConstraintType.SHOULD));
		}
	}
	cs.push(DatasetFactory.createConstraint("DUPLICATED_GRU", 0, 0, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("ATIVO_GRU", 1, 1, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("CODDEPARTAMENTO_DEP", Number(codDepartamento), Number(codDepartamento), ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("CODCELULADEPARTAMENTO_CEL", Number(codCelulaDepartamento), Number(codCelulaDepartamento), ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("ATIVOGERAL_AMA", 1, 1, ConstraintType.MUST));

	var ds = DatasetFactory.getDataset("ds_uf_gestor_chamados_atividades", fields, cs, null);

	return ds;
}
function getSubgrupoAtendimento(permissionsData, codDepartamento, codCelulaDepartamento, codGrupoAtendimento) {
	var fields = ['CODSUBGRUPOATENDIMENTO_SGR', 'DESCSUBGRUPOATENDIMENTO_SGR', 'IMGDOCUMENTID_SGR', 'IMGDOCUMENTVERSION_SGR', 'OBSSUBGRUPOATENDIMENTO_SGR'];

	var cs = [];

	if (permissionsData) {
		for (var i = 0; i < permissionsData.rowsCount; i++) {
			var codSubgruAtd = permissionsData.getValue(i, "codSubgrupoAtendimento");

			if (!codSubgruAtd || codSubgruAtd == "") continue;
			cs.push(DatasetFactory.createConstraint("CODSUBGRUPOATENDIMENTO_SGR", Number(codSubgruAtd), Number(codSubgruAtd), ConstraintType.SHOULD));
		}
	}
	cs.push(DatasetFactory.createConstraint("DUPLICATED_SGR", 0, 0, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("ATIVO_SGR", 1, 1, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("CODDEPARTAMENTO_DEP", Number(codDepartamento), Number(codDepartamento), ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("CODCELULADEPARTAMENTO_CEL", Number(codCelulaDepartamento), Number(codCelulaDepartamento), ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("CODGRUPOATENDIMENTO_GRU", Number(codGrupoAtendimento), Number(codGrupoAtendimento), ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("ATIVOGERAL_AMA", 1, 1, ConstraintType.MUST));

	var ds = DatasetFactory.getDataset("ds_uf_gestor_chamados_atividades", fields, cs, null);

	return ds;
}
function getAtividade(permissionsData, codDepartamento, codCelulaDepartamento, codGrupoAtendimento, codSubgrupoAtendimento) {
	var fields = ['CODATIVIDADE_ATI', 'DESCATIVIDADE_ATI', 'CODPROCESS_AMA'];

	var cs = [];

	if (permissionsData) {
		for (var i = 0; i < permissionsData.rowsCount; i++) {
			var codAtv = permissionsData.getValue(i, "codAtividade");

			if (!codAtv || codAtv == "") continue;
			cs.push(DatasetFactory.createConstraint("CODATIVIDADE_ATI", Number(codAtv), Number(codAtv), ConstraintType.SHOULD));
		}
	}
	cs.push(DatasetFactory.createConstraint("ATIVO_ATI", 1, 1, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("CODDEPARTAMENTO_DEP", Number(codDepartamento), Number(codDepartamento), ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("CODCELULADEPARTAMENTO_CEL", Number(codCelulaDepartamento), Number(codCelulaDepartamento), ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("CODGRUPOATENDIMENTO_GRU", Number(codGrupoAtendimento), Number(codGrupoAtendimento), ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("CODSUBGRUPOATENDIMENTO_SGR", Number(codSubgrupoAtendimento), Number(codSubgrupoAtendimento), ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("ATIVOGERAL_AMA", 1, 1, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("ATIVO_AMA", 1, 1, ConstraintType.MUST));

	var ds = DatasetFactory.getDataset("ds_uf_gestor_chamados_atividades", fields, cs, null);

	return ds;
}
function getDatasetUsuarioExterno(userId) {
	var cols = ['login'];
	var cs = [];
	cs.push(DatasetFactory.createConstraint("companyId", getValue("WKCompany"), getValue("WKCompany"), ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("metadata#active", true, true, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("login", userId, userId, ConstraintType.MUST));
	var ds = DatasetFactory.getDataset("ds_uf_form_cadastro_usuario_externo", cols, cs, null);

	if (!ds || ds.rowsCount == 0) throw 'Usuário externo não está cadastrado';

	var cs = [];
	cs.push(DatasetFactory.createConstraint("companyId", getValue("WKCompany"), getValue("WKCompany"), ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("metadata#active", true, true, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("tableName", "tblAcessos", "tblAcessos", ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("metadata#id", ds.getValue(0, 'metadata#id'), ds.getValue(0, 'metadata#id'), ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("metadata#version", ds.getValue(0, 'metadata#version'), ds.getValue(0, 'metadata#version'), ConstraintType.MUST));
	var ds = DatasetFactory.getDataset("ds_uf_form_cadastro_usuario_externo", null, cs, null);

	if (!ds || ds.rowsCount == 0) return false;

	return ds;
}
