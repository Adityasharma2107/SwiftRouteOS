package com.swiftroute.domain.repository;

import com.swiftroute.domain.entity.Technician;
import com.swiftroute.domain.enums.TechnicianStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TechnicianRepository extends JpaRepository<Technician, Long> {
    List<Technician> findByStatus(TechnicianStatus status);
    Optional<Technician> findByUserId(Long userId);

    /**
     * Find all technicians who hold a given skill ID and have status AVAILABLE or BUSY.
     */
    @Query("""
        SELECT DISTINCT t FROM Technician t
        JOIN t.technicianSkills ts
        WHERE ts.skill.id = :skillId
        AND t.status <> 'OFF_DUTY'
    """)
    List<Technician> findEligibleTechniciansBySkill(@Param("skillId") Long skillId);
}
