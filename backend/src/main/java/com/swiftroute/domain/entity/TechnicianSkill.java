package com.swiftroute.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "technician_skills")
public class TechnicianSkill {

    @EmbeddedId
    private TechnicianSkillId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("technicianId")
    @JoinColumn(name = "technician_id")
    private Technician technician;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("skillId")
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @Column(name = "proficiency_level", nullable = false)
    private Integer proficiencyLevel = 1;

    public TechnicianSkill() {
    }

    public TechnicianSkill(Technician technician, Skill skill, Integer proficiencyLevel) {
        this.technician = technician;
        this.skill = skill;
        this.proficiencyLevel = proficiencyLevel;
        if (technician != null && skill != null) {
            this.id = new TechnicianSkillId(technician.getId(), skill.getId());
        }
    }

    public TechnicianSkillId getId() {
        return id;
    }

    public void setId(TechnicianSkillId id) {
        this.id = id;
    }

    public Technician getTechnician() {
        return technician;
    }

    public void setTechnician(Technician technician) {
        this.technician = technician;
    }

    public Skill getSkill() {
        return skill;
    }

    public void setSkill(Skill skill) {
        this.skill = skill;
    }

    public Integer getProficiencyLevel() {
        return proficiencyLevel;
    }

    public void setProficiencyLevel(Integer proficiencyLevel) {
        this.proficiencyLevel = proficiencyLevel;
    }
}
