package org.example.project.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.container.ResourceInfo;
import java.io.IOException;

@Provider
public class CORSFilter implements DynamicFeature {

	@Override
	public void configure(ResourceInfo resourceInfo, FeatureContext context) {
		context.register(new javax.ws.rs.container.ContainerResponseFilter() {
			@Override
			public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
				responseContext.getHeaders().add("Access-Control-Allow-Origin", "*");  
				responseContext.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
				responseContext.getHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With");
				responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true");
			}
		});
	}
}
