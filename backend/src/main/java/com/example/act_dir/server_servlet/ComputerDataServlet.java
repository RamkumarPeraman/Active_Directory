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

public class ComputerDataServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String computerName = request.getParameter("computerName");
        String description = request.getParameter("description");

        try (Connection conn = DBConnection.getConnection()) {
            if (computerExists(conn, computerName, description)) {
                response.getWriter().println("Computer data already exists!");
            } else {
                if(insertComputer(conn, computerName, description)) {
                    response.getWriter().println("Computer data inserted successfully!");
                }else {
                    response.getWriter().println("Computer data insertion failed!");
                }
            }
        }catch(SQLException e) {
            response.getWriter().println("Error: Unable to connect to the database.");
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Set the response content type
        response.setContentType("text/html");

        // Write the response
        response.getWriter().println("<html><body><h1>Computer Data Inserted</h1></body></html>");
    }

    private boolean computerExists(Connection conn, String computerName, String description) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM computer_det WHERE computer_name = ? AND description = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, computerName);
            checkStmt.setString(2, description);

            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean insertComputer(Connection conn, String computerName, String description) throws SQLException {
        String insertSql = "INSERT INTO computer_det (computer_name, description) VALUES (?, ?)";
        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
            insertStmt.setString(1, computerName);
            insertStmt.setString(2, description);

            int rowsInserted = insertStmt.executeUpdate();
            return rowsInserted > 0;
        }
    }
}
