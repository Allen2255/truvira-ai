package com.truvira.repository;

import com.truvira.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    // SELECT * FROM app_user WHERE username = ?   (Optional = maybe there is no such user)
    Optional<AppUser> findByUsername(String username);
}
