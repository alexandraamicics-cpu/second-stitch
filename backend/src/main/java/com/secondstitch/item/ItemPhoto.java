package com.secondstitch.item;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A piece's photo, stored as bytes in the database (a "bytea" column in PostgreSQL).
 *
 * It lives in its own table, not inside Item, so listing the closet doesn't
 * load every photo's bytes. The photo is only loaded when the browser asks for it.
 * The id is the same as the item's id: one photo per item.
 */
@Entity
@Table(name = "item_photo")
public class ItemPhoto {

    @Id
    private Long itemId;

    @Column(nullable = false)
    private String contentType;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(nullable = false, length = 6_000_000)
    private byte[] data;

    protected ItemPhoto() {
        // for JPA
    }

    public ItemPhoto(Long itemId, String contentType, byte[] data) {
        this.itemId = itemId;
        this.contentType = contentType;
        this.data = data;
    }

    public Long getItemId() { return itemId; }

    public String getContentType() { return contentType; }

    public byte[] getData() { return data; }
}
