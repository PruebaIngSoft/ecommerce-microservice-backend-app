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

import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.wrapper.CartNotFoundException;
import com.selimhorri.app.repository.CartRepository;

@SpringBootTest
@ActiveProfiles("test")
class CartServiceTest {

    @Autowired
    private CartService cartService;

    @MockBean
    private CartRepository cartRepository;

    @MockBean
    private RestTemplate restTemplate;

    @Test
    void findAll_returnsAllCartsFromRepository() {
        Cart cart1 = Cart.builder()
                .cartId(1)
                .userId(10)
                .build();

        Cart cart2 = Cart.builder()
                .cartId(2)
                .userId(20)
                .build();

        UserDto user1 = UserDto.builder().userId(10).build();
        UserDto user2 = UserDto.builder().userId(20).build();

        given(cartRepository.findAll()).willReturn(Arrays.asList(cart1, cart2));
        given(restTemplate.getForObject(anyString(), any(Class.class)))
                .willReturn(user1, user2);

        List<CartDto> result = cartService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(CartDto::getCartId)
                .containsExactlyInAnyOrder(1, 2);

        verify(cartRepository).findAll();
    }

    @Test
    void findById_returnsCartWhenExists() {
        Integer id = 1;
        Cart cart = Cart.builder()
                .cartId(id)
                .userId(10)
                .build();

        UserDto user = UserDto.builder().userId(10).build();

        given(cartRepository.findById(id)).willReturn(Optional.of(cart));
        given(restTemplate.getForObject(anyString(), any(Class.class))).willReturn(user);

        CartDto result = cartService.findById(id);

        assertThat(result.getCartId()).isEqualTo(id);
        assertThat(result.getUserId()).isEqualTo(10);

        verify(cartRepository).findById(id);
    }

    @Test
    void findById_throwsExceptionWhenCartDoesNotExist() {
        Integer id = 99;
        given(cartRepository.findById(id)).willReturn(Optional.empty());

        assertThrows(CartNotFoundException.class, () -> cartService.findById(id));
        verify(cartRepository).findById(id);
    }

    @Test
    void save_persistsAndReturnsMappedCart() {
        CartDto toSave = CartDto.builder()
                .cartId(null)
                .userId(10)
                .build();

        Cart savedEntity = Cart.builder()
                .cartId(1)
                .userId(10)
                .build();

        given(cartRepository.save(any(Cart.class))).willReturn(savedEntity);

        CartDto result = cartService.save(toSave);

        assertThat(result.getCartId()).isEqualTo(1);
        assertThat(result.getUserId()).isEqualTo(10);

        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void update_withDto_updatesAndReturnsMappedCart() {
        CartDto toUpdate = CartDto.builder()
                .cartId(1)
                .userId(20)
                .build();

        Cart updatedEntity = Cart.builder()
                .cartId(1)
                .userId(20)
                .build();

        given(cartRepository.save(any(Cart.class))).willReturn(updatedEntity);

        CartDto result = cartService.update(toUpdate);

        assertThat(result.getCartId()).isEqualTo(1);
        assertThat(result.getUserId()).isEqualTo(20);

        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void update_withId_usesExistingCartAndReturnsMappedCart() {
        Integer id = 1;

        Cart existing = Cart.builder()
                .cartId(id)
                .userId(10)
                .build();

        UserDto user = UserDto.builder().userId(10).build();

        given(cartRepository.findById(id)).willReturn(Optional.of(existing));
        given(restTemplate.getForObject(anyString(), any(Class.class))).willReturn(user);
        given(cartRepository.save(any(Cart.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        CartDto anyDto = CartDto.builder().cartId(id).build();

        CartDto result = cartService.update(id, anyDto);

        assertThat(result.getCartId()).isEqualTo(id);
        assertThat(result.getUserId()).isEqualTo(10);

        verify(cartRepository).findById(id);
        verify(cartRepository).save(any(Cart.class));
    }

    @Test
    void update_withId_throwsExceptionWhenCartDoesNotExist() {
        Integer id = 99;
        given(cartRepository.findById(id)).willReturn(Optional.empty());

        assertThrows(CartNotFoundException.class,
                () -> cartService.update(id, CartDto.builder().build()));

        verify(cartRepository).findById(id);
    }

    @Test
    void deleteById_deletesCart() {
        Integer id = 1;

        cartService.deleteById(id);

        verify(cartRepository).deleteById(id);
    }
}

