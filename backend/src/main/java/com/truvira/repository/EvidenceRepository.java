package com.truvira.repository;

import com.truvira.model.Evidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * REPOSITORY = the layer that talks to the database.
 *
 * Notice: this is an INTERFACE with NO code inside. Spring Data JPA generates the real
 * implementation at runtime. By extending JpaRepository<Evidence, Long> we get, for free:
 *   save(entity)      -> INSERT or UPDATE
 *   findById(id)      -> SELECT ... WHERE id = ?
 *   findAll()         -> SELECT * FROM evidence
 *   deleteById(id)    -> DELETE
 *   count()           -> SELECT COUNT(*)
 * <Evidence, Long> = "manages Evidence objects, whose primary key type is Long".
 */
@Repository
public interface EvidenceRepository extends JpaRepository<Evidence, Long> {

    // DERIVED QUERY: Spring reads the method NAME and builds the SQL itself:
    //   SELECT * FROM evidence WHERE sha256 = ?
    // Handy for finding if the exact same file was uploaded before.
    List<Evidence> findBySha256(String sha256);

    // Newest first: SELECT * FROM evidence ORDER BY uploaded_at DESC
    List<Evidence> findAllByOrderByUploadedAtDesc();

    // ---------- Slice 4 ----------
    // "InvestigationCaseId" = follow the field investigationCase, then its id. Spring builds:
    //   SELECT * FROM evidence WHERE case_id = ? ORDER BY uploaded_at DESC
    List<Evidence> findByInvestigationCaseIdOrderByUploadedAtDesc(Long caseId);

    // "count" prefix -> SELECT COUNT(*) FROM evidence WHERE case_id = ?   (returns a number, not rows)
    long countByInvestigationCaseId(Long caseId);
}
