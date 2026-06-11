package br.com.userflow.fluig.rest.util.fluig;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.fluig.sdk.exception.ApplicationKeyNotFoundException;

import br.com.userflow.fluig.rest.security.AuthorizationManager;
import br.com.userflow.fluig.rest.util.TenantContext;
import br.com.userflow.fluig.rest.util.fluig.DatasetRestService.ConstraintParams;
import br.com.userflow.fluig.rest.util.fluig.DatasetRestService.SearchDatasetParams;
import oauth.signpost.commonshttp.CommonsHttpOAuthConsumer;

public class DocumentRestService extends RestService {

	private boolean checkAuthorization = true;

	public DocumentRestService() throws Exception, ApplicationKeyNotFoundException {
		super();
	}

	@Deprecated
	public DocumentRestService(Long tenantId) throws Exception, ApplicationKeyNotFoundException {
		super(tenantId);
	}

	public void setCheckAuthorization(boolean checkAuthorization) {
		this.checkAuthorization = checkAuthorization;
	}

	public String uploadAndPublish(String fileName, String folderNameParam, Integer processInstanceId,
			InputStream fileStream) throws Exception {

		if (this.checkAuthorization && processInstanceId != 0) {
			try {
				AuthorizationManager authorizationManager = new AuthorizationManager();
				authorizationManager.canMove(String.valueOf(processInstanceId));
			} catch (Exception e) {
				throw new Exception("Erro de Autorização (Processo " + processInstanceId + "): " + e.getMessage());
			}
		}

		Integer rootId = getRootAttachFolder();

		String targetFolderName;
		if (processInstanceId == 0) {
			if (folderNameParam == null || folderNameParam.trim().isEmpty()) {
				throw new Exception("Para processo 0, o nome da pasta (folderName) é obrigatório.");
			}
			targetFolderName = folderNameParam;
		} else {
			targetFolderName = "Solicitação " + processInstanceId;
		}

		FolderService folderService = new FolderService();
		Integer targetFolderId = folderService.getOrCreateFolder(rootId, targetFolderName);

		String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.toString()).replace("+", "%20");

		String endpoint = fluigURL + "/content-management/api/v2/documents/upload/" + encodedFileName + "/"
				+ targetFolderId + "/publish";

		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			HttpPost uploadRequest = new HttpPost(endpoint);

			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
			builder.setCharset(StandardCharsets.UTF_8);

			builder.addBinaryBody("file", fileStream, ContentType.DEFAULT_BINARY, fileName);

			HttpEntity multipart = builder.build();
			uploadRequest.setEntity(multipart);

			CommonsHttpOAuthConsumer apacheConsumer = new CommonsHttpOAuthConsumer(this.consumer.getConsumerKey(),
					this.consumer.getConsumerSecret());
			apacheConsumer.setTokenWithSecret(this.consumer.getToken(), this.consumer.getTokenSecret());

			apacheConsumer.sign(uploadRequest);

			HttpResponse response = httpClient.execute(uploadRequest);
			HttpEntity responseEntity = response.getEntity();
			String responseString = EntityUtils.toString(responseEntity, StandardCharsets.UTF_8);

			int statusCode = response.getStatusLine().getStatusCode();

			if (statusCode < 200 || statusCode >= 300) {
				throw new Exception("Erro Fluig [" + statusCode + "]: " + responseString);
			}

			return responseString;
		}
	}

	private Integer getRootAttachFolder() throws Exception {
		Long tenantId = TenantContext.getTenantId();

		DatasetRestService datasetService = new DatasetRestService();
		datasetService.setCheckAuthorization(false);

		SearchDatasetParams params = new SearchDatasetParams();
		params.name = "ds_uf_form_parametros_globais";
		params.fields = new ArrayList<>();
		params.fields.add("attachFolder");

		params.constraints = new ArrayList<>();
		params.constraints.add(new ConstraintParams("companyid", String.valueOf(tenantId)));
		params.constraints.add(new ConstraintParams("metadata#active", "true"));

		String jsonResult = datasetService.searchDataset(params, false);
		List<Map<String, Object>> rows = datasetService.formatValuesDataset(jsonResult);

		if (rows != null && !rows.isEmpty()) {
			Object folderObj = rows.get(0).get("attachFolder");
			if (folderObj != null && !folderObj.toString().isEmpty()) {
				String idStr = String.valueOf(folderObj).replace(".0", "");
				return Integer.parseInt(idStr);
			}
		}

		throw new Exception("Pasta raiz de anexos (attachFolder) não configurada nos Parâmetros Globais.");
	}
}