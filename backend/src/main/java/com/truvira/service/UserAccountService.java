package com.truvira.service;

import com.truvira.model.AppUser;
import com.truvira.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for user accounts. For now: changing your own password.
 */
@Service
public class UserAccountService {

    private final AppUserRepository users;
    private final PasswordEncoder encoder;

    public UserAccountService(AppUserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        AppUser user = users.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found: " + username));

        // encoder.matches(typed, storedHash) = hash what was typed and compare with the stored hash.
        // (We can never "decrypt" the stored hash; we can only compare.)
        if (!encoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is wrong");
        }
        if (currentPassword.equals(newPassword)) {
            throw new IllegalArgumentException("New password must be different from the current one");
        }
        // Store only the HASH of the new password. Dirty checking saves it when the transaction ends.
        user.setPasswordHash(encoder.encode(newPassword));
    }
}
