function defineStructure() {

	addColumn('ERROR', DatasetFieldType.TEXT);

	var cols = getColsDepartamentos();
	for (i in cols) {
		addColumn(cols[i].id.toUpperCase() + '_DEP', DatasetFieldType[cols[i].type]);
	}
	addColumn('CODDESCDEPARTAMENTO_DEP', DatasetFieldType.TEXT);

	var cols = getColsCelulasDepartamento();
	for (i in cols) {
		addColumn(cols[i].id.toUpperCase() + '_CEL', DatasetFieldType[cols[i].type]);
	}
	addColumn('CODDESCCELULADEPARTAMENTO_CEL', DatasetFieldType.TEXT);

	var cols = getColsGruposAtendimento();
	for (i in cols) {
		addColumn(cols[i].id.toUpperCase() + '_GRU', DatasetFieldType[cols[i].type]);
	}
	addColumn('CODDESCGRUPOATENDIMENTO_GRU', DatasetFieldType.TEXT);

	var cols = getColsSubgruposAtendimento();
	for (i in cols) {
		addColumn(cols[i].id.toUpperCase() + '_SGR', DatasetFieldType[cols[i].type]);
	}
	addColumn('CODDESCSUBGRUPOATENDIMENTO_SGR', DatasetFieldType.TEXT);

	var cols = getColsAmarracaoCabecalho();
	for (i in cols) {
		addColumn(cols[i].id.toUpperCase() + '_AMA', DatasetFieldType[cols[i].type]);
	}

	var cols = getColsAtividades();
	for (i in cols) {
		addColumn(cols[i].id.toUpperCase() + '_ATI', DatasetFieldType[cols[i].type]);
	}
	addColumn('CODDESCATIVIDADE_ATI', DatasetFieldType.TEXT);

	var cols = getColsAmarracoesFilho();
	for (i in cols) {
		addColumn(cols[i].id.toUpperCase() + '_AMA', DatasetFieldType[cols[i].type]);
	}

	addColumn('DUPLICATED_DEP', DatasetFieldType.NUMBER);
	addColumn('DUPLICATED_CEL', DatasetFieldType.NUMBER);
	addColumn('DUPLICATED_GRU', DatasetFieldType.NUMBER);
	addColumn('DUPLICATED_SGR', DatasetFieldType.NUMBER);
	addColumn('DUPLICATED_ATI', DatasetFieldType.NUMBER);
	addColumn('EXISTE_FLUXO_PROCESSO', DatasetFieldType.TEXT);
	addColumn('FLUXO_PROCESSO', DatasetFieldType.TEXT);
	addColumn('DOCID_FLUXO_PROCESSO', DatasetFieldType.NUMBER);

	// >>> PK vem ANTES do ROW_ID
	addColumn('PK', DatasetFieldType.TEXT);
	addColumn('ROW_ID', DatasetFieldType.NUMBER);

	setKey(["PK"]);

	// Índices (pares curtos)
	// DEP
	addIndex(['CODDEPARTAMENTO_DEP']);
	addIndex(['CODDEPARTAMENTO_DEP', 'ATIVOGERAL_AMA']);
	addIndex(['ATIVO_DEP']);
	// CEL
	addIndex(['CODDEPARTAMENTO_DEP', 'CODCELULADEPARTAMENTO_CEL']);
	addIndex(['CODCELULADEPARTAMENTO_CEL', 'ATIVO_CEL']);
	// GRU
	addIndex(['CODCELULADEPARTAMENTO_CEL', 'CODGRUPOATENDIMENTO_GRU']);
	addIndex(['CODGRUPOATENDIMENTO_GRU', 'ATIVO_GRU']);
	// SGR
	addIndex(['CODGRUPOATENDIMENTO_GRU', 'CODSUBGRUPOATENDIMENTO_SGR']);
	addIndex(['CODSUBGRUPOATENDIMENTO_SGR', 'ATIVO_SGR']);
	// ATI
	addIndex(['CODSUBGRUPOATENDIMENTO_SGR', 'CODATIVIDADE_ATI']);
	addIndex(['CODATIVIDADE_ATI', 'ATIVO_ATI']);
}

