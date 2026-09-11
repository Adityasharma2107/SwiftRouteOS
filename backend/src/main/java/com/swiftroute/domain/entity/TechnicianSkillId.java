package com.swiftroute.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/**
 * Composite primary key for TechnicianSkill join table.
 */
@Embeddable
public class TechnicianSkillId implements Serializable {

    @Column(name = "technician_id")
    private Long technicianId;

    @Column(name = "skill_id")
    private Long skillId;

    public TechnicianSkillId() {
    }

    public TechnicianSkillId(Long technicianId, Long skillId) {
        this.technicianId = technicianId;
        this.skillId = skillId;
    }

    public Long getTechnicianId() {
        return technicianId;
    }

    public void setTechnicianId(Long technicianId) {
        this.technicianId = technicianId;
    }

    public Long getSkillId() {
        return skillId;
    }

    public void setSkillId(Long skillId) {
        this.skillId = skillId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TechnicianSkillId that)) return false;
        return Objects.equals(technicianId, that.technicianId) &&
               Objects.equals(skillId, that.skillId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(technicianId, skillId);
    }
}
