package com.selimhorri.app.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.Order;
import com.selimhorri.app.repository.CartRepository;
import com.selimhorri.app.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@AutoConfigureWireMock(port = 0)
@ActiveProfiles("test")
class OrderServiceIntegrationTest {

    private static final DateTimeFormatter ORDER_DATE_FORMATTER =
            DateTimeFormatter.ofPattern(AppConstant.LOCAL_DATE_TIME_FORMAT);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private CartRepository cartRepository;

    @BeforeEach
    void cleanDatabase() {
        orderRepository.deleteAll();
        cartRepository.deleteAll();
        WireMock.reset();
    }

    @Test
    void placeOrder_shouldSucceed_whenProductExists() throws Exception {
        stubFor(get(urlMatching("/api/products/1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"productId\":1,\"price\":100.0,\"productTitle\":\"Test Product\"}")
                        .withStatus(200)));

        int cartId = createCartForUser();

        Map<String, Object> orderPayload = buildOrderPayload(cartId);

        JsonNode orderResponse = objectMapper.readTree(
                mockMvc.perform(post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(orderPayload)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.orderId").isNumber())
                        .andExpect(jsonPath("$.orderDesc").value(orderPayload.get("orderDesc")))
                        .andReturn()
                        .getResponse()
                        .getContentAsString());

        int orderId = orderResponse.get("orderId").asInt();

        Order persistedOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new AssertionError("Order should be persisted in H2"));

        assertEquals(orderPayload.get("orderDesc"), persistedOrder.getOrderDesc());
        assertNotNull(persistedOrder.getCart(), "Order must be associated with a cart");
        assertEquals(cartId, persistedOrder.getCart().getCartId());
    }

    private int createCartForUser() throws Exception {
        Map<String, Object> cartPayload = new HashMap<>();
        cartPayload.put("userId", ThreadLocalRandom.current().nextInt(1, 10_000));

        JsonNode response = objectMapper.readTree(
                mockMvc.perform(post("/api/carts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(cartPayload)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.cartId").isNumber())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());

        return response.get("cartId").asInt();
    }

    private Map<String, Object> buildOrderPayload(final int cartId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderDate", ORDER_DATE_FORMATTER.format(LocalDateTime.now()));
        payload.put("orderDesc", "Integration order for product 1");
        payload.put("orderFee", 100.0);
        Map<String, Object> cart = new HashMap<>();
        cart.put("cartId", cartId);
        payload.put("cart", cart);
        return payload;
    }
}

