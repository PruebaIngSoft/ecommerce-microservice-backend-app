package com.selimhorri.app.controller;

import static org.mockito.ArgumentMatchers.any;
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
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.exception.ApiExceptionHandler;
import com.selimhorri.app.exception.wrapper.CartNotFoundException;
import com.selimhorri.app.service.CartService;

@WebMvcTest(controllers = com.selimhorri.app.resource.CartResource.class)
@Import(ApiExceptionHandler.class)
@ActiveProfiles("test")
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CartService cartService;

    @Test
    void getAllCarts_returnsCollectionResponse() throws Exception {
        CartDto c1 = CartDto.builder().cartId(1).userId(10).build();
        CartDto c2 = CartDto.builder().cartId(2).userId(20).build();
        List<CartDto> list = Arrays.asList(c1, c2);

        given(cartService.findAll()).willReturn(list);

        mockMvc.perform(get("/api/carts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection.length()").value(2))
                .andExpect(jsonPath("$.collection[0].cartId").value(1))
                .andExpect(jsonPath("$.collection[1].cartId").value(2));
    }

    @Test
    void getCartById_returnsCart() throws Exception {
        CartDto cart = CartDto.builder()
                .cartId(1)
                .userId(10)
                .build();

        given(cartService.findById(1)).willReturn(cart);

        mockMvc.perform(get("/api/carts/{id}", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").value(1))
                .andExpect(jsonPath("$.userId").value(10));
    }

    @Test
    void getCartById_whenNotFound_returnsBadRequestWithErrorBody() throws Exception {
        given(cartService.findById(99))
                .willThrow(new CartNotFoundException("Cart with id: 99 not found"));

        mockMvc.perform(get("/api/carts/{id}", "99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.httpStatus").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.msg").exists());
    }

    @Test
    void saveCart_returnsSavedCart() throws Exception {
        CartDto request = CartDto.builder()
                .userId(10)
                .build();

        CartDto response = CartDto.builder()
                .cartId(1)
                .userId(10)
                .build();

        given(cartService.save(any(CartDto.class))).willReturn(response);

        mockMvc.perform(post("/api/carts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").value(1))
                .andExpect(jsonPath("$.userId").value(10));
    }

    @Test
    void updateCart_returnsUpdatedCart() throws Exception {
        CartDto request = CartDto.builder()
                .cartId(1)
                .userId(20)
                .build();

        CartDto response = CartDto.builder()
                .cartId(1)
                .userId(20)
                .build();

        given(cartService.update(any(CartDto.class))).willReturn(response);

        mockMvc.perform(put("/api/carts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").value(1))
                .andExpect(jsonPath("$.userId").value(20));
    }

    @Test
    void updateCartWithPathId_returnsUpdatedCart() throws Exception {
        CartDto request = CartDto.builder()
                .userId(20)
                .build();

        CartDto response = CartDto.builder()
                .cartId(1)
                .userId(20)
                .build();

        given(cartService.update(1, request)).willReturn(response);

        mockMvc.perform(put("/api/carts/{id}", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").value(1))
                .andExpect(jsonPath("$.userId").value(20));
    }

    @Test
    void deleteCart_returnsTrue() throws Exception {
        doNothing().when(cartService).deleteById(1);

        mockMvc.perform(delete("/api/carts/{id}", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        verify(cartService).deleteById(1);
    }
}

