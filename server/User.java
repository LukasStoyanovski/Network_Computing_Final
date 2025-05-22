package server;

import java.io.Serializable;

public class User implements Serializable {
    private String username;
    private String password;
    private String firstName;
    private String lastName;
    private String email;
    private String ipAddress;

    // Constructor
    public User(String username, String password, String firstName, String lastName, String email, String ipAddress) {
        this.username = username;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.ipAddress = ipAddress;
    }

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    // Convert User object to string for file storage
    @Override
    public String toString() {
        return String.format("%s|%s|%s|%s|%s|%s",
            username, password, firstName, lastName, email, ipAddress);
    }

    // Create User object from string (for file reading)
    public static User fromString(String userString) {
        String[] parts = userString.split("\\|");
        if (parts.length != 6) {
            throw new IllegalArgumentException("Invalid user string format");
        }
        return new User(
            parts[0], // username
            parts[1], // password
            parts[2], // firstName
            parts[3], // lastName
            parts[4], // email
            parts[5]  // ipAddress
        );
    }

    // Override equals and hashCode for proper object comparison
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return username.equals(user.username);
    }

    @Override
    public int hashCode() {
        return username.hashCode();
    }
}
