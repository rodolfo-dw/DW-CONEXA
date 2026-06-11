function defineStructure() {
	addColumn("SUCCESS");
	addColumn("LIST_ID");
	addColumn("START_DATE");
	addColumn("PROCESSNUMBER", DatasetFieldType.NUMBER);
	addColumn("PROCESS_ID");
	addColumn("OBSERVATION");

	setKey(["LIST_ID", "PROCESSNUMBER", "START_DATE"]);
	addIndex(["LIST_ID", "PROCESSNUMBER", "START_DATE"]);
}

function onSync(lastSyncDate) {
	var dataset = DatasetBuilder.newDataset();

	try {
		var clientKey = "poc";
		var integrationId = "1";
		var pagesize = "20";
		var initialDueDate = String(java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC).minusMonths(2).toInstant().truncatedTo(java.time.temporal.ChronoUnit.MILLIS)))
		var endDueDate = String(java.time.format.DateTimeFormatter.ISO_INSTANT.format(java.time.ZonedDateTime.now(java.time.ZoneOffset.UTC).plusMonths(1).toInstant().truncatedTo(java.time.temporal.ChronoUnit.MILLIS)));

		var token = getToken();
		var mapEmpresas = getEmpresasMap();
		var mapCentros = getCentrosCustoMap();
		var mapContas = getContasContabeisMap();
		var mapTiposDoc = getTiposDocumentosMap();
		var mapFormasPgto = getFormasPagamentoMap();
		var mapCondicoesPgto = getCondicoesPagamentoMap();
		var mapItens = getItensMap();

		var currentPage = 1;
		var totalPages = 1;

		while (currentPage <= totalPages) {

			var endpoint = "/api/ExecutionInfo/Integration"
				+ "?integrationId=" + integrationId
				+ "&initialDueDate=" + initialDueDate
				+ "&endDueDate=" + endDueDate
				+ "&page=" + currentPage
				+ "&pagesize=" + pagesize;

			var clientService = fluigAPI.getAuthorizeClientService();
			var response = clientService.invoke(JSON.stringify({
				companyId: String(getValue("WKCompany")),
				serviceCode: 'AtisworkAPI',
				endpoint: endpoint,
				method: 'GET',
				timeoutService: '100',
				headers: {
					"Accept": "application/json",
					"Content-Type": "application/json",
					"Authorization": "Bearer " + token,
					"clientKey": clientKey
				}
			}));

			if (response.getResult() == null || response.getResult().isEmpty()) throw 'Resposta da API vazia na página ' + currentPage;

			var json = JSON.parse(response.getResult());

			if (json.nPaginas && parseInt(json.nPaginas) > 0) {
				totalPages = parseInt(json.nPaginas);
			} else {
				log.dir("** DATSET ds_ws_atiswork_startProcess ** - Consulta da api da atiswork está vazia!");
				log.dir(response);
				dataset.addRow(["NOK", "0", String(new java.text.SimpleDateFormat('yyyy-MM-dd HH:mm:ss').format(new Date())), 0, "", "Não foi encontrado nenhum documento na AtisWork, página: " + currentPage]);
			}

			if (json && json.regs && json.regs.Value) {
				var items = json.regs.Value;

				for (var i = 0; i < items.length; i++) {
					var item = items[i];
					var idAtis = String(item.Id || "");

					try {
						var companyUnity = (item.Config && item.Config.CompanyUnity) ? item.Config.CompanyUnity : {};
						var cnpjUnidadeKey = String(companyUnity.Document || "").replace(/[\.\-\/]/g, "").trim();
						var unidade = mapEmpresas[cnpjUnidadeKey];

						if (!unidade) throw 'Empresa/filial não encontrada para o CNPJ da unidade: ' + cnpjUnidadeKey;

						var company = companyUnity.Company || {};
						var cnpjEmpresaKey = String(company.Document || "").replace(/[\.\-\/]/g, "").trim();
						var empresa = mapEmpresas[cnpjEmpresaKey];

						if (!empresa) throw 'Empresa não encontrada para o CNPJ do cabeçalho: ' + cnpjEmpresaKey;

						var centroCusto = mapCentros[unidade['CODCENTROCUSTO']];
						if (!centroCusto || centroCusto == null || centroCusto == undefined || centroCusto == "") throw 'Centro de Custo não encontrado, reveja o cadastro de empresa/filial no Fluig (centro de custo administrativo): ' + unidade['CODCENTROCUSTO'];

						var contaContabil = mapContas[item.Config.AccountingCode];
						if (!contaContabil || contaContabil == null || contaContabil == undefined || contaContabil == "") throw 'Conta Contábil não encontrada: ' + item.Config.AccountingCode;

						var processId = 0;

						if (false && String(empresa.ERPEMPRESA).toLowerCase() == "protheus") {
							var condicoesPgto = mapCondicoesPgto["001"];
							if (!condicoesPgto) throw 'Forma de Pagamento 001 - A VISTA não encontrada';

							var itens = mapItens["SER00107"];
							if (!itens) throw 'Forma de Pagamento SER00107 não encontrada';

							processId = startProcessNovo(item, empresa, centroCusto, contaContabil, condicoesPgto, itens);

							var registerErrorNovo = tryRegisterIntegration(token, clientKey, "true", "Indiretos novo iniciado no fluig com sucesso, número: " + processId, idAtis, integrationId);
							var observationNovo = registerErrorNovo ? "Processo iniciado no Fluig, mas houve erro ao comunicar a Atiswork: " + registerErrorNovo : "Integrado com sucesso";

							dataset.addRow([registerErrorNovo ? "NOK" : "OK", idAtis, String(new java.text.SimpleDateFormat('yyyy-MM-dd HH:mm:ss').format(new Date())), parseInt(processId), "Indiretos Novo", observationNovo]);
						} else {
							var descConfig = item.Config.Description ? String(item.Config.Description) : "";
							if (descConfig.indexOf(" - ") === -1) throw 'Não foi possível quebrar o texto ' + descConfig + ' do campo Description pois ele não contem a palavra " - "';
							
							var tipoDocKey = descConfig.split(" - ")[0] || "";
							var tipoDocumento = mapTiposDoc[tipoDocKey];
							if (!tipoDocumento) throw 'Tipo de Documento não encontrado: ' + tipoDocKey;

							var formaPagamento = mapFormasPgto["BOL"];
							if (!formaPagamento) throw 'Forma de Pagamento BOL não encontrada';

							processId = startProcessAntigo(item, empresa, centroCusto, contaContabil, tipoDocumento, formaPagamento);

							var registerErrorAntigo = tryRegisterIntegration(token, clientKey, "true", "Indiretos antigo iniciado no fluig com sucesso, número: " + processId, idAtis, integrationId);
							var observationAntigo = registerErrorAntigo ? "Processo iniciado no Fluig, mas houve erro ao comunicar a Atiswork: " + registerErrorAntigo : "Integrado com sucesso";

							dataset.addRow([registerErrorAntigo ? "NOK" : "OK", idAtis, String(new java.text.SimpleDateFormat('yyyy-MM-dd HH:mm:ss').format(new Date())), parseInt(processId), "Indiretos Antigo", observationAntigo]);
						}

					} catch (eItem) {
						var descProcesso = "";
						if (empresa && false && String(empresa.ERPEMPRESA).toLowerCase() == "protheus") {
							descProcesso = "Indiretos Novo";
						} else {
							descProcesso = "Indiretos Antigo";
						}
						if (eItem && eItem.error) {
							eItem = eItem.error;
						}
						var registerError = tryRegisterIntegration(token, clientKey, "false", "Erro ao tentar iniciar o processo de " + descProcesso + " erro: " + eItem.toString(), idAtis, integrationId);
						var observation = registerError ? eItem.toString() + " | Erro ao comunicar a Atiswork: " + registerError : eItem.toString();
						dataset.addRow(["NOK", idAtis, String(new java.text.SimpleDateFormat('yyyy-MM-dd HH:mm:ss').format(new Date())), 0, descProcesso, observation]);
					}
				}
			}
			currentPage++;
		}

	} catch (e) {
		log.error("Erro onSync ds_atiswork: " + e);
		dataset.addRow(["NOK", "0", String(new java.text.SimpleDateFormat('yyyy-MM-dd HH:mm:ss').format(new Date())), 0, "", e.toString()]);
	}

	return dataset;
}

