package com.example.act_dir.client_servlet;
import com.example.act_dir.cors_filter.CORS_Filter;
import com.example.act_dir.db.DBConnection;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.sql.*;
public class UserServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        CORS_Filter.setCORSHeaders(response);
        response.setContentType("application/json");

        String id = request.getParameter("id");
        String firstName = request.getParameter("first_name");  // Searching by first name
        String lastName = request.getParameter("last_name");
        String description = request.getParameter("description");
        String sortBy = request.getParameter("sortBy");
        String search = request.getParameter("search");  // Search by first name

        try (PrintWriter out = response.getWriter()) {
            String jsonData = getUserDataAsJson(id, firstName, lastName, description, sortBy, search);
            out.write(jsonData);
        }
    }
    public String getUserDataAsJson(String id, String firstName, String lastName, String description, String sortBy, String search) {
        StringBuilder query = new StringBuilder("SELECT id, name, description FROM act_dit WHERE type = 'User' AND isDeleted <> 'YES'");
        boolean hasCondition = false;

        // Add conditions based on the provided parameters
        if (id != null && !id.isEmpty()) {
            query.append(" AND id = ?");
            hasCondition = true;
        }
        if (firstName != null && !firstName.isEmpty()) {
            query.append(hasCondition ? " AND" : " AND").append(" name LIKE ?");
            hasCondition = true;
        }
        if (lastName != null && !lastName.isEmpty()) {
            query.append(hasCondition ? " AND" : " AND").append(" name LIKE ?");
        }
        if (description != null && !description.isEmpty()) {
            query.append(hasCondition ? " AND" : " AND").append(" description = ?");
        }
        if (search != null && !search.isEmpty()) {
            query.append(hasCondition ? " AND" : " AND").append(" name LIKE ?");
            hasCondition = true;
        }

        // Add sorting based on the sortBy parameter
        if (sortBy != null) {
            switch (sortBy) {
                case "asc-desc":
                    query.append(" ORDER BY name ASC");
                    break;
                case "desc-asc":
                    query.append(" ORDER BY name DESC");
                    break;
                case "new-old":
                    query.append(" ORDER BY updated_time DESC");  // Assuming you have a created_at column
                    break;
                case "old-new":
                    query.append(" ORDER BY updated_time ASC");
                    break;
                default:
                    query.append(" ORDER BY name ASC");
                    break;
            }
        } else {
            query.append(" ORDER BY name ASC");
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query.toString())) {
            int index = 1;
            if (id != null && !id.isEmpty())
                pstmt.setInt(index++, Integer.parseInt(id));
            if (firstName != null && !firstName.isEmpty())
                pstmt.setString(index++, "%" + firstName + "%");
            if (lastName != null && !lastName.isEmpty())
                pstmt.setString(index++, "%" + lastName + "%");
            if (description != null && !description.isEmpty())
                pstmt.setString(index++, description);
            if (search != null && !search.isEmpty())
                pstmt.setString(index++, "%" + search + "%");

            try (ResultSet rs = pstmt.executeQuery()) {
                return buildJsonData(rs, id);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return "{\"error\":\"Database error\"}";
        }
    }

    public String buildJsonData(ResultSet rs, String id) throws SQLException {
        StringBuilder jsonData = new StringBuilder();

        if (id != null && !id.isEmpty() && rs.next()) {
            jsonData.append("{")
                    .append("\"id\":\"").append(rs.getInt("id")).append("\", ")
                    .append("\"first_name\":\"").append(rs.getString("name").split(" ")[0]).append("\", ")
                    .append("\"last_name\":\"").append(rs.getString("name").split(" ").length > 1 ? rs.getString("name").split(" ")[1] : "").append("\", ")
                    .append("\"description\":\"").append(rs.getString("description")).append("\"")
                    .append("}");
        } else {
            jsonData.append("[");
            while (rs.next()) {
                jsonData.append("{")
                        .append("\"id\":\"").append(rs.getInt("id")).append("\", ")
                        .append("\"first_name\":\"").append(rs.getString("name").split(" ")[0]).append("\", ")
                        .append("\"last_name\":\"").append(rs.getString("name").split(" ").length > 1 ? rs.getString("name").split(" ")[1] : "").append("\", ")
                        .append("\"description\":\"").append(rs.getString("description")).append("\"")
                        .append("},");
            }
            if (jsonData.length() > 1) {
                jsonData.setLength(jsonData.length() - 1);  // Remove the last comma
            }
            jsonData.append("]");
        }

        return jsonData.toString();
    }
}