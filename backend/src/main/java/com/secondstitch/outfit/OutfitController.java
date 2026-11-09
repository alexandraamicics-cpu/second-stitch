package com.secondstitch.outfit;

import com.secondstitch.auth.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 *   GET    /api/outfits            list your outfits
 *   POST   /api/outfits            create { name, occasion, itemIds: [1, 2, 3] }
 *   POST   /api/outfits/{id}/wear  wore the whole look (+1 wear on every piece)
 *   DELETE /api/outfits/{id}/wear  undo that (-1 on the outfit and every piece, never below 0)
 *   DELETE /api/outfits/{id}       delete (the items stay in your closet)
 */
@RestController
@RequestMapping("/api/outfits")
public class OutfitController {

    private final OutfitService service;

    public OutfitController(OutfitService service) {
        this.service = service;
    }

    @GetMapping
    public List<OutfitResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return service.list(CurrentUser.id(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OutfitResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OutfitRequest request) {
        return service.create(CurrentUser.id(jwt), request);
    }

    @PostMapping("/{id}/wear")
    public OutfitResponse wear(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.recordWear(CurrentUser.id(jwt), id);
    }

    @DeleteMapping("/{id}/wear")
    public OutfitResponse removeWear(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return service.removeWear(CurrentUser.id(jwt), id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        service.delete(CurrentUser.id(jwt), id);
    }
}