function onSync(lastSyncDate) {

	var dataset = DatasetBuilder.newDataset();

	var params = {
		debug: false,
		codDepartamento: false,
		codCelulaDepartamento: false,
		codGrupoAtendimento: false,
		codSubgrupoAtendimento: false,
		grouped: false,
	}

	try {
		var departamentos = getDepartamentos(params).toArray();
		var celulasDepartamento = getCelulasDepartamento(params).toArray();
		var gruposAtendimento = getGruposAtendimento(params).toArray();
		var subgruposAtendimento = getSubgruposAtendimento(params).toArray();
		var atividades = getAtividades(params).toArray();
		var amarracaoCabecalho = getAmarracaoCabecalho(params).toArray();
		var amarracoesFilho = getAmarracoesFilho(params).toArray();
		var fluxosProcesso = getFluxosProcesso().toArray();

		var colsDepartamentos = getColsDepartamentos();
		var colsCelulasDepartamento = getColsCelulasDepartamento();
		var colsGruposAtendimento = getColsGruposAtendimento();
		var colsSubgruposAtendimento = getColsSubgruposAtendimento();
		var colsAmarracaoCabecalho = getColsAmarracaoCabecalho();
		var colsAtividades = getColsAtividades();
		var colsAmarracoesFilho = getColsAmarracoesFilho();

		var finded_departamento = [];
		var finded_celulasDepartamento = [];
		var finded_grupoAtendimento = [];
		var finded_subgrupoAtendimento = [];
		var finded_atividade = [];
		var fluxosProcessoPrimeiraOrdem = {};
		var fluxosProcessoPorDocumento = {};

		var rows = [];
		var rowId = 1;

		for (var idxFluxo in fluxosProcesso) {
			var fluxoProcesso = fluxosProcesso[idxFluxo];
			var fluxoProcessoDocId = normPkPart(fluxoProcesso.get('documentid'));

			if (!fluxoProcessoDocId) continue;

			if (!fluxosProcessoPorDocumento[fluxoProcessoDocId]) {
				fluxosProcessoPorDocumento[fluxoProcessoDocId] = [];
			}
			fluxosProcessoPorDocumento[fluxoProcessoDocId].push(fluxoProcesso);

			if (normPkPart(fluxoProcesso.get('ordemFluxoProcesso')) != '001') continue;

			var fluxoProcessoKey = buildFluxoProcessoKey(
				fluxoProcesso.get('codDepartamento'),
				fluxoProcesso.get('codCelulaDepartamento'),
				fluxoProcesso.get('codGrupoAtendimento'),
				fluxoProcesso.get('codSubgrupoAtendimento'),
				fluxoProcesso.get('codAtividade')
			);

			if (fluxoProcessoKey && !fluxosProcessoPrimeiraOrdem[fluxoProcessoKey]) {
				fluxosProcessoPrimeiraOrdem[fluxoProcessoKey] = fluxoProcessoDocId;
			}
		}

		for (var fluxoProcessoDocId in fluxosProcessoPorDocumento) {
			fluxosProcessoPorDocumento[fluxoProcessoDocId].sort(sortFluxoProcessoByOrdem);
		}

		for (attr in amarracaoCabecalho) {

			var codDepartamento = amarracaoCabecalho[attr].get('codDepartamento');
			var codCelulaDepartamento = amarracaoCabecalho[attr].get('codCelulaDepartamento');
			var codGrupoAtendimento = amarracaoCabecalho[attr].get('codGrupoAtendimento');
			var codSubgrupoAtendimento = amarracaoCabecalho[attr].get('codSubgrupoAtendimento');

			var temp = [];

			temp.push(''); //LINHA DE ERRO

			var departamento = departamentos.filter(function(o) {
				if (!o.get('codDepartamento') || !codDepartamento) return false;
				return String(o.get('codDepartamento')) == String(codDepartamento);
			});

			if (!departamento.length) {
				log.error('FLUIG DESK - DEBUG - Não foi encontrado nenhum departamento com o código ' + codDepartamento + ' no formuário de amarração');
				continue;
			}

			for (i in colsDepartamentos) {
				var value = departamento[0].get(colsDepartamentos[i].id) || "";
				temp.push(colsDepartamentos[i].type == "NUMBER" ? Number(value) : value);
			}
			temp.push(departamento[0].get('codDepartamento') + ' - ' + departamento[0].get('descDepartamento'));

			// CEL
			var celulaDepartamento = celulasDepartamento.filter(function(o) {
				if (!o.get('codCelulaDepartamento') || !codCelulaDepartamento) return false;
				return String(o.get('codCelulaDepartamento')) == String(codCelulaDepartamento);
			});
			if (!celulaDepartamento.length) {
				log.error('FLUIG DESK - DEBUG - Não foi encontrado nenhuma célula do departamento com o código ' + codCelulaDepartamento + ' no formuário de amarração');
				continue;
			}

			for (i in colsCelulasDepartamento) {
				var value = celulaDepartamento[0].get(colsCelulasDepartamento[i].id) || "";
				temp.push(colsCelulasDepartamento[i].type == "NUMBER" ? Number(value) : value);
			}
			temp.push(celulaDepartamento[0].get('codCelulaDepartamento') + ' - ' + celulaDepartamento[0].get('descCelulaDepartamento'));

			// GRU
			var grupoAtendimento = gruposAtendimento.filter(function(o) {
				if (!o.get('codGrupoAtendimento') || !codGrupoAtendimento) return false;
				return String(o.get('codGrupoAtendimento')) == String(codGrupoAtendimento);
			});

			for (i in colsGruposAtendimento) {
				var value = !grupoAtendimento.length ? "" : grupoAtendimento[0].get(colsGruposAtendimento[i].id) || "";
				temp.push(colsGruposAtendimento[i].type == "NUMBER" ? Number(value) : value);
			}
			if (!grupoAtendimento.length) {
				temp.push('');
			} else {
				temp.push(grupoAtendimento[0].get('codGrupoAtendimento') + ' - ' + grupoAtendimento[0].get('descGrupoAtendimento'));
			}

			// SGR
			var subgrupoAtendimento = subgruposAtendimento.filter(function(o) {
				if (!o.get('codSubgrupoAtendimento') || !codSubgrupoAtendimento) return false;
				return String(o.get('codSubgrupoAtendimento')) == String(codSubgrupoAtendimento);
			});

			for (i in colsSubgruposAtendimento) {
				var value = !subgrupoAtendimento.length ? "" : subgrupoAtendimento[0].get(colsSubgruposAtendimento[i].id) || ""
				temp.push(colsSubgruposAtendimento[i].type == "NUMBER" ? Number(value) : value);
			}
			if (!subgrupoAtendimento.length) {
				temp.push("");
			} else {
				temp.push(subgrupoAtendimento[0].get('codSubgrupoAtendimento') + ' - ' + subgrupoAtendimento[0].get('descSubgrupoAtendimento'));
			}

			// AMA (cabeçalho)
			for (i in colsAmarracaoCabecalho) {
				var value = amarracaoCabecalho[attr].get(colsAmarracaoCabecalho[i].id);
				temp.push(colsAmarracaoCabecalho[i].type == "NUMBER" ? Number(value) : value);
			}

			// filhos
			amarracoesFilho.filter(function(o) {
				if (!o.get('documentid') || !amarracaoCabecalho[attr].get('documentid')) return false;
				temp2 = temp.slice();

				if (String(o.get('documentid')) == String(amarracaoCabecalho[attr].get('documentid'))) {

					var atividade = atividades.filter(function(o2) {
						if (!o2.get('codAtividade') || !o.get('codAtividade')) return false;
						return String(o2.get('codAtividade')) == String(o.get('codAtividade'));
					});
					if (!atividade.length) {
						log.error('FLUIG DESK - DEBUG - Não foi encontrado nenhuma atividade com o código ' + o.get('codAtividade') + ' no formuário de amarração');
						return false;
					}
					for (i in colsAtividades) {
						var value = atividade[0].get(colsAtividades[i].id) || "";
						temp2.push(colsAtividades[i].type == "NUMBER" ? Number(value) : value);
					}
					temp2.push(atividade[0].get('codAtividade') + ' - ' + atividade[0].get('descAtividade'));

					for (i in colsAmarracoesFilho) {
						var colsAmarracoesFilhoCustom = getColsAmarracoesFilhoCustom();
						var value = "";
						if (o.get("customizarAtividade") == "0" && colsAmarracoesFilhoCustom.indexOf(colsAmarracoesFilho[i].id) > -1) {
							value = atividade[0].get(colsAmarracoesFilho[i].id) || "";
						} else {
							value = o.get(colsAmarracoesFilho[i].id) || "";
						}
						temp2.push(colsAmarracoesFilho[i].type == "NUMBER" ? Number(value) : value);
					}

					// flags de duplicidade
					var find = false;
					if (finded_departamento.length) {
						find = finded_departamento.filter(function(v) { return String(v) == String(codDepartamento); });
					}
					temp2.push(find && find.length ? 1 : 0);
					finded_departamento.push(codDepartamento);

					var searcher = [codDepartamento, codCelulaDepartamento].join();
					find = false;
					if (finded_celulasDepartamento.length) {
						find = finded_celulasDepartamento.filter(function(v) { return String(v) == String(searcher); });
					}
					temp2.push(find && find.length ? 1 : 0);
					finded_celulasDepartamento.push(searcher);

					searcher = [codDepartamento, codCelulaDepartamento, codGrupoAtendimento].join();
					find = false;
					if (finded_grupoAtendimento.length) {
						find = finded_grupoAtendimento.filter(function(v) { return String(v) == String(searcher); });
					}
					temp2.push(find && find.length ? 1 : 0);
					finded_grupoAtendimento.push(searcher);

					searcher = [codDepartamento, codCelulaDepartamento, codGrupoAtendimento, codSubgrupoAtendimento].join();
					find = false;
					if (finded_subgrupoAtendimento.length) {
						find = finded_subgrupoAtendimento.filter(function(v) { return String(v) == String(searcher); });
					}
					temp2.push(find && find.length ? 1 : 0);
					finded_subgrupoAtendimento.push(searcher);

					searcher = [codDepartamento, codCelulaDepartamento, codGrupoAtendimento, codSubgrupoAtendimento, atividade[0].get('codAtividade')].join();
					find = false;
					if (finded_atividade.length) {
						find = finded_atividade.filter(function(v) { return String(v) == String(searcher); });
					}
					temp2.push(find && find.length ? 1 : 0);
					finded_atividade.push(searcher);

					var fluxoProcessoInfo = getFluxoProcessoInfo(
						fluxosProcessoPrimeiraOrdem,
						fluxosProcessoPorDocumento,
						codDepartamento,
						codCelulaDepartamento,
						codGrupoAtendimento,
						codSubgrupoAtendimento,
						atividade[0].get('codAtividade')
					);
					temp2.push(fluxoProcessoInfo.existe);
					temp2.push(fluxoProcessoInfo.fluxo);
					temp2.push(fluxoProcessoInfo.docId);

					// >>> PK (normalizada/upper) ANTES do ROW_ID
					var pk = [
						normPkPart(codDepartamento),
						normPkPart(codCelulaDepartamento),
						normPkPart(codGrupoAtendimento),
						normPkPart(codSubgrupoAtendimento),
						normPkPart(atividade && atividade.length ? atividade[0].get('codAtividade') : '')
					].join('|').toUpperCase();

					temp2.push(pk);				// PK
					temp2.push(Number(rowId));	// ROW_ID (debug/ordenação)
					rowId++;

					rows.push(temp2);
				}
			});
		}

		var response = clearDataset(dataset, rows, lastSyncDate);

		for (var k = 0; k < response.length; k++) {
			dataset.addOrUpdateRow(response[k]);
		}

	} catch (e) {
		dataset = DatasetBuilder.newDataset();

		var row = [e.toString()];

		// placeholders na MESMA ORDEM das colunas criadas em defineStructure()
		var cols = new Array().concat(
			getColsDepartamentos(), '',
			getColsCelulasDepartamento(), '',
			getColsGruposAtendimento(), '',
			getColsSubgruposAtendimento(), '',
			getColsAmarracaoCabecalho(), '',
			getColsAtividades(), '',
			getColsAmarracoesFilho()
		);

		for (i in cols) {
			if (!cols[i] || !cols[i].type) { // trata os '' (CODDESC*)
				row.push('');
			} else {
				cols[i].type == "NUMBER" ? row.push(0) : row.push('');
			}
		}

		// 5 duplicados, fluxo processo, PK (''), ROW_ID (0)
		row.push(0, 0, 0, 0, 0, '', '', 0, '', 0);

		dataset.addOrUpdateRow(row);

	} finally {
		return dataset;
	}
}

