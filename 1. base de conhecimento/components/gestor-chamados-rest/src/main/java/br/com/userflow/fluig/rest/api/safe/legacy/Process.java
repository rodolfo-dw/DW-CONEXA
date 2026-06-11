package br.com.userflow.fluig.rest.api.safe.legacy;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.userflow.fluig.rest.util.fluig.ProcessSoapService;
import br.com.userflow.fluig.rest.util.fluig.soap.generated.StringArrayArray;
import br.com.userflow.fluig.rest.util.fluig.soap.generated.StringArray;
import br.com.userflow.fluig.rest.security.Auth;
import br.com.userflow.fluig.rest.security.Chck;
import br.com.userflow.fluig.rest.util.ErrorStatus;

@Path("/safe/legacy/process")
public class Process {
	/*
	@POST
	@Path("/start")
	@Auth
	@Chck
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response startProcessWS(ProcessSoapService.StartProcessParams params) throws Exception {

		try {

			if (params == null || params.processId == null || params.processId.isEmpty()) {
				return Response.status(Response.Status.BAD_REQUEST)
						.entity(new ErrorStatus("Parâmetro 'processId' é obrigatório", "BadRequestException", null))
						.build();
			}

			ProcessSoapService processSoapService = new ProcessSoapService();
			StringArrayArray result = processSoapService.startProcess(params);

			System.out.println(String.format("RESPONSE: %d - %s: Processo iniciado: %s", 200, "OK", params.processId));
			return Response.status(200).entity(result).build();

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(String.format("RESPONSE: %d - %s: %s", 500, "ERRO", e.getMessage()));
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorStatus(e)).build();
		}
	}

	@POST
	@Path("/simple-start")
	@Auth
	@Chck
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response simpleStartProcessWS(ProcessSoapService.SimpleStartProcessParams params) throws Exception {

		try {

			if (params == null || params.processId == null || params.processId.isEmpty()) {
				return Response.status(Response.Status.BAD_REQUEST)
						.entity(new ErrorStatus("Parâmetro 'processId' é obrigatório", "BadRequestException", null))
						.build();
			}

			ProcessSoapService processSoapService = new ProcessSoapService();
			StringArray result = processSoapService.simpleStartProcess(params);

			System.out.println(
					String.format("RESPONSE: %d - %s: Processo simples iniciado: %s", 200, "OK", params.processId));
			return Response.status(200).entity(result).build();

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(String.format("RESPONSE: %d - %s: %s", 500, "ERRO", e.getMessage()));
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorStatus(e)).build();
		}
	}

	@POST
	@Path("/move")
	@Auth
	@Chck
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response moveProcessWS(ProcessSoapService.SaveAndSendTaskParams params) throws Exception {

		try {

			if (params == null || params.processInstanceId <= 0) {
				return Response.status(Response.Status.BAD_REQUEST)
						.entity(new ErrorStatus("Parâmetro 'processInstanceId' é obrigatório e deve ser maior que 0",
								"BadRequestException", null))
						.build();
			}

			ProcessSoapService processSoapService = new ProcessSoapService();
			StringArrayArray result = processSoapService.saveAndSendTask(params);

			System.out.println(
					String.format("RESPONSE: %d - %s: Processo movido: %d", 200, "OK", params.processInstanceId));
			return Response.status(200).entity(result).build();

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(String.format("RESPONSE: %d - %s: %s", 500, "ERRO", e.getMessage()));
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorStatus(e)).build();
		}
	}
	*/
}
