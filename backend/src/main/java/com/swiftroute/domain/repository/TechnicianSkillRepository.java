package com.swiftroute.domain.repository;

import com.swiftroute.domain.entity.TechnicianSkill;
import com.swiftroute.domain.entity.TechnicianSkillId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TechnicianSkillRepository extends JpaRepository<TechnicianSkill, TechnicianSkillId> {
    List<TechnicianSkill> findByTechnicianId(Long technicianId);
}
