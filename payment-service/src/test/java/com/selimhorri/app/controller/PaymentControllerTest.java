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
import com.selimhorri.app.domain.PaymentStatus;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.PaymentDto;
import com.selimhorri.app.exception.ApiExceptionHandler;
import com.selimhorri.app.exception.wrapper.PaymentNotFoundException;
import com.selimhorri.app.service.PaymentService;

@WebMvcTest(controllers = com.selimhorri.app.resource.PaymentResource.class)
@Import(ApiExceptionHandler.class)
@ActiveProfiles("test")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    @Test
    void getAllPayments_returnsCollectionResponse() throws Exception {
        OrderDto orderDto = OrderDto.builder().orderId(10).build();
        PaymentDto p1 = PaymentDto.builder()
                .paymentId(1)
                .orderDto(orderDto)
                .isPayed(true)
                .paymentStatus(PaymentStatus.COMPLETED)
                .build();
        PaymentDto p2 = PaymentDto.builder()
                .paymentId(2)
                .orderDto(orderDto)
                .isPayed(false)
                .paymentStatus(PaymentStatus.IN_PROGRESS)
                .build();
        List<PaymentDto> list = Arrays.asList(p1, p2);

        given(paymentService.findAll()).willReturn(list);

        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection.length()").value(2))
                .andExpect(jsonPath("$.collection[0].paymentId").value(1))
                .andExpect(jsonPath("$.collection[1].paymentId").value(2));
    }

    @Test
    void getPaymentById_returnsPayment() throws Exception {
        OrderDto orderDto = OrderDto.builder().orderId(10).build();
        PaymentDto payment = PaymentDto.builder()
                .paymentId(1)
                .orderDto(orderDto)
                .isPayed(true)
                .paymentStatus(PaymentStatus.COMPLETED)
                .build();

        given(paymentService.findById(1)).willReturn(payment);

        mockMvc.perform(get("/api/payments/{id}", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(1))
                .andExpect(jsonPath("$.isPayed").value(true))
                .andExpect(jsonPath("$.paymentStatus").value("COMPLETED"));
    }

    @Test
    void getPaymentById_whenNotFound_returnsBadRequestWithErrorBody() throws Exception {
        given(paymentService.findById(99))
                .willThrow(new PaymentNotFoundException("Payment with id: 99 not found"));

        mockMvc.perform(get("/api/payments/{id}", "99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.httpStatus").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.msg").exists());
    }

    @Test
    void savePayment_returnsSavedPayment() throws Exception {
        OrderDto orderDto = OrderDto.builder().orderId(10).build();
        PaymentDto request = PaymentDto.builder()
                .orderDto(orderDto)
                .isPayed(false)
                .paymentStatus(PaymentStatus.NOT_STARTED)
                .build();

        PaymentDto response = PaymentDto.builder()
                .paymentId(1)
                .orderDto(orderDto)
                .isPayed(false)
                .paymentStatus(PaymentStatus.NOT_STARTED)
                .build();

        given(paymentService.save(any(PaymentDto.class))).willReturn(response);

        mockMvc.perform(post("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(1))
                .andExpect(jsonPath("$.isPayed").value(false))
                .andExpect(jsonPath("$.paymentStatus").value("NOT_STARTED"));
    }

    @Test
    void updatePayment_returnsUpdatedPayment() throws Exception {
        OrderDto orderDto = OrderDto.builder().orderId(10).build();
        PaymentDto request = PaymentDto.builder()
                .paymentId(1)
                .orderDto(orderDto)
                .isPayed(true)
                .paymentStatus(PaymentStatus.COMPLETED)
                .build();

        PaymentDto response = PaymentDto.builder()
                .paymentId(1)
                .orderDto(orderDto)
                .isPayed(true)
                .paymentStatus(PaymentStatus.COMPLETED)
                .build();

        given(paymentService.update(any(PaymentDto.class))).willReturn(response);

        mockMvc.perform(put("/api/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(1))
                .andExpect(jsonPath("$.isPayed").value(true))
                .andExpect(jsonPath("$.paymentStatus").value("COMPLETED"));
    }

    @Test
    void deletePayment_returnsTrue() throws Exception {
        doNothing().when(paymentService).deleteById(1);

        mockMvc.perform(delete("/api/payments/{id}", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        verify(paymentService).deleteById(1);
    }
}

