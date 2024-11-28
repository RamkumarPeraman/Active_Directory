package com.example.act_dir.server_servlet;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.example.act_dir.db.DBConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class OUDataServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String ouName = request.getParameter("ouName");
        String description = request.getParameter("description");

        try (Connection conn = DBConnection.getConnection()) {
            if (ouExists(conn, ouName, description)) {
                response.getWriter().println("OU data already exists!");
            } else {
                if (insertOU(conn, ouName, description)) {
                    response.getWriter().println("OU data inserted successfully!");
                } else {
                    response.getWriter().println("OU data insertion failed!");
                }
            }
        } catch (SQLException e) {
            response.getWriter().println("Error: Unable to connect to the database.");
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Set the response content type
        response.setContentType("text/html");

        // Write the response
        response.getWriter().println("<html><body><h1>OU Data Inserted</h1></body></html>");
    }

    private boolean ouExists(Connection conn, String ouName, String description) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM ou_det WHERE ou_name = ? AND description = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, ouName);
            checkStmt.setString(2, description);

            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean insertOU(Connection conn, String ouName, String description) throws SQLException {
        String insertSql = "INSERT INTO ou_det (ou_name, description) VALUES (?, ?)";
        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
            insertStmt.setString(1, ouName);
            insertStmt.setString(2, description);

            int rowsInserted = insertStmt.executeUpdate();
            return rowsInserted > 0;
        }
    }
}
