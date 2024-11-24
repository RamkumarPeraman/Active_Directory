package org.example.project.dao;

import org.example.project.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {
	private final String jdbcURL = "jdbc:mysql://localhost:3306/act_dir";
	private final String jdbcUsername = "ram";
	private final String jdbcPassword = "Dudububu@27";

	public List<User> getAllUsers() {
		List<User> users = new ArrayList<>();
		try (Connection connection = DriverManager.getConnection(jdbcURL, jdbcUsername, jdbcPassword)) {
			String query = "SELECT id, first_name from users";
			PreparedStatement statement = connection.prepareStatement(query);
			ResultSet resultSet = statement.executeQuery();

			while (resultSet.next()) {
				User user = new User();
				user.setId(resultSet.getInt("id"));
				user.setFirstName(resultSet.getString("first_name"));
				users.add(user);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return users;
	}

	public User getUserById(int userId) {
		User user = null;
		try (Connection connection = DriverManager.getConnection(jdbcURL, jdbcUsername, jdbcPassword)) {
			String query = "SELECT id, first_name, last_name, phone_number FROM users WHERE id = ?";
			PreparedStatement statement = connection.prepareStatement(query);
			statement.setInt(1, userId);
			ResultSet resultSet = statement.executeQuery();

			if (resultSet.next()) {
				user = new User();
				user.setId(resultSet.getInt("id"));
				user.setFirstName(resultSet.getString("first_name"));
				user.setLastName(resultSet.getString("last_name"));
				user.setPhoneNumber(resultSet.getString("phone_number"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return user;
	}
}
