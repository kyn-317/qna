package com.kyn.qna.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.kyn.qna.dto.CategoryRequest;
import com.kyn.qna.entity.Category;
import com.kyn.qna.mapper.EntityDtoMapper;
import com.kyn.qna.repository.CategoryRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class CategoryService {
    
    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }
    

    public Flux<Category> findAll() {
        return categoryRepository.findAll();
    }

    public Mono<Category> findByName(String name) {
        return categoryRepository.findByName(name);
    }

    public Mono<Category> save(CategoryRequest categoryRequest) {
        var category = EntityDtoMapper.toCategory(categoryRequest);
        return categoryRepository.findByName(category.getName())
            .switchIfEmpty(categoryRepository.save(category));
    }

    public Mono<Void> delete(String id) {
        return categoryRepository.deleteById(id);
    }

    public Mono<Category> update(CategoryRequest categoryRequest) {
        var category = EntityDtoMapper.toCategory(categoryRequest);
        return categoryRepository.findByName(category.getName())
            .flatMap(existingCategory -> {
                existingCategory.setDescription(category.getDescription());
                existingCategory.setUpdatedAt(LocalDateTime.now());
                return categoryRepository.save(existingCategory);
            });
    }
    
}
