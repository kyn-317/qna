package com.kyn.qna.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.kyn.qna.dto.CategoryRequest;
import com.kyn.qna.entity.Category;
import com.kyn.qna.service.CategoryManageService;
import com.kyn.qna.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
    
@RestController
@RequiredArgsConstructor
@Slf4j
public class CategoryController {
    
    private final CategoryService categoryService;
    private final CategoryManageService categoryManageService;
    
    @GetMapping("category")
    public Flux<Category> findAll() {
        return categoryService.findAll();
    }

    @GetMapping("category/{name}")
    public Mono<Category> findByName(@PathVariable String name) {
        return categoryService.findByName(name);
    }

    @PutMapping("category")
    public Mono<Category> save(@RequestBody CategoryRequest categoryRequest) {
        return categoryService.save(categoryRequest);
    }

    @PostMapping("category")
    public Mono<Category> update(@RequestBody CategoryRequest categoryRequest) {
        return categoryService.update(categoryRequest);
    }

    @DeleteMapping("category/{id}")
    public Mono<Void> delete(@PathVariable String id) {
        return categoryService.delete(id);
    }

    @PostMapping("category/manage/{name}")
    public Mono<Category> manageCategoryByName(@PathVariable String name) {
        log.info("Managing category by name: {}", name);
        return categoryManageService.manageSingleCategory(name);
    }

    @PostMapping("category/manage")
    public Flux<Category> manageCategory() {
        log.info("Managing all categories");    
        return categoryManageService.manageAllCategories();
    }
}
