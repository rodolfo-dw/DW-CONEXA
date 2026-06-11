package br.com.userflow.fluig.rest.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import br.com.userflow.fluig.rest.security.SessionManager;
import br.com.userflow.fluig.rest.security.Auth;
import br.com.userflow.fluig.rest.util.ErrorStatus;

@Path("/passport")
public class Passport {

	@POST
	@Path("/signin")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response signIn(UserCredentials credentials) throws Exception {

		try {

			SessionManager sessionManager = new SessionManager();
			SessionManager.SessionData sessionData = sessionManager.authenticate(credentials.getLogin(),
					credentials.getPassword());

			return Response.ok(sessionData).build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(Response.Status.UNAUTHORIZED).entity("{\"error\": \"" + e.getMessage() + "\"}")
					.build();
		}

	}

	@POST
	@Path("/signout")
	@Auth
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response signOut(@Context HttpHeaders headers) throws Exception {

		try {

			String authorizationHeader = headers.getHeaderString(HttpHeaders.AUTHORIZATION);
			if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
				return Response.status(Response.Status.BAD_REQUEST).entity("Token não fornecido").build();
			}

			String token = authorizationHeader.substring("Bearer".length()).trim();

			SessionManager sessionManager = new SessionManager();
			sessionManager.invalidateSession(token);

			String message = "Logout realizado com sucesso.";
			return Response.status(200).entity(message).build();

		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorStatus(e)).build();
		}
	}

	@POST
	@Path("/signup")
	@Auth
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response signUp() throws Exception {

		try {

			String message = "Recurso indisponível para uso.";
			return Response.status(403).entity(message).build();

		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(new ErrorStatus(e)).build();
		}
	}

	public static class UserCredentials {
		private String login;
		private String password;

		// getters e setters
		public String getLogin() {
			return login;
		}

		public void setLogin(String l) {
			this.login = l;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String p) {
			this.password = p;
		}
	}

}
