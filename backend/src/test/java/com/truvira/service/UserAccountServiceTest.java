package com.truvira.service;

import com.truvira.model.AppUser;
import com.truvira.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Tests for changing a password. The repository is a MOCK, but the encoder is the REAL BCrypt,
 * so we test real hashing. (The passwords below are made-up test values, not anyone's real password.)
 */
@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {

    @Mock AppUserRepository users;
    PasswordEncoder encoder = new BCryptPasswordEncoder();
    UserAccountService service;
    AppUser alice;

    @BeforeEach
    void setUp() {
        service = new UserAccountService(users, encoder);
        alice = new AppUser("alice", encoder.encode("old-test-pass"), "INVESTIGATOR");
        when(users.findByUsername("alice")).thenReturn(Optional.of(alice));
    }

    @Test
    void changesPasswordAndStoresOnlyAHash() {
        service.changePassword("alice", "old-test-pass", "new-test-pass");

        assertNotEquals("new-test-pass", alice.getPasswordHash());            // NOT stored as plain text
        assertTrue(encoder.matches("new-test-pass", alice.getPasswordHash()));  // new one works
        assertFalse(encoder.matches("old-test-pass", alice.getPasswordHash())); // old one no longer works
    }

    @Test
    void refusesWhenCurrentPasswordIsWrong() {
        String before = alice.getPasswordHash();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.changePassword("alice", "wrong-guess", "new-test-pass"));

        assertEquals("Current password is wrong", ex.getMessage());
        assertEquals(before, alice.getPasswordHash()); // unchanged
    }

    @Test
    void refusesReusingTheSamePassword() {
        assertThrows(IllegalArgumentException.class,
                () -> service.changePassword("alice", "old-test-pass", "old-test-pass"));
    }
}
