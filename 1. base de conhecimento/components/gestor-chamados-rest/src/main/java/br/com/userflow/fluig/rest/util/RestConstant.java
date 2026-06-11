package br.com.userflow.fluig.rest.util;

import java.nio.charset.StandardCharsets;

public class RestConstant {

	/**
	 * ATENÇÃO: o valor dessa chave precisa ser o mesmo de Activate.APP_KEY, no
	 * pacote sample-component-config
	 */
	public static final String APP_KEY = "6585-5698-9865-1253";
	public static final String UTF_8_ENCODE = StandardCharsets.UTF_8.name();

	/**
	 * HTTP Headers.
	 */
	public static final String REQUEST_METHOD_GET = "GET";
	public static final String REQUEST_METHOD_POST = "POST";
}
