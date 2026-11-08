package com.secondstitch.outfit;

import com.secondstitch.item.Item;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderColumn;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A saved look made from items in your closet.
 * Many outfits can share the same item, so this is a many-to-many
 * relationship, stored in the "outfit_items" join table.
 */
@Entity
public class Outfit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** e.g. "rainy day", "date night", "sensory-friendly / soft fabrics" */
    private String occasion;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "outfit_items")
    @OrderColumn(name = "position")
    private List<Item> items = new ArrayList<>();

    private int wearCount;

    private Instant createdAt = Instant.now();

    /** Wearing an outfit counts as wearing every piece in it. */
    public void recordWear() {
        this.wearCount++;
        items.forEach(Item::recordWear);
    }

    /** Undo wearing the outfit: one wear off the outfit and off every piece in it. */
    public void removeWear() {
        if (this.wearCount > 0) {
            this.wearCount--;
            items.forEach(Item::removeWear);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getOccasion() { return occasion; }
    public void setOccasion(String occasion) { this.occasion = occasion; }

    public List<Item> getItems() { return items; }
    public void setItems(List<Item> items) { this.items = items; }

    public int getWearCount() { return wearCount; }
    public void setWearCount(int wearCount) { this.wearCount = wearCount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