function clearDataset(datasetParam, responseParam, lastSyncDate) {
	var datasetClear = DatasetFactory.getDataset('ds_uf_gestor_chamados_atividades', null, null, null);

	if (!datasetClear || datasetClear.rowsCount == 0 || lastSyncDate <= 0) {
		return responseParam;
	}

	var datasetClearArr = datasetClear.getValues();

	var chavesResponse = {};
	var idxPk = responseParam[0].length - 2; // PK é a penúltima coluna (antes do ROW_ID)
	var idxRowId = responseParam[0].length - 1; // ROW_ID é a última coluna

	// mapa PK das novas linhas
	for (var ir = 0; ir < responseParam.length; ir++) {
		responseParam[ir]["created"] = true;
		responseParam[ir]["signature"] = getRowSignature(responseParam[ir], idxRowId);
		var keyResp = String(responseParam[ir][idxPk] || '').trim().toUpperCase();
		chavesResponse[keyResp] = ir;
	}

	// apaga apenas o que não existe mais
	for (var i = 0; i < datasetClear.rowsCount; i++) {
		var lElimina = true;
		var keyOld = String(datasetClearArr[i][idxPk] || '').trim().toUpperCase();

		if (chavesResponse[keyOld] != null && chavesResponse[keyOld] > -1) {
			lElimina = false;
			var oldSignature = getRowSignature(datasetClearArr[i], idxRowId);
			if (oldSignature == responseParam[chavesResponse[keyOld]]["signature"]) {
				responseParam[chavesResponse[keyOld]]["created"] = false; // já existe e não mudou
			}
		}
		if (lElimina === true) {
			var row = datasetClear.values[i];
			datasetParam.deleteRow(row);
		}
	}

	// insere/atualiza só o que é novo
	var newResponse = [];
	for (var ir = 0; ir < responseParam.length; ir++) {
		if (responseParam[ir]["created"] === true) {
			newResponse.push(responseParam[ir]);
		}
	}
	return newResponse;
}
function getRowSignature(row, idxRowId) {
	var signature = [];

	for (var i = 0; i < row.length; i++) {
		if (i == idxRowId) continue; // ignora ROW_ID
		signature.push(normalizeComparableCellValue(row[i]));
	}

	return signature.join('|#|');
}
function normalizeComparableCellValue(value) {
	if (value === null || value === undefined) return '';

	var normalized = String(value).trim();
	if (normalized == 'null' || normalized == 'undefined') return '';

	// normaliza valores numéricos serializados pelo Fluig
	normalized = normalized.replace(/\.0+$/, '');
	normalized = normalized.replace(/^0E-?\d+$/i, '0');

	return normalized;
}

