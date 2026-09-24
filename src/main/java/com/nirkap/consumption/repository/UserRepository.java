package com.nirkap.consumption.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nirkap.consumption.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {
}
