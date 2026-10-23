package com.truvira.controller;

import com.truvira.dto.ChangePasswordRequest;
import com.truvira.service.UserAccountService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * GET /api/auth/me -> {"username": "admin"}
 * React calls this right after the user types their login: if it answers 200 the login is
 * correct; if Spring Security answers 401, it's wrong.
 *
 * "Authentication auth" = Spring fills this parameter with WHO is logged in for this request.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "${truvira.frontend-origin:http://localhost:5173}") // see EvidenceController
public class AuthController {

    private final UserAccountService accounts;

    public AuthController(UserAccountService accounts) {
        this.accounts = accounts;
    }

    @GetMapping("/me")
    public Map<String, String> me(Authentication auth) {
        return Map.of("username", auth.getName());
    }

    /**
     * PUT /api/auth/password -> change the LOGGED-IN user's own password.
     * The username comes from the login, so you can only ever change YOUR password.
     */
    @PutMapping("/password")
    public Map<String, String> changePassword(@Valid @RequestBody ChangePasswordRequest request, Authentication auth) {
        accounts.changePassword(auth.getName(), request.currentPassword(), request.newPassword());
        return Map.of("message", "Password changed");
    }
}
