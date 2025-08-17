package com.momnect.productservice.command.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@Tag(name = "Example API", description = " 관련 기능을 제공합니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/example")
public class ExampleController {

}
