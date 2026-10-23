package com.truvira.security;

import com.truvira.model.AppUser;
import com.truvira.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Creates the FIRST user ("admin") when the app starts with an empty user table.
 *
 * ApplicationRunner = Spring calls run(...) once, right after the app has started.
 * @Component = "Spring, create one object of this class" (like @Service, but for general helpers).
 *
 * The password is NOT written in the code:
 *   - if application.properties has  truvira.admin.password=...  that is used;
 *   - otherwise a RANDOM password is generated and printed ONCE in the log.
 * (Spring Boot itself does the same with its "Using generated security password" message.)
 */
@Component
public class AdminUserInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserInitializer.class);

    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final String configuredPassword;

    // "${truvira.admin.password:}" -> the setting, or "" (empty) if it isn't set.
    public AdminUserInitializer(AppUserRepository users, PasswordEncoder encoder,
                                @Value("${truvira.admin.password:}") String configuredPassword) {
        this.users = users;
        this.encoder = encoder;
        this.configuredPassword = configuredPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            return; // users already exist: do nothing
        }
        boolean generated = configuredPassword.isBlank();
        String password = generated ? randomPassword() : configuredPassword;

        // encoder.encode(...) = BCrypt hash. Only the HASH goes into the database.
        users.save(new AppUser("admin", encoder.encode(password), "INVESTIGATOR"));

        if (generated) {
            log.warn("Created first user 'admin' with generated password: {}  "
                    + "(shown only once; set truvira.admin.password to choose your own)", password);
        } else {
            log.info("Created first user 'admin' with the password from truvira.admin.password");
        }
    }

    /** 12 random bytes -> 16 URL-safe characters. SecureRandom = random numbers safe for passwords. */
    private static String randomPassword() {
        byte[] bytes = new byte[12];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
