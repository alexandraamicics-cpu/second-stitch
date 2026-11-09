package com.secondstitch.auth;

import com.jayway.jsonpath.JsonPath;
import com.secondstitch.email.ConsoleEmailSender;
import com.secondstitch.email.EmailSender;
import com.secondstitch.email.EmailToken;
import com.secondstitch.email.EmailTokens;
import com.secondstitch.email.EmailTokenRepository;
import com.secondstitch.item.Category;
import com.secondstitch.item.Item;
import com.secondstitch.item.ItemPhotoRepository;
import com.secondstitch.item.ItemRepository;
import com.secondstitch.outfit.OutfitRepository;
import com.secondstitch.user.AppUser;
import com.secondstitch.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Accounts end to end, with real signed tokens, exactly like the frontend.
 * No BREVO_API_KEY is set in tests, so emails go to ConsoleEmailSender,
 * and the tests read the links out of those emails.
 */
@SpringBootTest(properties = "app.seed-sample-data=true")
@AutoConfigureMockMvc
class AuthApiTest {

    private static final Pattern VERIFY_LINK = Pattern.compile("\\?verify=([A-Za-z0-9_-]+)");
    private static final Pattern RESET_LINK = Pattern.compile("\\?reset=([A-Za-z0-9_-]+)");

    @Autowired
    MockMvc mvc;

    @Autowired
    EmailSender emailSender;

    @Autowired
    AppUserRepository users;

    @Autowired
    EmailTokenRepository emailTokens;

    @Autowired
    ItemRepository items;

    @Autowired
    OutfitRepository outfits;

    @Autowired
    ItemPhotoRepository photos;

    ConsoleEmailSender inbox;

    @BeforeEach
    void startFresh() {
        outfits.deleteAll();
        photos.deleteAll();
        items.deleteAll();
        emailTokens.deleteAll();
        users.deleteAll();
        inbox = (ConsoleEmailSender) emailSender;
        inbox.clear();
    }

    // ---------- helpers ----------

