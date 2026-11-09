package com.secondstitch.item;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data writes the SQL for these methods from their names.
 * Every lookup includes the owner, so one account can never reach another's pieces.
 */
public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    List<Item> findByOwnerIdAndCategoryOrderByCreatedAtDesc(Long ownerId, Category category);

    List<Item> findByOwnerId(Long ownerId);

    Optional<Item> findByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByOwnerIdIsNull();

    /** Gives every piece without an owner (made before accounts existed) to this account. */
    @Modifying
    @Query("update Item i set i.ownerId = :ownerId where i.ownerId is null")
    int claimUnowned(@Param("ownerId") Long ownerId);
}
