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
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OutfitService {

    private final OutfitRepository outfits;
    private final ItemRepository items;

    public OutfitService(OutfitRepository outfits, ItemRepository items) {
        this.outfits = outfits;
        this.items = items;
    }

    @Transactional(readOnly = true)
    public List<OutfitResponse> list() {
        return outfits.findAllByOrderByCreatedAtDesc().stream().map(OutfitResponse::from).toList();
    }

    @Transactional
    public OutfitResponse create(OutfitRequest request) {
        Outfit outfit = new Outfit();
        outfit.setName(request.name().trim());
        outfit.setOccasion(request.occasion() == null || request.occasion().isBlank() ? null : request.occasion().trim());
        outfit.setItems(loadItems(request.itemIds()));
        return OutfitResponse.from(outfits.save(outfit));
    }

    @Transactional
    public OutfitResponse recordWear(Long id) {
        Outfit outfit = find(id);
        outfit.recordWear();
        return OutfitResponse.from(outfit);
    }

    @Transactional
    public OutfitResponse removeWear(Long id) {
        Outfit outfit = find(id);
        outfit.removeWear();
        return OutfitResponse.from(outfit);
    }

    @Transactional
    public void delete(Long id) {
        outfits.delete(find(id));
    }

    /** Looks up every id, keeps the order you picked them in, and drops duplicates. */
    private List<Item> loadItems(List<Long> ids) {
        List<Long> unique = new ArrayList<>(new LinkedHashSet<>(ids));
        Map<Long, Item> byId = items.findAllById(unique).stream()
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

    private Outfit find(Long id) {
        return outfits.findById(id).orElseThrow(() -> new NotFoundException("Outfit", id));
    }
}
