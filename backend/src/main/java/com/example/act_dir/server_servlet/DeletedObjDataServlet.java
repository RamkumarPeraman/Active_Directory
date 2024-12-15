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

public class DeletedObjDataServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String objectName = request.getParameter("objectName");
        String description = request.getParameter("description");
        String type = request.getParameter("type");
        System.out.print(objectName+"----------------");
        response.setContentType("text/plain");
        try (Connection conn = DBConnection.getConnection()) {
            if (objectExists(conn, objectName, description)) {
                response.getWriter().println("Object data already exists!");
            } else {
                if (insertObject(conn, type, objectName, description)) {
                    response.getWriter().println("Object data inserted successfully!");
                } else {
                    response.getWriter().println("Object data insertion failed!");
                }
            }
        } catch (SQLException e) {
            response.getWriter().println("Error: Unable to connect to the database.");
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Set the response content type
        response.setContentType("text/html");

        // Write the response
        response.getWriter().println("<html><body><h1>Insert Object Data</h1></body></html>");
    }

    private boolean objectExists(Connection conn, String objectName, String description) throws SQLException {
        String checkSql = "SELECT COUNT(*) FROM act_dit WHERE name = ? AND description = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, objectName);
            checkStmt.setString(2, description);
            System.out.print(objectName+"----------------");
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean insertObject(Connection conn, String type, String objectName, String description) throws SQLException {
        String insertSql = "INSERT INTO act_dit (type, name, description,isDeleted) VALUES (?, ?, ?,'YES')";
        try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
            insertStmt.setString(1, type);
            insertStmt.setString(2, objectName);
            insertStmt.setString(3, description);
            System.out.print(objectName+"----------------");
            int rowsInserted = insertStmt.executeUpdate();
            return rowsInserted > 0;
        }
    }
}
