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

public class UserDataServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String userName = request.getParameter("userName");
        // Other parameters (uncomment if needed)
        // String sn = request.getParameter("sn");
        // String telephoneNumber = request.getParameter("telephoneNumber");
        // String mail = request.getParameter("mail");
        // String office = request.getParameter("office");
        String description = request.getParameter("description");
        String type = request.getParameter("type");

        try (Connection conn = DBConnection.getConnection()) {
            if (userExists(conn, userName, description)) {
                response.getWriter().println("Data already exists!");
            } else {
                if (insertUser(conn, type, userName, description)) {
                    response.getWriter().println("Data inserted successfully!");
                } else {
                    response.getWriter().println("Data insertion failed!");
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
        response.getWriter().println("<html><body><h1>Data Inserted</h1></body></html>");
    }

    // Modify this method if you need to check more fields (e.g., sn, telephoneNumber, etc.)
    private boolean userExists(Connection conn, String userName, String description) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM act_dit WHERE name = ? AND description = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, userName);
            checkStmt.setString(2, description);

            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    // Modify this method if you want to insert more fields like sn, telephoneNumber, etc.
    private boolean insertUser(Connection conn, String type, String userName, String description) throws SQLException {
        String insertSql = "INSERT INTO act_dit (type, name, description,isDeleted) VALUES (?, ?, ?,'NO')";
        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
            insertStmt.setString(1, type);
            insertStmt.setString(2, userName);
            insertStmt.setString(3, description);

            int rowsInserted = insertStmt.executeUpdate();
            return rowsInserted > 0;
        }
    }
}
