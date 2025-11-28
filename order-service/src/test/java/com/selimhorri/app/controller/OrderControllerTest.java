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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.exception.ApiExceptionHandler;
import com.selimhorri.app.exception.wrapper.OrderNotFoundException;
import com.selimhorri.app.service.OrderService;

@WebMvcTest(controllers = com.selimhorri.app.resource.OrderResource.class)
@Import(ApiExceptionHandler.class)
@ActiveProfiles("test")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @Test
    void getAllOrders_returnsCollectionResponse() throws Exception {
        CartDto cartDto = CartDto.builder().cartId(1).userId(10).build();
        OrderDto o1 = OrderDto.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS))
                .orderDesc("Order 1")
                .orderFee(100.0)
                .cartDto(cartDto)
                .build();
        OrderDto o2 = OrderDto.builder()
                .orderId(2)
                .orderDate(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS))
                .orderDesc("Order 2")
                .orderFee(200.0)
                .cartDto(cartDto)
                .build();
        List<OrderDto> list = Arrays.asList(o1, o2);

        given(orderService.findAll()).willReturn(list);

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection.length()").value(2))
                .andExpect(jsonPath("$.collection[0].orderId").value(1))
                .andExpect(jsonPath("$.collection[1].orderId").value(2));
    }

    @Test
    void getOrderById_returnsOrder() throws Exception {
        CartDto cartDto = CartDto.builder().cartId(1).userId(10).build();
        OrderDto order = OrderDto.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS))
                .orderDesc("Order 1")
                .orderFee(100.0)
                .cartDto(cartDto)
                .build();

        given(orderService.findById(1)).willReturn(order);

        mockMvc.perform(get("/api/orders/{id}", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.orderDesc").value("Order 1"))
                .andExpect(jsonPath("$.orderFee").value(100.0));
    }

    @Test
    void getOrderById_whenNotFound_returnsBadRequestWithErrorBody() throws Exception {
        given(orderService.findById(99))
                .willThrow(new OrderNotFoundException("Order with id: 99 not found"));

        mockMvc.perform(get("/api/orders/{id}", "99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.httpStatus").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.msg").exists());
    }

    @Test
    void saveOrder_returnsSavedOrder() throws Exception {
        CartDto cartDto = CartDto.builder().cartId(1).userId(10).build();
        OrderDto request = OrderDto.builder()
                .orderDate(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS))
                .orderDesc("New Order")
                .orderFee(150.0)
                .cartDto(cartDto)
                .build();

        OrderDto response = OrderDto.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS))
                .orderDesc("New Order")
                .orderFee(150.0)
                .cartDto(cartDto)
                .build();

        given(orderService.save(any(OrderDto.class))).willReturn(response);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.orderDesc").value("New Order"));
    }

    @Test
    void updateOrder_returnsUpdatedOrder() throws Exception {
        CartDto cartDto = CartDto.builder().cartId(1).userId(10).build();
        OrderDto request = OrderDto.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS))
                .orderDesc("Updated")
                .orderFee(250.0)
                .cartDto(cartDto)
                .build();

        OrderDto response = OrderDto.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS))
                .orderDesc("Updated")
                .orderFee(250.0)
                .cartDto(cartDto)
                .build();

        given(orderService.update(any(OrderDto.class))).willReturn(response);

        mockMvc.perform(put("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.orderDesc").value("Updated"));
    }

    @Test
    void updateOrderWithPathId_returnsUpdatedOrder() throws Exception {
        CartDto cartDto = CartDto.builder().cartId(1).userId(10).build();
        OrderDto request = OrderDto.builder()
                .orderDesc("Updated")
                .cartDto(cartDto)
                .build();

        OrderDto response = OrderDto.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS))
                .orderDesc("Updated")
                .orderFee(100.0)
                .cartDto(cartDto)
                .build();

        given(orderService.update(1, request)).willReturn(response);

        mockMvc.perform(put("/api/orders/{id}", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.orderDesc").value("Updated"));
    }

    @Test
    void deleteOrder_returnsTrue() throws Exception {
        doNothing().when(orderService).deleteById(1);

        mockMvc.perform(delete("/api/orders/{id}", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        verify(orderService).deleteById(1);
    }
}

