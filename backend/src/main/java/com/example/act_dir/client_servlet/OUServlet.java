package com.example.act_dir.client_servlet;

import com.example.act_dir.cors_filter.CORS_Filter;
import com.example.act_dir.db.DBConnection;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.*;

public class OUServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        CORS_Filter.setCORSHeaders(response);
        response.setContentType("application/json");

        String id = request.getParameter("id");
        String ouName = request.getParameter("ou_name");
        String description = request.getParameter("description");

        try (PrintWriter out = response.getWriter()) {
            String jsonData = getOUDataAsJson(id, ouName, description);
            out.write(jsonData);
        }
    }

    public String getOUDataAsJson(String id, String ouName, String description) {
        StringBuilder query = new StringBuilder("SELECT id, ou_name, description FROM ou_det");
        boolean hasCondition = false;

        if (id != null && !id.isEmpty()) {
            query.append(" WHERE id = ?");
            hasCondition = true;
        }
        if (ouName != null && !ouName.isEmpty()) {
            query.append(hasCondition ? " AND" : " WHERE").append(" ou_name = ?");
            hasCondition = true;
        }
        if (description != null && !description.isEmpty()) {
            query.append(hasCondition ? " AND" : " WHERE").append(" description = ?");
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query.toString())) {
            int index = 1;
            if (id != null && !id.isEmpty())
                pstmt.setInt(index++, Integer.parseInt(id));
            if (ouName != null && !ouName.isEmpty())
                pstmt.setString(index++, ouName);
            if (description != null && !description.isEmpty())
                pstmt.setString(index++, description);

            try (ResultSet rs = pstmt.executeQuery()) {
                return buildJsonData(rs, id);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "{\"error\":\"Database error\"}";
        }
    }

    public String buildJsonData(ResultSet rs, String id) throws SQLException {
        StringBuilder jsonData = new StringBuilder();
        if (id != null && !id.isEmpty() && rs.next()) {
            jsonData.append("{")
                    .append("\"id\":\"").append(rs.getInt("id")).append("\", ")
                    .append("\"ou_name\":\"").append(rs.getString("ou_name")).append("\", ")
                    .append("\"description\":\"").append(rs.getString("description"))
                    .append("\"}");
        } else {
            jsonData.append("[");
            while (rs.next()) {
                jsonData.append("{")
                        .append("\"id\":\"").append(rs.getInt("id")).append("\", ")
                        .append("\"ou_name\":\"").append(rs.getString("ou_name")).append("\", ")
                        .append("\"description\":\"").append(rs.getString("description"))
                        .append("\"},");
            }
            if (jsonData.length() > 1) {
                jsonData.setLength(jsonData.length() - 1);  // Remove last comma
            }
            jsonData.append("]");
        }

        return jsonData.toString();
    }
}
