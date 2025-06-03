package com.kyn.qna.service;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyn.qna.dto.CategoryRequest;
import com.kyn.qna.dto.CategoryResult;
import com.kyn.qna.entity.Category;
import com.kyn.qna.util.JsonStringUtil;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class CategoryManageService {
    private final CategoryService categoryService;
    private final GeminiService geminiService;
    private final ObjectMapper objectMapper;

    public CategoryManageService(CategoryService categoryService, GeminiService geminiService, ObjectMapper objectMapper) {
        this.categoryService = categoryService;
        this.geminiService = geminiService;
        this.objectMapper = objectMapper;
    }

    public Mono<Category> manageSingleCategory(String name){
        log.info("Managing category: {}", name);
        
        return categoryService.findByName(name)
            .flatMap(category -> 
                geminiService.generateResponse(
                    singleCategoryManagePrompt
                    .replace("{category_name}", category.getName())
                    .replace("{category_description}", category.getDescription()))
                .flatMap(response -> parseAndProcessResponse(response, category))
            )
            .doOnSuccess(result -> log.info("Category management completed for: {}", name))
            .doOnError(error -> log.error("Error managing category: {}", name, error));
    }

    public Flux<Category> manageAllCategories(){
        log.info("Managing all categories");
        
        return categoryService.findAll()
            .collectList()
            .flatMap(categories -> geminiService
                .generateResponse(multiCategoryManagePrompt
                    .replace("{category_data}", categories.toString())))
            .flatMapMany(this::parseAllCategoriesResponse)
            .flatMap(categoryResult ->
                categoryService.findByName(categoryResult.getName())
                .flatMap(category -> {
                    if(categoryResult.isShouldUpdate()){
                        return categoryService.update(CategoryRequest.builder()
                            .name(categoryResult.getName())
                            .description(categoryResult.getDescription())
                            .build());
                    } else {
                        return Mono.just(category);
                    }
                })
            )
            .doOnComplete(() -> log.info("All categories management completed"))
            .doOnError(error -> log.error("Error managing all categories", error));
    }

    private Mono<Category> parseAndProcessResponse(String jsonResponse, Category originalCategory) {
        try {
            //extract Json from Response
            String cleanJson = JsonStringUtil.extractJsonFromResponse(jsonResponse);            
            CategoryResult result = objectMapper.readValue(cleanJson, CategoryResult.class);
            
            if (result.isShouldUpdate()) {                
                return categoryService.update(CategoryRequest.builder()
                    .name(result.getName())
                    .description(result.getDescription())
                    .build());
            } else {
                return Mono.just(originalCategory);
            }
            
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON response: {}", jsonResponse, e);
            return Mono.just(originalCategory);
        }
    }



    private Flux<CategoryResult> parseAllCategoriesResponse(String jsonResponse) {
        try {
            String cleanJson = JsonStringUtil.extractJsonFromResponse(jsonResponse);
            CategoryResult[] results = objectMapper.readValue(cleanJson, CategoryResult[].class);            
            return Flux.fromArray(results);
            
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON response for all categories: {}", jsonResponse, e);
            return Flux.error(new RuntimeException("Failed to parse response from AI: " + e.getMessage()));
        }
    }

    private String singleCategoryManagePrompt = """
                You are a category manager.
                The given data is a category for development technology.
                name and description in the above categories are the technical names and their descriptions, respectively.
                1. The description of the data must be no more than 100 characters and must be briefly described.
                2. Duplicate or ambiguous expressions should be avoided.
                3. if description should be updated, shouldUpdate should be true, otherwise false.
                If the description is appropriate, you do not need to update it.
                
                Please respond with ONLY a valid JSON object in this exact format:
                {
                "shouldUpdate": boolean,
                "name": "category name",
                "description": "category description"
                }
                
                Do not include any markdown formatting or additional text.
                
                =======================
                "name": "{category_name}",
                "description": "{category_description}"
            """;

    private String multiCategoryManagePrompt = """
                You are a category manager.
                The given data is a category for development technology.
                name and description in the above categories are the technical names and their descriptions, respectively.
                1. The description of the data must be no more than 100 characters and must be briefly described.
                2. Duplicate or ambiguous expressions should be avoided.
                3. if description should be updated, shouldUpdate should be true, otherwise false.
                If the description is appropriate, you do not need to update it.
                
                Please respond with ONLY a valid JSON object in this exact format:
                [{
                "shouldUpdate": boolean,
                "name": "category name",
                "description": "category description"
                },...]

                =======================
                {category_data}
            """;
}
