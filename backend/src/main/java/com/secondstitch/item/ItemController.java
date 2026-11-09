package com.secondstitch.item;

import com.secondstitch.auth.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
 * REST endpoints for closet items. All of them need a login token, except the
 * photo itself, and all of them only see the logged-in user's pieces.
 *
 *   GET    /api/items?category=TOP   list (optionally filtered)
 *   GET    /api/items/{id}           one item
 *   POST   /api/items                create
 *   PUT    /api/items/{id}           update
 *   DELETE /api/items/{id}           delete
 *   POST   /api/items/{id}/wear      add a wear (+1)
 *   DELETE /api/items/{id}/wear      remove a wear (-1, never below 0)
 *   POST   /api/items/{id}/photo     upload a photo (multipart, field "file")
 *   GET    /api/items/{id}/photo?v=  the photo itself (the ?v= code is in the item's photoUrl)
 */
@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService service;

    public ItemController(ItemService service) {
        this.service = service;
    }

    @GetMapping
    public List<ItemResponse> list(@AuthenticationPrincipal Jwt jwt,
                                   @RequestParam(required = false) Category category) {
        return service.list(CurrentUser.id(jwt), category);
    }

    @GetMapping("/{id}")
    public ItemResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.get(CurrentUser.id(jwt), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ItemResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody ItemRequest request) {
        return service.create(CurrentUser.id(jwt), request);
    }

    @PutMapping("/{id}")
    public ItemResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                               @Valid @RequestBody ItemRequest request) {
        return service.update(CurrentUser.id(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.delete(CurrentUser.id(jwt), id);
    }

    @PostMapping("/{id}/wear")
    public ItemResponse wear(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.recordWear(CurrentUser.id(jwt), id);
    }

    @DeleteMapping("/{id}/wear")
    public ItemResponse removeWear(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.removeWear(CurrentUser.id(jwt), id);
    }

    @PostMapping(value = "/{id}/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ItemResponse uploadPhoto(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id,
                                    @RequestParam("file") MultipartFile file) {
        return service.savePhoto(CurrentUser.id(jwt), id, file);
    }

    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> photo(@PathVariable Long id,
                                        @RequestParam(name = "v", required = false) String version) {
        ItemPhoto photo = service.photo(id, version);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(photo.getContentType()))
                // The link changes whenever the photo changes, so browsers can keep it for a long time.
                // "private": shared caches between you and the server shouldn't keep a copy.
                .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePrivate())
                .body(photo.getData());
    }
}
