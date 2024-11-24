package org.example.project;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.example.project.filter.CORSFilter;

import java.net.URI;

public class Main {

	private static final String BASE_URI = "http://localhost:8080/api/";

	public static HttpServer startServer() throws Exception {
		
		final ResourceConfig config = new ResourceConfig()
			.packages("org.example.project.controller")
			.register(JacksonFeature.class) 
			.register(CORSFilter.class);
		HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), config);
		System.out.println("Server started at: " + BASE_URI);

		return server;
	}

	public static void main(String[] args) {
		try {
			final HttpServer server = startServer();
			System.out.println("Server started at: " + BASE_URI);
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				server.shutdownNow();
				System.out.println("Server stopped.");
			}));

			Thread.currentThread().join();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
