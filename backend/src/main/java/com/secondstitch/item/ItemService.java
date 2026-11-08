package com.secondstitch.item;

import com.secondstitch.common.NotFoundException;
import com.secondstitch.outfit.Outfit;
import com.secondstitch.outfit.OutfitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** The business logic for items. Controllers stay thin and call into here. */
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
    public List<ItemResponse> list(Category category) {
        List<Item> found = (category == null)
                ? items.findAllByOrderByCreatedAtDesc()
                : items.findByCategoryOrderByCreatedAtDesc(category);
        return found.stream().map(ItemResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ItemResponse get(Long id) {
        return ItemResponse.from(find(id));
    }

    @Transactional
    public ItemResponse create(ItemRequest request) {
        Item item = new Item();
        request.applyTo(item);
        return ItemResponse.from(items.save(item));
    }

    @Transactional
    public ItemResponse update(Long id, ItemRequest request) {
        Item item = find(id);
        request.applyTo(item);
        return ItemResponse.from(item);
    }

    @Transactional
    public ItemResponse recordWear(Long id) {
        Item item = find(id);
        item.recordWear();
        return ItemResponse.from(item);
    }

    @Transactional
    public ItemResponse removeWear(Long id) {
        Item item = find(id);
        item.removeWear();
        return ItemResponse.from(item);
    }

    @Transactional
    public void delete(Long id) {
        Item item = find(id);
        // Take the item out of any outfits first, otherwise the database
        // refuses to delete a row that another table still points to.
        for (Outfit outfit : outfits.findByItemsContaining(item)) {
            outfit.getItems().remove(item);
        }
        photos.delete(item.getPhotoFilename());
        items.delete(item);
    }

    @Transactional
    public ItemResponse savePhoto(Long id, MultipartFile file) {
        Item item = find(id);
        String newName = photos.save(id, file);
        photos.delete(item.getPhotoFilename());
        item.setPhotoFilename(newName);
        return ItemResponse.from(item);
    }

    @Transactional(readOnly = true)
    public String photoFilename(Long id) {
        String name = find(id).getPhotoFilename();
        if (name == null) {
            throw new NotFoundException("Photo for item", id);
        }
        return name;
    }

    private Item find(Long id) {
        return items.findById(id).orElseThrow(() -> new NotFoundException("Item", id));
    }
}
