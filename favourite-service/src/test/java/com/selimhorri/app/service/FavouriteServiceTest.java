package com.selimhorri.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.domain.Favourite;
import com.selimhorri.app.domain.id.FavouriteId;
import com.selimhorri.app.dto.FavouriteDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.wrapper.FavouriteNotFoundException;
import com.selimhorri.app.repository.FavouriteRepository;

@SpringBootTest
@ActiveProfiles("test")
class FavouriteServiceTest {

    @Autowired
    private FavouriteService favouriteService;

    @MockBean
    private FavouriteRepository favouriteRepository;

    @MockBean
    private RestTemplate restTemplate;

    @Test
    void findAll_returnsAllFavouritesWithUserAndProduct() {
        Favourite fav1 = buildFavourite(1, 10, LocalDateTime.now());
        Favourite fav2 = buildFavourite(2, 20, LocalDateTime.now().plusMinutes(1));

        given(favouriteRepository.findAll()).willReturn(Arrays.asList(fav1, fav2));

        given(restTemplate.getForObject(org.mockito.ArgumentMatchers.contains("/USER-SERVICE/"),
                org.mockito.ArgumentMatchers.eq(UserDto.class)))
                .willReturn(UserDto.builder().userId(1).build());
        given(restTemplate.getForObject(org.mockito.ArgumentMatchers.contains("/PRODUCT-SERVICE/"),
                org.mockito.ArgumentMatchers.eq(ProductDto.class)))
                .willReturn(ProductDto.builder().productId(10).build());

        List<FavouriteDto> result = favouriteService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUserDto()).isNotNull();
        assertThat(result.get(0).getProductDto()).isNotNull();

        verify(favouriteRepository).findAll();
    }

    @Test
    void findById_returnsFavouriteWithUserAndProduct() {
        FavouriteId id = buildFavouriteId(1, 10, LocalDateTime.now());
        Favourite fav = buildFavourite(1, 10, id.getLikeDate());

        given(favouriteRepository.findById(id)).willReturn(Optional.of(fav));
        given(restTemplate.getForObject(org.mockito.ArgumentMatchers.contains("/USER-SERVICE/"),
                org.mockito.ArgumentMatchers.eq(UserDto.class)))
                .willReturn(UserDto.builder().userId(1).build());
        given(restTemplate.getForObject(org.mockito.ArgumentMatchers.contains("/PRODUCT-SERVICE/"),
                org.mockito.ArgumentMatchers.eq(ProductDto.class)))
                .willReturn(ProductDto.builder().productId(10).build());

        FavouriteDto result = favouriteService.findById(id);

        assertThat(result.getUserId()).isEqualTo(1);
        assertThat(result.getProductId()).isEqualTo(10);
        assertThat(result.getUserDto()).isNotNull();
        assertThat(result.getProductDto()).isNotNull();

        verify(favouriteRepository).findById(id);
    }

    @Test
    void findById_throwsExceptionWhenNotFound() {
        FavouriteId id = buildFavouriteId(99, 999, LocalDateTime.now());
        given(favouriteRepository.findById(id)).willReturn(Optional.empty());

        assertThrows(FavouriteNotFoundException.class, () -> favouriteService.findById(id));
        verify(favouriteRepository).findById(id);
    }

    @Test
    void save_persistsAndReturnsMappedFavourite() {
        FavouriteDto toSave = FavouriteDto.builder()
                .userId(1)
                .productId(10)
                .likeDate(LocalDateTime.now())
                .build();

        Favourite savedEntity = Favourite.builder()
                .userId(1)
                .productId(10)
                .likeDate(toSave.getLikeDate())
                .build();

        given(favouriteRepository.save(org.mockito.ArgumentMatchers.any(Favourite.class)))
                .willReturn(savedEntity);

        FavouriteDto result = favouriteService.save(toSave);

        assertThat(result.getUserId()).isEqualTo(1);
        assertThat(result.getProductId()).isEqualTo(10);

        verify(favouriteRepository).save(org.mockito.ArgumentMatchers.any(Favourite.class));
    }

    @Test
    void update_updatesAndReturnsMappedFavourite() {
        FavouriteDto toUpdate = FavouriteDto.builder()
                .userId(1)
                .productId(10)
                .likeDate(LocalDateTime.now())
                .build();

        Favourite updatedEntity = Favourite.builder()
                .userId(1)
                .productId(10)
                .likeDate(toUpdate.getLikeDate())
                .build();

        given(favouriteRepository.save(org.mockito.ArgumentMatchers.any(Favourite.class)))
                .willReturn(updatedEntity);

        FavouriteDto result = favouriteService.update(toUpdate);

        assertThat(result.getUserId()).isEqualTo(1);
        assertThat(result.getProductId()).isEqualTo(10);

        verify(favouriteRepository).save(org.mockito.ArgumentMatchers.any(Favourite.class));
    }

    @Test
    void deleteById_deletesFavourite() {
        FavouriteId id = buildFavouriteId(1, 10, LocalDateTime.now());

        favouriteService.deleteById(id);

        verify(favouriteRepository).deleteById(id);
    }

    private Favourite buildFavourite(Integer userId, Integer productId, LocalDateTime likeDate) {
        return Favourite.builder()
                .userId(userId)
                .productId(productId)
                .likeDate(likeDate)
                .build();
    }

    private FavouriteId buildFavouriteId(Integer userId, Integer productId, LocalDateTime likeDate) {
        return new FavouriteId(userId, productId, likeDate);
    }
}


