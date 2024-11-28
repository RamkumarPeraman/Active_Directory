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
        String givenName = request.getParameter("givenName");
        String sn = request.getParameter("sn");
        String telephoneNumber = request.getParameter("telephoneNumber");
        String mail = request.getParameter("mail");
        String office = request.getParameter("office");
        String description = request.getParameter("description");

        try (Connection conn = DBConnection.getConnection()) {
            if (userExists(conn, givenName, sn, telephoneNumber, mail, office, description)) {
                response.getWriter().println("Data already exists!");
            } else {
                if (insertUser(conn, givenName, sn, telephoneNumber, mail, office, description)) {
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

    private boolean userExists(Connection conn, String givenName, String sn, String telephoneNumber, String mail, String office, String description) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM user_det WHERE first_name = ? AND last_name = ? AND phone_number = ? AND email = ? AND office = ? AND description = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, givenName);
            checkStmt.setString(2, sn);
            checkStmt.setString(3, telephoneNumber);
            checkStmt.setString(4, mail);
            checkStmt.setString(5, office);
            checkStmt.setString(6, description);

            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean insertUser(Connection conn, String givenName, String sn, String telephoneNumber, String mail, String office, String description) throws SQLException {
        String insertSql = "INSERT INTO user_det (first_name, last_name, phone_number, email, office, description) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
            insertStmt.setString(1, givenName);
            insertStmt.setString(2, sn);
            insertStmt.setString(3, telephoneNumber);
            insertStmt.setString(4, mail);
            insertStmt.setString(5, office);
            insertStmt.setString(6, description);

            int rowsInserted = insertStmt.executeUpdate();
            return rowsInserted > 0;
        }
    }
}