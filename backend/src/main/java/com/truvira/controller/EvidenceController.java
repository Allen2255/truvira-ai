package com.truvira.controller;

import com.truvira.dto.ReviewRequest;
import com.truvira.model.Evidence;
import com.truvira.service.EvidenceService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * CONTROLLER = the layer that receives HTTP requests from the outside (React, Postman)
 * and sends back HTTP responses. It should stay thin: pass the work to the service.
 *
 * @RestController = @Controller + @ResponseBody -> every return value is converted to JSON.
 * @RequestMapping("/api/evidence") = all URLs in this class start with /api/evidence.
 * @CrossOrigin = allows our React app (running on a different port, 5173) to call this API.
 *   Browsers block cross-origin calls by default (CORS security rule) unless the server allows it.
 *   "${truvira.frontend-origin:http://localhost:5173}" = read the allowed address from the settings
 *   (needed when the site is deployed online); the part after ":" is the default for local use.
 */
@RestController
@RequestMapping("/api/evidence")
@CrossOrigin(origins = "${truvira.frontend-origin:http://localhost:5173}")
public class EvidenceController {

    private final EvidenceService service;

    // Constructor injection again: Spring supplies the EvidenceService object.
    public EvidenceController(EvidenceService service) {
        this.service = service;
    }

    /**
     * POST /api/evidence/upload
     * consumes multipart/form-data = the format browsers use to upload files.
     * @RequestParam("file") = take the form field named "file" and give it to us as MultipartFile.
     * ResponseEntity<?> lets us control the HTTP status code (201 Created) along with the body.
     * Slice 4: an OPTIONAL form field "caseId" puts the evidence into that case.
     *   required = false -> if the field is missing, caseId is null (not an error).
     */
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<Evidence> upload(@RequestParam("file") MultipartFile file,
                                           @RequestParam(value = "caseId", required = false) Long caseId,
                                           Authentication auth) throws IOException {
        // Authentication = who is logged in (filled by Spring Security). auth.getName() = the username.
        Evidence saved = service.storeEvidence(file, caseId, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);   // 201 + JSON of saved row
    }

    /** GET /api/evidence  -> list of all evidence, newest first. */
    @GetMapping
    public List<Evidence> list() {
        return service.listAll();
    }

    /** GET /api/evidence/5  -> one record. @PathVariable pulls "5" out of the URL. */
    @GetMapping("/{id}")
    public Evidence get(@PathVariable Long id) {
        return service.getById(id);
    }

    /**
     * POST /api/evidence/5/detect -> run the AI check again for evidence #5 (Slice 5).
     * POST (not GET) because it CHANGES data: it stores a new result.
     */
    @PostMapping("/{id}/detect")
    public Evidence detect(@PathVariable Long id) {
        return service.rerunDetection(id);
    }

    /**
     * PUT /api/evidence/5/review -> save the human review of evidence #5 (Slice 6).
     * PUT (not POST) because sending the same review twice gives the same result:
     * it REPLACES the review instead of adding a second one. (This property is called "idempotent".)
     */
    @PutMapping("/{id}/review")
    public Evidence review(@PathVariable Long id, @Valid @RequestBody ReviewRequest request, Authentication auth) {
        // Slice 7: the reviewer is WHOEVER IS LOGGED IN, so nobody can sign a review with someone else's name.
        return service.saveReview(id, request.conclusion(), request.note(), auth.getName());
    }

    // Error handling moved to GlobalExceptionHandler (Slice 4) so it covers every controller.
}
