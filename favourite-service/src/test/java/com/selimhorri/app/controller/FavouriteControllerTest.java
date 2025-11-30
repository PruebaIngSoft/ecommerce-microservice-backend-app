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
import java.time.format.DateTimeFormatter;
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
import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.id.FavouriteId;
import com.selimhorri.app.dto.FavouriteDto;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.dto.response.collection.DtoCollectionResponse;
import com.selimhorri.app.exception.ApiExceptionHandler;
import com.selimhorri.app.exception.wrapper.FavouriteNotFoundException;
import com.selimhorri.app.service.FavouriteService;

@WebMvcTest(controllers = com.selimhorri.app.resource.FavouriteResource.class)
@Import(ApiExceptionHandler.class)
@ActiveProfiles("test")
class FavouriteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FavouriteService favouriteService;

    @Test
    void getAllFavourites_returnsCollectionResponse() throws Exception {
        FavouriteDto f1 = FavouriteDto.builder()
                .userId(1)
                .productId(10)
                .likeDate(LocalDateTime.now())
                .build();
        FavouriteDto f2 = FavouriteDto.builder()
                .userId(2)
                .productId(20)
                .likeDate(LocalDateTime.now().plusMinutes(1))
                .build();
        List<FavouriteDto> list = Arrays.asList(f1, f2);

        given(favouriteService.findAll()).willReturn(list);

        mockMvc.perform(get("/api/favourites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection.length()").value(2));
    }

    @Test
    void getFavouriteByPathId_returnsFavourite() throws Exception {
        // Truncar a microsegundos para que coincida con el formato
        LocalDateTime likeDate = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        String likeDateStr = likeDate.format(DateTimeFormatter.ofPattern(AppConstant.LOCAL_DATE_TIME_FORMAT));

        FavouriteDto favourite = FavouriteDto.builder()
                .userId(1)
                .productId(10)
                .likeDate(likeDate)
                .userDto(UserDto.builder().userId(1).build())
                .productDto(ProductDto.builder().productId(10).build())
                .build();

        // Usar any() porque el controller crea un nuevo FavouriteId al parsear
        given(favouriteService.findById(any(FavouriteId.class))).willReturn(favourite);

        mockMvc.perform(get("/api/favourites/{userId}/{productId}/{likeDate}",
                        "1", "10", likeDateStr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.productId").value(10));
    }

    @Test
    void getFavouriteByPathId_whenNotFound_returnsBadRequest() throws Exception {
        // Truncar a microsegundos para que coincida con el formato
        LocalDateTime likeDate = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        String likeDateStr = likeDate.format(DateTimeFormatter.ofPattern(AppConstant.LOCAL_DATE_TIME_FORMAT));

        // Usar any() porque el controller crea un nuevo FavouriteId al parsear
        given(favouriteService.findById(any(FavouriteId.class)))
                .willThrow(new FavouriteNotFoundException("Favourite not found!"));

        mockMvc.perform(get("/api/favourites/{userId}/{productId}/{likeDate}",
                        "1", "10", likeDateStr))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.httpStatus").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.msg").exists());
    }

    @Test
    void findByIdWithBody_returnsFavourite() throws Exception {
        // Truncar a microsegundos
        LocalDateTime likeDate = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        FavouriteId id = new FavouriteId(1, 10, likeDate);

        FavouriteDto favourite = FavouriteDto.builder()
                .userId(1)
                .productId(10)
                .likeDate(likeDate)
                .build();

        given(favouriteService.findById(id)).willReturn(favourite);

        mockMvc.perform(get("/api/favourites/find")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.productId").value(10));
    }

    @Test
    void saveFavourite_returnsSavedFavourite() throws Exception {
        // Truncar a microsegundos
        LocalDateTime likeDate = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);

        FavouriteDto request = FavouriteDto.builder()
                .userId(1)
                .productId(10)
                .likeDate(likeDate)
                .build();

        FavouriteDto response = FavouriteDto.builder()
                .userId(1)
                .productId(10)
                .likeDate(likeDate)
                .build();

        given(favouriteService.save(any(FavouriteDto.class))).willReturn(response);

        mockMvc.perform(post("/api/favourites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.productId").value(10));
    }

    @Test
    void updateFavourite_returnsUpdatedFavourite() throws Exception {
        // Truncar a microsegundos
        LocalDateTime likeDate = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);

        FavouriteDto request = FavouriteDto.builder()
                .userId(1)
                .productId(10)
                .likeDate(likeDate)
                .build();

        FavouriteDto response = FavouriteDto.builder()
                .userId(1)
                .productId(10)
                .likeDate(likeDate)
                .build();

        given(favouriteService.update(any(FavouriteDto.class))).willReturn(response);

        mockMvc.perform(put("/api/favourites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.productId").value(10));
    }

    @Test
    void deleteByPathId_returnsTrue() throws Exception {
        // Truncar a microsegundos para que coincida con el formato
        LocalDateTime likeDate = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        String likeDateStr = likeDate.format(DateTimeFormatter.ofPattern(AppConstant.LOCAL_DATE_TIME_FORMAT));

        // Usar any() porque el controller crea un nuevo FavouriteId al parsear
        doNothing().when(favouriteService).deleteById(any(FavouriteId.class));

        mockMvc.perform(delete("/api/favourites/{userId}/{productId}/{likeDate}",
                        "1", "10", likeDateStr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        verify(favouriteService).deleteById(any(FavouriteId.class));
    }

    @Test
    void deleteByBody_returnsTrue() throws Exception {
        // Truncar a microsegundos
        LocalDateTime likeDate = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        FavouriteId id = new FavouriteId(1, 10, likeDate);

        doNothing().when(favouriteService).deleteById(any(FavouriteId.class));

        mockMvc.perform(delete("/api/favourites/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        verify(favouriteService).deleteById(any(FavouriteId.class));
    }
}


