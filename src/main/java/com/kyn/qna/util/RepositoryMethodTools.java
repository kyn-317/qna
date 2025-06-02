package com.kyn.qna.util;

import java.lang.reflect.Method;

import org.springframework.stereotype.Component;

import com.kyn.qna.service.QuestionService;

@Component
public class RepositoryMethodTools {   
    
    public Method findByCategory(String category) {
        try {
            return QuestionService.class.getMethod("findByCategory", String.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Method not found", e);
        }
    }

    public Method findByExpYears(int expYears) {
        try {
            return QuestionService.class.getMethod("findByExpYears", int.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Method not found", e);
        }
    }

    public Method findByCategoryAndExpYears(String category, int expYears) {
        try {
            return QuestionService.class.getMethod("findByCategoryAndExpYears", String.class, int.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Method not found", e);
        }
    }
}
