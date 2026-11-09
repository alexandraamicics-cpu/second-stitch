package com.secondstitch.outfit;

import com.secondstitch.item.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OutfitRepository extends JpaRepository<Outfit, Long> {

    List<Outfit> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    Optional<Outfit> findByIdAndOwnerId(Long id, Long ownerId);

    List<Outfit> findByItemsContaining(Item item);

    /** Gives every outfit without an owner (made before accounts existed) to this account. */
    @Modifying
    @Query("update Outfit o set o.ownerId = :ownerId where o.ownerId is null")
    int claimUnowned(@Param("ownerId") Long ownerId);
}
