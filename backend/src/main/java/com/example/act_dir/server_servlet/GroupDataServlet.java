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
//public class GroupDataServlet extends HttpServlet {
//    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//        String groupName = request.getParameter("groupName");
//        String description = request.getParameter("description");
//        String type = request.getParameter("type");
//
//        try (Connection conn = DBConnection.getConnection()) {
//            if (groupExists(conn, groupName, description)) {
//                response.getWriter().println("Group data already exists!");
//            } else {
//                if (insertGroup(conn,type, groupName, description)) {
//                    response.getWriter().println("Group data inserted successfully!");
//                } else {
//                    response.getWriter().println("Group data insertion failed!");
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
//        response.getWriter().println("<html><body><h1>Group Data Inserted</h1></body></html>");
//    }
//
//    private boolean groupExists(Connection conn, String groupName, String description) throws SQLException {
//        String checkSql = "SELECT COUNT(*) FROM act_dit WHERE name = ? AND description = ?";
//        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
//            checkStmt.setString(1, groupName);
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
//    private boolean insertGroup(Connection conn,String type, String groupName, String description) throws SQLException {
//        String insertSql = "INSERT INTO act_dit (type,name, description,isDeleted) VALUES (?,?, ?,'NO')";
//        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
//            insertStmt.setString(1, type);
//            insertStmt.setString(2, groupName);
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
//public class GroupDataServlet extends HttpServlet {
//
//    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//        // Get parameters for multiple groups
//        String[] groupNames = request.getParameterValues("groupName");
//        String[] descriptions = request.getParameterValues("description");
//        String[] types = request.getParameterValues("type");
//
//        // Prepare lists to hold the groups
//        List<String> successfulInserts = new ArrayList<>();
//        List<String> failedInserts = new ArrayList<>();
//
//        try (Connection conn = DBConnection.getConnection()) {
//            for (int i = 0; i < groupNames.length; i++) {
//                String groupName = groupNames[i];
//                String description = descriptions[i];
//                String type = types[i];
//
//                if (groupExists(conn, groupName, description)) {
//                    failedInserts.add(groupName); // Add failed group to the list
//                } else {
//                    if (insertGroup(conn, type, groupName, description)) {
//                        successfulInserts.add(groupName); // Add successful group to the list
//                    } else {
//                        failedInserts.add(groupName); // Add failed group to the list
//                    }
//                }
//            }
//
//            // Respond based on success and failure lists
//            response.setContentType("text/html");
//            response.getWriter().println("<html><body>");
//            response.getWriter().println("<h1>Group Data Processing Results</h1>");
//            response.getWriter().println("<h2>Successfully Inserted Groups: " + successfulInserts.size() + "</h2>");
//            response.getWriter().println("<h2>Failed to Insert Groups: " + failedInserts.size() + "</h2>");
//
//            if (!failedInserts.isEmpty()) {
//                response.getWriter().println("<p>Some groups failed to insert (probably already exist).</p>");
//            }
//
//            response.getWriter().println("</body></html>");
//
//        } catch (SQLException e) {
//            response.getWriter().println("Error: Unable to connect to the database.");
//            e.printStackTrace();
//        }
//    }
//
//    private boolean groupExists(Connection conn, String groupName, String description) throws SQLException {
//        String checkSql = "SELECT COUNT(*) FROM act_dit WHERE name = ? AND description = ?";
//        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
//            checkStmt.setString(1, groupName);
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
//    private boolean insertGroup(Connection conn, String type, String groupName, String description) throws SQLException {
//        String insertSql = "INSERT INTO act_dit (type, name, description, isDeleted) VALUES (?, ?, ?, 'NO')";
//        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
//            insertStmt.setString(1, type);
//            insertStmt.setString(2, groupName);
//            insertStmt.setString(3, description);
//
//            int rowsInserted = insertStmt.executeUpdate();
//            return rowsInserted > 0;
//        }
//    }
//}
//package com.example.act_dir.server_servlet;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServlet;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.json.simple.JSONArray;
//import org.json.simple.JSONObject;
//import org.json.simple.parser.JSONParser;
//import org.json.simple.parser.ParseException;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.sql.Connection;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.util.ArrayList;
//import java.util.List;
//
//import com.example.act_dir.db.DBConnection;
//
//public class GroupDataServlet extends HttpServlet {
//    @Override
//    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
//        StringBuilder jsonData = new StringBuilder();
//        String line;
//        try (BufferedReader reader = request.getReader()) {
//            while ((line = reader.readLine()) != null) {
////                System.out.println(line+"<----------testing - 1---------------->");
//                jsonData.append(line);
//            }
//        }
////        System.out.println(jsonData +"-------testing - 2----------------->");
//        String jsonString = jsonData.toString();
////        System.out.println(jsonString +"-------testing - 3----------------->");
//        JSONParser parser = new JSONParser();
////        System.out.println(parser +"-------testing - 4----------------->");
//        JSONObject data;
//        try {
//            data = (JSONObject) parser.parse(jsonString); //org.json.simple.parser.JSONParser@50aef2f2
//        } catch (ParseException e) {
//            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
//            response.getWriter().write("Invalid JSON format");
//            return;
//        }
//
//        System.out.println(data +"----------------------just test -------------");
//
//        String type = data.containsKey("type") ? (String) data.get("type") : null;
//        if (type == null || !data.containsKey("groups")) {
//            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
//            response.getWriter().write("Invalid input: 'type' or 'groups' key is missing");
//            return;
//        }
//
//        JSONArray groups = (JSONArray) data.get("groups");
//
//        List<String> successfulInserts = new ArrayList<>();
//        List<String> failedInserts = new ArrayList<>();
//
//        try (Connection conn = DBConnection.getConnection()) {
//            for (Object groupObj : groups) {
//                JSONObject group = (JSONObject) groupObj;
////                System.out.println(group +"---------------------------testing 5-----------");
//                String groupName = (String) group.get("groupName");
//                String description = (String) group.get("description");
//
//                if (groupName == null || description == null) {
//                    failedInserts.add(groupName);
//                    continue;
//                }
//
//                if (groupExists(conn, groupName, description)) {
//                    failedInserts.add(groupName);
//                } else {
//                    if (insertGroup(conn, type, groupName, description)) {
//                        successfulInserts.add(groupName);
//                    } else {
//                        failedInserts.add(groupName);
//                    }
//                }
//            }
//            response.setStatus(HttpServletResponse.SC_OK);
////            response.getWriter().write("Successful inserts: " + successfulInserts.toString() + "\nFailed inserts: " + failedInserts.toString());
//
//        } catch (SQLException e) {
//            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
//            e.printStackTrace();
//        }
//    }
//
//    private boolean groupExists(Connection conn, String groupName, String description) throws SQLException {
//        String checkSql = "SELECT COUNT(*) FROM act_dit WHERE name = ? AND description = ?";
//        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
//            checkStmt.setString(1, groupName);
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
//    private boolean insertGroup(Connection conn, String type, String groupName, String description) throws SQLException {
//        String insertSql = "INSERT INTO act_dit (type, name, description, isDeleted) VALUES (?, ?, ?, 'NO')";
//        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
//            insertStmt.setString(1, type);
//            insertStmt.setString(2, groupName);
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
import javax.json.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import com.example.act_dir.db.DBConnection;

