package br.com.userflow.fluig.rest.security;

import java.io.IOException;
import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

@Chck
@Provider
@Priority(Priorities.AUTHORIZATION)
public class ChckFilter implements ContainerRequestFilter {
	private static final String REALM = "ufrest";
	private static final String AUTHENTICATION_SCHEME = "Bearer";

	@Override
	public void filter(ContainerRequestContext requestContext) throws IOException {

		UriInfo uriInfo = requestContext.getUriInfo();
		String path = uriInfo.getPath();
		System.out.println(String.format("RESPONSE: %d - %s: data: %s", 200, "OK", path));

		// MultivaluedMap<String, String> pathParams = uriInfo.getPathParameters(); //
		// Não mais necessário para auth

		try {
			AuthorizationManager authorizationManager = new AuthorizationManager();

			// Chama a função canAccess passando o path como o "codigoLiberado".
			// Se não tiver acesso, o authorizationManager vai lançar uma Exception.
			authorizationManager.canAccess(path,"",true);

		} catch (Exception e) {
			e.printStackTrace();
			// Se der erro (Recurso não liberado), aborta.
			abortWithUnauthorized(requestContext, e.getMessage());
		}

	}

	private void abortWithUnauthorized(ContainerRequestContext requestContext, String message) {
		String msgFinal = (message != null && !message.isEmpty()) ? message : "Acesso não autorizado ao dataset.";

		requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED)
				.header(HttpHeaders.WWW_AUTHENTICATE, AUTHENTICATION_SCHEME + " realm=\"" + REALM + "\"")
				.entity(msgFinal).build());
	}

	// Sobrecarga para manter compatibilidade interna se necessário, mas o principal
	// é o acima
	private void abortWithUnauthorized(ContainerRequestContext requestContext) {
		abortWithUnauthorized(requestContext, "Acesso não autorizado ao dataset.");
	}

}