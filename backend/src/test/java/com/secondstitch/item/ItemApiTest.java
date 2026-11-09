package com.secondstitch.item;

import com.jayway.jsonpath.JsonPath;
import com.secondstitch.outfit.OutfitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests: start the real app (with an in-memory database)
 * and call the API the same way the React frontend does.
 * Every request is made as logged-in user 1, unless a test says otherwise.
 */
@SpringBootTest
class ItemApiTest {

    static final long ME = 1L;
    static final long SOMEONE_ELSE = 2L;

    /** Pretends the request comes with a valid login token for this user. */
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

    @Autowired
    ItemPhotoRepository photos;

    @BeforeEach
    void emptyCloset() {
        outfits.deleteAll();
        photos.deleteAll();
        items.deleteAll();
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .defaultRequest(get("/").with(asUser(ME)))
                .build();
    }

    private String photoUrlAfterUploading(long id, MockMultipartFile file) throws Exception {
        String json = mvc.perform(multipart("/api/items/{id}/photo", id).file(file))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.photoUrl");
    }

    private static final String JEANS = """
            {
              "name": "Straight-leg 501s",
              "category": "BOTTOM",
              "brand": "Levi's",
              "colorHex": "#5B7493",
              "pricePaid": 350,
              "estimatedRetail": 4290,
              "store": "Ukay-ukay, Cubao"
            }
            """;

