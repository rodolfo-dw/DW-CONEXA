function createDataset(fields, constraints, sortFields) {
	const dataset = DatasetBuilder.newDataset();
	try {
		if (!constraints || !constraints.length || constraints.length == 0) throw 'Não foi encontrado nenhuma constraint, reveja os parâmetros e envie novamente.';

		let startDate = '';
		let endDate = '';
		let periodId = '';

		for (attr in constraints) {
			let field = constraints[attr]['fieldName'].toUpperCase();
			let value = constraints[attr]['initialValue'];
			if (field == "STARTDATE") {
				startDate = value;
			}
			if (field == "ENDDATE") {
				endDate = value;
			}
			if (field == "PERIODID") {
				periodId = value;
			}
		}
		if (!startDate || startDate == null || startDate == '') throw 'Obrigatório o envio da constraint STARTDATE.';
		if (!endDate || endDate == null || endDate == '') throw 'Obrigatório o envio da constraint ENDDATE.';
		if (!periodId || periodId == null || periodId == '') throw 'Obrigatório o envio da constraint PERIODID.';

		const clientService = fluigAPI.getAuthorizeClientService();
		const data = {
			companyId: getValue("WKCompany") + '',
			serviceCode: 'UF_FluigAPI' + '',
			endpoint: '/process-management/api/v2/date-calculator/duration?startDate=' + startDate + '&endDate=' + endDate + '&periodId=' + periodId,
			method: 'get',
		}

		const vo = clientService.invoke(JSON.stringify(data));

		if (vo.getResult() == null || vo.getResult().isEmpty()) throw "Retorno da API vazio";
		if (vo.httpStatusResult != 200) throw vo.getResult();

		const dataRet = JSON.parse(vo.getResult());

		dataset.addColumn("totalInSeconds");
		dataset.addColumn("totalInMinutes");
		dataset.addColumn("totalInHours");
		dataset.addColumn("totalInDays");
		dataset.addColumn("countdownSeconds");
		dataset.addColumn("countdownMinutes");
		dataset.addColumn("countdownHours");
		dataset.addColumn("countdownDays");
		dataset.addRow([
			dataRet.totalInSeconds + '',
			dataRet.totalInMinutes + '',
			dataRet.totalInHours + '',
			dataRet.totalInDays + '',
			dataRet.countdownSeconds + '',
			dataRet.countdownMinutes + '',
			dataRet.countdownHours + '',
			dataRet.countdownDays + ''
		]);
	} catch (e) {
		dataset.addColumn("ERROR");
		dataset.addRow(new Array(e));
	} finally {
		return dataset;
	}
}