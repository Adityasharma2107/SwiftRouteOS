package com.swiftroute.domain.repository;

import com.swiftroute.domain.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {
    Optional<Skill> findByCode(String code);
    boolean existsByCode(String code);
}
