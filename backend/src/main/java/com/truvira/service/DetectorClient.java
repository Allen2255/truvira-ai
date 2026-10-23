package com.truvira.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.truvira.model.DetectionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.file.Path;
import java.time.Duration;

/**
 * Calls the Python AI service over HTTP. In simple words: Spring Boot becomes the CLIENT here
 * (the one asking), and Python is the SERVER (the one answering), the same way React asks Spring Boot.
 *
 * FALLBACK: if Python is switched off we do NOT crash and do NOT make up a score.
 * We return UNAVAILABLE, the evidence is still saved, and the check can be re-run later.
 */
@Service
public class DetectorClient {

    private static final Logger log = LoggerFactory.getLogger(DetectorClient.class);

    private final RestClient restClient;

    /**
     * RestClient = Spring's tool for SENDING HTTP requests (Spring 6.1+), like fetch() in api.js.
     * RestClient.Builder is provided by Spring Boot already set up with its JSON converter (Jackson).
     *
     * @Value("${truvira.detector.url:http://localhost:8000}") = read the setting from
     * application.properties; the part after ":" is the DEFAULT if the setting is missing.
     */
    public DetectorClient(RestClient.Builder builder,
                          @Value("${truvira.detector.url:http://localhost:8000}") String detectorUrl) {
        // TIMEOUTS: how long we are willing to wait.
        //   connect = 2 s  -> if Python is off, we find out fast instead of hanging.
        //   read    = 60 s -> the model can take a while on a slow CPU (longest the first time).
        SimpleClientHttpRequestFactory timeouts = new SimpleClientHttpRequestFactory();
        timeouts.setConnectTimeout(Duration.ofSeconds(2));
        timeouts.setReadTimeout(Duration.ofSeconds(60));

        this.restClient = builder.baseUrl(detectorUrl).requestFactory(timeouts).build();
    }

    /** Send the saved image file to Python's POST /detect and turn the answer into a DetectionResult. */
    public DetectionResult detect(Path imageFile) {
        // Build a multipart/form-data body with one field "file" (the same format React uses to upload).
        // MultiValueMap = a map where one key can hold several values (forms allow that).
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("file", new FileSystemResource(imageFile)); // FileSystemResource = "send this file from disk"

        try {
            DetectorResponse answer = restClient.post()
                    .uri("/detect")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(form)
                    .retrieve()                       // send it; a 4xx/5xx answer throws RestClientException
                    .body(DetectorResponse.class);    // JSON -> Java record (Jackson)

            if (answer == null || answer.aiScore() == null) {
                return DetectionResult.failed("Detector returned no score");
            }
            return DetectionResult.completed(answer.aiScore(), answer.model());

        } catch (ResourceAccessException e) {
            // Could not CONNECT at all (service off, wrong port) or it timed out.
            log.warn("AI detector unavailable: {}", e.getMessage());
            return DetectionResult.unavailable("AI detector service is not running");
        } catch (RestClientException e) {
            // Connected, but Python answered with an error (e.g. 400 "Not a readable image").
            log.warn("AI detector error: {}", e.getMessage());
            return DetectionResult.failed("AI detector returned an error");
        }
    }

    /**
     * The JSON Python sends back. We only need two of its fields.
     * @JsonIgnoreProperties(ignoreUnknown = true) = ignore extra fields ("labels", "elapsedMs")
     * instead of failing. A NESTED record: it's only used inside this class.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record DetectorResponse(Double aiScore, String model) {
    }
}
