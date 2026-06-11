package br.com.userflow.fluig.rest;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@ApplicationPath("/api/v1")
public class ApplicationConfig extends Application {

	public ApplicationConfig() {
		super();
		initOpenApi();
	}

	private void initOpenApi() {

		OpenAPI oas = new OpenAPI();
		Info info = new Info().title("API REST UserFlow").description("Documentação dos serviços integrados ao Fluig")
				.version("1.0.0");
		oas.info(info);

		SwaggerConfiguration oasConfig = new SwaggerConfiguration().openAPI(oas).prettyPrint(true)
				.resourcePackages(Stream.of("br.com.userflow.fluig.rest.api").collect(Collectors.toSet()));

		try {
			new io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder().openApiConfiguration(oasConfig)
					.buildContext(true);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