    private ResultActions postJson(String path, String json) throws Exception {
        return mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions signUp(String email, String password) throws Exception {
        return postJson("/api/auth/signup",
                "{\"email\": \"%s\", \"password\": \"%s\", \"name\": \"Alex\"}".formatted(email, password));
    }

    private ResultActions logIn(String email, String password, String fromAddress) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .header("X-Forwarded-For", fromAddress)
                .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password)));
    }

    private List<ConsoleEmailSender.Sent> emailsTo(String email) {
        return inbox.sent().stream().filter(sent -> sent.to().equals(email)).toList();
    }

    /** The code from the newest email to this address matching the link pattern. */
    private String codeFromLatestEmail(String email, Pattern link) {
        List<ConsoleEmailSender.Sent> mail = emailsTo(email);
        assertThat(mail).as("emails to " + email).isNotEmpty();
        Matcher matcher = link.matcher(mail.get(mail.size() - 1).text());
        assertThat(matcher.find()).as("link in the email").isTrue();
        return matcher.group(1);
    }

    private ResultActions verify(String code) throws Exception {
        return postJson("/api/auth/verify", "{\"token\": \"%s\"}".formatted(code));
    }

    /** Signs up and clicks the confirmation link; returns the login token. */
    private String signUpAndConfirm(String email, String password) throws Exception {
        signUp(email, password).andExpect(status().isCreated());
        return tokenFrom(verify(codeFromLatestEmail(email, VERIFY_LINK)).andExpect(status().isOk()));
    }

    private static String tokenFrom(ResultActions result) throws Exception {
        return JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.token");
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    // ---------- signing up and confirming ----------

    @Test
    void signingUpSendsAConfirmationEmailButDoesNotLogYouIn() throws Exception {
        signUp("ana@example.com", "thrift-forever")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.message", not(emptyString())));

        List<ConsoleEmailSender.Sent> mail = emailsTo("ana@example.com");
        assertThat(mail).hasSize(1);
        assertThat(mail.get(0).subject()).isEqualTo("Confirm your email for Second Stitch");
        assertThat(mail.get(0).text()).contains("http://localhost:5173/?verify=");
    }

    @Test
    void clickingTheLinkConfirmsAndLogsYouIn() throws Exception {
        signUp("ana@example.com", "thrift-forever");
        String code = codeFromLatestEmail("ana@example.com", VERIFY_LINK);

        String token = tokenFrom(verify(code)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("ana@example.com")));

        mvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alex"));
        assertThat(users.findByEmail("ana@example.com").orElseThrow().isEmailVerified()).isTrue();
    }

    @Test
    void aConfirmationLinkOnlyWorksOnce() throws Exception {
        signUp("ana@example.com", "thrift-forever");
        String code = codeFromLatestEmail("ana@example.com", VERIFY_LINK);
        verify(code).andExpect(status().isOk());
        verify(code)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("That link has expired or was already used. Log in to get a new one."));
    }

    @Test
    void aMadeUpLinkDoesNothing() throws Exception {
        verify("made-up-code").andExpect(status().isBadRequest());
    }

    @Test
    void cannotLogInBeforeConfirming() throws Exception {
        signUp("ana@example.com", "thrift-forever");
        logIn("ana@example.com", "thrift-forever", "10.2.0.1")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    void canAskForTheConfirmationEmailAgain_butNotEverySecond() throws Exception {
        signUp("ana@example.com", "thrift-forever");
        postJson("/api/auth/resend-verification", "{\"email\": \"ana@example.com\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(AuthService.RESEND_REPLY));
        // Signing up just sent one, so the one-minute wait applies
        assertThat(emailsTo("ana@example.com")).hasSize(1);
    }

    @Test
    void resendingGivesTheSameReplyForUnknownEmails() throws Exception {
        postJson("/api/auth/resend-verification", "{\"email\": \"nobody@example.com\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(AuthService.RESEND_REPLY));
        assertThat(inbox.sent()).isEmpty();
    }

    @Test
    void confirmedAccountsStartWithTheSampleCloset() throws Exception {
        String token = signUpAndConfirm("ana@example.com", "thrift-forever");
        mvc.perform(get("/api/items").header("Authorization", bearer(token))).andExpect(jsonPath("$", hasSize(8)));
        mvc.perform(get("/api/outfits").header("Authorization", bearer(token))).andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void theFirstConfirmedAccountClaimsTheClosetMadeBeforeAccounts() throws Exception {
        Item old = new Item();
        old.setName("Thrifted before accounts existed");
        old.setCategory(Category.TOP);
        old.setPricePaid(new BigDecimal("100"));
        items.save(old); // no owner

        String token = signUpAndConfirm("me@example.com", "my-own-closet");

        mvc.perform(get("/api/items").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Thrifted before accounts existed"));
    }

    @Test
    void eachAccountHasItsOwnCloset() throws Exception {
        String ana = signUpAndConfirm("ana@example.com", "thrift-forever");
        String ben = signUpAndConfirm("ben@example.com", "vintage-vibes");

        String anasFirstId = JsonPath.read(mvc.perform(get("/api/items").header("Authorization", bearer(ana)))
                .andReturn().getResponse().getContentAsString(), "$[0].id").toString();

        mvc.perform(get("/api/items/" + anasFirstId).header("Authorization", bearer(ben)))
                .andExpect(status().isNotFound());
        assertThat(items.count()).isEqualTo(16); // 8 sample pieces each
    }

    @Test
    void passwordsAreStoredAsHashesNotAsTyped() throws Exception {
        signUp("ana@example.com", "thrift-forever");
        AppUser saved = users.findByEmail("ana@example.com").orElseThrow();
        assertThat(saved.getPasswordHash()).isNotEqualTo("thrift-forever").startsWith("$2"); // BCrypt
    }

    @Test
    void emailLinkCodesAreStoredAsHashes() throws Exception {
        signUp("ana@example.com", "thrift-forever");
        String code = codeFromLatestEmail("ana@example.com", VERIFY_LINK);
        // The database has the hash of the code, never the code itself
        assertThat(emailTokens.findByTokenHashAndPurpose(code, EmailToken.Purpose.VERIFY_EMAIL)).isEmpty();
        assertThat(emailTokens.findByTokenHashAndPurpose(EmailTokens.hash(code), EmailToken.Purpose.VERIFY_EMAIL))
                .isPresent();
    }

    @Test
    void anEmailCanOnlyBeUsedOnce_ignoringCapitals() throws Exception {
        signUp("ana@example.com", "thrift-forever").andExpect(status().isCreated());
        signUp("ANA@Example.com", "another-password").andExpect(status().isConflict());
    }

    @Test
    void signUpChecksTheFields() throws Exception {
        signUp("not-an-email", "short")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.email").value("Enter a valid email"))
                .andExpect(jsonPath("$.fields.password").value("Use 8 to 72 characters"));
    }

    // ---------- logging in ----------

    @Test
    void loggingInAfterConfirmingWorks() throws Exception {
        signUpAndConfirm("ana@example.com", "thrift-forever");
        String token = tokenFrom(logIn("Ana@Example.com", "thrift-forever", "10.2.0.2").andExpect(status().isOk()));
        mvc.perform(get("/api/items").header("Authorization", bearer(token))).andExpect(status().isOk());
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameMessage() throws Exception {
        signUpAndConfirm("ana@example.com", "thrift-forever");
        logIn("ana@example.com", "wrong-password", "10.2.0.3")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("That email and password don't match"));
        logIn("nobody@example.com", "whatever-123", "10.2.0.3")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("That email and password don't match"));
    }

    @Test
    void tooManyWrongPasswordsMeansWaiting() throws Exception {
        signUpAndConfirm("ana@example.com", "thrift-forever");
        for (int i = 0; i < FailedAttempts.MAX_FAILURES; i++) {
            logIn("ana@example.com", "guess-" + i, "10.2.0.4").andExpect(status().isUnauthorized());
        }
        logIn("ana@example.com", "thrift-forever", "10.2.0.4").andExpect(status().isTooManyRequests());
        logIn("ana@example.com", "thrift-forever", "10.2.0.5").andExpect(status().isOk());
    }

    @Test
    void noTokenOrAFakeTokenGets401() throws Exception {
        mvc.perform(get("/api/items"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Log in to continue"));
        mvc.perform(get("/api/items").header("Authorization", "Bearer not.a.real-token"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- forgot password ----------

    @Test
    void forgotPasswordEmailsAResetLink() throws Exception {
        signUpAndConfirm("ana@example.com", "thrift-forever");
        postJson("/api/auth/forgot-password", "{\"email\": \"ana@example.com\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(AuthService.FORGOT_REPLY));

        List<ConsoleEmailSender.Sent> mail = emailsTo("ana@example.com");
        assertThat(mail.get(mail.size() - 1).subject()).isEqualTo("Reset your Second Stitch password");
        assertThat(mail.get(mail.size() - 1).text()).contains("/?reset=");
    }

    @Test
    void forgotPasswordGivesTheSameReplyForUnknownEmails() throws Exception {
        postJson("/api/auth/forgot-password", "{\"email\": \"nobody@example.com\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(AuthService.FORGOT_REPLY));
        assertThat(inbox.sent()).isEmpty();
    }

    @Test
    void resettingChangesThePasswordAndLogsYouIn() throws Exception {
        signUpAndConfirm("ana@example.com", "thrift-forever");
        postJson("/api/auth/forgot-password", "{\"email\": \"ana@example.com\"}");
        String code = codeFromLatestEmail("ana@example.com", RESET_LINK);

        String token = tokenFrom(postJson("/api/auth/reset-password",
                "{\"token\": \"%s\", \"password\": \"brand-new-pass\"}".formatted(code))
                .andExpect(status().isOk()));
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(token))).andExpect(status().isOk());

        logIn("ana@example.com", "brand-new-pass", "10.2.0.6").andExpect(status().isOk());
        logIn("ana@example.com", "thrift-forever", "10.2.0.6").andExpect(status().isUnauthorized());
    }

    @Test
    void aResetLinkOnlyWorksOnce() throws Exception {
        signUpAndConfirm("ana@example.com", "thrift-forever");
        postJson("/api/auth/forgot-password", "{\"email\": \"ana@example.com\"}");
        String code = codeFromLatestEmail("ana@example.com", RESET_LINK);
        String body = "{\"token\": \"%s\", \"password\": \"brand-new-pass\"}".formatted(code);

        postJson("/api/auth/reset-password", body).andExpect(status().isOk());
        postJson("/api/auth/reset-password", body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("That reset link has expired or was already used. Ask for a new one."));
    }

    @Test
    void resettingAlsoConfirmsAnUnconfirmedEmail() throws Exception {
        signUp("ana@example.com", "thrift-forever"); // never clicked the confirmation link
        postJson("/api/auth/forgot-password", "{\"email\": \"ana@example.com\"}");
        String code = codeFromLatestEmail("ana@example.com", RESET_LINK);

        postJson("/api/auth/reset-password", "{\"token\": \"%s\", \"password\": \"brand-new-pass\"}".formatted(code))
                .andExpect(status().isOk());
        logIn("ana@example.com", "brand-new-pass", "10.2.0.7").andExpect(status().isOk());
    }
}
