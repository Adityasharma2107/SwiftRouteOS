package com.swiftroute.domain.entity;

import com.swiftroute.domain.enums.TechnicianStatus;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "technicians")
public class Technician extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "phone", nullable = false, length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TechnicianStatus status = TechnicianStatus.AVAILABLE;

    @Column(name = "current_latitude", nullable = false)
    private Double currentLatitude;

    @Column(name = "current_longitude", nullable = false)
    private Double currentLongitude;

    @Column(name = "max_daily_jobs", nullable = false)
    private Integer maxDailyJobs = 6;

    @OneToMany(mappedBy = "technician", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TechnicianSkill> technicianSkills = new ArrayList<>();

    public Technician() {
    }

    public Technician(User user, String name, String phone, TechnicianStatus status, Double currentLatitude, Double currentLongitude, Integer maxDailyJobs) {
        this.user = user;
        this.name = name;
        this.phone = phone;
        this.status = status != null ? status : TechnicianStatus.AVAILABLE;
        this.currentLatitude = currentLatitude;
        this.currentLongitude = currentLongitude;
        this.maxDailyJobs = maxDailyJobs != null ? maxDailyJobs : 6;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public TechnicianStatus getStatus() {
        return status;
    }

    public void setStatus(TechnicianStatus status) {
        this.status = status;
    }

    public Double getCurrentLatitude() {
        return currentLatitude;
    }

    public void setCurrentLatitude(Double currentLatitude) {
        this.currentLatitude = currentLatitude;
    }

    public Double getCurrentLongitude() {
        return currentLongitude;
    }

    public void setCurrentLongitude(Double currentLongitude) {
        this.currentLongitude = currentLongitude;
    }

    public Integer getMaxDailyJobs() {
        return maxDailyJobs;
    }

    public void setMaxDailyJobs(Integer maxDailyJobs) {
        this.maxDailyJobs = maxDailyJobs;
    }

    public List<TechnicianSkill> getTechnicianSkills() {
        return technicianSkills;
    }

    public void setTechnicianSkills(List<TechnicianSkill> technicianSkills) {
        this.technicianSkills = technicianSkills;
    }
}
