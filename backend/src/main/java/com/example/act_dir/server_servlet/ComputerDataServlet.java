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
//public class ComputerDataServlet extends HttpServlet {
//    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//        String computerName = request.getParameter("computerName");
//        String description = request.getParameter("description");
//        String type = request.getParameter("type");
//
//        try (Connection conn = DBConnection.getConnection()) {
//            if (computerExists(conn, computerName, description)) {
//                response.getWriter().println("Computer data already exists!");
//            } else {
//                if (insertComputer(conn, type, computerName, description)) {
//                    response.getWriter().println("Computer data inserted successfully!");
//                } else {
//                    response.getWriter().println("Computer data insertion failed!");
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
//        response.getWriter().println("<html><body><h1>Computer Data Inserted</h1></body></html>");
//    }
//
//    private boolean computerExists(Connection conn, String computerName, String description) throws SQLException {
//        String checkSql = "SELECT COUNT(*) FROM act_dit WHERE name = ? AND description = ?";
//        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
//            checkStmt.setString(1, computerName);
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
//    private boolean insertComputer(Connection conn, String type, String computerName, String description) throws SQLException {
//        String insertSql = "INSERT INTO act_dit (type, name, description,isDeleted) VALUES (?, ?, ?,'NO')";
//        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
//            insertStmt.setString(1, type);
//            insertStmt.setString(2, computerName);
//            insertStmt.setString(3, description);
////            insertStmt.setString(4, isDeleted);
//
//            int rowsInserted = insertStmt.executeUpdate();
//            return rowsInserted > 0;
//        }
//    }
//}

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
//public class ComputerDataServlet extends HttpServlet {
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
//
//        // Convert the input string to a Map
//        String jsonString = jsonBuffer.toString();
//        Map<String, Object> data = parseJsonString(jsonString);
//
//        // Check for the presence of the required keys
//        if (!data.containsKey("type") || !data.containsKey("computers")) {
//            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
//            response.getWriter().write("Invalid input: 'type' or 'computers' key is missing");
//            return;
//        }
//
//        // Extract the type and computer data
//        String type = (String) data.get("type");
//        List<Map<String, String>> computers = (List<Map<String, String>>) data.get("computers");
//
//        // Prepare lists to hold the computers
//        List<String> successfulInserts = new ArrayList<>();
//        List<String> failedInserts = new ArrayList<>();
//
//        try (Connection conn = DBConnection.getConnection()) {
//            for (Map<String, String> computer : computers) {
//                String computerName = computer.get("computerName");
//                String description = computer.get("description");
//
//                if (computerExists(conn, computerName, description)) {
//                    failedInserts.add(computerName); // Add failed computer to the list
//                } else {
//                    if (insertComputer(conn, type, computerName, description)) {
//                        successfulInserts.add(computerName); // Add successful computer to the list
//                    } else {
//                        failedInserts.add(computerName); // Add failed computer to the list
//                    }
//                }
//            }
//            response.setStatus(HttpServletResponse.SC_OK);
//
//        } catch (SQLException e) {
//            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//            e.printStackTrace();
//            response.getWriter().write("{\"error\":\"Unable to connect to the database.\"}");
//        }
//    }
//
//    private Map<String, Object> parseJsonString(String jsonString) {
//        Map<String, Object> data = new HashMap<>();
//        List<Map<String, String>> computers = new ArrayList<>();
//        String[] entries = jsonString.replaceAll("[\\[\\]{}]", "").split(",");
//
//        String type = null;
//        Map<String, String> currentComputer = new HashMap<>();
//        for (String entry : entries) {
//            String[] keyValue = entry.split(":");
//            if (keyValue.length == 2) {
//                String key = keyValue[0].trim().replaceAll("\"", "");
//                String value = keyValue[1].trim().replaceAll("\"", "");
//                if (key.equals("type")) {
//                    type = value;
//                } else if (key.equals("computerName") || key.equals("description")) {
//                    currentComputer.put(key, value);
//                    if (currentComputer.size() == 2) {
//                        computers.add(new HashMap<>(currentComputer));
//                        currentComputer.clear();
//                    }
//                }
//            }
//        }
//
//        if (type != null) {
//            data.put("type", type);
//        }
//        data.put("computers", computers);
//
//        return data;
//    }
//
//    private boolean computerExists(Connection conn, String computerName, String description) throws SQLException {
//        String checkSql = "SELECT COUNT(*) FROM act_dit WHERE name = ? AND description = ?";
//        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
//            checkStmt.setString(1, computerName);
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
//    private boolean insertComputer(Connection conn, String type, String computerName, String description) throws SQLException {
//        String insertSql = "INSERT INTO act_dit (type, name, description, isDeleted) VALUES (?, ?, ?, 'NO')";
//        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
//            insertStmt.setString(1, type);
//            insertStmt.setString(2, computerName);
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

public class ComputerDataServlet extends HttpServlet {

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

        // Check for the presence of the required keys ('type' and 'computers')
        String type = data.getString("type", null);
        if (type == null || !data.containsKey("computers")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Invalid input: 'type' or 'computers' key is missing");
            return;
        }

        // Retrieve the computers array from the JSON data
        JsonArray computers = data.getJsonArray("computers");

        // Prepare lists for tracking successful and failed inserts
        List<String> successfulInserts = new ArrayList<>();
        List<String> failedInserts = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection()) {
            // Process each computer
            for (int i = 0; i < computers.size(); i++) {
                JsonObject computer = computers.getJsonObject(i);
                String computerName = computer.getString("computerName", null);
                String description = computer.getString("description", null);

                if (computerName == null || description == null) {
                    failedInserts.add(computerName);
                    continue;
                }

                // Check if the computer exists in the database
                if (computerExists(conn, computerName, description)) {
                    failedInserts.add(computerName);
                } else {
                    // Insert the new computer if it does not exist
                    if (insertComputer(conn, type, computerName, description)) {
                        successfulInserts.add(computerName);
                    } else {
                        failedInserts.add(computerName);
                    }
                }
            }

            // Return the result as a response
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("Successful inserts: " + successfulInserts.toString() + "\nFailed inserts: " + failedInserts.toString());

        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
            response.getWriter().write("{\"error\":\"Unable to connect to the database.\"}");
        }
    }

    // Check if the computer already exists in the database
    private boolean computerExists(Connection conn, String computerName, String description) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM act_dit WHERE name = ? AND description = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, computerName);
            checkStmt.setString(2, description);

            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return true; // Computer exists
                }
            }
        }
        return false; // Computer does not exist
    }

    // Insert a new computer into the database
    private boolean insertComputer(Connection conn, String type, String computerName, String description) throws SQLException {
        String insertSql = "INSERT INTO act_dit (type, name, description, isDeleted) VALUES (?, ?, ?, 'NO')";
        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
            insertStmt.setString(1, type);
            insertStmt.setString(2, computerName);
            insertStmt.setString(3, description);

            int rowsInserted = insertStmt.executeUpdate();
            return rowsInserted > 0; // Return true if insertion was successful
        }
    }
}
