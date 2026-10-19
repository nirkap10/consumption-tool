package com.nirkap.consumption.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nirkap.consumption.entity.User;

public interface UserRepository extends JpaRepository<User, UUID> {

    // One UPDATE, so two reports at the same moment cannot overwrite each other.
    // No floor at zero: usage has already happened, so the balance may go negative.
    @Modifying
    @Query("UPDATE User u SET u.remainingCredit = u.remainingCredit - :tokens WHERE u.id = :id")
    void spend(@Param("id") UUID id, @Param("tokens") long tokens);

    // Takes the tokens only if there are enough, checked and taken in one UPDATE
    // so two reservations at the same moment cannot share the same credit.
    // Returns 1 when reserved, 0 when there was not enough.
    @Modifying
    @Query("UPDATE User u SET u.remainingCredit = u.remainingCredit - :tokens "
            + "WHERE u.id = :id AND u.remainingCredit >= :tokens")
    int reserve(@Param("id") UUID id, @Param("tokens") long tokens);

    // One UPDATE, for the same reason as spend().
    @Modifying
    @Query("UPDATE User u SET u.remainingCredit = u.remainingCredit + :tokens WHERE u.id = :id")
    void addCredit(@Param("id") UUID id, @Param("tokens") long tokens);

    @Query("SELECT u.remainingCredit FROM User u WHERE u.id = :id")
    long findRemainingCredit(@Param("id") UUID id);
}
