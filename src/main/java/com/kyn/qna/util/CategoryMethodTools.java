package com.kyn.qna.util;

import java.lang.reflect.Method;

import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.kyn.qna.dto.CategoryRequest;
import com.kyn.qna.service.CategoryService;

@Service
public class CategoryMethodTools {
    
    
    public Method getCategoryByName() {
        try {
            return CategoryService.class.getMethod("findByName", String.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Method not found", e);
        }
    }

    public Method getAllCategories() {
        try {
            return CategoryService.class.getMethod("findAll");
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Method not found", e);
        }
    }

    public Method updateCategory() {
        try {
            return CategoryService.class.getMethod("update", CategoryRequest.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Method not found", e);
        }
    }
}
