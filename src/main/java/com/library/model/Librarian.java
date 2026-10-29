package com.library.model;

/**
 * Concrete subclass representing staff members operating the circulation and catalog systems.
 * [Concept: Inheritance - Subclass of Person]
 * [Concept: super keyword]
 * [Concept: Method Overriding]
 */
public class Librarian extends Person {

    private String employeeId;
    private String shift;
    private String department;

    public Librarian(int id, String name, String email, String phone, String passwordHash,
                     String employeeId, String shift, String department) {
        super(id, name, email, phone, Role.LIBRARIAN, passwordHash);
        this.employeeId = employeeId;
        this.shift = shift;
        this.department = department;
    }

    public Librarian(String name, String email, String phone, String passwordHash,
                     String employeeId, String shift, String department) {
        this(0, name, email, phone, passwordHash, employeeId, shift, department);
    }

    // [Concept: Method Overriding]
    @Override
    public String getProfileSummary() {
        return String.format("Staff ID: %s | Shift: %s | Dept: %s",
                employeeId, shift, department);
    }

    @Override
    public boolean matchesQuery(String query) {
        if (super.matchesQuery(query)) return true;
        if (query == null || query.isBlank()) return true;
        String q = query.trim().toLowerCase();
        return (employeeId != null && employeeId.toLowerCase().contains(q)) ||
               (department != null && department.toLowerCase().contains(q));
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getShift() {
        return shift;
    }

    public void setShift(String shift) {
        this.shift = shift;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}
