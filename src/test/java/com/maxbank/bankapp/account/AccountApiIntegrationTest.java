package com.maxbank.bankapp.account;

import static org.assertj.core.api.Assertions.assertThat;
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
    void loggedInUserCanOpenAnotherBankAccount() throws Exception {
        String token = register("accountowner", "accountowner@example.com");

        CreateAccountRequest request = new CreateAccountRequest(CurrencyCode.EUR, AccountType.SAVINGS);
        String createdBody = mockMvc.perform(post("/api/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        AccountResponse created = objectMapper.readValue(createdBody, AccountResponse.class);
        List<AccountResponse> accounts = accounts(token);

        assertThat(created.accountNumber()).startsWith("PL");
        assertThat(created.balance()).isZero();
        assertThat(created.currency()).isEqualTo(CurrencyCode.EUR);
        assertThat(created.accountType()).isEqualTo(AccountType.SAVINGS);
        assertThat(accounts).hasSize(2);
        assertThat(accounts).extracting(AccountResponse::accountNumber).contains(created.accountNumber());
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
