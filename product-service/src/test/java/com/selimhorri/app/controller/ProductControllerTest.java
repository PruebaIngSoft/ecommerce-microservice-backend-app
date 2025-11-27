package com.selimhorri.app.controller;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.exception.ApiExceptionHandler;
import com.selimhorri.app.exception.wrapper.ProductNotFoundException;
import com.selimhorri.app.service.ProductService;

@WebMvcTest(controllers = com.selimhorri.app.resource.ProductResource.class)
@Import(ApiExceptionHandler.class)
@ActiveProfiles("test")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @Test
    void getAllProducts_returnsCollectionResponse() throws Exception {
        ProductDto p1 = ProductDto.builder().productId(1).productTitle("P1").build();
        ProductDto p2 = ProductDto.builder().productId(2).productTitle("P2").build();
        List<ProductDto> list = Arrays.asList(p1, p2);

        given(productService.findAll()).willReturn(list);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection.length()").value(2))
                .andExpect(jsonPath("$.collection[0].productId").value(1))
                .andExpect(jsonPath("$.collection[1].productId").value(2));
    }

    @Test
    void getProductById_returnsProduct() throws Exception {
        ProductDto product = ProductDto.builder()
                .productId(1)
                .productTitle("P1")
                .build();

        given(productService.findById(1)).willReturn(product);

        mockMvc.perform(get("/api/products/{id}", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.productTitle").value("P1"));
    }

    @Test
    void getProductById_whenNotFound_returnsBadRequestWithErrorBody() throws Exception {
        given(productService.findById(99)).willThrow(new ProductNotFoundException("Product with id: 99 not found"));

        mockMvc.perform(get("/api/products/{id}", "99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.httpStatus").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.msg").exists());
    }

    @Test
    void saveProduct_returnsSavedProduct() throws Exception {
        CategoryDto categoryDto = CategoryDto.builder()
                .categoryId(10)
                .categoryTitle("Category 1")
                .build();

        ProductDto request = ProductDto.builder()
                .productTitle("New Product")
                .categoryDto(categoryDto)
                .build();

        ProductDto response = ProductDto.builder()
                .productId(1)
                .productTitle("New Product")
                .categoryDto(categoryDto)
                .build();

        given(productService.save(request)).willReturn(response);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.productTitle").value("New Product"));
    }

    @Test
    void updateProduct_returnsUpdatedProduct() throws Exception {
        ProductDto request = ProductDto.builder()
                .productId(1)
                .productTitle("Updated")
                .build();

        ProductDto response = ProductDto.builder()
                .productId(1)
                .productTitle("Updated")
                .build();

        given(productService.update(request)).willReturn(response);

        mockMvc.perform(put("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.productTitle").value("Updated"));
    }

    @Test
    void updateProductWithPathId_returnsUpdatedProduct() throws Exception {
        ProductDto request = ProductDto.builder()
                .productTitle("Updated")
                .build();

        ProductDto response = ProductDto.builder()
                .productId(1)
                .productTitle("Updated")
                .build();

        given(productService.update(1, request)).willReturn(response);

        mockMvc.perform(put("/api/products/{id}", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.productTitle").value("Updated"));
    }

    @Test
    void deleteProduct_returnsTrue() throws Exception {
        doNothing().when(productService).deleteById(1);

        mockMvc.perform(delete("/api/products/{id}", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        verify(productService).deleteById(1);
    }
}

