package com.secondstitch.item;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.Set;

/**
 * Saves item photos in the database, so they survive restarts and redeploys.
 * (Hosts like Render wipe their disk on restart, so files saved there disappear.)
 *
 * Kept in its own class so it could later be swapped for cloud storage
 * (Azure Blob, S3, Cloudinary...) without touching the controller.
 */
@Component
public class PhotoStorage {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final ItemPhotoRepository photos;

    public PhotoStorage(ItemPhotoRepository photos) {
        this.photos = photos;
    }

    /** Saves (or replaces) the photo for an item. */
    public void save(Long itemId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Choose a photo to upload");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new IllegalArgumentException("Photos must be JPG, PNG or WebP");
        }
        try {
            photos.save(new ItemPhoto(itemId, file.getContentType(), file.getBytes()));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the photo", e);
        }
    }

    public Optional<ItemPhoto> load(Long itemId) {
        return photos.findById(itemId);
    }

    public void delete(Long itemId) {
        photos.deleteById(itemId);
    }
}
