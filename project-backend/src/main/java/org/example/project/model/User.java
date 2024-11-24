package org.example.project.model;

public class User {
    private int id;
    private String first_name;
    private String last_name;
    private String phone_number;

    public int getId() {

        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getFirstName() {
        return first_name;
    }

    public void setFirstName(String first_name) {

        this.first_name = first_name;
    }
    public String getLastName() {

        return last_name;
    }

    public void setLastName(String last_name) {

        this.last_name = last_name;
    }
    public String getPhoneNumber() {

        return phone_number;
    }
    public void setPhoneNumber(String phone_number) {
        this.phone_number = phone_number;
    }
}
