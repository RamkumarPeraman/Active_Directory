package com.example.act_dir.client_servlet;

import com.example.act_dir.db.DBConnection;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.*;
import com.example.act_dir.cors_filter.CORS_Filter;

public class ComputerServlet extends HttpServlet {
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
                // Fetch all computers' ids and names
                String query = "SELECT id, computer_name FROM computer_det";
                pstmt = conn.prepareStatement(query);
                rs = pstmt.executeQuery();

                StringBuilder computers = new StringBuilder("[");
                while (rs.next()) {
                    String id = rs.getString("id");
                    String computerName = rs.getString("computer_name");
                    computers.append("{\"id\":\"").append(id).append("\",")
                            .append("\"computer_name\":\"").append(computerName).append("\"},");
                }
                if (computers.length() > 1) {
                    computers.setLength(computers.length() - 1);
                }
                computers.append("]");

                out.write(computers.toString());
            } else {
                String computerId = pathInfo.substring(1);
                String query = "SELECT computer_name, computer_version, description FROM computer_det WHERE id = ?";
                pstmt = conn.prepareStatement(query);
                pstmt.setString(1, computerId);
                rs = pstmt.executeQuery();

                if (rs.next()) {
                    String computerName = rs.getString("computer_name");
                    String computerVersion = rs.getString("computer_version");
                    String description = rs.getString("description");

                    StringBuilder computerDetails = new StringBuilder("{");
                    computerDetails.append("\"computer_name\":\"").append(computerName).append("\",")
                            .append("\"computer_version\":\"").append(computerVersion).append("\",")
                            .append("\"description\":\"").append(description).append("\"")
                            .append("}");

                    out.write(computerDetails.toString());
                } else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.write("{\"error\":\"Computer not found\"}");
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
