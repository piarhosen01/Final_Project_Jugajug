package com.example.jugajug.model;

import com.google.firebase.Timestamp;
import java.util.List;
import java.util.ArrayList;

public class UserModel {
    private String phone;
    private String firstName;
    private String lastName;
    private Timestamp createdTimestamp;
    private String userId;
    private String fcmToken;
    private List<String> searchKeywords;

    public UserModel() {}

    // Constructor for flexible initialization
    public UserModel(String phone, String firstName, String lastName,
                     Timestamp createdTimestamp, String userId) {
        this.phone = phone;
        this.firstName = firstName;
        this.lastName = lastName;
        this.createdTimestamp = createdTimestamp;
        this.userId = userId;
    }

    // Getters and setters
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public Timestamp getCreatedTimestamp() { return createdTimestamp; }
    public void setCreatedTimestamp(Timestamp createdTimestamp) {
        this.createdTimestamp = createdTimestamp;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFcmToken() { return fcmToken; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }

    public List<String> getSearchKeywords() { return searchKeywords; }
    public void setSearchKeywords(List<String> searchKeywords) {
        this.searchKeywords = searchKeywords;
    }

    public String getUsername() {
        return firstName + " " + lastName;
    }

    public void setUsername(String username) {
        String[] names = username.split(" ");
        if (names.length > 1) {
            this.firstName = names[0];
            this.lastName = names[1];
        } else {
            this.firstName = username;
            this.lastName = "";
        }
    }

    // Utility method to generate search keywords
    public static List<String> generateSearchKeywords(String firstName, String lastName, String email, String phone) {
        List<String> keywords = new ArrayList<>();

        // Add variations of name
        if (firstName != null) {
            keywords.add(firstName.toLowerCase());
        }
        if (lastName != null) {
            keywords.add(lastName.toLowerCase());
        }
        if (firstName != null && lastName != null) {
            keywords.add((firstName + " " + lastName).toLowerCase());
            keywords.add((lastName + " " + firstName).toLowerCase());
        }

        // Add email username if available
        if (email != null && email.contains("@")) {
            String emailUsername = email.split("@")[0].toLowerCase();
            keywords.add(emailUsername);
        }

        // Add phone number variations
        if (phone != null) {
            keywords.add(phone);
            // Remove non-digit characters for flexible phone search
            String phoneDigits = phone.replaceAll("\\D", "");
            keywords.add(phoneDigits);
        }

        return keywords;
    }
}
