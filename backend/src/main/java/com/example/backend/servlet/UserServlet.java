package com.example.backend.servlet;

import com.example.backend.db.DBConnection;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.*;
public class UserServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");





        response.setContentType("application/json");
        PrintWriter out = response.getWriter();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        String pathInfo = request.getPathInfo();
        try {
            conn = DBConnection.getConnection();

            if (pathInfo == null || pathInfo.equals("/")) {
                // Fetch all users' ids and first names
                String query = "SELECT id, first_name FROM users";
                pstmt = conn.prepareStatement(query);
                rs = pstmt.executeQuery();

                StringBuilder users = new StringBuilder("[");
                while (rs.next()) {
                    String id = rs.getString("id");
                    String firstName = rs.getString("first_name");
                    users.append("{\"id\":\"").append(id).append("\",")
                            .append("\"first_name\":\"").append(firstName).append("\"},");
                }
                if (users.length() > 1) {
                    users.setLength(users.length() - 1);
                }
                users.append("]");

                out.write(users.toString());
            }
            else {
                String userId = pathInfo.substring(1);
                String query = "SELECT first_name, last_name, phone_number FROM users WHERE id = ?";
                pstmt = conn.prepareStatement(query);
                pstmt.setString(1, userId);
                rs = pstmt.executeQuery();

                if (rs.next()) {
                    String firstName = rs.getString("first_name");
                    String lastName = rs.getString("last_name");
                    String phoneNumber = rs.getString("phone_number");

                    StringBuilder userDetails = new StringBuilder("{");
                    userDetails.append("\"first_name\":\"").append(firstName).append("\",")
                            .append("\"last_name\":\"").append(lastName).append("\",")
                            .append("\"phone_number\":\"").append(phoneNumber).append("\"")
                            .append("}");

                    out.write(userDetails.toString());
                }
                else {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    out.write("{\"error\":\"User not found\"}");
                }
            }
        }
        catch(SQLException e) {
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
