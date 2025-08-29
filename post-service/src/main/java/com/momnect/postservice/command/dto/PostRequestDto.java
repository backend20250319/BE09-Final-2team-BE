package com.momnect.postservice.command.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PostRequestDto {

    @NotNull
    private Long userId;

    @NotBlank
    private String title;

    @NotBlank
    private String contentHtml;

    @NotBlank
    private String categoryName;

    private Boolean hasImage;
}
