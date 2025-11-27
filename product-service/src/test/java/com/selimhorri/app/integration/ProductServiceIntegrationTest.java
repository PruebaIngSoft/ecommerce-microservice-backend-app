package com.selimhorri.app.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.dto.ProductDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.closeTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createAndRetrieveProduct() throws Exception {
        JsonNode categoryResponse = objectMapper.readTree(
                mockMvc.perform(post("/api/categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(
                                        com.selimhorri.app.dto.CategoryDto.builder()
                                                .categoryTitle("Integration Category")
                                                .imageUrl("https://cdn.example.com/category.png")
                                                .build())))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.categoryId").isNumber())
                        .andReturn()
                        .getResponse()
                        .getContentAsString());

        int categoryId = categoryResponse.get("categoryId").asInt();

        ProductDto request = ProductDto.builder()
                .productTitle("Integration Product")
                .imageUrl("https://cdn.example.com/product.png")
                .sku("SKU-" + UUID.randomUUID())
                .priceUnit(49.99)
                .quantity(10)
                .categoryDto(com.selimhorri.app.dto.CategoryDto.builder()
                        .categoryId(categoryId)
                        .categoryTitle("Integration Category")
                        .imageUrl("https://cdn.example.com/category.png")
                        .build())
                .build();

        String payload = objectMapper.writeValueAsString(request);

        JsonNode creationResponse = objectMapper.readTree(
                mockMvc.perform(post("/api/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.productId").isNumber())
                        .andExpect(jsonPath("$.productTitle").value(request.getProductTitle()))
                        .andExpect(jsonPath("$.imageUrl").value(request.getImageUrl()))
                        .andExpect(jsonPath("$.sku").value(request.getSku()))
                        .andExpect(jsonPath("$.quantity").value(request.getQuantity()))
                        .andExpect(jsonPath("$.priceUnit").value(closeTo(request.getPriceUnit(), 0.001)))
                        .andReturn()
                        .getResponse()
                        .getContentAsString());

        int productId = creationResponse.get("productId").asInt();

        mockMvc.perform(get("/api/products/{productId}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(productId))
                .andExpect(jsonPath("$.productTitle").value(request.getProductTitle()))
                .andExpect(jsonPath("$.imageUrl").value(request.getImageUrl()))
                .andExpect(jsonPath("$.sku").value(request.getSku()))
                .andExpect(jsonPath("$.quantity").value(request.getQuantity()))
                .andExpect(jsonPath("$.priceUnit").value(closeTo(request.getPriceUnit(), 0.001)))
                .andExpect(jsonPath("$.category.categoryId").value(categoryId));
    }
}

