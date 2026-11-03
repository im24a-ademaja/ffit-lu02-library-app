package ch.bzz;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @Column(name = "project_name", nullable = false, unique = true, length = 255)
    private String projectName;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    public Project() {
    }

    public Project(String projectName) {
        this.projectName = projectName;
    }

    public Project(String projectName, String passwordHash) {
        this.projectName = projectName;
        this.passwordHash = passwordHash;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
