package com.momnect.userservice.command.repository;

import java.util.Optional;

import com.momnect.userservice.command.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByLoginId(String loginId);

    Optional<User> findByRefreshToken(String refreshToken);
}
