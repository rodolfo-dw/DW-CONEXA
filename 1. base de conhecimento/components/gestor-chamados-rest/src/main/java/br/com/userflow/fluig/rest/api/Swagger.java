package br.com.userflow.fluig.rest.api;

import javax.ws.rs.Path;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;

@Path("/openapi.{type:json|yaml}")
public class Swagger extends OpenApiResource {

}