function startProcessNovo(item, empresa, centroCusto, contaContabil, condicoesPgto, itens) {
	var mapValues = {};
	if (item.Values) {
		for (var y = 0; y < item.Values.length; y++) {
			var kv = item.Values[y];
			if (kv && kv.Key) mapValues[kv.Key] = String(kv.Value || "").trim();
		}
	}
	var cnpjFornecedor = mapValues["CNPJ FORNECEDOR"] || "";
	var descFornecedor = mapValues["NOME FORNECEDOR"] || "";

	if (!cnpjFornecedor) throw 'CNPJ Fornecedor ausente no item ' + item.Id;

	validaLancamentoExisteNovo(item.Id, String(empresa['CNPJEMPRESA']), cnpjFornecedor, String(mapValues["NOTA FISCAL"] || ""), String(mapValues["NF - SERIE"] || ""));

	var orcamentoData = getOrcamentoIndiretos(item, centroCusto, contaContabil);

	var strDataEmissao = String(item.DataEmissao || "");
	if (strDataEmissao.indexOf("T") === -1) throw 'Não foi possível quebrar o texto ' + strDataEmissao + ' do campo DataEmissao pois ele não contem a palavra "T"';
	
	var strDataVencimento = String(item.DataVencimento || "");
	if (strDataVencimento.indexOf("T") === -1) throw 'Não foi possível quebrar o texto ' + strDataVencimento + ' do campo DataVencimento pois ele não contem a palavra "T"';

	var formData = {
		idAtisWork: String(item.Id),

		creationDate: String(new java.text.SimpleDateFormat('yyyy-MM-dd HH:mm:ss').format(new Date()).replace(" ", "T")),
		idUserCreation: String(getValue("WKUser")),
		nameUserCreation: String(fluigAPI.getUserService().getCurrent().getFullName()),
		mailUserCreation: String(fluigAPI.getUserService().getCurrent().getEmail()),

		slaRealizaValidacao: "024:00",

		codCondicaoPagamento: String(condicoesPgto["CODIGO"]),
		descCondicaoPagamento: String(condicoesPgto["CODIGO"]),

		erpEmpresa: String(empresa['ERPEMPRESA']),
		empresa: String(empresa['EMPRESA']),
		cnpjEmpresa: String(empresa['CNPJEMPRESA']),
		codigoEmpresa: String(empresa['CODIGOEMPRESA']),
		descricaoEmpresa: String(empresa['DESCRICAOEMPRESA']),
		descricaoLoja: String(empresa['DESCRICAOLOJA']),
		descricaoFilial: String(empresa['DESCRICAOFILIAL']),
		codigoFilial: String(empresa['CODIGOFILIAL']),

		orcamentoConsumido: "false",
		isentarAprovacao: "nao",
		cnpjFornecedor: String(cnpjFornecedor),
		razaoSocial: String(descFornecedor),
		tipoPessoa: "PJ",

		numeroNotaFiscal: String(mapValues["NOTA FISCAL"] || ""),

		dataEmissao: String(formatarDataBR(strDataEmissao.split("T")[0])),
		dataVencimento: String(formatarDataBR(strDataVencimento.split("T")[0])),

		especie: "NFS",
		retencao: "N",
		serieNota: String(mapValues["NF - SERIE"] || ""),
		boleto: "N",
		valorTotalNota: String(formatarDecimalBR(item.Valor)),

		preAprovacao: "Não",

		sequencialItem___1: "1",
		codItem___1: itens["CODIGO"],
		descItem___1: itens["DESCRICAO"],
		quantidadeItem___1: "1",
		valorUnitario___1: String(formatarDecimalBR(item.Valor)),
		valorTotal___1: String(formatarDecimalBR(item.Valor)),

		sequencialItemRateio___1: "1",
		percentualRateio___1: "100,00",
		valorRateio___1: String(formatarDecimalBR(item.Valor)),
		codCentroCustoRateio___1: String(centroCusto['CODIGO']),
		descCentroCustoRateio___1: String(centroCusto['DESCRICAO']),
		codContaContabilRateio___1: String(contaContabil['CODIGO']),
		descContaContabilRateio___1: String(contaContabil['DESCRICAO']),

		orcamentoMesAnoRateio___1: String(orcamentoData.mesAno),
		txtCentroCustoRateio___1: String(centroCusto['CODIGO']) + ' - ' + String(centroCusto['DESCRICAO']),
		txtContaContabilRateio___1: String(contaContabil['CODIGO']) + ' - ' + String(contaContabil['DESCRICAO']),
		orcamentoDetalheRateio___1: String(orcamentoData.orcamentoDetalhe),
		orcamentoDisponivelRateio___1: String(orcamentoData.orcamentoDisponivel),
		orcamentoAposCompraRateio___1: String(orcamentoData.orcamentoAposCompra),
		orcamentoReservadoRateio___1: String(orcamentoData.orcamentoReservado),
	};

	var attachments = [];
	if (item.Execution && item.Execution.Files) {
		for (var attr in item.Execution.Files) {
			attachments.push({
				fileName: item.Execution.Files[attr]['OriginalFullName'],
				fileContent: item.Execution.Files[attr]['Content']
			});
		}
	}
	var observation = 'Esse processo foi iniciado automaticamente pela rotina de integração entre o Fluig e a Atiswork. Id Atiswork: ' + item.Id;

	var cs = [];
	cs.push(DatasetFactory.createConstraint("ATTACHMENTS", JSON.stringify(attachments), null, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("COMPANY", 1, 1, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("PROCESSID", 'integracao_nota_fiscal_indiretos', 'integracao_nota_fiscal_indiretos', ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("CHOOSEDSTATE", 46, 46, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("COMMENTS", observation, observation, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("COMPLETETASK", true, true, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("MANAGERMODE", false, false, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("USERIDREQUESTER", getValue("WKUser"), getValue("WKUser"), ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("RESPONSIBLE", 'System:Auto', 'System:Auto', ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("DATA", JSON.stringify(formData), null, ConstraintType.MUST));

	var dsStart = DatasetFactory.getDataset("ds_ws_soap_startProcess", null, cs, null);

	if (!dsStart || dsStart.rowsCount == 0) throw 'Erro ao chamar ds_ws_soap_startProcess: Retorno vazio';
	if (dsStart.getColumnName(0) == "ERROR") throw dsStart.getValue(0, 'ERROR');

	return dsStart.getValue(0, "iProcess");
}

function startProcessAntigo(item, empresa, centroCusto, contaContabil, tipoDocumento, formaPagamento) {
	var mapValues = {};
	if (item.Values) {
		for (var y = 0; y < item.Values.length; y++) {
			var kv = item.Values[y];
			if (kv && kv.Key) mapValues[kv.Key] = String(kv.Value || "");
		}
	}

	var cnpjFornecedor = mapValues["CNPJ FORNECEDOR"] || "";
	var descFornecedor = mapValues["NOME FORNECEDOR"] || "";

	if (!cnpjFornecedor) throw 'CNPJ Fornecedor ausente no item ' + item.Id + ', CNPJ: ' + cnpjFornecedor;

	validaLancamentoExisteAntigo(item.Id, String(mapValues["NOTA FISCAL"] || ""), cnpjFornecedor, String(tipoDocumento['CODIGO']));

	var orcamentoData = getOrcamentoIndiretos(item, centroCusto, contaContabil);

	var strDataEmissao = String(item.DataEmissao || "");
	if (strDataEmissao.indexOf("T") === -1) throw 'Não foi possível quebrar o texto ' + strDataEmissao + ' do campo DataEmissao pois ele não contem a palavra "T"';
	
	var strDataVencimento = String(item.DataVencimento || "");
	if (strDataVencimento.indexOf("T") === -1) throw 'Não foi possível quebrar o texto ' + strDataVencimento + ' do campo DataVencimento pois ele não contem a palavra "T"';

	var formData = {
		decisaoAprovacao: "",
		idAtisWork: String(item.Id),
		nm_justAtraso: String("Esse processo foi iniciado automaticamente pela rotina de integração entre o Fluig e a Atiswork."),
		forceApproval: String("true"),
		cb_proc_RPA: String("Nao"),
		cb_obras_projetos: String("Nao"),
		orcamentoConsumido: String("false"),
		initiatedByFacilities: String("false"),
		fieldsRequired: String("true"),
		cb_rateio: String("Nao"),

		slaLancamento: String("024:00"),
		creationDate: String(new java.text.SimpleDateFormat('yyyy-MM-dd HH:mm:ss').format(new Date()).replace(" ", "T")),
		idUserCreation: String(getValue("WKUser")),
		nameUserCreation: String(fluigAPI.getUserService().getCurrent().getFullName()),
		mailUserCreation: String(fluigAPI.getUserService().getCurrent().getEmail()),
		numeroInstalacao: String(mapValues["ID DOCUMENTO"]),
		nm_numNF: String(zeroLeft(mapValues["NOTA FISCAL"] || "")),
		serieNota: String(mapValues["NF - SERIE"] || ""),
		dt_emissaoNF: String(strDataEmissao.split("T")[0]),
		dt_vectoNF: String(strDataVencimento.split("T")[0]),
		nm_tipoServico: String(tipoDocumento['CODIGO']),
		nm_descTipoServico: String(tipoDocumento['DESCRICAO']),
		nm_formaPagamento: String(formaPagamento['CODIGO']),
		nm_descFormaPagto: String(formaPagamento['DESCRICAO']),
		digitaDadosFormaPagamento: String(formaPagamento['DIGITA_DADOS']),
		nm_direto_cp: String(formaPagamento['CONTAS_PAGAR']),
		nm_baixa_caixa: String(formaPagamento['BAIXA_CAIXA']),
		cb_pessoa: String("PJ"),
		nm_docPessoa: String(cnpjFornecedor),
		nm_nomePessoa: String(descFornecedor),
		cb_condPagto: String("A VISTA"),
		vl_totalNF: String(formatarDecimalBR(item.Valor)),
		erpCadastro: String(empresa['ERPEMPRESA']),
		empresa: String(empresa['EMPRESA']),
		cnpjEmpresa: String(empresa['CNPJEMPRESA']),
		nm_codigoEmpresa: String(empresa['CODIGOEMPRESA']),
		nm_nomeEmpresa: String(empresa['DESCRICAOEMPRESA']),
		nm_descLoja: String(empresa['DESCRICAOLOJA']),
		nm_nomeFilial: String(empresa['DESCRICAOFILIAL']),
		nm_codigoFilial: String(empresa['CODIGOFILIAL']),

		nm_ccRateio: String(centroCusto['CODIGO']),
		nm_descCentroCustoP: String(centroCusto['DESCRICAO']),
		nm_centro_custo_principal: String(centroCusto['CODIGO']),
		desc_centro_custo_principal: String(centroCusto['DESCRICAO']),

		nm_cContabil: String(contaContabil['CODIGO']),
		nm_descContaContabil: String(contaContabil['DESCRICAO']),
		nm_conta_contabil_principal: String(contaContabil['CODIGO']),
		desc_conta_contabil_principal: String(contaContabil['DESCRICAO']),

		orcamentoDisponivel: String(orcamentoData.orcamentoDisponivel),
		orcamentoReservado: String(orcamentoData.orcamentoReservado),
		orcamentoAposCompra: String(orcamentoData.orcamentoAposCompra),
		orcamentoDetalhe: String(orcamentoData.orcamentoDetalhe),
		orcamentoMesAno: String(orcamentoData.mesAno),
		mesAnoCompra: String(orcamentoData.mesAno)
	};

	var attachments = [];
	if (item.Execution && item.Execution.Files) {
		for (var attr in item.Execution.Files) {
			attachments.push({
				fileName: item.Execution.Files[attr]['OriginalFullName'],
				fileContent: item.Execution.Files[attr]['Content']
			});
		}
	}
	var observation = 'Esse processo foi iniciado automaticamente pela rotina de integração entre o Fluig e a Atiswork. Id Atiswork: ' + item.Id;

	var cs = [];
	cs.push(DatasetFactory.createConstraint("ATTACHMENTS", JSON.stringify(attachments), null, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("COMPANY", 1, 1, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("PROCESSID", 'NFS - Lançamento de NFS e Consumo', 'NFS - Lançamento de NFS e Consumo', ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("CHOOSEDSTATE", 197, 197, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("COMMENTS", observation, observation, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("COMPLETETASK", true, true, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("MANAGERMODE", false, false, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("USERIDREQUESTER", getValue("WKUser"), getValue("WKUser"), ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("RESPONSIBLE", 'System:Auto', 'System:Auto', ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("DATA", JSON.stringify(formData), null, ConstraintType.MUST));

	var dsStart = DatasetFactory.getDataset("ds_ws_soap_startProcess", null, cs, null);

	if (!dsStart || dsStart.rowsCount == 0) throw 'Erro ao chamar ds_ws_soap_startProcess: Retorno vazio';
	if (dsStart.getColumnName(0) == "ERROR") throw dsStart.getValue(0, 'ERROR');

	return dsStart.getValue(0, "iProcess");
}

function getOrcamentoIndiretos(item, centroCusto, contaContabil) {
	var retorno = {
		orcamentoDisponivel: "0,00",
		orcamentoReservado: "0,00",
		orcamentoAposCompra: "0,00",
		orcamentoDetalhe: "",
		mesAno: ""
	};

	try {
		var months = ["JAN", "FEV", "MAR", "ABR", "MAI", "JUN", "JUL", "AGO", "SET", "OUT", "NOV", "DEZ"];
		
		var strDataEmissao = String(item.DataEmissao || "");
		if (strDataEmissao.indexOf("T") === -1) throw 'Não foi possível quebrar o texto ' + strDataEmissao + ' do campo DataEmissao pois ele não contem a palavra "T"';
		
		var dataEmissao = strDataEmissao.split("T")[0];
		
		if (dataEmissao.indexOf("-") === -1) throw 'Não foi possível quebrar o texto ' + dataEmissao + ' do campo DataEmissao formatada pois ele não contem a palavra "-"';
		var parts = dataEmissao.split("-");
		
		var ano = parts[0];
		var mes = parts[1];

		if (mes && ano) {
			retorno.mesAno = months[parseInt(mes) - 1] + "/" + ano;
		}

		var total = parseFloat(item.Valor);

		var ccCod = String(centroCusto['CODIGO']);
		var ccDesc = String(centroCusto['DESCRICAO']);
		var contaCod = String(contaContabil['CODIGO']);
		var contaDesc = String(contaContabil['DESCRICAO']);

		if (ccCod && contaCod && mes && ano && !isNaN(total)) {

			var filter = ccCod + "_COL_" + ccDesc + "_COL_" + contaCod + "_COL_" + contaDesc + "_COL_" + total;

			var constraints = new Array();
			constraints.push(DatasetFactory.createConstraint("MES", months[parseInt(mes) - 1], months[parseInt(mes) - 1], ConstraintType.MUST));
			constraints.push(DatasetFactory.createConstraint("ANO", ano, ano, ConstraintType.MUST));
			constraints.push(DatasetFactory.createConstraint("FILTER", filter, filter, ConstraintType.MUST));
			var ds = DatasetFactory.getDataset("CONSULTA-ORCAMENTO-V3", null, constraints, null);


			if (ds && ds.rowsCount > 0) {
				var ID = String(ds.getValue(0, "ID"));
				var SALDO_AC = parseFloat(ds.getValue(0, "SALDO_AC"));

				if (ID && ID != "null" && ID != "undefined") {
					var saldo = parseFloat(ds.getValue(0, "SALDO"));
					var reservado = parseFloat(ds.getValue(0, "RESERVADO"));

					retorno.orcamentoDisponivel = saldo.toFixed(2).replace(".", ",");
					retorno.orcamentoReservado = reservado.toFixed(2).replace(".", ",");
					retorno.orcamentoAposCompra = SALDO_AC.toFixed(2).replace(".", ",");

					if (SALDO_AC < 0) {
						retorno.orcamentoDetalhe = "ORÇAMENTO ESTOURADO";
					} else {
						retorno.orcamentoDetalhe = "DENTRO DO ORÇAMENTO";
					}
				} else {
					retorno.orcamentoDisponivel = "0,00";
					retorno.orcamentoReservado = "0,00";
					retorno.orcamentoAposCompra = SALDO_AC.toFixed(2).replace(".", ",");
					retorno.orcamentoDetalhe = "NÃO EXISTE ORÇAMENTO";
				}
			}
		}
	} catch (e) {
		log.error("Erro ao consultar orçamento no startProcessAntigo: " + e);
	}

	return retorno;
}

function executeQueryMap(query, keyColumn, cleanKey) {
	var ds = DatasetFactory.getDataset("ds_execute_sql", [query], null, null);
	var map = {};
	if (ds && ds.rowsCount > 0 && ds.getColumnName(0) != "ERRO") {
		for (var i = 0; i < ds.rowsCount; i++) {
			var rawKey = String(ds.getValue(i, keyColumn));
			var key = cleanKey ? rawKey.replace(/[\.\-\/]/g, "").trim() : rawKey.trim();
			var obj = {};
			for (var k = 0; k < ds.getColumnsCount(); k++) {
				obj[ds.getColumnName(k)] = String(ds.getValue(i, ds.getColumnName(k)));
			}
			map[key] = obj;
		}
	}
	return map;
}

function getEmpresasMap() {
	var query = "SELECT MLEMP.NM_CODEMPRESA AS CODIGOEMPRESA, MLEMP.NM_DESCEMPRESA AS DESCRICAOEMPRESA, MLEMP.NM_CODFILIAL AS CODIGOFILIAL, MLEMP.NM_DESCFILIAL AS DESCRICAOFILIAL, MLEMP.NM_CNPJ AS CNPJEMPRESA, MLEMP.NM_DESCLOJA AS DESCRICAOLOJA, MLEMP.ERPEMPRESA AS ERPEMPRESA, MLEMP.NM_DESCEMPRESA + ' | ' + MLEMP.NM_DESCLOJA + ' | ' + MLEMP.NM_CNPJ AS EMPRESA, MLEMP.CODCENTROCUSTO FROM ML001033(NOLOCK) MLEMP INNER JOIN DOCUMENTO(NOLOCK) DOCEMP ON DOCEMP.NUM_DOCTO_PROPRIED = MLEMP.CARDID AND DOCEMP.NR_DOCUMENTO = MLEMP.DOCUMENTID AND DOCEMP.NR_VERSAO = MLEMP.VERSION AND DOCEMP.VERSAO_ATIVA = 1 AND DOCEMP.TP_DOCUMENTO = '5' AND DOCEMP.COD_LISTA = 32";
	return executeQueryMap(query, "CNPJEMPRESA", true);
}

function getCentrosCustoMap() {
	var query = "SELECT DISTINCT ML.nm_codigo CODIGO, ML.nm_descricao DESCRICAO FROM ML001035(NOLOCK) ML INNER JOIN DOCUMENTO(NOLOCK) DOCEMP ON DOCEMP.NUM_DOCTO_PROPRIED = ML.CARDID AND DOCEMP.NR_DOCUMENTO = ML.DOCUMENTID AND DOCEMP.NR_VERSAO = ML.VERSION AND DOCEMP.VERSAO_ATIVA = 1 AND DOCEMP.TP_DOCUMENTO = '5' AND DOCEMP.COD_LISTA = 34";
	return executeQueryMap(query, "CODIGO", false);
}

function getContasContabeisMap() {
	var query = "SELECT DISTINCT ML.NM_CONTA CODIGO, ML.NM_DESCRICAO_CONTA DESCRICAO FROM ML001059(NOLOCK) ML INNER JOIN DOCUMENTO(NOLOCK) DOCEMP ON DOCEMP.NUM_DOCTO_PROPRIED = ML.CARDID AND DOCEMP.NR_DOCUMENTO = ML.DOCUMENTID AND DOCEMP.NR_VERSAO = ML.VERSION AND DOCEMP.VERSAO_ATIVA = 1 AND DOCEMP.TP_DOCUMENTO = '5' AND DOCEMP.COD_LISTA = 58";
	return executeQueryMap(query, "CODIGO", false);
}
// INDIRETOS ANTIGO INICIO
function getTiposDocumentosMap() {
	var query = "SELECT DISTINCT ML.NM_ITEM CODIGO, ML.NM_DESCRICAO DESCRICAO FROM ML001027(NOLOCK) ML INNER JOIN DOCUMENTO(NOLOCK) DOCEMP ON DOCEMP.NUM_DOCTO_PROPRIED = ML.CARDID AND DOCEMP.NR_DOCUMENTO = ML.DOCUMENTID AND DOCEMP.NR_VERSAO = ML.VERSION AND DOCEMP.VERSAO_ATIVA = 1 AND DOCEMP.TP_DOCUMENTO = '5' AND DOCEMP.COD_LISTA = 26";
	return executeQueryMap(query, "CODIGO", false);
}
function getFormasPagamentoMap() {
	var query = "SELECT DISTINCT ML.nm_codigo CODIGO, ML.nm_descricao DESCRICAO, ML.cb_contasPagar CONTAS_PAGAR, ML.cb_dadosBancarios DIGITA_DADOS, ML.cb_baixaCaixa BAIXA_CAIXA FROM ML001031(NOLOCK) ML INNER JOIN DOCUMENTO(NOLOCK) DOCEMP ON DOCEMP.NUM_DOCTO_PROPRIED = ML.CARDID AND DOCEMP.NR_DOCUMENTO = ML.DOCUMENTID AND DOCEMP.NR_VERSAO = ML.VERSION AND DOCEMP.VERSAO_ATIVA = 1 AND DOCEMP.TP_DOCUMENTO = '5' AND DOCEMP.COD_LISTA = 30";
	return executeQueryMap(query, "CODIGO", false);
}
function validaLancamentoExisteAntigo(id, numNF, docPessoa, nm_tipoServico) {
	var query = "SELECT PW.NUM_PROCES , ( CASE WHEN PW.STATUS = 0 THEN 'ABERTA' WHEN PW.STATUS = 1 THEN 'CANCELADA' ELSE 'FINALIZADA' END ) STATUS FROM PROCES_WORKFLOW PW INNER JOIN ML001038(NOLOCK) ML ON ML.CARDID = PW.NR_DOCUMENTO_CARD_INDEX AND ML.DOCUMENTID = PW.NR_DOCUMENTO_CARD AND ML.COMPANYID = PW.COD_EMPRESA CROSS APPLY ( SELECT MAX(DOC.NR_VERSAO) AS NR_VERSAO , DOC.NUM_DOCTO_PROPRIED , DOC.NR_DOCUMENTO , DOC.VERSAO_ATIVA , DOC.TP_DOCUMENTO , DOC.COD_LISTA FROM DOCUMENTO(NOLOCK) DOC WHERE DOC.NUM_DOCTO_PROPRIED = PW.NR_DOCUMENTO_CARD_INDEX AND DOC.NR_DOCUMENTO = PW.NR_DOCUMENTO_CARD AND DOC.NR_VERSAO = ML.VERSION AND DOC.VERSAO_ATIVA = 1 AND DOC.TP_DOCUMENTO = 5 AND DOC.COD_LISTA = 38 GROUP BY DOC.NUM_DOCTO_PROPRIED , DOC.NR_DOCUMENTO , DOC.NR_VERSAO , DOC.VERSAO_ATIVA , DOC.TP_DOCUMENTO , DOC.COD_LISTA ) DOC WHERE PW.COD_DEF_PROCES = 'NFS - Lançamento de NFS e Consumo' AND PW.COD_EMPRESA = 1 AND PW.STATUS NOT IN (1) AND ML.decisaoAprovacao NOT IN ('rejected', 'rejected_lanc') AND ML.nm_numNF = '" + numNF + "' AND ML.nm_docPessoa = '" + docPessoa + "' AND ML.nm_tipoServico = '" + nm_tipoServico + "'";
	var ds = DatasetFactory.getDataset("ds_execute_sql", [query], null, null);

	if (!ds || ds.rowsCount == 0) return;

	throw {
		register: false,
		error: '<p class="text-danger"><b>Atenção!</b></p><p>A solicitação <strong class="fs-cursor-pointer" data-open-detail=""> ' + ds.getValue(0, 'NUM_PROCES') + ' </strong> encontra-se cadastrada com este id da AtisWork: ' + id + ', NF: ' + numNF + ', CNPJ Fornecedor: ' + docPessoa + ', Tipo de Serviço: ' + nm_tipoServico + '</p>'
	}
}
// INDIRETOS ANTIGO FIM

// INDIRETOS NOVO INICIO
function getCondicoesPagamentoMap() {
	var query = "SELECT DISTINCT ML.codigo CODIGO , ML.descricao DESCRICAO FROM ML001295(NOLOCK) ML INNER JOIN DOCUMENTO(NOLOCK) DOCEMP ON DOCEMP.NUM_DOCTO_PROPRIED = ML.CARDID AND DOCEMP.NR_DOCUMENTO = ML.DOCUMENTID AND DOCEMP.NR_VERSAO = ML.VERSION AND DOCEMP.VERSAO_ATIVA = 1 AND DOCEMP.TP_DOCUMENTO = '5' AND DOCEMP.COD_LISTA = 295";
	return executeQueryMap(query, "CODIGO", false);
}
function getItensMap() {
	var query = "SELECT DISTINCT ML.codigo CODIGO , ML.descricao DESCRICAO FROM ML001293(NOLOCK) ML INNER JOIN DOCUMENTO(NOLOCK) DOCEMP ON DOCEMP.NUM_DOCTO_PROPRIED = ML.CARDID AND DOCEMP.NR_DOCUMENTO = ML.DOCUMENTID AND DOCEMP.NR_VERSAO = ML.VERSION AND DOCEMP.VERSAO_ATIVA = 1 AND DOCEMP.TP_DOCUMENTO = '5' AND DOCEMP.COD_LISTA = 293";
	return executeQueryMap(query, "CODIGO", false);
}
function validaLancamentoExisteNovo(id, cnpjEmpresa, cnpjFornecedor, numeroNotaFiscal, serieNota) {
	var query = "SELECT PW.NUM_PROCES , ( CASE WHEN PW.STATUS = 0 THEN 'ABERTA' WHEN PW.STATUS = 1 THEN 'CANCELADA' ELSE 'FINALIZADA' END ) STATUS FROM PROCES_WORKFLOW PW INNER JOIN ML001289(NOLOCK) ML ON ML.CARDID = PW.NR_DOCUMENTO_CARD_INDEX AND ML.DOCUMENTID = PW.NR_DOCUMENTO_CARD AND ML.COMPANYID = PW.COD_EMPRESA CROSS APPLY ( SELECT MAX(DOC.NR_VERSAO) AS NR_VERSAO , DOC.NUM_DOCTO_PROPRIED , DOC.NR_DOCUMENTO , DOC.VERSAO_ATIVA , DOC.TP_DOCUMENTO , DOC.COD_LISTA FROM DOCUMENTO(NOLOCK) DOC WHERE DOC.NUM_DOCTO_PROPRIED = PW.NR_DOCUMENTO_CARD_INDEX AND DOC.NR_DOCUMENTO = PW.NR_DOCUMENTO_CARD AND DOC.NR_VERSAO = ML.VERSION AND DOC.VERSAO_ATIVA = 1 AND DOC.TP_DOCUMENTO = 5 AND DOC.COD_LISTA = 289 GROUP BY DOC.NUM_DOCTO_PROPRIED , DOC.NR_DOCUMENTO , DOC.NR_VERSAO , DOC.VERSAO_ATIVA , DOC.TP_DOCUMENTO , DOC.COD_LISTA ) DOC WHERE PW.COD_DEF_PROCES = 'integracao_nota_fiscal_indiretos' AND PW.COD_EMPRESA = 1 AND PW.STATUS NOT IN (1) AND ML.DECISAOAPROVACAO NOT IN ('rejected') AND ML.cnpjEmpresa = '" + cnpjEmpresa + "' AND ML.numeroNotaFiscal = '" + numeroNotaFiscal + "' AND ML.serieNota = '" + serieNota + "'";
	var ds = DatasetFactory.getDataset("ds_execute_sql", [query], null, null);

	if (!ds || ds.rowsCount == 0) return;

	throw {
		register: false,
		error: '<p class="text-danger"><b>Atenção!</b></p><p>A solicitação <strong class="fs-cursor-pointer" data-open-detail=""> ' + ds.getValue(0, 'NUM_PROCES') + ' </strong> encontra-se cadastrada com este id da AtisWork: ' + id + ', NF: ' + numeroNotaFiscal + ', CNPJ Empresa: ' + cnpjEmpresa + ', CNPJ/CPF Fornecedor: ' + cnpjFornecedor + ' e Serie: ' + serieNota + '</p>'
	}
}
// INDIRETOS NOVO FIM


function getToken() {
	var ds = DatasetFactory.getDataset('ds_ws_atiswork_getToken', null, null, null);
	if (!ds || ds.rowsCount <= 0) throw 'Token não encontrado (ds_ws_atiswork_getToken).';
	var token = String(ds.getValue(0, "TOKEN"));
	if (!token || token == "null") throw 'Token inválido.';
	return token;
}

function formatarDataBR(dataAmericana) {
	if (dataAmericana == null || dataAmericana == "") return "";
	var strData = String(dataAmericana);
	if (strData.indexOf("-") === -1) throw 'Não foi possível quebrar o texto ' + strData + ' do parâmetro de Data pois ele não contem a palavra "-"';
	var partes = strData.split("-");
	return partes[2] + "/" + partes[1] + "/" + partes[0];
}

function formatarDecimalBR(valor) {
	if (valor == null || valor === "") return "0,00";

	var numero = parseFloat(valor);

	if (isNaN(numero)) return "0,00";

	var localeBR = new java.util.Locale("pt", "BR");
	var symbols = new java.text.DecimalFormatSymbols(localeBR);

	var decimalFormat = new java.text.DecimalFormat("#,##0.00", symbols);

	return decimalFormat.format(numero);
}

function registerIntegration(token, clientKey, success, observation, executionInfoId, integrationId) {
	var cs = new Array();
	cs.push(DatasetFactory.createConstraint("TOKEN", token, token, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("CLIENTKEY", clientKey, clientKey, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("SUCCESS", success, success, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("OBSERVATION", observation, observation, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("EXECUTIONINFOID", executionInfoId, executionInfoId, ConstraintType.MUST));
	cs.push(DatasetFactory.createConstraint("INTEGRATIONID", integrationId, integrationId, ConstraintType.MUST));

	var ds = DatasetFactory.getDataset("ds_ws_atiswork_register", null, cs, null);

	if (!ds || ds.rowsCount == 0) throw 'Erro ao chamar ds_ws_atiswork_register: Retorno vazio';
	if (ds.getColumnName(0) == "ERROR") throw ds.getValue(0, 'ERROR');
}
function tryRegisterIntegration(token, clientKey, success, observation, executionInfoId, integrationId) {
	try {
		registerIntegration(token, clientKey, success, observation, executionInfoId, integrationId);
		return "";
	} catch (e) {
		var msgErro = (e && e.message) ? e.message : e.toString();
		log.error("Erro ao comunicar status para Atiswork. executionInfoId: " + executionInfoId + ", integrationId: " + integrationId + ", erro: " + msgErro);
		return msgErro;
	}
}
var zeroLeft = function(value, maxLength) {
	maxLength = maxLength || 9;

	if (value === "") {
		return "";
	}

	value = String(value);

	if (value.length >= maxLength) {
		return value;
	}

	return new Array(maxLength - value.length + 1).join('0') + value;
};
