package com.cameroun.jobscraper.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/users")
@Tag(name = "User", description = "Gestion des utilisateurs. à implémenter")
public class UserController {

}
