package com.truvira.service;

import com.truvira.dto.CaseSummary;
import com.truvira.dto.CreateCaseRequest;
import com.truvira.model.CaseStatus;
import com.truvira.model.Evidence;
import com.truvira.model.InvestigationCase;
import com.truvira.repository.CaseRepository;
import com.truvira.repository.EvidenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Business logic for investigation cases.
 *
 * @Transactional = run the whole method inside ONE database transaction:
 *   either every change in it is saved (COMMIT) or, if an exception is thrown, none is (ROLLBACK).
 *   readOnly = true is a hint for methods that only read: Hibernate skips checking for changes.
 */
@Service
public class CaseService {

    private final CaseRepository caseRepository;
    private final EvidenceRepository evidenceRepository;

    public CaseService(CaseRepository caseRepository, EvidenceRepository evidenceRepository) {
        this.caseRepository = caseRepository;
        this.evidenceRepository = evidenceRepository;
    }

    @Transactional
    public CaseSummary create(CreateCaseRequest request) {
        InvestigationCase c = new InvestigationCase();
        c.setTitle(request.title().strip());   // strip() removes spaces around the title
        c.setDescription(request.description() == null ? null : request.description().strip());
        c.setStatus(CaseStatus.OPEN);          // every new case starts OPEN
        c.setCreatedAt(LocalDateTime.now());
        return CaseSummary.from(caseRepository.save(c), 0);
    }

    @Transactional(readOnly = true)
    public List<CaseSummary> listAll() {
        // .stream().map(...).toList() = turn each entity into a DTO (Java Streams API).
        // Note: this runs one COUNT query per case ("N+1 queries"). Fine for dozens of cases;
        // with thousands we would write one grouped query instead.
        return caseRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(c -> CaseSummary.from(c, evidenceRepository.countByInvestigationCaseId(c.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public CaseSummary getSummary(Long id) {
        InvestigationCase c = getEntity(id);
        return CaseSummary.from(c, evidenceRepository.countByInvestigationCaseId(id));
    }

    @Transactional
    public CaseSummary updateStatus(Long id, CaseStatus status) {
        InvestigationCase c = getEntity(id);
        c.setStatus(status);
        // No save() call needed! Inside a transaction, Hibernate tracks loaded entities
        // ("dirty checking") and runs the UPDATE automatically when the method finishes.
        return CaseSummary.from(c, evidenceRepository.countByInvestigationCaseId(id));
    }

    @Transactional(readOnly = true)
    public List<Evidence> evidenceFor(Long id) {
        getEntity(id); // throws 404 if the case doesn't exist (instead of silently returning [])
        return evidenceRepository.findByInvestigationCaseIdOrderByUploadedAtDesc(id);
    }

    /** Used here and by EvidenceService when an upload names a case. */
    public InvestigationCase getEntity(Long id) {
        return caseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Case not found: " + id));
    }
}