function getColsDepartamentos() {
	return [
		{ id: 'codDepartamento', type: 'NUMBER' },
		{ id: 'descDepartamento', type: 'TEXT' },
		{ id: 'codExpediente', type: 'TEXT' },
		{ id: 'ordemExibicao', type: 'NUMBER' },
		{ id: 'ativo', type: 'NUMBER' },
		{ id: 'imgDocumentId', type: 'NUMBER' },
		{ id: 'imgDocumentVersion', type: 'NUMBER' }
	];
}
function getColsCelulasDepartamento() {
	return [
		{ id: 'codCelulaDepartamento', type: 'NUMBER' },
		{ id: 'descCelulaDepartamento', type: 'TEXT' },
		{ id: 'obsCelulaDepartamento', type: 'TEXT' },
		{ id: 'ativo', type: 'NUMBER' },
		{ id: 'imgDocumentId', type: 'NUMBER' },
		{ id: 'imgDocumentVersion', type: 'NUMBER' }
	];
}
function getColsGruposAtendimento() {
	return [
		{ id: 'codGrupoAtendimento', type: 'NUMBER' },
		{ id: 'descGrupoAtendimento', type: 'TEXT' },
		{ id: 'obsGrupoAtendimento', type: 'TEXT' },
		{ id: 'ativo', type: 'NUMBER' },
		{ id: 'imgDocumentId', type: 'NUMBER' },
		{ id: 'imgDocumentVersion', type: 'NUMBER' }
	];
}
function getColsSubgruposAtendimento() {
	return [
		{ id: 'codSubgrupoAtendimento', type: 'NUMBER' },
		{ id: 'descSubgrupoAtendimento', type: 'TEXT' },
		{ id: 'obsSubgrupoAtendimento', type: 'TEXT' },
		{ id: 'ativo', type: 'NUMBER' },
		{ id: 'imgDocumentId', type: 'NUMBER' },
		{ id: 'imgDocumentVersion', type: 'NUMBER' }
	];
}
function getColsAtividades() {
	return [
		{ id: 'codAtividade', type: 'TEXT' },
		{ id: 'descAtividade', type: 'TEXT' },
		{ id: 'ativo', type: 'NUMBER' }
	];
}
function getColsAmarracaoCabecalho() {
	return [
		{ id: 'codDepartamento', type: 'NUMBER' },
		{ id: 'codCelulaDepartamento', type: 'NUMBER' },
		{ id: 'codGrupoAtendimento', type: 'NUMBER' },
		{ id: 'codSubgrupoAtendimento', type: 'NUMBER' },
		{ id: 'ativoGeral', type: 'NUMBER' },
		{ id: 'grupoAtendimento', type: 'TEXT' },
		{ id: 'regraGrupoAtendimento', type: 'TEXT' }
	];
}
function getColsAmarracoesFilho() {
	return [
		{ id: 'codAtividade', type: 'NUMBER' },
		{ id: 'codProcess', type: 'TEXT' },
		{ id: 'descProcess', type: 'TEXT' },
		{ id: 'ativo', type: 'NUMBER' },
		{ id: 'customizarAtividade', type: 'NUMBER' },
		{ id: 'aprovacao', type: 'NUMBER' },
		{ id: 'visivelSolicitante', type: 'NUMBER' },
		{ id: 'utiliza_classificacao', type: 'NUMBER' },
		{ id: 'utiliza_N1', type: 'NUMBER' },
		{ id: 'utiliza_N2', type: 'NUMBER' },
		{ id: 'utiliza_N3', type: 'NUMBER' },
		{ id: 'atividade_default_atd', type: 'STRING' },
		{ id: 'anexo', type: 'NUMBER' },
		{ id: 'quantidadeAnexo', type: 'NUMBER' },
		{ id: 'anexoMsg', type: 'TEXT' },
		{ id: 'gerenciamentoSLA', type: 'TEXT' },
		{ id: 'formaCalcularSLA', type: 'TEXT' },
		{ id: 'codPrioridade', type: 'NUMBER' },
		{ id: 'descPrioridade', type: 'TEXT' },
		{ id: 'SLACLASS', type: 'TEXT' },
		{ id: 'SLAN1', type: 'TEXT' },
		{ id: 'SLAN2', type: 'TEXT' },
		{ id: 'SLAN3', type: 'TEXT' },
		{ id: 'SLACLASS_baixo', type: 'TEXT' },
		{ id: 'SLACLASS_medio', type: 'TEXT' },
		{ id: 'SLACLASS_alto', type: 'TEXT' },
		{ id: 'SLACLASS_critico', type: 'TEXT' },
		{ id: 'SLAN1_baixo', type: 'TEXT' },
		{ id: 'SLAN1_medio', type: 'TEXT' },
		{ id: 'SLAN1_alto', type: 'TEXT' },
		{ id: 'SLAN1_critico', type: 'TEXT' },
		{ id: 'SLAN2_baixo', type: 'TEXT' },
		{ id: 'SLAN2_medio', type: 'TEXT' },
		{ id: 'SLAN2_alto', type: 'TEXT' },
		{ id: 'SLAN2_critico', type: 'TEXT' },
		{ id: 'SLAN3_baixo', type: 'TEXT' },
		{ id: 'SLAN3_medio', type: 'TEXT' },
		{ id: 'SLAN3_alto', type: 'TEXT' },
		{ id: 'SLAN3_critico', type: 'TEXT' },
		{ id: 'permiteAlteracao', type: 'NUMBER' },
		{ id: 'criticidadeCLASS', type: 'TEXT' },
		{ id: 'criticidadeN1', type: 'TEXT' },
		{ id: 'criticidadeN2', type: 'TEXT' },
		{ id: 'criticidadeN3', type: 'TEXT' },
		{ id: 'impactoPrioridadeCLASS', type: 'TEXT' },
		{ id: 'urgenciaPrioridadeCLASS', type: 'TEXT' },
		{ id: 'impactoPrioridadeN1', type: 'TEXT' },
		{ id: 'urgenciaPrioridadeN1', type: 'TEXT' },
		{ id: 'impactoPrioridadeN2', type: 'TEXT' },
		{ id: 'urgenciaPrioridadeN2', type: 'TEXT' },
		{ id: 'impactoPrioridadeN3', type: 'TEXT' },
		{ id: 'urgenciaPrioridadeN3', type: 'TEXT' },
		{ id: 'SLA_AT11', type: 'TEXT' },
		{ id: 'SLA_AT12', type: 'TEXT' },
		{ id: 'SLA_AT14', type: 'TEXT' },
		{ id: 'SLA_AT16', type: 'TEXT' },
		{ id: 'SLA_AT18', type: 'TEXT' },
		{ id: 'telefone_obrigatorio', type: 'NUMBER' },
		{ id: 'solic_cancela_pesq_satis', type: 'NUMBER' },
		{ id: 'regra_retorno_atendimento', type: 'TEXT' }
	];
}
function getColsAmarracoesFilhoCustom() {
	return [
		'aprovacao',
		'visivelSolicitante',
		'utiliza_classificacao',
		'utiliza_N1',
		'utiliza_N2',
		'utiliza_N3',
		'atividade_default_atd',
		'anexo',
		'quantidadeAnexo',
		'anexoMsg',
		'gerenciamentoSLA',
		'formaCalcularSLA',
		'codPrioridade',
		'descPrioridade',
		'SLACLASS',
		'SLAN1',
		'SLAN2',
		'SLAN3',
		'SLACLASS_baixo',
		'SLACLASS_medio',
		'SLACLASS_alto',
		'SLACLASS_critico',
		'SLAN1_baixo',
		'SLAN1_medio',
		'SLAN1_alto',
		'SLAN1_critico',
		'SLAN2_baixo',
		'SLAN2_medio',
		'SLAN2_alto',
		'SLAN2_critico',
		'SLAN3_baixo',
		'SLAN3_medio',
		'SLAN3_alto',
		'SLAN3_critico',
		'permiteAlteracao',
		'criticidadeCLASS',
		'criticidadeN1',
		'criticidadeN2',
		'criticidadeN3',
		'impactoPrioridadeCLASS',
		'urgenciaPrioridadeCLASS',
		'impactoPrioridadeN1',
		'urgenciaPrioridadeN1',
		'impactoPrioridadeN2',
		'urgenciaPrioridadeN2',
		'impactoPrioridadeN3',
		'urgenciaPrioridadeN3',
		'SLA_AT11',
		'SLA_AT12',
		'SLA_AT14',
		'SLA_AT16',
		'SLA_AT18',
		'telefone_obrigatorio',
		'solic_cancela_pesq_satis',
		'regra_retorno_atendimento',
	];
}
function createColumn(dataset) {
	// DESATIVADO POR ENQUANTO
	var cols = getColsDepartamentos();
	for (attr in cols) {
		dataset.addColumn(cols[attr].id + '_DEP');
	}
	var cols = getColsCelulasDepartamento();
	for (attr in cols) {
		dataset.addColumn(cols[attr].id + '_CEL');
	}
	var cols = getColsGruposAtendimento();
	for (attr in cols) {
		dataset.addColumn(cols[attr].id + '_GRU');
	}
	var cols = getColsSubgruposAtendimento();
	for (attr in cols) {
		dataset.addColumn(cols[attr].id + '_SGR');
	}
	var cols = getColsAtividades();
	for (attr in cols) {
		dataset.addColumn(cols[attr].id + '_ATI');
	}
	var cols = getColsAmarracoesFilho();
	for (attr in cols) {
		dataset.addColumn(cols[attr].id + '_AMA');
	}
}

