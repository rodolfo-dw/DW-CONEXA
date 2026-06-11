package br.com.userflow.fluig.rest.api.safe;

import java.io.InputStream;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import br.com.userflow.fluig.rest.security.Auth;
import br.com.userflow.fluig.rest.security.Chck;
import br.com.userflow.fluig.rest.util.ErrorStatus;
import br.com.userflow.fluig.rest.util.fluig.DocumentRestService;

@Path("/safe/documents")
public class Document {

	@POST
	@Path("/upload/{fileName}/{folderName}/{processInstanceId}/publish")
	@Auth
	@Chck
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadAndPublish(@PathParam("fileName") String fileName, @PathParam("folderName") String folderName,
			@PathParam("processInstanceId") Integer processInstanceId, MultipartFormDataInput input) {

		try {
			DocumentRestService documentService = new DocumentRestService();

			InputStream fileStream = input.getFormDataPart("file", InputStream.class, null);

			String result = documentService.uploadAndPublish(fileName, folderName, processInstanceId, fileStream);

			return Response.status(200).entity(result).build();

		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorStatus(e)).build();
		}
	}
}