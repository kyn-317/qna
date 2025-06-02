package com.kyn.qna.service;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kyn.qna.dto.CategoryRequest;
import com.kyn.qna.dto.CategoryResult;
import com.kyn.qna.entity.Category;

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
            .flatMap(category -> {
                String prompt = singleCategoryManagePrompt.replace("category_name", category.getName())
                    .replace("category_description", category.getDescription());
                
                return geminiService.generateResponse(prompt)
                    .flatMap(response -> parseAndProcessResponse(response, category));
            })
            .doOnSuccess(result -> log.info("Category management completed for: {}", name))
            .doOnError(error -> log.error("Error managing category: {}", name, error));
    }

    public Flux<Category> manageAllCategories(){
        log.info("Managing all categories");
        
        return categoryService.findAll()
            .collectList()
            .flatMap(categories -> {      
                String categoriesData = categories.toString();
                String prompt = multiCategoryManagePrompt.replace("allCategoryData__", categoriesData);
                log.debug("Generated prompt for all categories: {}", prompt);
            
                return geminiService.generateResponse(prompt);
            })
            .flatMapMany(this::parseAllCategoriesResponse)
            .flatMap(categoryResult ->
                categoryService.findByName(categoryResult.getName())
                .flatMap(category -> {
                    if(categoryResult.isShouldUpdate()){
                        CategoryRequest updateRequest = new CategoryRequest();
                        updateRequest.setName(categoryResult.getName());
                        updateRequest.setDescription(categoryResult.getDescription());
                        return categoryService.update(updateRequest);
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
            String cleanJson = extractJsonFromResponse(jsonResponse);
            log.debug("Parsing JSON response: {}", cleanJson);
            
            CategoryResult result = objectMapper.readValue(cleanJson, CategoryResult.class);
            log.info("Parsed result - shouldUpdate: {}, name: {}, description: {}", 
                    result.isShouldUpdate(), result.getName(), result.getDescription());
            
            if (result.isShouldUpdate()) {
                log.info("Updating category '{}' with new description: {}", result.getName(), result.getDescription());
                
                CategoryRequest updateRequest = new CategoryRequest();
                updateRequest.setName(result.getName());
                updateRequest.setDescription(result.getDescription());
                
                return categoryService.update(updateRequest);
            } else {
                return Mono.just(originalCategory);
            }
            
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON response: {}", jsonResponse, e);
            return Mono.just(originalCategory);
        }
    }

    private String extractJsonFromResponse(String response) {

        String cleaned = response.trim();
        
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7); 
        }
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3); 
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3); 
        }
        
        return cleaned.trim();
    }

    private Flux<CategoryResult> parseAllCategoriesResponse(String jsonResponse) {
        try {
            String cleanJson = extractJsonFromResponse(jsonResponse);
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
                "name": "category_name",
                "description": "category_description"
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
                allCategoryData__
            """;
}
