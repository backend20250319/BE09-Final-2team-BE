package com.momnect.chatservice.command.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class PageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long total; // 필요 없으면 -1 사용
}
