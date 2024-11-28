package com.example.act_dir.client_servlet;

import com.example.act_dir.db.DBConnection;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.*;
import com.example.act_dir.cors_filter.CORS_Filter;

public class GroupServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        CORS_Filter.setCORSHeaders(response);

        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        String pathInfo = request.getPathInfo();
        try {
            conn = DBConnection.getConnection();

            if (pathInfo == null || pathInfo.equals("/")) {
                // Fetch all groups' ids and names
                String query = "SELECT id, group_name FROM group_det";
                pstmt = conn.prepareStatement(query);
                rs = pstmt.executeQuery();

                StringBuilder groups = new StringBuilder("[");
                while (rs.next()) {
                    String id = rs.getString("id");
                    String groupName = rs.getString("group_name");
                    groups.append("{\"id\":\"").append(id).append("\",")
                            .append("\"group_name\":\"").append(groupName).append("\"},");
                }
                if (groups.length() > 1) {
                    groups.setLength(groups.length() - 1);
                }
                groups.append("]");

                out.write(groups.toString());
            } else {
                String groupId = pathInfo.substring(1);
                String query = "SELECT group_name, email, description FROM group_det WHERE id = ?";
                pstmt = conn.prepareStatement(query);
                pstmt.setString(1, groupId);
                rs = pstmt.executeQuery();

                if (rs.next()) {
                    String groupName = rs.getString("group_name");
                    String email = rs.getString("email");
                    String description = rs.getString("description");

                    StringBuilder groupDetails = new StringBuilder("{");
                    groupDetails.append("\"group_name\":\"").append(groupName).append("\",")
                            .append("\"email\":\"").append(email).append("\",")
                            .append("\"description\":\"").append(description).append("\"")
                            .append("}");

                    out.write(groupDetails.toString());
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.write("{\"error\":\"Group not found\"}");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            out.write("{\"error\":\"Database error\"}");
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }
}
