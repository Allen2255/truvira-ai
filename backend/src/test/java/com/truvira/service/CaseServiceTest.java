package com.truvira.service;

import com.truvira.dto.CaseSummary;
import com.truvira.dto.CreateCaseRequest;
import com.truvira.model.CaseStatus;
import com.truvira.model.InvestigationCase;
import com.truvira.repository.CaseRepository;
import com.truvira.repository.EvidenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CaseService.
 * @InjectMocks = "create a REAL CaseService and pass the @Mock objects into its constructor".
 */
@ExtendWith(MockitoExtension.class)
class CaseServiceTest {

    @Mock CaseRepository caseRepository;
    @Mock EvidenceRepository evidenceRepository;
    @InjectMocks CaseService service;

    @Test
    void newCaseStartsOpenWithTrimmedTitle() {
        when(caseRepository.save(any(InvestigationCase.class))).thenAnswer(call -> call.getArgument(0));

        CaseSummary created = service.create(new CreateCaseRequest("  Fake profile photo  ", null));

        assertEquals("Fake profile photo", created.title());
        assertEquals(CaseStatus.OPEN, created.status());
        assertEquals(0, created.evidenceCount());
        assertNotNull(created.createdAt());
    }

    @Test
    void unknownCaseThrowsNotFound() {
        when(caseRepository.findById(42L)).thenReturn(Optional.empty()); // "no row with id 42"

        NotFoundException ex = assertThrows(NotFoundException.class, () -> service.getEntity(42L));
        assertEquals("Case not found: 42", ex.getMessage());
    }
}
