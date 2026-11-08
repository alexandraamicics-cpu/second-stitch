package com.secondstitch.item;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/**
 * Saves item photos to a folder on disk (./uploads by default).
 * Kept in its own class so it could later be swapped for cloud storage
 * (Azure Blob, S3...) without touching the controller.
 */
@Component
public class PhotoStorage {

    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final Path root;

    public PhotoStorage(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create upload folder " + root, e);
        }
    }

    /** Saves the file and returns the new file name. */
    public String save(Long itemId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Choose a photo to upload");
        }
        String extension = ALLOWED_TYPES.get(file.getContentType());
        if (extension == null) {
            throw new IllegalArgumentException("Photos must be JPG, PNG or WebP");
        }
        // Random part means a new upload never overwrites a cached old one.
        String filename = "item-" + itemId + "-" + UUID.randomUUID() + "." + extension;
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, root.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save photo", e);
        }
        return filename;
    }

    public Resource load(String filename) {
        Path file = resolveSafely(filename);
        try {
            Resource resource = new UrlResource(file.toUri());
            return resource.exists() ? resource : null;
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public void delete(String filename) {
        if (filename == null) {
            return;
        }
        try {
            Files.deleteIfExists(resolveSafely(filename));
        } catch (IOException ignored) {
            // A leftover file is harmless; don't fail the request over it.
        }
    }

    public String contentType(String filename) {
        if (filename.endsWith(".png")) return "image/png";
        if (filename.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    /** Stops tricks like "../../etc/passwd" from reading files outside the upload folder. */
    private Path resolveSafely(String filename) {
        Path file = root.resolve(filename).normalize();
        if (!file.startsWith(root)) {
            throw new IllegalArgumentException("Invalid file name");
        }
        return file;
    }
}
