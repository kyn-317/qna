package com.kyn.qna.gemini;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@RestController
public class GenerateController {
    
    @Autowired
    private GenerateTextFromTextInput generateTextFromTextInput;
    @GetMapping("generate/{inputText}")
    public String getMethodName(@PathVariable String inputText) {
        generateTextFromTextInput.generateTextFromTextInput(inputText);
        return "success";
    }
    
}
