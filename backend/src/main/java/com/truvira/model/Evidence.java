package com.truvira.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * ENTITY = a Java class that maps to ONE database table.
 * Each object of this class = one ROW in the table. Each field = one COLUMN.
 * Hibernate (the JPA engine) reads these annotations and creates/uses the table for us.
 *
 * This represents one uploaded piece of evidence (an image).
 */
@Entity                          // "this class is a DB table"
@Table(name = "evidence")        // table name in MySQL
public class Evidence {

    @Id                                                    // primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY)    // MySQL auto-increments it (1,2,3...)
    private Long id;

    @Column(nullable = false)
    private String originalFileName;   // name the user uploaded, e.g. "photo.jpg"

    @Column(nullable = false)
    private String storedFileName;     // random name we save it under on disk (safe, no clashes)

    private String contentType;        // e.g. "image/jpeg"

    private long fileSize;             // size in bytes

    // SHA-256 hash is always 64 hex characters. unique=false because the same file may be
    // uploaded twice (we can detect duplicates later). length=64 sets VARCHAR(64).
    @Column(nullable = false, length = 64)
    private String sha256;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;  // when we received it (chain-of-custody timestamp)

    @Column(length = 50)
    private String uploadedBy;         // Slice 7: logged-in user who uploaded it (null for older rows)

    // ---------- METADATA (Slice 3) ----------
    // Copied from ImageMetadata at upload time. Hibernate's ddl-auto=update adds these columns
    // to the existing table automatically. Old rows get NULL (they were uploaded before this feature).
    // Wrapper types (Boolean, Integer, Double) because a primitive (boolean, int) cannot be NULL.
    private Boolean exifFound;
    private String cameraMake;
    private String cameraModel;
    private String software;
    private String takenAt;            // kept as the camera's own text: cameras have no timezone, so we don't "convert" it
    private Integer imageWidth;
    private Integer imageHeight;
    private Double gpsLatitude;
    private Double gpsLongitude;

    // ---------- AI DETECTOR (Slice 5) ----------
    // aiScore = the model's probability (0..1) that the image is AI-generated. A GUESS, never a verdict.
    // All null = never checked (uploaded before Slice 5).
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DetectionStatus aiStatus;
    private Double aiScore;
    private String aiModel;            // which model answered: results from different models aren't comparable
    private LocalDateTime aiCheckedAt; // when the check ran (for the report)

    // ---------- HUMAN REVIEW (Slice 6) ----------
    // The reviewer's decision lives NEXT TO the AI score, but separately: the AI suggests, the human decides.
    // All null = not reviewed yet.
    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private ReviewConclusion reviewConclusion;
    @Column(length = 2000)
    private String reviewNote;         // the reviewer's reasoning in their own words
    @Column(length = 100)
    private String reviewedBy;         // typed name for now; comes from the login in Slice 7
    private LocalDateTime reviewedAt;

    // ---------- CASE LINK (Slice 4) ----------
    /**
     * @ManyToOne = MANY evidence rows can point to ONE case. This side OWNS the relationship:
     * @JoinColumn(name = "case_id") creates the FOREIGN KEY column in the evidence table.
     * nullable by default -> evidence uploaded before Slice 4 (or without a case) has case_id = NULL.
     *
     * fetch = LAZY: Hibernate does NOT load the case from the DB until someone calls a method on it.
     * (The default for @ManyToOne is EAGER = always join the case table, even when nobody needs it.)
     *
     * @JsonIgnore: we don't put the whole case object inside every evidence JSON (it could loop back).
     * Instead getCaseId() below sends just the number.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id")
    @JsonIgnore
    private InvestigationCase investigationCase;

    // JPA REQUIRES a no-argument constructor (it creates empty objects, then fills the fields).
    public Evidence() {
    }

    // Getters and setters: how other classes read/write the private fields.
    // Spring also uses getters to turn this object into JSON for the API response.
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOriginalFileName() { return originalFileName; }
    public void setOriginalFileName(String originalFileName) { this.originalFileName = originalFileName; }

    public String getStoredFileName() { return storedFileName; }
    public void setStoredFileName(String storedFileName) { this.storedFileName = storedFileName; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public String getUploadedBy() { return uploadedBy; }
    public void setUploadedBy(String uploadedBy) { this.uploadedBy = uploadedBy; }

    public Boolean getExifFound() { return exifFound; }
    public void setExifFound(Boolean exifFound) { this.exifFound = exifFound; }

    public String getCameraMake() { return cameraMake; }
    public void setCameraMake(String cameraMake) { this.cameraMake = cameraMake; }

    public String getCameraModel() { return cameraModel; }
    public void setCameraModel(String cameraModel) { this.cameraModel = cameraModel; }

    public String getSoftware() { return software; }
    public void setSoftware(String software) { this.software = software; }

    public String getTakenAt() { return takenAt; }
    public void setTakenAt(String takenAt) { this.takenAt = takenAt; }

    public Integer getImageWidth() { return imageWidth; }
    public void setImageWidth(Integer imageWidth) { this.imageWidth = imageWidth; }

    public Integer getImageHeight() { return imageHeight; }
    public void setImageHeight(Integer imageHeight) { this.imageHeight = imageHeight; }

    public Double getGpsLatitude() { return gpsLatitude; }
    public void setGpsLatitude(Double gpsLatitude) { this.gpsLatitude = gpsLatitude; }

    public Double getGpsLongitude() { return gpsLongitude; }
    public void setGpsLongitude(Double gpsLongitude) { this.gpsLongitude = gpsLongitude; }

    public InvestigationCase getInvestigationCase() { return investigationCase; }
    public void setInvestigationCase(InvestigationCase investigationCase) { this.investigationCase = investigationCase; }

    /**
     * Shows up in the JSON as "caseId": 3 (or null). Jackson turns every public getX() into a field "x".
     * Reading only the id of a lazy case does not trigger an extra database query.
     */
    public Long getCaseId() {
        return investigationCase == null ? null : investigationCase.getId();
    }

    public DetectionStatus getAiStatus() { return aiStatus; }
    public Double getAiScore() { return aiScore; }
    public String getAiModel() { return aiModel; }
    public LocalDateTime getAiCheckedAt() { return aiCheckedAt; }

    public ReviewConclusion getReviewConclusion() { return reviewConclusion; }
    public String getReviewNote() { return reviewNote; }
    public String getReviewedBy() { return reviewedBy; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }

    /** Record (or replace) the human review. Time is set by the server, not by the client. */
    public void applyReview(ReviewConclusion conclusion, String note, String reviewer) {
        this.reviewConclusion = conclusion;
        this.reviewNote = note;
        this.reviewedBy = reviewer;
        this.reviewedAt = LocalDateTime.now();
    }

    /** Copy the detector's answer into this entity (same idea as applyMetadata). */
    public void applyDetection(DetectionResult r) {
        this.aiStatus = r.status();
        this.aiScore = r.aiScore();
        this.aiModel = r.model();
        this.aiCheckedAt = r.checkedAt();
    }

    /** Copy every value from the metadata record into this entity (keeps EvidenceService short). */
    public void applyMetadata(ImageMetadata m) {
        this.exifFound = m.exifFound();     // record "getters" have no "get" prefix
        this.cameraMake = m.cameraMake();
        this.cameraModel = m.cameraModel();
        this.software = m.software();
        this.takenAt = m.takenAt();
        this.imageWidth = m.imageWidth();
        this.imageHeight = m.imageHeight();
        this.gpsLatitude = m.gpsLatitude();
        this.gpsLongitude = m.gpsLongitude();
    }
}
