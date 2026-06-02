package com.mehmetserin.reconcile;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReconcileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void reconcile_endpoint() throws Exception {
        String body = """
                {
                  "expected": [{"id":"E1","date":"2026-06-01","amount":50.00,"reference":"R1"}],
                  "statement": [{"id":"S1","date":"2026-06-01","amount":50.00,"description":"xfer R1"}]
                }
                """;
        mockMvc.perform(post("/api/reconcile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matched.length()").value(1));
    }
}
