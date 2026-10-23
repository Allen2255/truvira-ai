package com.truvira.controller;

import com.truvira.dto.CaseSummary;
import com.truvira.dto.CreateCaseRequest;
import com.truvira.dto.UpdateStatusRequest;
import com.truvira.model.Evidence;
import com.truvira.service.CaseService;
import com.truvira.service.ReportService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for investigation cases. Thin, like EvidenceController: all logic is in CaseService.
 *
 * REST naming: the URL names the RESOURCE (a noun, plural), the HTTP METHOD says the action:
 *   POST   /api/cases                 create a case
 *   GET    /api/cases                 list cases
 *   GET    /api/cases/{id}            one case
 *   PATCH  /api/cases/{id}/status     change ONE field (PATCH = partial update; PUT would replace it all)
 *   GET    /api/cases/{id}/evidence   the evidence inside a case (a "sub-resource")
 */
@RestController
@RequestMapping("/api/cases")
@CrossOrigin(origins = "${truvira.frontend-origin:http://localhost:5173}") // see EvidenceController
public class CaseController {

    private final CaseService service;
    private final ReportService reportService; // Slice 6

    public CaseController(CaseService service, ReportService reportService) {
        this.service = service;
        this.reportService = reportService;
    }

    /**
     * @RequestBody = read the JSON body and convert it into a CreateCaseRequest (Jackson does this).
     * @Valid = run the validation annotations (@NotBlank, @Size) BEFORE this method runs.
     *          If they fail, Spring throws MethodArgumentNotValidException -> our handler returns 400.
     */
    @PostMapping
    public ResponseEntity<CaseSummary> create(@Valid @RequestBody CreateCaseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    public List<CaseSummary> list() {
        return service.listAll();
    }

    @GetMapping("/{id}")
    public CaseSummary get(@PathVariable Long id) {
        return service.getSummary(id);
    }

    @PatchMapping("/{id}/status")
    public CaseSummary updateStatus(@PathVariable Long id, @Valid @RequestBody UpdateStatusRequest request) {
        return service.updateStatus(id, request.status());
    }

    @GetMapping("/{id}/evidence")
    public List<Evidence> evidence(@PathVariable Long id) {
        return service.evidenceFor(id);
    }

    /**
     * GET /api/cases/1/report -> the XML forensic report as a downloadable FILE (Slice 6).
     *   Content-Type: application/xml        = "this is XML, not JSON"
     *   Content-Disposition: attachment; ... = "browser, SAVE this as a file with this name, don't display it"
     */
    @GetMapping("/{id}/report")
    public ResponseEntity<String> report(@PathVariable Long id) {
        String xml = reportService.caseReportXml(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"truvira-case-" + id + "-report.xml\"")
                .body(xml);
    }
}