public class GroupDataServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        StringBuilder jsonData = new StringBuilder();
        String line;
        try (BufferedReader reader = request.getReader()) {
            while((line = reader.readLine()) != null) {
                jsonData.append(line);
            }
        }
        JsonObject data;
        try (JsonReader jsonReader = Json.createReader(new StringReader(jsonData.toString()))) {
            data = jsonReader.readObject();
        }catch(Exception e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Invalid JSON format");
            return;
        }
//        System.out.print(data +" ***********");
        String type = data.getString("type", null);
        if(type == null || !data.containsKey("groups")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Invalid input: 'type' or 'groups' key is missing");
            return;
        }

        JsonArray groups = data.getJsonArray("groups");

        List<String> successfulInserts = new ArrayList<>();
        List<String> failedInserts = new ArrayList<>();

        try(Connection conn = DBConnection.getConnection()) {
            for(JsonValue groupValue : groups) {
                JsonObject group = groupValue.asJsonObject();
                String groupName = group.getString("groupName", null);
                String description = group.getString("description", null);

                System.out.println(groupValue +"-----------------------test-1---------------------");

                if (groupName == null || description == null) {
                    failedInserts.add(groupName);
                    continue;
                }

                if (groupExists(conn, groupName, description)) {
                    failedInserts.add(groupName);
                } else {
                    if (insertGroup(conn, type, groupName, description)) {
                        successfulInserts.add(groupName);
                    } else {
                        failedInserts.add(groupName);
                    }
                }
            }
            response.setStatus(HttpServletResponse.SC_OK);
//            response.getWriter().write("Successful inserts: " + successfulInserts.toString() + "\nFailed inserts: " + failedInserts.toString());

        } catch (SQLException e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
        }
    }
    private boolean groupExists(Connection conn, String groupName, String description) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM act_dit WHERE name = ? AND description = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, groupName);
            checkStmt.setString(2, description);

            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean insertGroup(Connection conn, String type, String groupName, String description) throws SQLException {
        String insertSql = "INSERT INTO act_dit (type, name, description, isDeleted) VALUES (?, ?, ?, 'NO')";
        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
            insertStmt.setString(1, type);
            insertStmt.setString(2, groupName);
            insertStmt.setString(3, description);

            int rowsInserted = insertStmt.executeUpdate();
            return rowsInserted > 0;
        }
    }
}
