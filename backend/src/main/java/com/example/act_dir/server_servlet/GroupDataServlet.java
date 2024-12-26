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
