package com.example.act_dir.server_servlet;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.example.act_dir.db.DBConnection;

public class UserDataServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Read the input string from the request
        StringBuilder jsonBuffer = new StringBuilder();
        String line;
        try (BufferedReader reader = request.getReader()) {
            while ((line = reader.readLine()) != null) {
                jsonBuffer.append(line);
            }
        }

        String jsonString = jsonBuffer.toString();

        // Parse the JSON string using javax.json
        JsonObject data;
        try (JsonReader jsonReader = Json.createReader(new StringReader(jsonString))) {
            data = jsonReader.readObject();
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Invalid JSON format");
            return;
        }

        // Check for required keys ('type' and 'Users')
        String type = data.getString("type", null);
        if (type == null || !data.containsKey("Users")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Invalid input: 'type' or 'Users' key is missing");
            return;
        }

        // Retrieve the users array from the JSON data
        JsonArray users = data.getJsonArray("Users");

        // Prepare lists for tracking successful and failed inserts
        List<String> successfulInserts = new ArrayList<>();
        List<String> failedInserts = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection()) {
            // Process each user
            for (int i = 0; i < users.size(); i++) {
                JsonObject user = users.getJsonObject(i);
                String userName = user.getString("userName", null);
                String description = user.getString("description", null);

                if (userName == null || description == null) {
                    failedInserts.add(userName);
                    continue;
                }

                // Check if the user exists in the database
                if (userExists(conn, userName, description)) {
                    failedInserts.add(userName);
                } else {
                    // Insert the new user if they do not exist
                    if (insertUser(conn, type, userName, description)) {
                        successfulInserts.add(userName);
                    } else {
                        failedInserts.add(userName);
                    }
                }
            }

            // Return the result as a response
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("Successful inserts: " + successfulInserts.toString() + "\nFailed inserts: " + failedInserts.toString());

        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
        }
    }

    // Check if the user already exists in the database
    private boolean userExists(Connection conn, String userName, String description) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM act_dit WHERE name = ? AND description = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, userName);
            checkStmt.setString(2, description);

            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return true;  // User exists
                }
            }
        }
        return false;  // User does not exist
    }

    // Insert a new user into the database
    private boolean insertUser(Connection conn, String type, String userName, String description) throws SQLException {
        String insertSql = "INSERT INTO act_dit (type, name, description, isDeleted) VALUES (?, ?, ?, 'NO')";
        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
            insertStmt.setString(1, type);
            insertStmt.setString(2, userName);
            insertStmt.setString(3, description);

            int rowsInserted = insertStmt.executeUpdate();
            return rowsInserted > 0;  // Return true if insertion was successful
        }
    }
}
