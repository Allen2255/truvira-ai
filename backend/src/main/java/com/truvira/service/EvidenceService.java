package com.truvira.service;

import com.truvira.model.CaseStatus;
import com.truvira.model.Evidence;
import com.truvira.model.InvestigationCase;
import com.truvira.model.ReviewConclusion;
import com.truvira.repository.EvidenceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * SERVICE = the BUSINESS LOGIC layer. The controller receives the request, the service decides
 * what to do (validate, hash, store), the repository saves to the DB.
 * Keeping logic here (not in the controller) makes it reusable and easy to unit-test.
 */
@Service
public class EvidenceService {

    // Only these file types are allowed. Set = a collection with no duplicates, fast lookup.
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final EvidenceRepository repository;
    private final MetadataService metadataService; // Slice 3: reads EXIF from the saved file
    private final CaseService caseService;         // Slice 4: looks up the case an upload belongs to
    private final DetectorClient detectorClient;   // Slice 5: asks the Python AI service
    private final Path uploadDir;

    /**
     * CONSTRUCTOR INJECTION (dependency injection):
     * We don't write "new EvidenceRepository()". Spring creates it and passes it in here.
     * This is the "Inversion of Control" idea: Spring controls object creation.
     *
     * @Value reads "truvira.upload-dir" from application.properties.
     * MetadataService is also a @Service, so Spring injects it the same way (no "new").
     */
    public EvidenceService(EvidenceRepository repository,
                           MetadataService metadataService,
                           CaseService caseService,
                           DetectorClient detectorClient,
                           @Value("${truvira.upload-dir}") String uploadDir) throws IOException {
        this.repository = repository;
        this.metadataService = metadataService;
        this.caseService = caseService;
        this.detectorClient = detectorClient;
        // toAbsolutePath().normalize() cleans the path (removes "../" tricks).
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadDir); // make the folder if it doesn't exist
    }

    /**
     * Full flow: validate -> save file safely while hashing -> save record to DB.
     * caseId may be null (evidence not assigned to any case).
     * uploadedBy = the logged-in user's name (Slice 7), taken from the login, never from the form.
     */
    public Evidence storeEvidence(MultipartFile file, Long caseId, String uploadedBy) throws IOException {

        // ---- 1. VALIDATION ----
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file uploaded or file is empty.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Only JPEG, PNG or WEBP images are allowed.");
        }
        // Slice 4: look the case up BEFORE writing anything to disk, so a wrong caseId
        // (404) never leaves an orphan file behind. A closed case accepts no new evidence.
        InvestigationCase investigationCase = null;
        if (caseId != null) {
            investigationCase = caseService.getEntity(caseId);
            if (investigationCase.getStatus() == CaseStatus.CLOSED) {
                throw new IllegalArgumentException("Case " + caseId + " is closed; reopen it to add evidence.");
            }
        }

        // ---- 2. SAFE FILE NAME ----
        // NEVER trust the user's filename (it could be "../../hack.sh"). We generate our own
        // random name with UUID and keep only a safe extension.
        String original = file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename();
        String storedName = UUID.randomUUID() + extensionFor(contentType);
        Path target = uploadDir.resolve(storedName).normalize();

        // ---- 3. SAVE FILE + COMPUTE SHA-256 IN ONE PASS ----
        String hash = saveAndHash(file, target);

        // ---- 4. BUILD THE DB RECORD ----
        Evidence evidence = new Evidence();
        evidence.setOriginalFileName(original);
        evidence.setStoredFileName(storedName);
        evidence.setContentType(contentType);
        evidence.setFileSize(file.getSize());
        evidence.setSha256(hash);
        evidence.setUploadedAt(LocalDateTime.now());
        evidence.setUploadedBy(uploadedBy);
        evidence.setInvestigationCase(investigationCase); // sets the case_id foreign key (or NULL)

        // ---- 5. READ METADATA (Slice 3) ----
        // We read from the SAVED copy (after hashing), so the hash always describes the exact
        // bytes we received. Reading never modifies the file. If it fails we still save the evidence.
        evidence.applyMetadata(metadataService.extract(target));

        // ---- 6. AI DETECTOR (Slice 5) ----
        // Also on the saved copy. If Python is off this returns UNAVAILABLE quickly (2 s timeout)
        // and the evidence is STILL saved: the AI check is optional, the evidence is not.
        evidence.applyDetection(detectorClient.detect(target));

        // save() runs an SQL INSERT and returns the object WITH its generated id filled in.
        return repository.save(evidence);
    }

    public List<Evidence> listAll() {
        return repository.findAllByOrderByUploadedAtDesc();
    }

    public Evidence getById(Long id) {
        // findById returns Optional (a box that may be empty). orElseThrow opens it or throws.
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Evidence not found: " + id)); // -> 404
    }

    /**
     * Slice 6: save the human review. Calling it again REPLACES the previous review (hence PUT).
     * Evidence in a CLOSED case is frozen: reopen the case to change a review.
     * @Transactional keeps the database session open, so the LAZY case can be loaded to check its status.
     */
    @Transactional
    public Evidence saveReview(Long id, ReviewConclusion conclusion, String note, String reviewer) {
        Evidence evidence = getById(id);
        InvestigationCase c = evidence.getInvestigationCase();
        if (c != null && c.getStatus() == CaseStatus.CLOSED) {
            throw new IllegalArgumentException("Case " + c.getId() + " is closed; reopen it to change reviews.");
        }
        String cleanNote = (note == null || note.isBlank()) ? null : note.strip();
        evidence.applyReview(conclusion, cleanNote, reviewer.strip());
        return repository.save(evidence);
    }

    /**
     * Slice 5: run the AI check again for an existing record (e.g. it was UNAVAILABLE, or uploaded
     * before Slice 5). The stored file is never changed, so its SHA-256 stays the same.
     */
    public Evidence rerunDetection(Long id) {
        Evidence evidence = getById(id);
        Path file = uploadDir.resolve(evidence.getStoredFileName()).normalize();
        if (!Files.exists(file)) {
            throw new NotFoundException("Stored file for evidence " + id + " is missing");
        }
        evidence.applyDetection(detectorClient.detect(file));
        return repository.save(evidence); // save() on an existing id = SQL UPDATE
    }

    /**
     * Copies the upload to disk while a DigestInputStream feeds every byte into SHA-256.
     * "Streaming" means we never load the whole file into memory - good for big files.
     */
    private String saveAndHash(MultipartFile file, Path target) throws IOException {
        try {
            // MessageDigest = Java's built-in hashing tool (java.security package).
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            // try-with-resources: the stream is auto-closed at the end, even if an error happens.
            try (InputStream in = new DigestInputStream(file.getInputStream(), digest)) {
                // As Files.copy reads bytes from 'in', DigestInputStream also updates the digest.
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
            // digest.digest() finishes the calculation -> 32 raw bytes.
            // HexFormat turns those bytes into a readable 64-character hex string.
            return HexFormat.of().formatHex(digest.digest());

        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed on every Java install, so this should never happen.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
