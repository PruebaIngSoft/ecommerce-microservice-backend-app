package com.selimhorri.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import com.selimhorri.app.domain.Category;
import com.selimhorri.app.domain.Product;
import com.selimhorri.app.dto.ProductDto;
import com.selimhorri.app.dto.CategoryDto;
import com.selimhorri.app.exception.wrapper.ProductNotFoundException;
import com.selimhorri.app.repository.ProductRepository;

@SpringBootTest
@ActiveProfiles("test")
class ProductServiceTest {

    @Autowired
    private ProductService productService;

    @MockBean
    private ProductRepository productRepository;

    @Test
    void findAll_returnsAllProductsFromRepository() {
        Category category = Category.builder()
                .categoryId(10)
                .categoryTitle("Category 1")
                .imageUrl("cat.png")
                .build();

        Product product1 = Product.builder()
                .productId(1)
                .productTitle("Product 1")
                .category(category)
                .build();

        Product product2 = Product.builder()
                .productId(2)
                .productTitle("Product 2")
                .category(category)
                .build();

        given(productRepository.findAll()).willReturn(Arrays.asList(product1, product2));

        List<ProductDto> result = productService.findAll();

        assertThat(result).hasSize(2);
        assertThat(result)
                .extracting(ProductDto::getProductId)
                .containsExactlyInAnyOrder(1, 2);

        verify(productRepository).findAll();
    }

    @Test
    void findById_returnsProductWhenExists() {
        Integer id = 1;
        Category category = Category.builder()
                .categoryId(10)
                .categoryTitle("Category 1")
                .imageUrl("cat.png")
                .build();

        Product product = Product.builder()
                .productId(id)
                .productTitle("Product 1")
                .category(category)
                .build();

        given(productRepository.findById(id)).willReturn(Optional.of(product));

        ProductDto result = productService.findById(id);

        assertThat(result.getProductId()).isEqualTo(id);
        assertThat(result.getProductTitle()).isEqualTo("Product 1");

        verify(productRepository).findById(id);
    }

    @Test
    void findById_throwsExceptionWhenProductDoesNotExist() {
        Integer id = 99;
        given(productRepository.findById(id)).willReturn(Optional.empty());
    
        assertThrows(ProductNotFoundException.class, () -> productService.findById(id));
        verify(productRepository).findById(id);
    }

    @Test
    void save_persistsAndReturnsMappedProduct() {
        CategoryDto categoryDto = CategoryDto.builder()
                .categoryId(10)
                .categoryTitle("Category 1")
                .imageUrl("cat.png")
                .build();

        ProductDto toSave = ProductDto.builder()
                .productId(null)
                .productTitle("New Product")
                .imageUrl("img.png")
                .sku("SKU-1")
                .priceUnit(100.0)
                .quantity(5)
                .categoryDto(categoryDto)
                .build();

        Category category = Category.builder()
                .categoryId(10)
                .categoryTitle("Category 1")
                .imageUrl("cat.png")
                .build();

        Product savedEntity = Product.builder()
                .productId(1)
                .productTitle("New Product")
                .imageUrl("img.png")
                .sku("SKU-1")
                .priceUnit(100.0)
                .quantity(5)
                .category(category)
                .build();

        given(productRepository.save(org.mockito.ArgumentMatchers.any(Product.class)))
                .willReturn(savedEntity);

        ProductDto result = productService.save(toSave);

        assertThat(result.getProductId()).isEqualTo(1);
        assertThat(result.getProductTitle()).isEqualTo("New Product");
        assertThat(result.getCategoryDto().getCategoryId()).isEqualTo(10);

        verify(productRepository).save(org.mockito.ArgumentMatchers.any(Product.class));
    }

    @Test
    void update_withDto_updatesAndReturnsMappedProduct() {
        CategoryDto categoryDto = CategoryDto.builder()
                .categoryId(10)
                .categoryTitle("Category 1")
                .imageUrl("cat.png")
                .build();

        ProductDto toUpdate = ProductDto.builder()
                .productId(1)
                .productTitle("Updated Product")
                .imageUrl("img2.png")
                .sku("SKU-1")
                .priceUnit(150.0)
                .quantity(10)
                .categoryDto(categoryDto)
                .build();

        Category category = Category.builder()
                .categoryId(10)
                .categoryTitle("Category 1")
                .imageUrl("cat.png")
                .build();

        Product updatedEntity = Product.builder()
                .productId(1)
                .productTitle("Updated Product")
                .imageUrl("img2.png")
                .sku("SKU-1")
                .priceUnit(150.0)
                .quantity(10)
                .category(category)
                .build();

        given(productRepository.save(org.mockito.ArgumentMatchers.any(Product.class)))
                .willReturn(updatedEntity);

        ProductDto result = productService.update(toUpdate);

        assertThat(result.getProductId()).isEqualTo(1);
        assertThat(result.getProductTitle()).isEqualTo("Updated Product");
        assertThat(result.getPriceUnit()).isEqualTo(150.0);

        verify(productRepository).save(org.mockito.ArgumentMatchers.any(Product.class));
    }

    @Test
    void update_withId_usesExistingProductAndReturnsMappedProduct() {
        Integer id = 1;

        Category category = Category.builder()
                .categoryId(10)
                .categoryTitle("Category 1")
                .imageUrl("cat.png")
                .build();

        Product existing = Product.builder()
                .productId(id)
                .productTitle("Existing Product")
                .imageUrl("img.png")
                .sku("SKU-1")
                .priceUnit(50.0)
                .quantity(3)
                .category(category)
                .build();

        given(productRepository.findById(id)).willReturn(Optional.of(existing));
        given(productRepository.save(org.mockito.ArgumentMatchers.any(Product.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        ProductDto anyDto = ProductDto.builder().productId(id).build();

        ProductDto result = productService.update(id, anyDto);

        assertThat(result.getProductId()).isEqualTo(id);
        assertThat(result.getProductTitle()).isEqualTo("Existing Product");

        verify(productRepository).findById(id);
        verify(productRepository).save(org.mockito.ArgumentMatchers.any(Product.class));
    }

    @Test
    void update_withId_throwsExceptionWhenProductDoesNotExist() {
        Integer id = 99;
        given(productRepository.findById(id)).willReturn(Optional.empty());
    
        assertThrows(ProductNotFoundException.class,
                () -> productService.update(id, ProductDto.builder().build()));
    
        verify(productRepository).findById(id);
    }

    @Test
    void deleteById_deletesExistingProduct() {
        Integer id = 1;

        Category category = Category.builder()
                .categoryId(10)
                .categoryTitle("Category 1")
                .imageUrl("cat.png")
                .build();

        Product existing = Product.builder()
                .productId(id)
                .productTitle("To Delete")
                .imageUrl("img.png")
                .sku("SKU-1")
                .priceUnit(10.0)
                .quantity(1)
                .category(category)
                .build();

        given(productRepository.findById(id)).willReturn(Optional.of(existing));

        productService.deleteById(id);

        verify(productRepository).findById(id);
        verify(productRepository).delete(org.mockito.ArgumentMatchers.any(Product.class));
    }

    @Test
    void deleteById_throwsExceptionWhenProductDoesNotExist() {
        Integer id = 99;
        given(productRepository.findById(id)).willReturn(Optional.empty());
    
        assertThrows(ProductNotFoundException.class, () -> productService.deleteById(id));
        verify(productRepository).findById(id);
    }
}
