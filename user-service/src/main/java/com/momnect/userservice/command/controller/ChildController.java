// controller/ChildController.java
package com.momnect.userservice.command.controller;

import com.momnect.userservice.command.dto.child.ChildDTO;
import com.momnect.userservice.command.dto.child.CreateChildRequest;
import com.momnect.userservice.command.dto.child.UpdateChildRequest;
import com.momnect.userservice.command.service.ChildService;
import com.momnect.userservice.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@Tag(name = "Child API", description = "자녀 관리 기능을 제공합니다.")
@RestController
@RequiredArgsConstructor
@RequestMapping("/users/children")
public class ChildController {

    private final ChildService childService;

    /**
     * 자녀 등록
     */
    @Operation(summary = "자녀 등록", description = "새로운 자녀 정보를 등록합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<ChildDTO>> createChild(
            @Valid @RequestBody CreateChildRequest request,
            HttpServletRequest httpRequest) {

        Long userId = getUserIdFromRequest(httpRequest);
        ChildDTO child = childService.createChild(userId, request);
        return ResponseEntity.ok(ApiResponse.success(child));
    }

    /**
     * 자녀 목록 조회
     */
    @Operation(summary = "자녀 목록 조회", description = "등록된 자녀 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ChildDTO>>> getChildren(HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        List<ChildDTO> children = childService.getChildren(userId);
        return ResponseEntity.ok(ApiResponse.success(children));
    }

    /**
     * 자녀 정보 수정
     */
    @Operation(summary = "자녀 정보 수정", description = "등록된 자녀 정보를 수정합니다.")
    @PutMapping("/{childId}")
    public ResponseEntity<ApiResponse<ChildDTO>> updateChild(
            @PathVariable Long childId,
            @Valid @RequestBody UpdateChildRequest request,
            HttpServletRequest httpRequest) {

        Long userId = getUserIdFromRequest(httpRequest);
        ChildDTO child = childService.updateChild(userId, childId, request);
        return ResponseEntity.ok(ApiResponse.success(child));
    }

    /**
     * 자녀 삭제
     */
    @Operation(summary = "자녀 정보 삭제", description = "등록된 자녀 정보를 삭제합니다.")
    @DeleteMapping("/{childId}")
    public ResponseEntity<ApiResponse<Void>> deleteChild(
            @PathVariable Long childId,
            HttpServletRequest httpRequest) {

        Long userId = getUserIdFromRequest(httpRequest);
        childService.deleteChild(userId, childId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 요청에서 사용자 ID 추출
     */
    private Long getUserIdFromRequest(HttpServletRequest request) {
        String userIdHeader = request.getHeader("X-User-Id");
        if (userIdHeader != null) {
            return Long.parseLong(userIdHeader);
        }

        Object userIdAttr = request.getAttribute("X-User-Id");
        if (userIdAttr != null) {
            return Long.parseLong(userIdAttr.toString());
        }

        throw new RuntimeException("사용자 인증 정보를 찾을 수 없습니다.");
    }
}