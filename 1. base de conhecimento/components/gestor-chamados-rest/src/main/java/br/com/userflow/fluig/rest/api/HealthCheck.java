package br.com.userflow.fluig.rest.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import br.com.userflow.fluig.rest.util.ErrorStatus;

@Path("/healthcheck")
public class HealthCheck {

	@GET
	@Path("/ping")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response ping() throws Exception {

		try {

			String message = "pong";
			System.out.println(String.format("RESPONSE: %d - %s: data: %s", 200, "OK", message));
			return Response.status(200).entity(message).build();

		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorStatus(e)).build();
		}
	}

}