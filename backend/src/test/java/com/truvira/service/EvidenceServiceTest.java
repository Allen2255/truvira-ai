package com.truvira.service;

import com.truvira.model.CaseStatus;
import com.truvira.model.DetectionResult;
import com.truvira.model.DetectionStatus;
import com.truvira.model.Evidence;
import com.truvira.model.ImageMetadata;
import com.truvira.model.InvestigationCase;
import com.truvira.model.ReviewConclusion;
import com.truvira.repository.EvidenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * UNIT TESTS for EvidenceService: we test this ONE class on its own.
 *
 * JUnit 5:   @Test marks a test method; assertEquals / assertThrows check results.
 * Mockito:   @Mock creates a FAKE ("mock") of each dependency. A mock does nothing unless we
 *            tell it what to answer:  when(mock.method(...)).thenReturn(answer)
 *            and we can check how it was used:  verify(mock, never()).save(any())
 * So these tests need NO MySQL, NO Python AI service and NO running Spring Boot.
 *
 * @ExtendWith(MockitoExtension.class) = let JUnit create the @Mock objects before each test.
 * Pattern inside each test: ARRANGE (set up) -> ACT (call the method) -> ASSERT (check).
 */
@ExtendWith(MockitoExtension.class)
class EvidenceServiceTest {

    @Mock EvidenceRepository repository;
    @Mock MetadataService metadataService;
    @Mock CaseService caseService;
    @Mock DetectorClient detectorClient;

    // @TempDir = JUnit makes an empty temporary folder for each test and deletes it afterwards.
    @TempDir Path uploadDir;

    EvidenceService service;

    // @BeforeEach = runs before EVERY test, so each test starts fresh.
    @BeforeEach
    void setUp() throws Exception {
        service = new EvidenceService(repository, metadataService, caseService, detectorClient, uploadDir.toString());
    }

    // MockMultipartFile = a fake uploaded file (from Spring's test tools), no browser needed.
    private static MockMultipartFile jpeg(byte[] bytes) {
        return new MockMultipartFile("file", "photo.jpg", "image/jpeg", bytes);
    }

    private static InvestigationCase caseWithStatus(CaseStatus status) {
        InvestigationCase c = new InvestigationCase();
        c.setStatus(status);
        return c;
    }

    private long filesOnDisk() throws Exception {
        try (Stream<Path> files = Files.list(uploadDir)) { // try-with-resources closes the folder listing
            return files.count();
        }
    }

    @Test
    void rejectsFilesThatAreNotImages() {
        MockMultipartFile pdf = new MockMultipartFile("file", "report.pdf", "application/pdf", "%PDF-1.7".getBytes());

        // assertThrows = "this code MUST throw this exception"; it also returns it so we can check the message.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.storeEvidence(pdf, null, "alice"));

        assertTrue(ex.getMessage().contains("Only JPEG"));
        verifyNoInteractions(repository); // nothing was saved to the database
    }

    @Test
    void rejectsEmptyFiles() {
        assertThrows(IllegalArgumentException.class, () -> service.storeEvidence(jpeg(new byte[0]), null, "alice"));
    }

    @Test
    void storesTheFileWithTheCorrectSha256AndUploader() throws Exception {
        // ARRANGE
        byte[] bytes = "pretend these are image bytes".getBytes(StandardCharsets.UTF_8);
        when(metadataService.extract(any())).thenReturn(ImageMetadata.empty());
        when(detectorClient.detect(any())).thenReturn(DetectionResult.unavailable("AI service off"));
        // thenAnswer: return the same object save() was given (like the real repository does).
        when(repository.save(any(Evidence.class))).thenAnswer(call -> call.getArgument(0));

        // ACT
        Evidence saved = service.storeEvidence(jpeg(bytes), null, "alice");

        // ASSERT: the hash must equal SHA-256 calculated independently here.
        String expected = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        assertEquals(expected, saved.getSha256());
        assertEquals("alice", saved.getUploadedBy());
        assertEquals("photo.jpg", saved.getOriginalFileName());
        // Stored under a random UUID name, never the user's name:
        assertNotEquals("photo.jpg", saved.getStoredFileName());
        assertTrue(saved.getStoredFileName().endsWith(".jpg"));
        assertTrue(Files.exists(uploadDir.resolve(saved.getStoredFileName())));
        // AI service was "off": evidence still saved, with UNAVAILABLE and NO made-up score.
        assertEquals(DetectionStatus.UNAVAILABLE, saved.getAiStatus());
        assertNull(saved.getAiScore());
    }

    @Test
    void refusesUploadIntoAClosedCaseAndWritesNoFile() throws Exception {
        when(caseService.getEntity(7L)).thenReturn(caseWithStatus(CaseStatus.CLOSED));

        assertThrows(IllegalArgumentException.class,
                () -> service.storeEvidence(jpeg("x".getBytes()), 7L, "alice"));

        assertEquals(0, filesOnDisk());               // no orphan file left behind
        verify(repository, never()).save(any());      // nothing saved
    }

    @Test
    void unknownCaseGivesNotFound() throws Exception {
        when(caseService.getEntity(99L)).thenThrow(new NotFoundException("Case not found: 99"));

        assertThrows(NotFoundException.class, () -> service.storeEvidence(jpeg("x".getBytes()), 99L, "alice"));
        assertEquals(0, filesOnDisk());
    }

    @Test
    void reviewRecordsTheLoggedInReviewer() {
        Evidence evidence = new Evidence();
        when(repository.findById(5L)).thenReturn(Optional.of(evidence));
        when(repository.save(evidence)).thenReturn(evidence);

        Evidence result = service.saveReview(5L, ReviewConclusion.LIKELY_AI_GENERATED, "  AI score 98%  ", "bob");

        assertEquals(ReviewConclusion.LIKELY_AI_GENERATED, result.getReviewConclusion());
        assertEquals("AI score 98%", result.getReviewNote()); // spaces trimmed
        assertEquals("bob", result.getReviewedBy());
        assertNotNull(result.getReviewedAt());                 // time set by the server
    }

    @Test
    void reviewIsRefusedWhenTheCaseIsClosed() {
        Evidence evidence = new Evidence();
        evidence.setInvestigationCase(caseWithStatus(CaseStatus.CLOSED));
        when(repository.findById(5L)).thenReturn(Optional.of(evidence));

        assertThrows(IllegalArgumentException.class,
                () -> service.saveReview(5L, ReviewConclusion.LIKELY_AUTHENTIC, null, "bob"));
        verify(repository, never()).save(any());
    }
}
