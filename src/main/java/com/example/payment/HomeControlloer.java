package com.example.payment;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class HomeControlloer {

    @GetMapping("/")
    public String welcomePage() {
        return "Hello! This is welcome page!";
    }
}
