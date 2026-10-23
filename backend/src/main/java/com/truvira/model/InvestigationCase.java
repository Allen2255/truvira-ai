package com.truvira.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * An INVESTIGATION CASE groups related evidence, e.g. "Case #3: suspected fake profile photo".
 * Named InvestigationCase because "case" is a reserved word in Java (switch/case) and in SQL.
 *
 * RELATIONSHIP: one case has MANY evidence items; each evidence item belongs to at most ONE case.
 *   InvestigationCase  1 ────< *  Evidence
 * In the database this is stored as a FOREIGN KEY column "case_id" in the evidence table
 * (the "many" side always holds the foreign key).
 */
@Entity
@Table(name = "investigation_case")
public class InvestigationCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;              // e.g. "Suspected fake profile photo"

    @Column(length = 2000)
    private String description;        // free text from the investigator

    // @Enumerated(EnumType.STRING) stores the NAME ("OPEN") in the column.
    // The default (ORDINAL) would store 0/1/2: if someone later reorders the enum, every old row
    // silently changes meaning. STRING is safer and readable in MySQL.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CaseStatus status = CaseStatus.OPEN;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * @OneToMany(mappedBy = "investigationCase") = the "one" side of the relationship.
     * mappedBy names the FIELD in Evidence that owns the foreign key, so JPA does NOT create a
     * second link table. This list is filled by Hibernate when we read it (lazy loading).
     *
     * @JsonIgnore: without it, turning a case into JSON would include its evidence, and each
     * evidence would include its case, which includes its evidence... an INFINITE LOOP.
     * The API sends case data through DTOs instead (see model/CaseSummary).
     */
    @OneToMany(mappedBy = "investigationCase")
    @JsonIgnore
    private List<Evidence> evidence = new ArrayList<>();

    public InvestigationCase() {
    }

    public Long getId() { return id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public CaseStatus getStatus() { return status; }
    public void setStatus(CaseStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<Evidence> getEvidence() { return evidence; }
}
