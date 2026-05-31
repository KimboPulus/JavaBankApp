package com.maxbank.bankapp.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maxbank.bankapp.auth.RegisterRequest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loggedInUserCanOpenAnotherBankAccountFromApi() throws Exception {
        String token = register("accountowner", "accountowner@example.com");

        AccountResponse created = openAccount(token, CurrencyCode.EUR, AccountType.SAVINGS);
        List<AccountResponse> accounts = accounts(token);

        assertThat(created.accountNumber()).startsWith("PL");
        assertThat(created.balance()).isZero();
        assertThat(created.currency()).isEqualTo(CurrencyCode.EUR);
        assertThat(created.accountType()).isEqualTo(AccountType.SAVINGS);
        assertThat(created.closed()).isFalse();
        assertThat(accounts).hasSize(2);
        assertThat(accounts).extracting(AccountResponse::accountNumber).contains(created.accountNumber());
    }

    @Test
    void anonymousUserCannotOpenBankAccount() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest(CurrencyCode.PLN, AccountType.CHECKING);

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void userCanCloseEmptyBankAccount() throws Exception {
        String token = register("closer", "closer@example.com");
        AccountResponse account = openAccount(token, CurrencyCode.USD, AccountType.SAVINGS);

        mockMvc.perform(delete("/api/accounts/{accountNumber}", account.accountNumber())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        List<AccountResponse> accounts = accounts(token);
        assertThat(accounts).hasSize(1);
        assertThat(accounts).extracting(AccountResponse::accountNumber).doesNotContain(account.accountNumber());
    }

    @Test
    void userCannotCloseAccountWithMoneyStillInIt() throws Exception {
        String token = register("balanceowner", "balanceowner@example.com");
        AccountResponse firstAccount = accounts(token).get(0);

        mockMvc.perform(delete("/api/accounts/{accountNumber}", firstAccount.accountNumber())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());

        assertThat(accounts(token)).hasSize(1);
    }

    @Test
    void userCannotCloseSomeoneElsesBankAccount() throws Exception {
        String ownerToken = register("realowner", "realowner@example.com");
        String otherToken = register("otherowner", "otherowner@example.com");
        AccountResponse account = openAccount(ownerToken, CurrencyCode.GBP, AccountType.CHECKING);

        mockMvc.perform(delete("/api/accounts/{accountNumber}", account.accountNumber())
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        assertThat(accounts(ownerToken)).extracting(AccountResponse::accountNumber).contains(account.accountNumber());
    }

    private AccountResponse openAccount(String token, CurrencyCode currency, AccountType accountType) throws Exception {
        CreateAccountRequest request = new CreateAccountRequest(currency, accountType);
        String createdBody = mockMvc.perform(post("/api/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(createdBody, AccountResponse.class);
    }

    private String register(String username, String email) throws Exception {
        RegisterRequest request = new RegisterRequest(username, email, "password123");
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return json.get("token").asText();
    }

    private List<AccountResponse> accounts(String token) throws Exception {
        String body = mockMvc.perform(get("/api/accounts")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readValue(body, new TypeReference<>() {
        });
    }
}
