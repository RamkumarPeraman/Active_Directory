package org.example.project.service;

import org.example.project.dao.UserDao;
import org.example.project.model.User;

import java.sql.SQLException;
import java.util.List;

public class UserService {
	private final UserDao userDao = new UserDao();

	public List<User> getAllUsers() throws SQLException {
		return userDao.getAllUsers();
	}

	public User getUserDetails(int id) throws SQLException {
		return userDao.getUserById(id);
	}
}