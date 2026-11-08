package com.secondstitch.outfit;

import com.secondstitch.item.Item;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutfitRepository extends JpaRepository<Outfit, Long> {

    List<Outfit> findAllByOrderByCreatedAtDesc();

    List<Outfit> findByItemsContaining(Item item);
}
