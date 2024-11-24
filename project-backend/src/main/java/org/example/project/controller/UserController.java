package org.example.project.controller;

import org.example.project.model.User;
import org.example.project.service.UserService;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UserController {

	private final UserService userService = new UserService();

	@GET
	public Response getUsers() {
		try {
			List<User> users = userService.getAllUsers();
			return Response.ok(users).build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
				.entity("Error fetching users: " + e.getMessage())
				.build();
		}
	}

	@GET
	@Path("/{id}")
	public Response getUserById(@PathParam("id") int id) {
		try {
			User user = userService.getUserDetails(id);
			if (user == null) {
				return Response.status(Response.Status.NOT_FOUND)
					.entity("User not found with ID: " + id)
					.build();
			}
			return Response.ok(user).build();
		} catch (Exception e) {
			e.printStackTrace();
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
				.entity("Error fetching user details: " + e.getMessage())
				.build();
		}
	}
}
