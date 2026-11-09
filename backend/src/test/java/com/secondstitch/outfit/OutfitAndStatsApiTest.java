package com.secondstitch.outfit;

import com.jayway.jsonpath.JsonPath;
import com.secondstitch.item.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Every request is made as logged-in user 1, unless a test says otherwise. */
@SpringBootTest
class OutfitAndStatsApiTest {

    static RequestPostProcessor asUser(long userId) {
        return jwt().jwt(token -> token.subject(Long.toString(userId)));
    }

    @Autowired
    WebApplicationContext context;

    MockMvc mvc;

    @Autowired
    ItemRepository items;

    @Autowired
    OutfitRepository outfits;

    @BeforeEach
    void emptyCloset() {
        outfits.deleteAll();
        items.deleteAll();
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .defaultRequest(get("/").with(asUser(1)))
                .build();
    }

    private long createItem(String name, String category, int paid, Integer retail, String store) throws Exception {
        String body = """
                {"name": "%s", "category": "%s", "pricePaid": %d, "estimatedRetail": %s, "store": "%s"}
                """.formatted(name, category, paid, retail == null ? "null" : retail.toString(), store);
        String json = mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private long createOutfit(String name, long... itemIds) throws Exception {
        StringBuilder ids = new StringBuilder();
        for (long id : itemIds) {
            ids.append(ids.isEmpty() ? "" : ",").append(id);
        }
        String json = mvc.perform(post("/api/outfits").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + name + "\", \"itemIds\": [" + ids + "]}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    @Test
    void createsAnOutfitWithItsTotalCost() throws Exception {
        long top = createItem("Linen shirt", "TOP", 180, 1490, "Cubao");
        long jeans = createItem("501s", "BOTTOM", 350, 4290, "Cubao");

        mvc.perform(post("/api/outfits").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Errand day\", \"itemIds\": [" + top + "," + jeans + "]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.totalPaid").value(530.0));
    }

    @Test
    void outfitShowsPriceNewAndSavingsForTheWholeLook() throws Exception {
        long top = createItem("Linen shirt", "TOP", 180, 1490, "Cubao");
        long jeans = createItem("501s", "BOTTOM", 350, 4290, "Cubao");
        long scarf = createItem("Scarf", "ACCESSORY", 60, null, "Baguio"); // price new unknown

        mvc.perform(post("/api/outfits").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Gallery hop\", \"itemIds\": [" + top + "," + jeans + "," + scarf + "]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totalPaid").value(590.0))
                .andExpect(jsonPath("$.totalRetail").value(5780.0))
                .andExpect(jsonPath("$.totalSaved").value(5250.0));
    }

    @Test
    void cannotPutSomeoneElsesPieceInMyOutfit() throws Exception {
        long theirs = createItem("Their shirt", "TOP", 100, null, "Cubao"); // made by user 1
        mvc.perform(post("/api/outfits").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Sneaky\", \"itemIds\": [" + theirs + "]}")
                        .with(asUser(2)))
                .andExpect(status().isNotFound());
    }

    @Test
    void eachUserOnlySeesTheirOwnOutfitsAndStats() throws Exception {
        long top = createItem("Linen shirt", "TOP", 180, 1490, "Cubao");
        createOutfit("Errand day", top);

        mvc.perform(get("/api/outfits").with(asUser(2))).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/stats").with(asUser(2))).andExpect(jsonPath("$.totalItems").value(0));
        mvc.perform(get("/api/outfits")).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void outfitNeedsAtLeastOnePiece() throws Exception {
        mvc.perform(post("/api/outfits").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Empty\", \"itemIds\": []}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.itemIds").value("Pick at least one piece"));
    }

    @Test
    void wearingAnOutfitCountsAWearForEveryPiece() throws Exception {
        long top = createItem("Linen shirt", "TOP", 180, null, "Cubao");
        long jeans = createItem("501s", "BOTTOM", 350, null, "Cubao");
        long outfit = createOutfit("Errand day", top, jeans);

        mvc.perform(post("/api/outfits/{id}/wear", outfit))
                .andExpect(jsonPath("$.wearCount").value(1))
                .andExpect(jsonPath("$.items[0].wearCount").value(1))
                .andExpect(jsonPath("$.items[1].wearCount").value(1));
    }

    @Test
    void removingAnOutfitWearUndoesItForEveryPiece() throws Exception {
        long top = createItem("Linen shirt", "TOP", 180, null, "Cubao");
        long jeans = createItem("501s", "BOTTOM", 350, null, "Cubao");
        long outfit = createOutfit("Errand day", top, jeans);
        mvc.perform(post("/api/outfits/{id}/wear", outfit));

        mvc.perform(delete("/api/outfits/{id}/wear", outfit))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wearCount").value(0))
                .andExpect(jsonPath("$.items[0].wearCount").value(0))
                .andExpect(jsonPath("$.items[1].wearCount").value(0));
    }

    @Test
    void removingAWearFromAnUnwornOutfitChangesNothing() throws Exception {
        long top = createItem("Linen shirt", "TOP", 180, null, "Cubao");
        long outfit = createOutfit("Just a shirt", top);
        mvc.perform(post("/api/items/{id}/wear", top)); // worn on its own, not as this outfit

        mvc.perform(delete("/api/outfits/{id}/wear", outfit))
                .andExpect(jsonPath("$.wearCount").value(0))
                .andExpect(jsonPath("$.items[0].wearCount").value(1));
    }

    @Test
    void deletingAnItemRemovesItFromOutfits() throws Exception {
        long top = createItem("Linen shirt", "TOP", 180, null, "Cubao");
        long jeans = createItem("501s", "BOTTOM", 350, null, "Cubao");
        createOutfit("Errand day", top, jeans);

        mvc.perform(delete("/api/items/{id}", top)).andExpect(status().isNoContent());

        mvc.perform(get("/api/outfits"))
                .andExpect(jsonPath("$[0].items", hasSize(1)))
                .andExpect(jsonPath("$[0].items[0].name").value("501s"));
    }

    @Test
    void statsAddUpTheCloset() throws Exception {
        long top = createItem("Linen shirt", "TOP", 180, 1490, "Cubao");
        createItem("501s", "BOTTOM", 350, 4290, "Cubao");
        createItem("Scarf", "ACCESSORY", 60, null, "Baguio");
        mvc.perform(post("/api/items/{id}/wear", top));

        mvc.perform(get("/api/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.totalWears").value(1))
                .andExpect(jsonPath("$.totalPaid").value(590.0))
                .andExpect(jsonPath("$.totalSaved").value(5250.0))
                .andExpect(jsonPath("$.mostWorn.name").value("Linen shirt"))
                .andExpect(jsonPath("$.waitingToBeWorn", hasSize(2)))
                .andExpect(jsonPath("$.topStores[0].store").value("Cubao"))
                .andExpect(jsonPath("$.topStores[0].count").value(2));
    }

    @Test
    void statsForAnEmptyClosetAreZero() throws Exception {
        mvc.perform(get("/api/stats"))
                .andExpect(jsonPath("$.totalItems").value(0))
                .andExpect(jsonPath("$.averageCostPerWear").value(0.0))
                .andExpect(jsonPath("$.mostWorn").doesNotExist());
    }
}
