package com.truvira.repository;

import com.truvira.model.InvestigationCase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Same idea as EvidenceRepository: an interface, Spring Data JPA writes the implementation.
 * JpaRepository<InvestigationCase, Long> = manages InvestigationCase rows, primary key type Long.
 */
@Repository
public interface CaseRepository extends JpaRepository<InvestigationCase, Long> {

    // Derived query: SELECT * FROM investigation_case ORDER BY created_at DESC
    List<InvestigationCase> findAllByOrderByCreatedAtDesc();
}
