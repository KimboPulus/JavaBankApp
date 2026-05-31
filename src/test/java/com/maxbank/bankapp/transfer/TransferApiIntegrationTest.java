package com.maxbank.bankapp.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maxbank.bankapp.account.AccountResponse;
import com.maxbank.bankapp.auth.RegisterRequest;
import java.math.BigDecimal;
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
class TransferApiIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void transferEndpointMovesMoneyEndToEnd() throws Exception {
        String senderToken = register("sender", "sender@example.com");
        String receiverToken = register("receiver", "receiver@example.com");

        AccountResponse senderAccount = accounts(senderToken).get(0);
        AccountResponse receiverAccount = accounts(receiverToken).get(0);

        TransferRequest transfer = new TransferRequest(
                senderAccount.accountNumber(),
                receiverAccount.accountNumber(),
                new BigDecimal("75.50")
        );

        mockMvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + senderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(transfer)))
                .andExpect(status().isCreated());

        AccountResponse updatedSender = accounts(senderToken).get(0);
        AccountResponse updatedReceiver = accounts(receiverToken).get(0);

        assertThat(updatedSender.balance()).isEqualByComparingTo("924.50");
        assertThat(updatedReceiver.balance()).isEqualByComparingTo("1075.50");
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
