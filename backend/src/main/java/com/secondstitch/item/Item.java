package com.secondstitch.item;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** One thrifted piece in your closet. JPA turns this class into the "item" table. */
@Entity
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    private String brand;
    private String size;

    /** A colour name you'd use ("sage", "oatmeal"). */
    private String color;

    /** Hex code used for the colour swatch when there's no photo, e.g. "#9CAF88". */
    private String colorHex;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal pricePaid;

    @Column(precision = 10, scale = 2)
    private BigDecimal estimatedRetail;

    /** Where you found it: a shop, ukay-ukay, online seller, swap... */
    private String store;

    private LocalDate dateFound;

    private int wearCount;

    /**
     * Changes every time a new photo is uploaded (null = no photo).
     * It goes in the photo's URL, so browsers fetch the new photo instead of a cached old one.
     * The photo itself is in the item_photo table (see ItemPhoto).
     */
    private String photoVersion;

    @Column(length = 1000)
    private String notes;

    /** The account this belongs to. Empty only for data made before accounts existed. */
    @Column(name = "owner_id")
    private Long ownerId;

    private Instant createdAt = Instant.now();

    public void recordWear() {
        this.wearCount++;
    }

    /** Undo a wear (e.g. after a mis-click). Never goes below zero. */
    public void removeWear() {
        if (this.wearCount > 0) {
            this.wearCount--;
        }
    }

    // --- getters and setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    public BigDecimal getPricePaid() { return pricePaid; }
    public void setPricePaid(BigDecimal pricePaid) { this.pricePaid = pricePaid; }

    public BigDecimal getEstimatedRetail() { return estimatedRetail; }
    public void setEstimatedRetail(BigDecimal estimatedRetail) { this.estimatedRetail = estimatedRetail; }

    public String getStore() { return store; }
    public void setStore(String store) { this.store = store; }

    public LocalDate getDateFound() { return dateFound; }
    public void setDateFound(LocalDate dateFound) { this.dateFound = dateFound; }

    public int getWearCount() { return wearCount; }
    public void setWearCount(int wearCount) { this.wearCount = wearCount; }

    public String getPhotoVersion() { return photoVersion; }
    public void setPhotoVersion(String photoVersion) { this.photoVersion = photoVersion; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long ownerId) { this.ownerId = ownerId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
