package br.com.userflow.fluig.rest.api.safe;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import br.com.userflow.fluig.rest.security.Auth;
import br.com.userflow.fluig.rest.util.ErrorStatus;

@Path("/safe/welcome")
public class Welcome {

	@GET
	@Path("/hello")
	@Auth
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response hello() throws Exception {

		try {

			String message = "Hi! Wellcome to Safe API REST UF";
			/*System.out.println(String.format("RESPONSE: %d - %s: data: %s", 200, "OK", message));*/
			return Response.status(200).entity(message).build();

		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorStatus(e)).build();
		}
	}

}
