package com.selimhorri.app.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.selimhorri.app.domain.Favourite;
import com.selimhorri.app.dto.FavouriteDto;
import com.selimhorri.app.repository.FavouriteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FavouriteServiceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FavouriteRepository favouriteRepository;

    @BeforeEach
    void cleanDatabase() {
        favouriteRepository.deleteAll();
    }

    @Test
    void shouldAddFavouriteAndPersist() throws Exception {
        LocalDateTime likeDate = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        FavouriteDto request = FavouriteDto.builder()
                .userId(101)
                .productId(202)
                .likeDate(likeDate)
                .build();

        mockMvc.perform(post("/api/favourites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(request.getUserId()))
                .andExpect(jsonPath("$.productId").value(request.getProductId()));

        assertThat(favouriteRepository.count()).isEqualTo(1);
        Favourite persisted = favouriteRepository.findAll().get(0);
        assertThat(persisted.getUserId()).isEqualTo(request.getUserId());
        assertThat(persisted.getProductId()).isEqualTo(request.getProductId());
        assertThat(persisted.getLikeDate()).isEqualTo(likeDate);
    }
}

