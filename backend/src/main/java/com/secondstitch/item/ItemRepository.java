package com.secondstitch.item;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Spring Data writes the SQL for these methods from their names. */
public interface ItemRepository extends JpaRepository<Item, Long> {

    List<Item> findAllByOrderByCreatedAtDesc();

    List<Item> findByCategoryOrderByCreatedAtDesc(Category category);
}
