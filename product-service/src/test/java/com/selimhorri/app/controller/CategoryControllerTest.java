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
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.exception.ApiExceptionHandler;
import com.selimhorri.app.exception.wrapper.CategoryNotFoundException;
import com.selimhorri.app.service.CategoryService;

@WebMvcTest(controllers = com.selimhorri.app.resource.CategoryResource.class)
@Import(ApiExceptionHandler.class)
@ActiveProfiles("test")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    @Test
    void getAllCategories_returnsCollectionResponse() throws Exception {
        CategoryDto c1 = CategoryDto.builder().categoryId(1).categoryTitle("C1").build();
        CategoryDto c2 = CategoryDto.builder().categoryId(2).categoryTitle("C2").build();
        List<CategoryDto> list = Arrays.asList(c1, c2);

        given(categoryService.findAll()).willReturn(list);

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.collection.length()").value(2))
                .andExpect(jsonPath("$.collection[0].categoryId").value(1))
                .andExpect(jsonPath("$.collection[1].categoryId").value(2));
    }

    @Test
    void getCategoryById_returnsCategory() throws Exception {
        CategoryDto category = CategoryDto.builder()
                .categoryId(1)
                .categoryTitle("C1")
                .build();

        given(categoryService.findById(1)).willReturn(category);

        mockMvc.perform(get("/api/categories/{id}", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.categoryTitle").value("C1"));
    }

    @Test
    void getCategoryById_whenNotFound_returnsBadRequestWithErrorBody() throws Exception {
        given(categoryService.findById(99)).willThrow(new CategoryNotFoundException("Category with id: 99 not found"));

        mockMvc.perform(get("/api/categories/{id}", "99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.httpStatus").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.msg").exists());
    }

    @Test
    void saveCategory_returnsSavedCategory() throws Exception {
        CategoryDto request = CategoryDto.builder()
                .categoryTitle("New Category")
                .build();

        CategoryDto response = CategoryDto.builder()
                .categoryId(1)
                .categoryTitle("New Category")
                .build();

        given(categoryService.save(request)).willReturn(response);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.categoryTitle").value("New Category"));
    }

    @Test
    void updateCategory_returnsUpdatedCategory() throws Exception {
        CategoryDto request = CategoryDto.builder()
                .categoryId(1)
                .categoryTitle("Updated")
                .build();

        CategoryDto response = CategoryDto.builder()
                .categoryId(1)
                .categoryTitle("Updated")
                .build();

        given(categoryService.update(request)).willReturn(response);

        mockMvc.perform(put("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.categoryTitle").value("Updated"));
    }

    @Test
    void updateCategoryWithPathId_returnsUpdatedCategory() throws Exception {
        CategoryDto request = CategoryDto.builder()
                .categoryTitle("Updated")
                .build();

        CategoryDto response = CategoryDto.builder()
                .categoryId(1)
                .categoryTitle("Updated")
                .build();

        given(categoryService.update(1, request)).willReturn(response);

        mockMvc.perform(put("/api/categories/{id}", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.categoryTitle").value("Updated"));
    }

    @Test
    void deleteCategory_returnsTrue() throws Exception {
        doNothing().when(categoryService).deleteById(1);

        mockMvc.perform(delete("/api/categories/{id}", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        verify(categoryService).deleteById(1);
    }
}


