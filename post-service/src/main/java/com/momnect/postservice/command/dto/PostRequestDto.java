package com.momnect.postservice.command.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PostRequestDto {
    private Long userId;
    private String title;
    private String contentHtml;
    private Boolean hasImage;
    private String categoryName;
    private List<String> imageUrls;
}
