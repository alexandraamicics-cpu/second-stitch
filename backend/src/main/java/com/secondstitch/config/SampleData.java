package com.secondstitch.config;

import com.secondstitch.item.Category;
import com.secondstitch.item.Item;
import com.secondstitch.item.ItemRepository;
import com.secondstitch.outfit.Outfit;
import com.secondstitch.outfit.OutfitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Fills an empty closet with sample pieces so the app looks alive the
 * first time you run it. Turn off with app.seed-sample-data=false.
 */
@Component
@ConditionalOnProperty(name = "app.seed-sample-data", havingValue = "true")
public class SampleData implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SampleData.class);

    private final ItemRepository items;
    private final OutfitRepository outfits;

    public SampleData(ItemRepository items, OutfitRepository outfits) {
        this.items = items;
        this.outfits = outfits;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (items.count() > 0) {
            return;
        }

        Item shirt = item("Oversized linen button-up", Category.TOP, "Uniqlo", "L", "sage", "#9CAF88",
                "180", "1490", "Ukay-ukay, Cubao", 9, "Breathable, no scratchy tag. Perfect for hot days.");
        Item jeans = item("Straight-leg 501s", Category.BOTTOM, "Levi's", "28", "faded indigo", "#5B7493",
                "350", "4290", "Ukay-ukay, Cubao", 14, "Already broken in. Hemmed at the tailor for ₱80.");
        Item vest = item("Crochet sweater vest", Category.TOP, null, "M", "oatmeal", "#D8CBB0",
                "120", null, "Thrift pop-up, Maginhawa", 3, null);
        Item skirt = item("Pleated midi skirt", Category.BOTTOM, null, "S", "plum", "#6E3B5C",
                "150", "1200", "Online live selling", 5, "Has pockets!");
        Item jacket = item("Corduroy chore jacket", Category.OUTERWEAR, "Carhartt", "M", "tobacco", "#8A5A3C",
                "650", "5500", "Thrift pop-up, Maginhawa", 6, null);
        Item loafers = item("Leather penny loafers", Category.SHOES, null, "38", "oxblood", "#5E2328",
                "450", "3800", "Ukay-ukay, Baguio", 8, "Resoled once. Comfy after week one.");
        Item scarf = item("Printed silk scarf", Category.ACCESSORY, null, null, "mustard", "#C9A13B",
                "60", "900", "Ukay-ukay, Baguio", 2, "Works as a hair tie too.");
        Item dress = item("Bias-cut slip dress", Category.DRESS, null, "S", "champagne", "#E6D3B3",
                "280", "2600", "Online live selling", 0, "Soft seams, sensory-friendly.");

        List<Item> saved = items.saveAll(List.of(shirt, jeans, vest, skirt, jacket, loafers, scarf, dress));

        // Spread the "created" times out so the newest-first order looks natural.
        for (int i = 0; i < saved.size(); i++) {
            saved.get(i).setCreatedAt(Instant.now().minusSeconds(3600L * (saved.size() - i)));
        }

        outfits.save(outfit("Errand day", "comfy", jeans, shirt, loafers));
        outfits.save(outfit("Gallery hop", "weekend", skirt, vest, loafers, scarf));

        log.info("Added {} sample items and 2 sample outfits", saved.size());
    }

    private static Item item(String name, Category category, String brand, String size, String color, String hex,
                             String paid, String retail, String store, int wears, String notes) {
        Item item = new Item();
        item.setName(name);
        item.setCategory(category);
        item.setBrand(brand);
        item.setSize(size);
        item.setColor(color);
        item.setColorHex(hex);
        item.setPricePaid(new BigDecimal(paid));
        item.setEstimatedRetail(retail == null ? null : new BigDecimal(retail));
        item.setStore(store);
        item.setDateFound(LocalDate.now().minusWeeks(wears + 2L));
        item.setWearCount(wears);
        item.setNotes(notes);
        return item;
    }

    private static Outfit outfit(String name, String occasion, Item... pieces) {
        Outfit outfit = new Outfit();
        outfit.setName(name);
        outfit.setOccasion(occasion);
        outfit.setItems(new java.util.ArrayList<>(List.of(pieces)));
        return outfit;
    }
}
