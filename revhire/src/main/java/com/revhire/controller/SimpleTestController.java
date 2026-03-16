package com.revhire.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SimpleTestController {
    
    @GetMapping("/ping")
    public String ping() {
        return "Application is running!";
    }
}