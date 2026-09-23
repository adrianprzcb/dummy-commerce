package com.adrianperezcobo.dummycommerce.store.category.application.service;

import com.adrianperezcobo.dummycommerce.store.category.application.command.CreateCategoryCommand;
import com.adrianperezcobo.dummycommerce.store.category.application.exception.CategoryNotFoundException;
import com.adrianperezcobo.dummycommerce.store.category.application.port.out.CategoryRepository;
import com.adrianperezcobo.dummycommerce.store.category.domain.Category;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void shouldCreateCategory() {
        CreateCategoryCommand command =
                new CreateCategoryCommand("Electronics");

        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Category result = categoryService.create(command);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getName()).isEqualTo("Electronics");

        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void shouldReturnCategoryById() {
        UUID id = UUID.randomUUID();

        Category category = new Category(
                id,
                "Electronics"
        );

        when(categoryRepository.findById(id))
                .thenReturn(Optional.of(category));

        Category result = categoryService.getById(id);

        assertThat(result).isSameAs(category);
    }

    @Test
    void shouldThrowWhenCategoryDoesNotExist() {
        UUID id = UUID.randomUUID();

        when(categoryRepository.findById(id))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getById(id))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void shouldReturnAllCategories() {
        List<Category> categories = List.of(
                new Category(UUID.randomUUID(), "Electronics"),
                new Category(UUID.randomUUID(), "Gaming")
        );

        when(categoryRepository.findAll())
                .thenReturn(categories);

        assertThat(categoryService.getAll())
                .containsExactlyElementsOf(categories);
    }
}