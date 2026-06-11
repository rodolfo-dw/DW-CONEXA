function createDataset(fields, constraints, sortFields) {
	var dataset = DatasetBuilder.newDataset();
	try {
		var processInstanceId = null;
		var taskUserId = null;

		if (constraints && constraints.length && constraints.length > 0) {
			for (attr in constraints) {
				var field = constraints[attr]['fieldName'].toUpperCase();
				var value = constraints[attr]['initialValue'];
				if (field == "PROCESSINSTANCEID") {
					processInstanceId = value;
				}
				if (field == "TASKUSERID") {
					taskUserId = value;
				}
			}
		}
		if (processInstanceId == null) {
			throw 'O Preenchimento da constraint PROCESSINSTANCEID é obrigatório';
		}
		if (taskUserId == null) {
			throw 'O Preenchimento da constraint TASKUSERID é obrigatório';
		}

		var data = {
			"companyId": getValue("WKCompany")+'',
			"serviceCode": 'UF_FluigAPI'+'',
			"endpoint": '/ecm/api/rest/ecm/workflowView/findDetailsMyRequests',
			"method": 'POST'+'',
			"timeoutService": "100"+'',
			"options": {
				"encoding": 'UTF-8'+'',
				"mediaType": 'application/json'+'',
				"useSSL": true
			},
			"headers": {
				"Content-Type": 'application/json;charset=UTF-8'+''
			},
			"params": {
				"processInstanceId": Number(processInstanceId),
				"taskUserId": String(taskUserId)
			},
		}
		var service = fluigAPI.getAuthorizeClientService();
		var vo = service.invoke(JSON.stringify(data));

		if (vo.getResult() == null || vo.getResult().isEmpty()) {
			throw 'Ocorreu um erro desconhecido';
		}
		if (vo.getHttpStatusResult() < 200 || vo.getHttpStatusResult() >= 300) {
			throw vo.getResult();
		}

		dataset.addColumn("SUCCESS");
		dataset.addRow(new Array(vo.getResult()));
	} catch (e) {
		dataset.addColumn("ERROR");
		dataset.addRow(new Array('Houve um erro inesperado ao executar o dataset. ' + e.toString()));
	}
	return dataset;
}