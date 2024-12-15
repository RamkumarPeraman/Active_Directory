package com.example.act_dir.client_servlet;

import com.example.act_dir.cors_filter.CORS_Filter;
import com.example.act_dir.db.DBConnection;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.*;

public class DeletedObjServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // Set CORS headers (if needed)
        CORS_Filter.setCORSHeaders(response);

        // Set the response type to JSON
        response.setContentType("application/json");

        // Get parameters from the request
        String id = request.getParameter("id");
        String type = request.getParameter("type");
        String name = request.getParameter("name");
        String description = request.getParameter("description");

        // Get the JSON data from the database
        try (PrintWriter out = response.getWriter()) {
            String jsonData = getGroupDataAsJson(id, type, name, description);
            out.write(jsonData);
        }
    }

    // Retrieves group data and constructs the query dynamically based on provided parameters
    public String getGroupDataAsJson(String id, String type, String name, String description) {
        StringBuilder query = new StringBuilder("SELECT id, type, name, description FROM act_dit WHERE isDeleted = 'YES'");
        boolean hasCondition = false;

        // Add conditions based on the provided parameters
        if (id != null && !id.isEmpty()) {
            query.append(" AND id = ?");
            hasCondition = true;
        }
        if (type != null && !type.isEmpty()) {
            query.append(hasCondition ? " AND" : " WHERE").append(" type = ?");
            hasCondition = true;
        }
        if (name != null && !name.isEmpty()) {
            query.append(hasCondition ? " AND" : " WHERE").append(" name = ?");
        }
        if (description != null && !description.isEmpty()) {
            query.append(hasCondition ? " AND" : " WHERE").append(" description = ?");
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query.toString())) {

            // Set parameters for the query dynamically
            int index = 1;
            if (id != null && !id.isEmpty())
                pstmt.setInt(index++, Integer.parseInt(id));
            if (type != null && !type.isEmpty())
                pstmt.setString(index++, type);
            if (name != null && !name.isEmpty())
                pstmt.setString(index++, name);
            if (description != null && !description.isEmpty())
                pstmt.setString(index++, description);

            // Execute the query and build the JSON response
            try (ResultSet rs = pstmt.executeQuery()) {
                return buildJsonData(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "{\"error\":\"Database error: " + e.getMessage() + "\"}";
        }
    }

    // Builds the JSON data from the ResultSet
    public String buildJsonData(ResultSet rs) throws SQLException {
        StringBuilder jsonData = new StringBuilder();
        boolean dataFound = false;

        // Begin the JSON array
        jsonData.append("[");

        // Process each row in the result set
        while (rs.next()) {
            dataFound = true;
            jsonData.append("{")
                    .append("\"id\":\"").append(rs.getInt("id")).append("\", ")
                    .append("\"type\":\"").append(rs.getString("type")).append("\", ")
                    .append("\"name\":\"").append(rs.getString("name")).append("\", ")
                    .append("\"description\":\"").append(rs.getString("description"))
                    .append("\"},");
        }

        // If data was found, remove the last comma
        if (dataFound && jsonData.length() > 1) {
            jsonData.setLength(jsonData.length() - 1); // Remove last comma
        } else if (!dataFound) {
            // If no data is found, return an empty array
            jsonData.append("]");
            return jsonData.toString();
        }

        // Close the JSON array
        jsonData.append("]");

        return jsonData.toString();
    }
}
