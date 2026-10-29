package com.library.model;

import java.util.Objects;

/**
 * Abstract base class representing all human actors in the library system.
 * [Concept: Abstract Class]
 * [Concept: Inheritance]
 * [Concept: Access Modifiers]
 */
public abstract class Person implements Searchable {

    protected int id;
    protected String name;
    protected String email;
    protected String phone;
    protected final Role role;
    protected String passwordHash;

    public Person(int id, String name, String email, String phone, Role role, String passwordHash) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "Name cannot be null");
        this.email = Objects.requireNonNull(email, "Email cannot be null");
        this.phone = Objects.requireNonNull(phone, "Phone cannot be null");
        this.role = Objects.requireNonNull(role, "Role cannot be null");
        this.passwordHash = Objects.requireNonNull(passwordHash, "Password hash cannot be null");
    }

    public Person(String name, String email, String phone, Role role, String passwordHash) {
        this(0, name, email, phone, role, passwordHash);
    }

    // [Concept: Abstract Method] Implemented differently for Members and Librarians
    public abstract String getProfileSummary();

    @Override
    public boolean matchesQuery(String query) {
        if (query == null || query.isBlank()) return true;
        String q = query.trim().toLowerCase();
        return name.toLowerCase().contains(q) ||
               email.toLowerCase().contains(q) ||
               phone.contains(q);
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Role getRole() {
        return role;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
