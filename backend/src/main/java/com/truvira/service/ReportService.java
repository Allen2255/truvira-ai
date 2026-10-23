package com.truvira.service;

import com.truvira.model.Evidence;
import com.truvira.model.InvestigationCase;
import com.truvira.report.ForensicReport;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Builds the XML forensic report for one case.
 * Step 1: copy data from the entities into the ForensicReport classes.
 * Step 2: "MARSHAL" them = JAXB's word for "turn Java objects into XML text".
 */
@Service
public class ReportService {

    static final String DISCLAIMER =
            "AI detection scores are probabilistic estimates from a pretrained model and can be wrong, "
          + "especially for compressed images or new generators. Missing EXIF metadata is not proof of "
          + "manipulation. Conclusions in this report are the human reviewer's judgement.";

    private final CaseService caseService;
    private final JAXBContext jaxb;

    public ReportService(CaseService caseService) throws JAXBException {
        this.caseService = caseService;
        // JAXBContext reads the annotations ONCE (slow) and can then be reused for every report (fast).
        this.jaxb = JAXBContext.newInstance(ForensicReport.class);
    }

    @Transactional(readOnly = true)
    public String caseReportXml(Long caseId) {
        InvestigationCase c = caseService.getEntity(caseId); // 404 if the case doesn't exist

        ForensicReport report = new ForensicReport();
        report.generatedAt = LocalDateTime.now().toString();
        report.disclaimer = DISCLAIMER;

        report.caseInfo = new ForensicReport.CaseInfo();
        report.caseInfo.id = c.getId();
        report.caseInfo.title = c.getTitle();
        report.caseInfo.description = c.getDescription();
        report.caseInfo.status = c.getStatus().name();
        report.caseInfo.createdAt = text(c.getCreatedAt());

        for (Evidence e : caseService.evidenceFor(caseId)) {
            report.evidence.add(toInfo(e));
        }

        try {
            Marshaller marshaller = jaxb.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true); // indent nicely
            marshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            StringWriter out = new StringWriter();
            marshaller.marshal(report, out);
            return out.toString();
        } catch (JAXBException ex) {
            // Wrap the CHECKED JAXBException in an unchecked one: this is a bug on our side (-> 500).
            throw new IllegalStateException("Could not build XML report", ex);
        }
    }

    private ForensicReport.EvidenceInfo toInfo(Evidence e) {
        ForensicReport.EvidenceInfo info = new ForensicReport.EvidenceInfo();
        info.id = e.getId();
        info.originalFileName = e.getOriginalFileName();
        info.storedFileName = e.getStoredFileName();
        info.contentType = e.getContentType();
        info.fileSize = e.getFileSize();
        info.sha256 = e.getSha256();
        info.uploadedAt = text(e.getUploadedAt());
        info.uploadedBy = e.getUploadedBy();

        if (e.getExifFound() != null) { // null = uploaded before Slice 3, never checked -> leave the block out
            ForensicReport.Metadata m = new ForensicReport.Metadata();
            m.exifFound = e.getExifFound();
            m.imageWidth = e.getImageWidth();
            m.imageHeight = e.getImageHeight();
            m.cameraMake = e.getCameraMake();
            m.cameraModel = e.getCameraModel();
            m.software = e.getSoftware();
            m.takenAt = e.getTakenAt();
            m.gpsLatitude = e.getGpsLatitude();
            m.gpsLongitude = e.getGpsLongitude();
            info.metadata = m;
        }

        ForensicReport.AiDetection ai = new ForensicReport.AiDetection();
        ai.status = e.getAiStatus() == null ? "NOT_RUN" : e.getAiStatus().name();
        ai.score = e.getAiScore();
        ai.model = e.getAiModel();
        ai.checkedAt = text(e.getAiCheckedAt());
        info.aiDetection = ai;

        if (e.getReviewConclusion() != null) {
            ForensicReport.HumanReview r = new ForensicReport.HumanReview();
            r.conclusion = e.getReviewConclusion().name();
            r.note = e.getReviewNote();
            r.reviewer = e.getReviewedBy();
            r.reviewedAt = text(e.getReviewedAt());
            info.humanReview = r;
        }
        return info;
    }

    /** LocalDateTime -> "2026-10-23T09:41:59.386" (or null). Objects.toString(x, null) avoids the text "null". */
    private static String text(LocalDateTime t) {
        return Objects.toString(t, null);
    }
}
