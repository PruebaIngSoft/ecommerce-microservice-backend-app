package com.selimhorri.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.domain.Order;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.exception.wrapper.OrderNotFoundException;
import com.selimhorri.app.repository.OrderRepository;

@SpringBootTest
@ActiveProfiles("test")
class OrderServiceTest {

    @Autowired
    private OrderService orderService;

    @MockBean
    private OrderRepository orderRepository;

    @Test
    void findAll_returnsAllOrdersFromRepository() {
        Cart cart = Cart.builder()
                .cartId(1)
                .userId(10)
                .build();

        Order order1 = Order.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS))
                .orderDesc("Order 1")
                .orderFee(100.0)
                .cart(cart)
                .build();

        Order order2 = Order.builder()
                .orderId(2)
                .orderDate(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS))
                .orderDesc("Order 2")
                .orderFee(200.0)
                .cart(cart)
                .build();

        given(orderRepository.findAll()).willReturn(Arrays.asList(order1, order2));

        List<OrderDto> result = orderService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(OrderDto::getOrderId)
                .containsExactlyInAnyOrder(1, 2);

        verify(orderRepository).findAll();
    }

    @Test
    void findById_returnsOrderWhenExists() {
        Integer id = 1;
        Cart cart = Cart.builder()
                .cartId(1)
                .userId(10)
                .build();

        Order order = Order.builder()
                .orderId(id)
                .orderDate(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS))
                .orderDesc("Order 1")
                .orderFee(100.0)
                .cart(cart)
                .build();

        given(orderRepository.findById(id)).willReturn(Optional.of(order));

        OrderDto result = orderService.findById(id);

        assertThat(result.getOrderId()).isEqualTo(id);
        assertThat(result.getOrderDesc()).isEqualTo("Order 1");
        assertThat(result.getOrderFee()).isEqualTo(100.0);

        verify(orderRepository).findById(id);
    }

    @Test
    void findById_throwsExceptionWhenOrderDoesNotExist() {
        Integer id = 99;
        given(orderRepository.findById(id)).willReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.findById(id));
        verify(orderRepository).findById(id);
    }

    @Test
    void save_persistsAndReturnsMappedOrder() {
        CartDto cartDto = CartDto.builder()
                .cartId(1)
                .userId(10)
                .build();

        OrderDto toSave = OrderDto.builder()
                .orderId(null)
                .orderDate(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS))
                .orderDesc("New Order")
                .orderFee(150.0)
                .cartDto(cartDto)
                .build();

        Cart cart = Cart.builder()
                .cartId(1)
                .userId(10)
                .build();

        Order savedEntity = Order.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS))
                .orderDesc("New Order")
                .orderFee(150.0)
                .cart(cart)
                .build();

        given(orderRepository.save(any(Order.class))).willReturn(savedEntity);

        OrderDto result = orderService.save(toSave);

        assertThat(result.getOrderId()).isEqualTo(1);
        assertThat(result.getOrderDesc()).isEqualTo("New Order");
        assertThat(result.getCartDto().getCartId()).isEqualTo(1);

        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void update_withDto_updatesAndReturnsMappedOrder() {
        CartDto cartDto = CartDto.builder()
                .cartId(1)
                .userId(10)
                .build();

        OrderDto toUpdate = OrderDto.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS))
                .orderDesc("Updated Order")
                .orderFee(250.0)
                .cartDto(cartDto)
                .build();

        Cart cart = Cart.builder()
                .cartId(1)
                .userId(10)
                .build();

        Order updatedEntity = Order.builder()
                .orderId(1)
                .orderDate(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS))
                .orderDesc("Updated Order")
                .orderFee(250.0)
                .cart(cart)
                .build();

        given(orderRepository.save(any(Order.class))).willReturn(updatedEntity);

        OrderDto result = orderService.update(toUpdate);

        assertThat(result.getOrderId()).isEqualTo(1);
        assertThat(result.getOrderDesc()).isEqualTo("Updated Order");
        assertThat(result.getOrderFee()).isEqualTo(250.0);

        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void update_withId_usesExistingOrderAndReturnsMappedOrder() {
        Integer id = 1;

        Cart cart = Cart.builder()
                .cartId(1)
                .userId(10)
                .build();

        Order existing = Order.builder()
                .orderId(id)
                .orderDate(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS))
                .orderDesc("Existing Order")
                .orderFee(100.0)
                .cart(cart)
                .build();

        given(orderRepository.findById(id)).willReturn(Optional.of(existing));
        given(orderRepository.save(any(Order.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        OrderDto anyDto = OrderDto.builder().orderId(id).build();

        OrderDto result = orderService.update(id, anyDto);

        assertThat(result.getOrderId()).isEqualTo(id);
        assertThat(result.getOrderDesc()).isEqualTo("Existing Order");

        verify(orderRepository).findById(id);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void update_withId_throwsExceptionWhenOrderDoesNotExist() {
        Integer id = 99;
        given(orderRepository.findById(id)).willReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class,
                () -> orderService.update(id, OrderDto.builder().build()));

        verify(orderRepository).findById(id);
    }

    @Test
    void deleteById_deletesExistingOrder() {
        Integer id = 1;

        Cart cart = Cart.builder()
                .cartId(1)
                .userId(10)
                .build();

        Order existing = Order.builder()
                .orderId(id)
                .orderDate(LocalDateTime.now().truncatedTo(ChronoUnit.MICROS))
                .orderDesc("To Delete")
                .orderFee(50.0)
                .cart(cart)
                .build();

        given(orderRepository.findById(id)).willReturn(Optional.of(existing));

        orderService.deleteById(id);

        verify(orderRepository).findById(id);
        verify(orderRepository).delete(any(Order.class));
    }

    @Test
    void deleteById_throwsExceptionWhenOrderDoesNotExist() {
        Integer id = 99;
        given(orderRepository.findById(id)).willReturn(Optional.empty());

        assertThrows(OrderNotFoundException.class, () -> orderService.deleteById(id));
        verify(orderRepository).findById(id);
    }
}

