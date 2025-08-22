package com.momnect.fileservice.command.controller;

import com.momnect.fileservice.common.ApiResponse;
import com.momnect.fileservice.command.service.ImageFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/files")
public class ImageFileController {

    private final ImageFileService imageFileService;
}
