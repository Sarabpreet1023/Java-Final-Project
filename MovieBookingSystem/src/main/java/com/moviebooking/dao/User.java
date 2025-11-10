package com.moviebooking.dao;

public class User {
    private int id;
    private String username;
    private String email;
    private String passwordHash;
    private boolean isAdmin;

    // getters / setters
    public int getId(){ return id; }
    public void setId(int id){ this.id = id; }

    public String getUsername(){ return username; }
    public void setUsername(String username){ this.username = username; }

    public String getEmail(){ return email; }
    public void setEmail(String email){ this.email = email; }

    public String getPasswordHash(){ return passwordHash; }
    public void setPasswordHash(String passwordHash){ this.passwordHash = passwordHash; }

    public boolean isAdmin(){ return isAdmin; }
    public void setAdmin(boolean admin){ isAdmin = admin; }
}
