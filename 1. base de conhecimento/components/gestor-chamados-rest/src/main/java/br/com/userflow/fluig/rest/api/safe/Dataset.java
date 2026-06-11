package br.com.userflow.fluig.rest.api.safe;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import br.com.userflow.fluig.rest.security.Auth;
import br.com.userflow.fluig.rest.security.Chck;
import br.com.userflow.fluig.rest.util.ErrorStatus;
import br.com.userflow.fluig.rest.util.fluig.DatasetRestService;

@Path("/safe/dataset")
public class Dataset {

	@POST
	@Path("/")
	@Auth
	@Chck
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response getDataset(DatasetRestService.SearchDatasetParams params) throws Exception {

		try {
			DatasetRestService datasetFluigService = new DatasetRestService();

			String jsonResponse = datasetFluigService.searchDataset(params, false);

			return Response.status(Response.Status.OK).entity(jsonResponse).build();

		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorStatus(e)).build();
		}
	}

}
