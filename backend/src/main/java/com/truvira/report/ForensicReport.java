package com.truvira.report;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;

/**
 * The SHAPE of the XML report. JAXB reads these annotations and writes the XML for us
 * (like Jackson does for JSON). In simple words: each class = one block of tags.
 *
 *   @XmlRootElement  = the outermost tag of the whole file: <forensicReport>
 *   @XmlAccessorType(FIELD) = use the FIELDS directly (no getters needed)
 *   @XmlAttribute    = a value INSIDE the tag:   <evidence id="10">
 *   @XmlElement      = a tag of its own:         <sha256>aa83...</sha256>
 *   @XmlElementWrapper = an outer tag around a list: <evidenceItems> <evidence/> <evidence/> </evidenceItems>
 *
 * JAXB needs plain classes with a no-argument constructor (records don't work), so these are
 * simple classes with public fields. Fields left null are simply left out of the XML.
 * Dates are Strings (ISO format "2026-10-23T09:41:59") because JAXB can't write LocalDateTime by itself.
 */
@XmlRootElement(name = "forensicReport")
@XmlAccessorType(XmlAccessType.FIELD)
public class ForensicReport {

    @XmlAttribute public String generatedAt;
    @XmlAttribute public String tool = "Truvira AI";

    @XmlElement public String disclaimer;
    @XmlElement(name = "case") public CaseInfo caseInfo;

    @XmlElementWrapper(name = "evidenceItems")
    @XmlElement(name = "evidence")
    public List<EvidenceInfo> evidence = new ArrayList<>();

    // "static nested class" = a class inside a class that doesn't need an outer object. Used only by the report.
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class CaseInfo {
        @XmlAttribute public Long id;
        @XmlElement public String title;
        @XmlElement public String description;
        @XmlElement public String status;
        @XmlElement public String createdAt;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class EvidenceInfo {
        @XmlAttribute public Long id;
        @XmlElement public String originalFileName;
        @XmlElement public String storedFileName;
        @XmlElement public String contentType;
        @XmlElement public long fileSize;
        @XmlElement public String sha256;
        @XmlElement public String uploadedAt;
        @XmlElement public String uploadedBy;
        @XmlElement public Metadata metadata;
        @XmlElement public AiDetection aiDetection;
        @XmlElement public HumanReview humanReview;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Metadata {
        @XmlAttribute public Boolean exifFound;
        @XmlElement public Integer imageWidth;
        @XmlElement public Integer imageHeight;
        @XmlElement public String cameraMake;
        @XmlElement public String cameraModel;
        @XmlElement public String software;
        @XmlElement public String takenAt;
        @XmlElement public Double gpsLatitude;
        @XmlElement public Double gpsLongitude;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class AiDetection {
        @XmlAttribute public String status;
        @XmlElement public Double score;          // probability 0..1, NOT a verdict
        @XmlElement public String model;
        @XmlElement public String checkedAt;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    public static class HumanReview {
        @XmlElement public String conclusion;
        @XmlElement public String note;
        @XmlElement public String reviewer;
        @XmlElement public String reviewedAt;
    }
}