function getDepartamentos(params) {
	try {
		var cs = [];
		cs.push(DatasetFactory.createConstraint("companyId", getValue("WKCompany"), getValue("WKCompany"), ConstraintType.MUST));
		cs.push(DatasetFactory.createConstraint("metadata#active", true, true, ConstraintType.MUST));
		if (params.codDepartamento) {
			cs.push(DatasetFactory.createConstraint("codDepartamento", params.codDepartamento, params.codDepartamento, ConstraintType.MUST));
		}

		var cols = getColsDepartamentos().map(function(o) { return o.id });

		var ds = DatasetFactory.getDataset("ds_uf_form_cadastro_departamento", cols, cs, ['ordemExibicao']);

		if (params.debug) {
			log.dir('FLUIG DESK - DEBUG - Finalizado a consulta do dataset ds_uf_form_cadastro_departamento');
			log.dir(cs);
			log.dir(ds);
		}

		if (!ds || ds.rowsCount == 0) return new java.util.ArrayList();

		return ds.getMap();
	} catch (e) {
		throw 'Houve um erro na função getDepartamentos(): ' + e;
	}
}
function getCelulasDepartamento(params) {
	try {
		var cs = [];
		cs.push(DatasetFactory.createConstraint("companyId", getValue("WKCompany"), getValue("WKCompany"), ConstraintType.MUST));
		cs.push(DatasetFactory.createConstraint("metadata#active", true, true, ConstraintType.MUST));

		if (params.codCelulaDepartamento) {
			cs.push(DatasetFactory.createConstraint("codCelulaDepartamento", params.codCelulaDepartamento, params.codCelulaDepartamento, ConstraintType.MUST));
		}

		var cols = getColsCelulasDepartamento().map(function(o) { return o.id });

		var ds = DatasetFactory.getDataset("ds_uf_form_cadastro_celula_departamento", cols, cs, null);

		if (params.debug) {
			log.dir('FLUIG DESK - DEBUG - Finalizado a consulta do dataset ds_uf_form_cadastro_celula_departamento');
			log.dir(cs);
			log.dir(ds);
		}

		if (!ds || ds.rowsCount == 0) return new java.util.ArrayList();

		return ds.getMap();
	} catch (e) {
		throw 'Houve um erro na função getCelulasDepartamento(): ' + e;
	}
}
function getGruposAtendimento(params) {
	try {
		var cs = [];
		cs.push(DatasetFactory.createConstraint("companyId", getValue("WKCompany"), getValue("WKCompany"), ConstraintType.MUST));
		cs.push(DatasetFactory.createConstraint("metadata#active", true, true, ConstraintType.MUST));

		if (params.codGrupoAtendimento) {
			cs.push(DatasetFactory.createConstraint("codGrupoAtendimento", params.codGrupoAtendimento, params.codGrupoAtendimento, ConstraintType.MUST));
		}

		var cols = getColsGruposAtendimento().map(function(o) { return o.id });

		var ds = DatasetFactory.getDataset("ds_uf_form_cadastro_grupo_atendimento", cols, cs, null);

		if (params.debug) {
			log.dir('FLUIG DESK - DEBUG - Finalizado a consulta do dataset ds_uf_form_cadastro_grupo_atendimento');
			log.dir(cs);
			log.dir(ds);
		}

		if (!ds || ds.rowsCount == 0) return new java.util.ArrayList();

		return ds.getMap();
	} catch (e) {
		throw 'Houve um erro na função getGruposAtendimento(): ' + e;
	}
}
function getSubgruposAtendimento(params) {
	try {
		var cs = [];
		cs.push(DatasetFactory.createConstraint("companyId", getValue("WKCompany"), getValue("WKCompany"), ConstraintType.MUST));
		cs.push(DatasetFactory.createConstraint("metadata#active", true, true, ConstraintType.MUST));

		if (params.codSubgrupoAtendimento) {
			cs.push(DatasetFactory.createConstraint("codSubgrupoAtendimento", params.codSubgrupoAtendimento, params.codSubgrupoAtendimento, ConstraintType.MUST));
		}

		var cols = getColsSubgruposAtendimento().map(function(o) { return o.id });

		var ds = DatasetFactory.getDataset("ds_uf_form_cadastro_subgrupo_atendimento", cols, cs, null);

		if (params.debug) {
			log.dir('FLUIG DESK - DEBUG - Finalizado a consulta do dataset ds_uf_form_cadastro_subgrupo_atendimento');
			log.dir(cs);
			log.dir(ds);
		}

		if (!ds || ds.rowsCount == 0) return new java.util.ArrayList();

		return ds.getMap();
	} catch (e) {
		throw 'Houve um erro na função getSubgruposAtendimento(): ' + e;
	}
}
function getAtividades(params) {
	try {
		var cs = [];
		cs.push(DatasetFactory.createConstraint("companyId", getValue("WKCompany"), getValue("WKCompany"), ConstraintType.MUST));
		cs.push(DatasetFactory.createConstraint("metadata#active", true, true, ConstraintType.MUST));

		var cols = getColsAtividades().map(function(o) { return o.id });
		cols = cols.concat(getColsAmarracoesFilhoCustom());

		var ds = DatasetFactory.getDataset("ds_uf_form_cadastro_atividade", cols, cs, null);
		if (params.debug) {
			log.dir('FLUIG DESK - DEBUG - Finalizado a consulta do dataset ds_uf_form_cadastro_atividade');
			log.dir(cs);
			log.dir(ds);
		}

		if (!ds || ds.rowsCount == 0) return new java.util.ArrayList();

		return ds.getMap();
	} catch (e) {
		throw 'Houve um erro na função getAtividades(): ' + e;
	}
}
function getAmarracaoCabecalho(params) {
	try {
		var cs = [];
		cs.push(DatasetFactory.createConstraint("companyId", getValue("WKCompany"), getValue("WKCompany"), ConstraintType.MUST));
		cs.push(DatasetFactory.createConstraint("metadata#active", true, true, ConstraintType.MUST));

		if (params.codDepartamento) {
			cs.push(DatasetFactory.createConstraint("codDepartamento", params.codDepartamento, params.codDepartamento, ConstraintType.MUST));
		}
		if (params.codCelulaDepartamento) {
			cs.push(DatasetFactory.createConstraint("codCelulaDepartamento", params.codCelulaDepartamento, params.codCelulaDepartamento, ConstraintType.MUST));
		}
		if (params.codGrupoAtendimento) {
			cs.push(DatasetFactory.createConstraint("codGrupoAtendimento", params.codGrupoAtendimento, params.codGrupoAtendimento, ConstraintType.MUST));
		}
		if (params.codSubgrupoAtendimento) {
			cs.push(DatasetFactory.createConstraint("codSubgrupoAtendimento", params.codSubgrupoAtendimento, params.codSubgrupoAtendimento, ConstraintType.MUST));
		}

		var cols = getColsAmarracaoCabecalho().map(function(o) { return o.id });

		var ds = DatasetFactory.getDataset("ds_uf_form_amarracao_cadastro", cols, cs, null);

		if (params.debug) {
			log.dir('FLUIG DESK - DEBUG - Finalizado a consulta do dataset ds_uf_form_amarracao_cadastro');
			log.dir(cs);
			log.dir(ds);
		}

		if (!ds || ds.rowsCount == 0) return new java.util.ArrayList();

		return ds.getMap();
	} catch (e) {
		throw 'Houve um erro na função getAmarracaoCabecalho(): ' + e;
	}
}
function getAmarracoesFilho() {
	try {
		var cs = [];
		cs.push(DatasetFactory.createConstraint("companyId", getValue("WKCompany"), getValue("WKCompany"), ConstraintType.MUST));
		cs.push(DatasetFactory.createConstraint("metadata#active", true, true, ConstraintType.MUST));
		cs.push(DatasetFactory.createConstraint("tableName", "tblAmarracao", "tblAmarracao", ConstraintType.MUST));

		var cols = getColsAmarracoesFilho().map(function(o) { return o.id });

		var ds = DatasetFactory.getDataset("ds_uf_form_amarracao_cadastro", cols, cs, null);

		if (!ds || ds.rowsCount == 0) return new java.util.ArrayList();

		return ds.getMap();
	} catch (e) {
		throw 'Houve um erro na função getAmarracaoFilho(): ' + e;
	}
}
function getColsFluxoProcesso() {
	return [
		{ id: 'codDepartamento', type: 'NUMBER' },
		{ id: 'codCelulaDepartamento', type: 'NUMBER' },
		{ id: 'codGrupoAtendimento', type: 'NUMBER' },
		{ id: 'codSubgrupoAtendimento', type: 'NUMBER' },
		{ id: 'codAtividade', type: 'NUMBER' },
		{ id: 'ordemFluxoProcesso', type: 'TEXT' },
		{ id: 'documentid', type: 'NUMBER' }
	];
}
function getFluxosProcesso() {
	try {
		var cs = [];
		cs.push(DatasetFactory.createConstraint("companyId", getValue("WKCompany"), getValue("WKCompany"), ConstraintType.MUST));
		cs.push(DatasetFactory.createConstraint("metadata#active", true, true, ConstraintType.MUST));
		cs.push(DatasetFactory.createConstraint("tableName", "tblFluxoProcesso", "tblFluxoProcesso", ConstraintType.MUST));

		var cols = getColsFluxoProcesso().map(function(o) { return o.id });

		var ds = DatasetFactory.getDataset("ds_uf_form_fluxo_processo", cols, cs, null);

		if (!ds || ds.rowsCount == 0) return new java.util.ArrayList();

		return ds.getMap();
	} catch (e) {
		throw 'Houve um erro na função getFluxosProcesso(): ' + e;
	}
}

