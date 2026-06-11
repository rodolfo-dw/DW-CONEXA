package br.com.userflow.fluig.rest.api.safe;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.Map;

import br.com.userflow.fluig.rest.util.fluig.ProcessRestService;
import br.com.userflow.fluig.rest.security.Auth;
import br.com.userflow.fluig.rest.security.Chck;
import br.com.userflow.fluig.rest.util.ErrorStatus;

@Path("/safe/process")
public class Process {

	@POST
	@Path("/start/{name}")
	@Auth
	@Chck
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response startProcess(@PathParam("name") String name, Map<String, Object> params) throws Exception {
		try {
			ProcessRestService processFluigService = new ProcessRestService();
			String result = processFluigService.startProcess(name, params);

			/*System.out.println(String.format("RESPONSE: %d - %s: data: %s", 200, "OK", result));*/
			return Response.status(200).entity(result).build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorStatus(e)).build();
		}
	}

	@PUT
	@Path("/move/{instanceId}")
	@Auth
	@Chck
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response moveProcess(@PathParam("instanceId") int instanceId, Map<String, Object> params) throws Exception {

		try {
			ProcessRestService processFluigService = new ProcessRestService();

			String result = processFluigService.moveProcess(instanceId, params);

			/*System.out.println(String.format("RESPONSE: %d - %s: data: %s", 200, "OK", result));*/
			return Response.status(200).entity(result).build();

		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorStatus(e)).build();
		}
	}
}