//package com.example.act_dir.server_servlet;
//
//import java.io.IOException;
//import java.sql.Connection;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import com.example.act_dir.db.DBConnection;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServlet;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//
//public class UserDataServlet extends HttpServlet {
//    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//        String userName = request.getParameter("userName");
//        String description = request.getParameter("description");
//        String type = request.getParameter("type");
//
//        try (Connection conn = DBConnection.getConnection()) {
//            if (userExists(conn, userName, description)) {
//                response.getWriter().println("Data already exists!");
//            } else {
//                if (insertUser(conn, type, userName, description)) {
//                    response.getWriter().println("Data inserted successfully!");
//                } else {
//                    response.getWriter().println("Data insertion failed!");
//                }
//            }
//        } catch (SQLException e) {
//            response.getWriter().println("Error: Unable to connect to the database.");
//            e.printStackTrace();
//        }
//    }
//
//    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//        // Set the response content type
//        response.setContentType("text/html");
//
//        // Write the response
//        response.getWriter().println("<html><body><h1>Data Inserted</h1></body></html>");
//    }
//
//    // Modify this method if you need to check more fields (e.g., sn, telephoneNumber, etc.)
//    private boolean userExists(Connection conn, String userName, String description) throws SQLException {
//        String checkSql = "SELECT COUNT(*) FROM act_dit WHERE name = ? AND description = ?";
//        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
//            checkStmt.setString(1, userName);
//            checkStmt.setString(2, description);
//
//            try (ResultSet rs = checkStmt.executeQuery()) {
//                if (rs.next() && rs.getInt(1) > 0) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }
//
//    // Modify this method if you want to insert more fields like sn, telephoneNumber, etc.
//    private boolean insertUser(Connection conn, String type, String userName, String description) throws SQLException {
//        String insertSql = "INSERT INTO act_dit (type, name, description,isDeleted) VALUES (?, ?, ?,'NO')";
//        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
//            insertStmt.setString(1, type);
//            insertStmt.setString(2, userName);
//            insertStmt.setString(3, description);
//
//            int rowsInserted = insertStmt.executeUpdate();
//            return rowsInserted > 0;
//        }
//    }
//}
//


//package com.example.act_dir.server_servlet;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.sql.Connection;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import com.example.act_dir.db.DBConnection;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServlet;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//
//public class UserDataServlet extends HttpServlet {
//    @Override
//    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//        // Read the input string from the request
//        StringBuilder jsonBuffer = new StringBuilder();
//        String line;
//        try (BufferedReader reader = request.getReader()) {
//            while ((line = reader.readLine()) != null) {
//                jsonBuffer.append(line);
//            }
//        }
//        System.out.println(jsonBuffer + "<---------------- for testing ------------>");
//        // Convert the input string to a Map
//        String jsonString = jsonBuffer.toString();
//        Map<String, Object> data = parseJsonString(jsonString);
//        System.out.print(data + "------------------------");
//        // Check for the presence of the required keys
//        if (!data.containsKey("type") || !data.containsKey("users")) {
//            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
//            response.getWriter().write("Invalid input: 'type' or 'users' key is missing");
//            return;
//        }
//        String type = (String) data.get("type");
//        List<Map<String, String>> users = (List<Map<String, String>>) data.get("users");
//
//        // Prepare lists to hold the users
//        List<String> successfulInserts = new ArrayList<>();
//        List<String> failedInserts = new ArrayList<>();
//
//        try (Connection conn = DBConnection.getConnection()) {
//            for (Map<String, String> user : users) {
//                String userName = user.get("userName");
//                String description = user.get("description");
//
//                if (userExists(conn, userName, description)) {
//                    failedInserts.add(userName);
//                } else {
//                    if (insertUser(conn, type, userName, description)) {
//                        successfulInserts.add(userName);
//                    } else {
//                        failedInserts.add(userName);
//                    }
//                }
//            }
//            response.setStatus(HttpServletResponse.SC_OK);
//
//        } catch (SQLException e) {
//            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//            e.printStackTrace();
//        }
//    }
//
//    private Map<String, Object> parseJsonString(String jsonString) {
//        Map<String, Object> data = new HashMap<>();
//        List<Map<String, String>> users = new ArrayList<>();
//        String[] entries = jsonString.replaceAll("[\\[\\]{}]", "").split(",");
//
//        String type = null;
//        Map<String, String> currentUser = new HashMap<>();
//        for (String entry : entries) {
//            System.out.println(entry + "<------------for testing ------------->");
//            String[] keyValue = entry.split(":");
//            if (keyValue.length == 2) {
//                String key = keyValue[0].trim().replaceAll("\"", "");
//                String value = keyValue[1].trim().replaceAll("\"", "");
//                if (key.equals("type")) {
//                    type = value;
//                } else if (key.equals("userName") || key.equals("description")) {
//                    currentUser.put(key, value);
//                    if (currentUser.size() == 2) {
//                        users.add(new HashMap<>(currentUser));
//                        currentUser.clear();
//                    }
//                }
//            }
//        }
//
//        if (type != null) {
//            data.put("type", type);
//        }
//        data.put("users", users);
//
//        System.out.println(data + " its for testing---------------");
//        return data;
//    }
//
//    private boolean userExists(Connection conn, String userName, String description) throws SQLException {
//        String checkSql = "SELECT COUNT(*) FROM act_dit WHERE name = ? AND description = ?";
//        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
//            checkStmt.setString(1, userName);
//            checkStmt.setString(2, description);
//
//            try (ResultSet rs = checkStmt.executeQuery()) {
//                if (rs.next() && rs.getInt(1) > 0) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }
//
//    private boolean insertUser(Connection conn, String type, String userName, String description) throws SQLException {
//        String insertSql = "INSERT INTO act_dit (type, name, description, isDeleted) VALUES (?, ?, ?, 'NO')";
//        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
//            insertStmt.setString(1, type);
//            insertStmt.setString(2, userName);
//            insertStmt.setString(3, description);
//
//            int rowsInserted = insertStmt.executeUpdate();
//            return rowsInserted > 0;
//        }
//    }
//}

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