function buildFluxoProcessoKey(codDepartamento, codCelulaDepartamento, codGrupoAtendimento, codSubgrupoAtendimento, codAtividade) {
	return [
		normPkPart(codDepartamento),
		normPkPart(codCelulaDepartamento),
		normPkPart(codGrupoAtendimento),
		normPkPart(codSubgrupoAtendimento),
		normPkPart(codAtividade)
	].join('|').toUpperCase();
}

function sortFluxoProcessoByOrdem(a, b) {
	var ordemA = Number(normPkPart(a.get('ordemFluxoProcesso')));
	var ordemB = Number(normPkPart(b.get('ordemFluxoProcesso')));

	if (isNaN(ordemA)) ordemA = 0;
	if (isNaN(ordemB)) ordemB = 0;

	return ordemA - ordemB;
}

function getFluxoProcessoValue(value) {
	var normalized = normPkPart(value);
	return normalized === '' ? '0' : normalized;
}

function getFluxoProcessoInfo(fluxosProcessoPrimeiraOrdem, fluxosProcessoPorDocumento, codDepartamento, codCelulaDepartamento, codGrupoAtendimento, codSubgrupoAtendimento, codAtividade) {
	var fluxoProcessoKey = buildFluxoProcessoKey(
		codDepartamento,
		codCelulaDepartamento,
		codGrupoAtendimento,
		codSubgrupoAtendimento,
		codAtividade
	);
	var fluxoProcessoDocId = fluxosProcessoPrimeiraOrdem[fluxoProcessoKey];

	if (!fluxoProcessoDocId || !fluxosProcessoPorDocumento[fluxoProcessoDocId]) {
		return {
			existe: 'false',
			fluxo: '',
			docId: 0
		};
	}

	var fluxoProcesso = fluxosProcessoPorDocumento[fluxoProcessoDocId];
	var fluxo = [];
	for (var idx in fluxoProcesso) {
		fluxo.push([
			getFluxoProcessoValue(fluxoProcesso[idx].get('codDepartamento')),
			getFluxoProcessoValue(fluxoProcesso[idx].get('codCelulaDepartamento')),
			getFluxoProcessoValue(fluxoProcesso[idx].get('codGrupoAtendimento')),
			getFluxoProcessoValue(fluxoProcesso[idx].get('codSubgrupoAtendimento')),
			getFluxoProcessoValue(fluxoProcesso[idx].get('codAtividade'))
		].join(','));
	}

	return {
		existe: 'true',
		fluxo: fluxo.join(';'),
		docId: Number(fluxoProcessoDocId) || 0
	};
}

function replaceAll(str, de, para) {
	try {
		var pos = str.indexOf(de);
		while (pos > -1) {
			str = str.replace(de, para);
			pos = str.indexOf(de);
		}
		return str;
	} catch (e) {
		return str
	}
}

function createDataset(fields, constraints, sortFields) {
	var dataset = DatasetBuilder.newDataset();

	dataset.addColumn("ERROR");
	dataset.addRow(new Array('Esse dataset não está habilitado para funcionar dessa maneira, ative a sincronização, sincronize os dados e tente novamente.'));
}

function normPkPart(v) {
	if (v === null || v === undefined) return '';
	var s = String(v).trim();
	if (s === 'null' || s === 'undefined') return '';
	// normaliza "1.0000000000" -> "1"
	s = s.replace(/\.0+$/, '');
	// normaliza "0E-11" -> "0"
	s = s.replace(/^0E-?\d+$/i, '0');
	return s;
}
