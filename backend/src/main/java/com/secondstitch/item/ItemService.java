package com.secondstitch.item;

import com.secondstitch.common.NotFoundException;
import com.secondstitch.outfit.Outfit;
import com.secondstitch.outfit.OutfitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * The business logic for items. Controllers stay thin and call into here.
 * Every method takes the logged-in user's id and only touches that user's pieces.
 * Someone else's piece gets "not found" (404), so you can't even tell it exists.
 */
@Service
public class ItemService {

    private final ItemRepository items;
    private final OutfitRepository outfits;
    private final PhotoStorage photos;

    public ItemService(ItemRepository items, OutfitRepository outfits, PhotoStorage photos) {
        this.items = items;
        this.outfits = outfits;
        this.photos = photos;
    }

    @Transactional(readOnly = true)
    public List<ItemResponse> list(Long userId, Category category) {
        List<Item> found = (category == null)
                ? items.findByOwnerIdOrderByCreatedAtDesc(userId)
                : items.findByOwnerIdAndCategoryOrderByCreatedAtDesc(userId, category);
        return found.stream().map(ItemResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ItemResponse get(Long userId, Long id) {
        return ItemResponse.from(find(userId, id));
    }

    @Transactional
    public ItemResponse create(Long userId, ItemRequest request) {
        Item item = new Item();
        request.applyTo(item);
        item.setOwnerId(userId);
        return ItemResponse.from(items.save(item));
    }

    @Transactional
    public ItemResponse update(Long userId, Long id, ItemRequest request) {
        Item item = find(userId, id);
        request.applyTo(item);
        return ItemResponse.from(item);
    }

    @Transactional
    public ItemResponse recordWear(Long userId, Long id) {
        Item item = find(userId, id);
        item.recordWear();
        return ItemResponse.from(item);
    }

    @Transactional
    public ItemResponse removeWear(Long userId, Long id) {
        Item item = find(userId, id);
        item.removeWear();
        return ItemResponse.from(item);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        Item item = find(userId, id);
        // Take the item out of any outfits first, otherwise the database
        // refuses to delete a row that another table still points to.
        for (Outfit outfit : outfits.findByItemsContaining(item)) {
            outfit.getItems().remove(item);
        }
        photos.delete(id);
        items.delete(item);
    }

    @Transactional
    public ItemResponse savePhoto(Long userId, Long id, MultipartFile file) {
        Item item = find(userId, id);
        photos.save(id, file);
        // A long random code: it goes in the photo's link, and only someone who has the link can load it
        item.setPhotoVersion(UUID.randomUUID().toString());
        return ItemResponse.from(item);
    }

    /**
     * Photos are loaded by <img> tags, which can't send a login token. Instead, the link itself
     * contains the photo's long random code (?v=...), and only the current code works.
     */
    @Transactional(readOnly = true)
    public ItemPhoto photo(Long id, String version) {
        Item item = items.findById(id).orElseThrow(() -> new NotFoundException("Photo for item", id));
        if (version == null || !version.equals(item.getPhotoVersion())) {
            throw new NotFoundException("Photo for item", id);
        }
        return photos.load(id).orElseThrow(() -> new NotFoundException("Photo for item", id));
    }

    private Item find(Long userId, Long id) {
        return items.findByIdAndOwnerId(id, userId).orElseThrow(() -> new NotFoundException("Item", id));
    }
}
