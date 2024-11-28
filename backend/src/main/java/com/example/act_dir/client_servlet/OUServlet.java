package com.example.act_dir.client_servlet;

import com.example.act_dir.db.DBConnection;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.*;
import com.example.act_dir.cors_filter.CORS_Filter;

public class OUServlet extends HttpServlet {
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
                // Fetch all OUs' ids and names
                String query = "SELECT id, ou_name FROM ou_det";
                pstmt = conn.prepareStatement(query);
                rs = pstmt.executeQuery();

                StringBuilder ous = new StringBuilder("[");
                while (rs.next()) {
                    String id = rs.getString("id");
                    String ouName = rs.getString("ou_name");
                    ous.append("{\"id\":\"").append(id).append("\",")
                            .append("\"ou_name\":\"").append(ouName).append("\"},");
                }
                if (ous.length() > 1) {
                    ous.setLength(ous.length() - 1);
                }
                ous.append("]");

                out.write(ous.toString());
            } else {
                String ouId = pathInfo.substring(1);
                String query = "SELECT ou_name, description FROM ou_det WHERE id = ?";
                pstmt = conn.prepareStatement(query);
                pstmt.setString(1, ouId);
                rs = pstmt.executeQuery();

                if (rs.next()) {
                    String ouName = rs.getString("ou_name");
                    String description = rs.getString("description");

                    StringBuilder ouDetails = new StringBuilder("{");
                    ouDetails.append("\"ou_name\":\"").append(ouName).append("\",")
                            .append("\"description\":\"").append(description).append("\"")
                            .append("}");

                    out.write(ouDetails.toString());
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.write("{\"error\":\"OU not found\"}");
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
