package br.com.userflow.fluig.rest;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import br.com.userflow.fluig.rest.util.TenantContext;

public class TenantExtractorServletFilter implements Filter {

	private static final Pattern TENANT_PATTERN = Pattern.compile("^/?([^/]+)/api/v1(.*)$");

	@Override
	public void init(FilterConfig config) throws ServletException {
		System.out.println("TenantRewriteServletFilter initialized");
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		if (!(request instanceof HttpServletRequest)) {
			chain.doFilter(request, response);
			return;
		}

		HttpServletRequest httpRequest = (HttpServletRequest) request;

		try {
			String path = httpRequest.getRequestURI();
			String contextPath = httpRequest.getContextPath();
			String relativePath = path.substring(contextPath.length());

			Matcher matcher = TENANT_PATTERN.matcher(relativePath);

			if (matcher.matches()) {
				String tenantId = matcher.group(1);
				String restPath = matcher.group(2);

				try {
					Long tenantIdLong = Long.parseLong(tenantId);
					TenantContext.setTenantId(tenantIdLong);
					System.out.println(String.format("TenantRewriteServletFilter - Tenant ID set: %d", tenantIdLong));

					String newPath = "/api/v1" + restPath;

					System.out.println(String.format("TenantRewriteServletFilter - Request rewritten to: %s", newPath));

					RequestDispatcher dispatcher = request.getRequestDispatcher(newPath);
					dispatcher.forward(request, response);

					System.out.println(String.format("TenantRewriteServletFilter - Forward completed"));

				} catch (NumberFormatException e) {
					System.err
							.println(String.format("TenantRewriteServletFilter - Invalid tenant format: %s", tenantId));
					TenantContext.clear();
					chain.doFilter(request, response);
				}
			} else {
				TenantContext.clear();
				System.out.println("TenantRewriteServletFilter - No tenant in URL, context cleared");
				chain.doFilter(request, response);
			}

		} catch (Exception e) {
			System.err.println("TenantRewriteServletFilter - Error: " + e.getMessage());
			e.printStackTrace();
			TenantContext.clear();
			chain.doFilter(request, response);
		}
	}

	@Override
	public void destroy() {
		System.out.println("TenantRewriteServletFilter destroyed");
	}
}
