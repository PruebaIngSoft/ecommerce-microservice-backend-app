package com.selimhorri.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.domain.Payment;
import com.selimhorri.app.domain.PaymentStatus;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.dto.PaymentDto;
import com.selimhorri.app.exception.wrapper.PaymentNotFoundException;
import com.selimhorri.app.repository.PaymentRepository;

@SpringBootTest
@ActiveProfiles("test")
class PaymentServiceTest {

    @Autowired
    private PaymentService paymentService;

    @MockBean
    private PaymentRepository paymentRepository;

    @MockBean
    private RestTemplate restTemplate;

    @Test
    void findAll_returnsAllPaymentsWithOrder() {
        Payment payment1 = Payment.builder()
                .paymentId(1)
                .orderId(10)
                .isPayed(true)
                .paymentStatus(PaymentStatus.COMPLETED)
                .build();

        Payment payment2 = Payment.builder()
                .paymentId(2)
                .orderId(20)
                .isPayed(false)
                .paymentStatus(PaymentStatus.IN_PROGRESS)
                .build();

        OrderDto order1 = OrderDto.builder().orderId(10).build();
        OrderDto order2 = OrderDto.builder().orderId(20).build();

        given(paymentRepository.findAll()).willReturn(Arrays.asList(payment1, payment2));
        given(restTemplate.getForObject(anyString(), any(Class.class)))
                .willReturn(order1, order2);

        List<PaymentDto> result = paymentService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(PaymentDto::getPaymentId)
                .containsExactlyInAnyOrder(1, 2);

        verify(paymentRepository).findAll();
    }

    @Test
    void findById_returnsPaymentWithOrder() {
        Integer id = 1;
        Payment payment = Payment.builder()
                .paymentId(id)
                .orderId(10)
                .isPayed(true)
                .paymentStatus(PaymentStatus.COMPLETED)
                .build();

        OrderDto order = OrderDto.builder().orderId(10).build();

        given(paymentRepository.findById(id)).willReturn(Optional.of(payment));
        given(restTemplate.getForObject(anyString(), any(Class.class))).willReturn(order);

        PaymentDto result = paymentService.findById(id);

        assertThat(result.getPaymentId()).isEqualTo(id);
        assertThat(result.getIsPayed()).isTrue();
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);

        verify(paymentRepository).findById(id);
    }

    @Test
    void findById_throwsExceptionWhenPaymentDoesNotExist() {
        Integer id = 99;
        given(paymentRepository.findById(id)).willReturn(Optional.empty());

        assertThrows(PaymentNotFoundException.class, () -> paymentService.findById(id));
        verify(paymentRepository).findById(id);
    }

    @Test
    void save_persistsAndReturnsMappedPayment() {
        OrderDto orderDto = OrderDto.builder()
                .orderId(10)
                .build();

        PaymentDto toSave = PaymentDto.builder()
                .paymentId(null)
                .orderDto(orderDto)
                .isPayed(false)
                .paymentStatus(PaymentStatus.NOT_STARTED)
                .build();

        Payment savedEntity = Payment.builder()
                .paymentId(1)
                .orderId(10)
                .isPayed(false)
                .paymentStatus(PaymentStatus.NOT_STARTED)
                .build();

        given(paymentRepository.save(any(Payment.class))).willReturn(savedEntity);

        PaymentDto result = paymentService.save(toSave);

        assertThat(result.getPaymentId()).isEqualTo(1);
        assertThat(result.getIsPayed()).isFalse();
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.NOT_STARTED);

        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void update_updatesAndReturnsMappedPayment() {
        OrderDto orderDto = OrderDto.builder()
                .orderId(10)
                .build();

        PaymentDto toUpdate = PaymentDto.builder()
                .paymentId(1)
                .orderDto(orderDto)
                .isPayed(true)
                .paymentStatus(PaymentStatus.COMPLETED)
                .build();

        Payment updatedEntity = Payment.builder()
                .paymentId(1)
                .orderId(10)
                .isPayed(true)
                .paymentStatus(PaymentStatus.COMPLETED)
                .build();

        given(paymentRepository.save(any(Payment.class))).willReturn(updatedEntity);

        PaymentDto result = paymentService.update(toUpdate);

        assertThat(result.getPaymentId()).isEqualTo(1);
        assertThat(result.getIsPayed()).isTrue();
        assertThat(result.getPaymentStatus()).isEqualTo(PaymentStatus.COMPLETED);

        verify(paymentRepository).save(any(Payment.class));
    }

    @Test
    void deleteById_deletesPayment() {
        Integer id = 1;

        paymentService.deleteById(id);

        verify(paymentRepository).deleteById(id);
    }
}

