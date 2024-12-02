package com.example.act_dir.client_servlet;

import com.example.act_dir.cors_filter.CORS_Filter;
import com.example.act_dir.db.DBConnection;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.*;

public class UserServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        CORS_Filter.setCORSHeaders(response);
        response.setContentType("application/json");

        String id = request.getParameter("id");
        String firstName = request.getParameter("first_name");
        String lastName = request.getParameter("last_name");
        String phoneNumber = request.getParameter("phone_number");
        String email = request.getParameter("email");
        String office = request.getParameter("office");
        String description = request.getParameter("description");

        try (PrintWriter out = response.getWriter()) {
            String jsonData = getUserDataAsJson(id, firstName, lastName, phoneNumber, email, office, description);
            out.write(jsonData);
        }
    }

    public String getUserDataAsJson(String id, String firstName, String lastName, String phoneNumber, String email, String office, String description) {
        StringBuilder query = new StringBuilder("SELECT id, first_name, last_name, phone_number, email, office, description FROM user_det");
        boolean hasCondition = false;

        if (id != null && !id.isEmpty()) {
            query.append(" WHERE id = ?");
            hasCondition = true;
        }
        if (firstName != null && !firstName.isEmpty()) {
            query.append(hasCondition ? " AND" : " WHERE").append(" first_name = ?");
            hasCondition = true;
        }
        if (lastName != null && !lastName.isEmpty()) {
            query.append(hasCondition ? " AND" : " WHERE").append(" last_name = ?");
            hasCondition = true;
        }
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            query.append(hasCondition ? " AND" : " WHERE").append(" phone_number = ?");
            hasCondition = true;
        }
        if (email != null && !email.isEmpty()) {
            query.append(hasCondition ? " AND" : " WHERE").append(" email = ?");
            hasCondition = true;
        }
        if (office != null && !office.isEmpty()) {
            query.append(hasCondition ? " AND" : " WHERE").append(" office = ?");
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
            if (firstName != null && !firstName.isEmpty())
                pstmt.setString(index++, firstName);
            if (lastName != null && !lastName.isEmpty())
                pstmt.setString(index++, lastName);
            if (phoneNumber != null && !phoneNumber.isEmpty())
                pstmt.setString(index++, phoneNumber);
            if (email != null && !email.isEmpty())
                pstmt.setString(index++, email);
            if (office != null && !office.isEmpty())
                pstmt.setString(index++, office);
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
                    .append("\"first_name\":\"").append(rs.getString("first_name")).append("\", ")
                    .append("\"last_name\":\"").append(rs.getString("last_name")).append("\", ")
                    .append("\"phone_number\":\"").append(rs.getString("phone_number")).append("\", ")
                    .append("\"email\":\"").append(rs.getString("email")).append("\", ")
                    .append("\"office\":\"").append(rs.getString("office")).append("\", ")
                    .append("\"description\":\"").append(rs.getString("description"))
                    .append("\"}");
        } else {
            jsonData.append("[");
            while (rs.next()) {
                jsonData.append("{")
                        .append("\"id\":\"").append(rs.getInt("id")).append("\", ")
                        .append("\"first_name\":\"").append(rs.getString("first_name")).append("\", ")
                        .append("\"last_name\":\"").append(rs.getString("last_name")).append("\", ")
                        .append("\"phone_number\":\"").append(rs.getString("phone_number")).append("\", ")
                        .append("\"email\":\"").append(rs.getString("email")).append("\", ")
                        .append("\"office\":\"").append(rs.getString("office")).append("\", ")
                        .append("\"description\":\"").append(rs.getString("description"))
                        .append("\"},");
            }
            if (jsonData.length() > 1) {
                jsonData.setLength(jsonData.length() - 1);
            }
            jsonData.append("]");
        }

        return jsonData.toString();
    }
}
