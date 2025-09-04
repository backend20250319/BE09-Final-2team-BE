package com.momnect.productservice.command.controller;

import com.momnect.productservice.command.dto.area.AreaDto;
import com.momnect.productservice.command.service.AreaService;
import com.momnect.productservice.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/areas")
public class AreaController {

    private final AreaService areaService;

    /**
     * 지역 정보 리스트 조회
     * */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AreaDto>>> getAreasByIds(
            @RequestParam("areaIds") List<Long> areaIds
    ) {

        List<AreaDto> areas = areaService.getAreasByIds(areaIds);
        return ResponseEntity.ok(ApiResponse.success(areas));
    }


    /***
     * 읍면동 검색
     *
     * @param emd 검색할 읍면동 이름
     * @return 검색된 읍면동 정보가 담긴 AreaDto 리스트
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<AreaDto>>> searchByEMD(@RequestParam String emd) {
        List<AreaDto> areas = areaService.searchByEMD(emd);
        return ResponseEntity.ok(ApiResponse.success(areas));
    }

    /**
     * API 사용 금지 (내부용)
     * resources에 있는 엑셀 파일로 지역 테이블 초기화
     */
    @GetMapping("/load")
    public ResponseEntity<String> loadAreas() {
        try {
            // resources/regions.xlsx 파일 읽기
            ClassPathResource resource = new ClassPathResource("area.xlsx");
            try (InputStream is = resource.getInputStream()) {
                areaService.loadFromExcel(is, 1L); // 1L: 시스템 유저 예시
            }
            return ResponseEntity.ok("엑셀 데이터 로딩 완료!");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body("엑셀 로딩 중 오류 발생: " + e.getMessage());
        }
    }
}