    private long createJeans() throws Exception {
        MvcResult result = mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON).content(JEANS))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    @Test
    void createsAnItemAndWorksOutSavings() throws Exception {
        mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON).content(JEANS))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.name").value("Straight-leg 501s"))
                .andExpect(jsonPath("$.savings").value(3940.00))
                .andExpect(jsonPath("$.costPerWear").value(350.00))
                .andExpect(jsonPath("$.wearCount").value(0));
    }

    @Test
    void rejectsAnItemWithoutNameOrPrice() throws Exception {
        mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\": \"TOP\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.name").value("Give the piece a name"))
                .andExpect(jsonPath("$.fields.pricePaid").value("Add what you paid"));
    }

    @Test
    void rejectsABadHexColour() throws Exception {
        mvc.perform(post("/api/items").contentType(MediaType.APPLICATION_JSON)
                        .content(JEANS.replace("#5B7493", "blue")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.colorHex", notNullValue()));
    }

    @Test
    void listsAndFiltersByCategory() throws Exception {
        createJeans();
        mvc.perform(get("/api/items")).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/items").param("category", "BOTTOM")).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/items").param("category", "SHOES")).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void wearingTwiceHalvesCostPerWear() throws Exception {
        long id = createJeans();
        mvc.perform(post("/api/items/{id}/wear", id)).andExpect(jsonPath("$.wearCount").value(1));
        mvc.perform(post("/api/items/{id}/wear", id))
                .andExpect(jsonPath("$.wearCount").value(2))
                .andExpect(jsonPath("$.costPerWear").value(175.00));
    }

    @Test
    void removingAWearUndoesAMisClick() throws Exception {
        long id = createJeans();
        mvc.perform(post("/api/items/{id}/wear", id));
        mvc.perform(post("/api/items/{id}/wear", id));
        mvc.perform(delete("/api/items/{id}/wear", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wearCount").value(1))
                .andExpect(jsonPath("$.costPerWear").value(350.00));
    }

    @Test
    void frontendOnAnyLocalPortCanChangeData() throws Exception {
        long id = createJeans();
        // Vite moves to port 5174 if 5173 is busy; that must not cause a 403
        mvc.perform(post("/api/items/{id}/wear", id).header("Origin", "http://localhost:5174"))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/items/{id}/wear", id).header("Origin", "http://localhost:5174"))
                .andExpect(status().isOk());
    }

    @Test
    void otherWebsitesCannotChangeData() throws Exception {
        long id = createJeans();
        mvc.perform(post("/api/items/{id}/wear", id).header("Origin", "https://some-other-site.example"))
                .andExpect(status().isForbidden());
    }

    @Test
    void wearCountNeverGoesBelowZero() throws Exception {
        long id = createJeans();
        mvc.perform(delete("/api/items/{id}/wear", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wearCount").value(0));
    }

    @Test
    void updatesAnItem() throws Exception {
        long id = createJeans();
        mvc.perform(put("/api/items/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(JEANS.replace("Straight-leg 501s", "Favourite 501s")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Favourite 501s"));
    }

    @Test
    void deletesAnItem() throws Exception {
        long id = createJeans();
        mvc.perform(delete("/api/items/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/items/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void newPiecesBelongToTheLoggedInUser() throws Exception {
        long id = createJeans();
        org.assertj.core.api.Assertions.assertThat(items.findById(id).orElseThrow().getOwnerId()).isEqualTo(ME);
    }

    @Test
    void someoneElseCannotSeeOrChangeMyPieces() throws Exception {
        long id = createJeans();

        mvc.perform(get("/api/items").with(asUser(SOMEONE_ELSE))).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/items/{id}", id).with(asUser(SOMEONE_ELSE))).andExpect(status().isNotFound());
        mvc.perform(post("/api/items/{id}/wear", id).with(asUser(SOMEONE_ELSE))).andExpect(status().isNotFound());
        mvc.perform(delete("/api/items/{id}", id).with(asUser(SOMEONE_ELSE))).andExpect(status().isNotFound());

        // Still mine, untouched
        mvc.perform(get("/api/items/{id}", id)).andExpect(jsonPath("$.wearCount").value(0));
    }

    @Test
    void notLoggedInGets401() throws Exception {
        mvc.perform(get("/api/items").with(anonymous()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Log in to continue"));
    }

    @Test
    void unknownItemIs404WithAMessage() throws Exception {
        mvc.perform(get("/api/items/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Item 9999 was not found"));
    }

    @Test
    void uploadsAndServesAPhoto() throws Exception {
        long id = createJeans();
        byte[] fakePng = {(byte) 0x89, 'P', 'N', 'G'};
        MockMultipartFile file = new MockMultipartFile("file", "jeans.png", "image/png", fakePng);

        String photoUrl = photoUrlAfterUploading(id, file);
        org.assertj.core.api.Assertions.assertThat(photoUrl).startsWith("/api/items/" + id + "/photo?v=");

        mvc.perform(get(photoUrl))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(fakePng));
    }

    @Test
    void photoIsStoredInTheDatabase() throws Exception {
        long id = createJeans();
        byte[] fakeJpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3};
        mvc.perform(multipart("/api/items/{id}/photo", id)
                        .file(new MockMultipartFile("file", "jeans.jpg", "image/jpeg", fakeJpeg)))
                .andExpect(status().isOk());

        // Saved as a row in item_photo, not as a file on disk, so it survives restarts
        ItemPhoto saved = photos.findById(id).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(saved.getData()).isEqualTo(fakeJpeg);
        org.assertj.core.api.Assertions.assertThat(saved.getContentType()).isEqualTo("image/jpeg");
    }

    @Test
    void replacingAPhotoChangesItsUrl() throws Exception {
        long id = createJeans();
        MockMultipartFile first = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1});
        MockMultipartFile second = new MockMultipartFile("file", "b.png", "image/png", new byte[]{2});

        String firstUrl = JsonPath.read(mvc.perform(multipart("/api/items/{id}/photo", id).file(first))
                .andReturn().getResponse().getContentAsString(), "$.photoUrl");
        String secondUrl = JsonPath.read(mvc.perform(multipart("/api/items/{id}/photo", id).file(second))
                .andReturn().getResponse().getContentAsString(), "$.photoUrl");

        org.assertj.core.api.Assertions.assertThat(secondUrl).isNotEqualTo(firstUrl);
        mvc.perform(get(secondUrl)).andExpect(content().bytes(new byte[]{2}));
        mvc.perform(get(firstUrl)).andExpect(status().isNotFound()); // the old link stops working
    }

    @Test
    void photoLinkWorksWithoutLoggingIn() throws Exception {
        // <img> tags can't send a login token, so the link itself is the key
        long id = createJeans();
        String photoUrl = photoUrlAfterUploading(id, new MockMultipartFile("file", "a.png", "image/png", new byte[]{7}));
        mvc.perform(get(photoUrl).with(anonymous()))
                .andExpect(status().isOk())
                .andExpect(content().bytes(new byte[]{7}));
    }

    @Test
    void photoNeedsTheRightCode() throws Exception {
        long id = createJeans();
        photoUrlAfterUploading(id, new MockMultipartFile("file", "a.png", "image/png", new byte[]{7}));
        mvc.perform(get("/api/items/{id}/photo", id).with(anonymous())).andExpect(status().isNotFound());
        mvc.perform(get("/api/items/{id}/photo", id).param("v", "guess").with(anonymous()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deletingAnItemDeletesItsPhoto() throws Exception {
        long id = createJeans();
        mvc.perform(multipart("/api/items/{id}/photo", id)
                .file(new MockMultipartFile("file", "a.png", "image/png", new byte[]{1})));

        mvc.perform(delete("/api/items/{id}", id)).andExpect(status().isNoContent());
        org.assertj.core.api.Assertions.assertThat(photos.findById(id)).isEmpty();
    }

    @Test
    void itemWithoutAPhotoHasNoPhotoUrlAnd404() throws Exception {
        long id = createJeans();
        mvc.perform(get("/api/items/{id}", id)).andExpect(jsonPath("$.photoUrl").doesNotExist());
        mvc.perform(get("/api/items/{id}/photo", id).param("v", "anything")).andExpect(status().isNotFound());
    }

    @Test
    void rejectsFilesThatAreNotPhotos() throws Exception {
        long id = createJeans();
        MockMultipartFile file = new MockMultipartFile("file", "notes.txt", "text/plain", "hi".getBytes());
        mvc.perform(multipart("/api/items/{id}/photo", id).file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Photos must be JPG, PNG or WebP"));
    }
}
