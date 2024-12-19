package com.example.act_dir.client_servlet;

import com.example.act_dir.cors_filter.CORS_Filter;
import com.example.act_dir.db.DBConnection;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.*;

public class DeletedObjServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        CORS_Filter.setCORSHeaders(response);
        response.setContentType("application/json");
        String id = request.getParameter("id");
        String type = request.getParameter("type");
        String name = request.getParameter("name");
        String description = request.getParameter("description");
        try (PrintWriter out = response.getWriter()) {
            String jsonData = getGroupDataAsJson(id, type, name, description);
            out.write(jsonData);
        }
    }
    public String getGroupDataAsJson(String id, String type, String name, String description) {
        StringBuilder query = new StringBuilder("SELECT id, type, name, description FROM act_dit WHERE isDeleted = 'YES'");
        boolean hasCondition = false;

        if (id != null && !id.isEmpty()) {
            query.append(" AND id = ?");
            hasCondition = true;
        }
        if (type != null && !type.isEmpty()) {
            query.append(hasCondition ? " AND" : " WHERE").append(" type = ?");
            hasCondition = true;
        }
        if (name != null && !name.isEmpty()) {
            query.append(hasCondition ? " AND" : " WHERE").append(" name = ?");
        }
        if (description != null && !description.isEmpty()) {
            query.append(hasCondition ? " AND" : " WHERE").append(" description = ?");
        }
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query.toString())) {
            int index = 1;
            if (id != null && !id.isEmpty())
                pstmt.setInt(index++, Integer.parseInt(id));
            if (type != null && !type.isEmpty())
                pstmt.setString(index++, type);
            if (name != null && !name.isEmpty())
                pstmt.setString(index++, name);
            if (description != null && !description.isEmpty())
                pstmt.setString(index++, description);
            try (ResultSet rs = pstmt.executeQuery()) {
                return buildJsonData(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "{\"error\":\"Database error: " + e.getMessage() + "\"}";
        }
    }
    public String buildJsonData(ResultSet rs) throws SQLException {
        StringBuilder jsonData = new StringBuilder();
        boolean dataFound = false;
        jsonData.append("[");
        while (rs.next()) {
            dataFound = true;
            jsonData.append("{")
                    .append("\"id\":\"").append(rs.getInt("id")).append("\", ")
                    .append("\"type\":\"").append(rs.getString("type")).append("\", ")
                    .append("\"name\":\"").append(rs.getString("name")).append("\", ")
                    .append("\"description\":\"").append(rs.getString("description"))
                    .append("\"},");
        }

        // If data was found, remove the last comma
        if (dataFound && jsonData.length() > 1) {
            jsonData.setLength(jsonData.length() - 1); // Remove last comma
        } else if (!dataFound) {
            jsonData.append("]");
            return jsonData.toString();
        }
        jsonData.append("]");
        return jsonData.toString();
    }
}
