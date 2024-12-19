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
//public class DeletedObjDataServlet extends HttpServlet {
//    @Override
//    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//        String objectName = request.getParameter("objectName");
//        String description = request.getParameter("description");
//        String type = request.getParameter("type");
//        System.out.print(objectName+"----------------");
//        response.setContentType("text/plain");
//        try (Connection conn = DBConnection.getConnection()) {
//            if (objectExists(conn, objectName, description)) {
//                response.getWriter().println("Object data already exists!");
//            } else {
//                if (insertObject(conn, type, objectName, description)) {
//                    response.getWriter().println("Object data inserted successfully!");
//                } else {
//                    response.getWriter().println("Object data insertion failed!");
//                }
//            }
//        } catch (SQLException e) {
//            response.getWriter().println("Error: Unable to connect to the database.");
//            e.printStackTrace();
//        }
//    }
//
//    @Override
//    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//        // Set the response content type
//        response.setContentType("text/html");
//        response.getWriter().println("<html><body><h1>Insert Object Data</h1></body></html>");
//    }
//
//    private boolean objectExists(Connection conn, String objectName, String description) throws SQLException {
//        String checkSql = "SELECT COUNT(*) FROM act_dit WHERE name = ? AND description = ?";
//        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
//            checkStmt.setString(1, objectName);
//            checkStmt.setString(2, description);
//            System.out.print(objectName+"----------------");
//            try (ResultSet rs = checkStmt.executeQuery()) {
//                if (rs.next() && rs.getInt(1) > 0) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }
//
//    private boolean insertObject(Connection conn, String type, String objectName, String description) throws SQLException {
//        String insertSql = "INSERT INTO act_dit (type, name, description,isDeleted) VALUES (?, ?, ?,'YES')";
//        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
//            insertStmt.setString(1, type);
//            insertStmt.setString(2, objectName);
//            insertStmt.setString(3, description);
//            System.out.print(objectName+"----------------");
//            int rowsInserted = insertStmt.executeUpdate();
//            return rowsInserted > 0;
//        }
//    }
//}


//package com.example.act_dir.server_servlet;
//
//import java.io.IOException;
//import java.sql.Connection;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.util.ArrayList;
//import java.util.List;
//import com.example.act_dir.db.DBConnection;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServlet;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//
//public class DeletedObjDataServlet extends HttpServlet {
//
//    @Override
//    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//        // Get parameters for multiple objects
//        String[] objectNames = request.getParameterValues("objectName");
//        String[] descriptions = request.getParameterValues("description");
//        String[] types = request.getParameterValues("type");
//
//        // Prepare lists to hold the objects
//        List<String> successfulInserts = new ArrayList<>();
//        List<String> failedInserts = new ArrayList<>();
//
//        response.setContentType("text/plain");
//        try (Connection conn = DBConnection.getConnection()) {
//            for (int i = 0; i < objectNames.length; i++) {
//                String objectName = objectNames[i];
//                String description = descriptions[i];
//                String type = types[i];
//
//                if (objectExists(conn, objectName, description)) {
//                    failedInserts.add(objectName); // Add failed object to the list
//                } else {
//                    if (insertObject(conn, type, objectName, description)) {
//                        successfulInserts.add(objectName); // Add successful object to the list
//                    } else {
//                        failedInserts.add(objectName); // Add failed object to the list
//                    }
//                }
//            }
//
//            // Do not send any output
//            response.setStatus(HttpServletResponse.SC_OK); // Set status to OK
//
//        } catch (SQLException e) {
//            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // Set status to error
//            e.printStackTrace();
//        }
//    }
//
//    private boolean objectExists(Connection conn, String objectName, String description) throws SQLException {
//        String checkSql = "SELECT COUNT(*) FROM act_dit WHERE name = ? AND description = ?";
//        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
//            checkStmt.setString(1, objectName);
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
//    private boolean insertObject(Connection conn, String type, String objectName, String description) throws SQLException {
//        String insertSql = "INSERT INTO act_dit (type, name, description, isDeleted) VALUES (?, ?, ?, 'YES')";
//        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
//            insertStmt.setString(1, type);
//            insertStmt.setString(2, objectName);
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

public class DeletedObjDataServlet extends HttpServlet {

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

        // Check for the presence of the 'deletedObjects' key
        if (!data.containsKey("deletedObjects")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("'deletedObjects' key is missing");
            return;
        }

        JsonArray deletedObjects = data.getJsonArray("deletedObjects");
        List<String> successfulInserts = new ArrayList<>();
        List<String> failedInserts = new ArrayList<>();

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            for (int i = 0; i < deletedObjects.size(); i++) {
                JsonObject deletedObject = deletedObjects.getJsonObject(i);
                String type = deletedObject.getString("type", null);
                String objectName = null;
                String description = deletedObject.getString("description", null);

                if (type != null) {
                    switch (type.toLowerCase()) {
                        case "user":
                            objectName = deletedObject.getString("userName", null);
                            break;
                        case "group":
                            objectName = deletedObject.getString("groupName", null);
                            break;
                        case "computer":
                            objectName = deletedObject.getString("computerName", null);
                            break;
                        default:
                            continue;
                    }
                }

                if (objectName == null || description == null) {
                    failedInserts.add(objectName != null ? objectName : type);
                    continue;
                }

                // Check if the object already exists in the database
                if (objectExists(conn, objectName, description)) {
                    failedInserts.add(objectName);
                } else {
                    // Insert the object into the database
                    if (insertDeletedObject(conn, type, objectName, description)) {
                        successfulInserts.add(objectName);
                    } else {
                        failedInserts.add(objectName);
                    }
                }
            }

            conn.commit();
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write("Successful inserts: " + successfulInserts.toString() + "\nFailed inserts: " + failedInserts.toString());

        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
        } finally {
            try {
                DBConnection.getConnection().setAutoCommit(true);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // Check if the object already exists in the database
    private boolean objectExists(Connection conn, String objectName, String description) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM act_dit WHERE name = ? AND description = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, objectName);
            checkStmt.setString(2, description);

            try (ResultSet rs = checkStmt.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    // Insert the deleted object into the database
    private boolean insertDeletedObject(Connection conn, String type, String objectName, String description) throws SQLException {
        String insertSql = "INSERT INTO act_dit (type, name, description, isDeleted) VALUES (?, ?, ?, 'YES')";
        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
            insertStmt.setString(1, type);
            insertStmt.setString(2, objectName);
            insertStmt.setString(3, description);

            int rowsInserted = insertStmt.executeUpdate();
            return rowsInserted > 0;
        }
    }
}
