package com.secondstitch.item;

import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;

/**
 * REST endpoints for closet items.
 *
 *   GET    /api/items?category=TOP   list (optionally filtered)
 *   GET    /api/items/{id}           one item
 *   POST   /api/items                create
 *   PUT    /api/items/{id}           update
 *   DELETE /api/items/{id}           delete
 *   POST   /api/items/{id}/wear      add a wear (+1)
 *   DELETE /api/items/{id}/wear      remove a wear (-1, never below 0)
 *   POST   /api/items/{id}/photo     upload a photo (multipart, field "file")
 *   GET    /api/items/{id}/photo     the photo itself
 */
@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService service;

    public ItemController(ItemService service) {
        this.service = service;
    }

    @GetMapping
    public List<ItemResponse> list(@RequestParam(required = false) Category category) {
        return service.list(category);
    }

    @GetMapping("/{id}")
    public ItemResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemResponse create(@Valid @RequestBody ItemRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public ItemResponse update(@PathVariable Long id, @Valid @RequestBody ItemRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PostMapping("/{id}/wear")
    public ItemResponse wear(@PathVariable Long id) {
        return service.recordWear(id);
    }

    @DeleteMapping("/{id}/wear")
    public ItemResponse removeWear(@PathVariable Long id) {
        return service.removeWear(id);
    }

    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ItemResponse uploadPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return service.savePhoto(id, file);
    }

    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> photo(@PathVariable Long id) {
        ItemPhoto photo = service.photo(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                // The URL changes (?v=...) whenever the photo changes, so browsers can keep it for a long time
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic())
                .body(photo.getData());
    }
}
