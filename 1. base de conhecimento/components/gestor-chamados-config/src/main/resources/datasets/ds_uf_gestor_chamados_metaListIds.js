function createDataset(fields, constraints, sortFields) {

	var dataset = DatasetBuilder.newDataset();
	var dataSource = "/jdbc/AppDS";
	var ic = new javax.naming.InitialContext();
	var ds = ic.lookup(dataSource);
	var created = false;

	var conn = null;
	var stmt = null;
	var rs = null;

	try {
		var WKCompany = getValue("WKCompany");

		conn = ds.getConnection();
		stmt = conn.createStatement();
		var dbMeta = conn.getMetaData();
		var dbName = dbMeta.getDatabaseProductName();

		if (dbName.toLowerCase() == "mysql") {
			var query = "SELECT A.COD_LISTA, A.COD_LISTA_FILHO, CONCAT('ML', LPAD(A.COD_EMPRESA, 3, '0'), LPAD(A.COD_LISTA, 3, '0')) AS COD_LISTA_ML, CONCAT('ML', LPAD(A.COD_EMPRESA, 3, '0'), LPAD(A.COD_LISTA_FILHO, 3, '0')) AS COD_LISTA_FILHO_ML, A.COD_TABELA FROM ( SELECT L.COD_EMPRESA, D.COD_LISTA, L.COD_LISTA_FILHO, L.COD_TABELA FROM DOCUMENTO AS D LEFT JOIN META_LISTA_REL AS L ON D.COD_LISTA = L.COD_LISTA_PAI AND D.COD_EMPRESA = L.COD_EMPRESA WHERE D.NM_DATASET = 'ds_uf_form_gestor_chamados' AND D.VERSAO_ATIVA = 1 AND D.COD_EMPRESA = " + WKCompany + " AND L.COD_EMPRESA = " + WKCompany + " ) A";
		}

		rs = stmt.executeQuery(query);

		var agrupado = {};

		while (rs.next()) {
			var codLista = rs.getString("COD_LISTA");
			var codListaML = rs.getString("COD_LISTA_ML");
			var codFilho = rs.getString("COD_LISTA_FILHO");
			var codFilhoML = rs.getString("COD_LISTA_FILHO_ML");
			var tabela = rs.getString("COD_TABELA");

			if (!agrupado[codLista]) {
				agrupado[codLista] = {
					gestorChamadosML: codLista,
					gestorChamadosMLFull: codListaML
				};
			}
			agrupado[codLista][tabela] = codFilho;
			agrupado[codLista][tabela + "Full"] = codFilhoML;
		}

		dataset.addColumn("METALISTIDS");

		for (var cod in agrupado) {
			var obj = agrupado[cod];
			dataset.addRow([JSONUtil.toJSON(obj)]);
		}

	} catch (e) {
		log.error('Erro ao consultar o dataset ds_uf_gestor_chamados_metaListIds');
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
