package com.secondstitch.outfit;

import com.secondstitch.common.NotFoundException;
import com.secondstitch.item.Item;
import com.secondstitch.item.ItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Outfits, always scoped to the logged-in user (see ItemService for why). */
@Service
public class OutfitService {

    private final OutfitRepository outfits;
    private final ItemRepository items;

    public OutfitService(OutfitRepository outfits, ItemRepository items) {
        this.outfits = outfits;
        this.items = items;
    }

    @Transactional(readOnly = true)
    public List<OutfitResponse> list(Long userId) {
        return outfits.findByOwnerIdOrderByCreatedAtDesc(userId).stream().map(OutfitResponse::from).toList();
    }

    @Transactional
    public OutfitResponse create(Long userId, OutfitRequest request) {
        Outfit outfit = new Outfit();
        outfit.setOwnerId(userId);
        outfit.setName(request.name().trim());
        outfit.setOccasion(request.occasion() == null || request.occasion().isBlank() ? null : request.occasion().trim());
        outfit.setItems(loadItems(userId, request.itemIds()));
        return OutfitResponse.from(outfits.save(outfit));
    }

    @Transactional
    public OutfitResponse recordWear(Long userId, Long id) {
        Outfit outfit = find(userId, id);
        outfit.recordWear();
        return OutfitResponse.from(outfit);
    }

    @Transactional
    public OutfitResponse removeWear(Long userId, Long id) {
        Outfit outfit = find(userId, id);
        outfit.removeWear();
        return OutfitResponse.from(outfit);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        outfits.delete(find(userId, id));
    }

    /**
     * Looks up every id, keeps the order you picked them in, and drops duplicates.
     * Only your own pieces count: anyone else's id is "not found".
     */
    private List<Item> loadItems(Long userId, List<Long> ids) {
        List<Long> unique = new ArrayList<>(new LinkedHashSet<>(ids));
        Map<Long, Item> byId = items.findAllById(unique).stream()
                .filter(item -> Objects.equals(item.getOwnerId(), userId))
                .collect(Collectors.toMap(Item::getId, Function.identity()));
        List<Item> result = new ArrayList<>();
        for (Long id : unique) {
            Item item = byId.get(id);
            if (item == null) {
                throw new NotFoundException("Item", id);
            }
            result.add(item);
        }
        return result;
    }

    private Outfit find(Long userId, Long id) {
        return outfits.findByIdAndOwnerId(id, userId).orElseThrow(() -> new NotFoundException("Outfit", id));
    }
}
