package com.momnect.productservice.command.service;

import com.momnect.productservice.command.dto.area.AreaDto;
import com.momnect.productservice.command.entity.area.Area;
import com.momnect.productservice.command.entity.area.AreaLevel;
import com.momnect.productservice.command.repository.AreaRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AreaService {

    private final AreaRepository areaRepository;

    /**
     * 지역 정보 리스트 조회
     */
    public List<AreaDto> getAreasByIds(List<Long> areaIds) {
        if (areaIds == null || areaIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Repository는 Integer 기반이므로 변환
        List<Integer> ids = areaIds.stream()
                .map(Long::intValue)
                .toList();

        List<Area> areas = areaRepository.findByIdIn(ids);

        List<AreaDto> result = new ArrayList<>();
        for (Area area : areas) {
            String fullName = buildFullName(area);
            result.add(AreaDto.builder()
                    .id(area.getId())
                    .emd(area.getName())
                    .fullName(fullName)
                    .build());
        }

        return result;
    }

    /**
     * 읍면동 이름으로 검색 후 해당 Area 정보를 AreaDto 리스트로 반환
     *
     * @param emd 검색할 읍면동 이름
     * @return 검색된 읍면동 정보가 담긴 AreaDto 리스트
     */
    public List<AreaDto> searchByEMD(String emd) {
        List<Area> emdAreas = areaRepository.findByNameContainingAndLevel(emd, AreaLevel.EMD);
        List<AreaDto> result = new ArrayList<>();

        for (Area area : emdAreas) {
            String fullName = buildFullName(area);
            result.add(AreaDto.builder()
                    .id(area.getId())
                    .emd(area.getName())
                    .fullName(fullName)
                    .build());
        }

        return result;
    }

    /***
     * 지역 풀네임 구하기
     * @param area
     * @return
     */
    private String buildFullName(Area area) {
        List<String> parts = new ArrayList<>();
        Area current = area;

        while (current != null) {
            parts.add(0, current.getName()); // 부모부터 순서대로
            current = current.getParent();
        }

        return String.join(" ", parts);
    }

    /**
     * 엑셀(InputStream) 읽어서 Area에 저장
     * 엑셀 컬럼은 (예) 법정동코드, 시도명, 시군구명, 읍면동명, 동리명 ... 순서라고 가정
     */
    @Transactional
    public void loadFromExcel(InputStream excelInputStream, Long systemUserId) throws Exception {
        Workbook wb = WorkbookFactory.create(excelInputStream);
        Sheet sheet = wb.getSheetAt(0);

        // 1) 모든 행을 읽어 DTO 리스트 생성 (헤더 스킵)
        List<Row> rows = new ArrayList<>();
        Iterator<Row> it = sheet.rowIterator();
        if (it.hasNext()) it.next(); // 헤더 한 줄 스킵(없으면 주석 처리)
        it.forEachRemaining(rows::add);

        // 2) 코드 -> Area (미저장) 매핑 준비
        Map<String, Area> map = new LinkedHashMap<>();

        for (Row r : rows) {
            if (r == null) continue;
            Cell codeCell = r.getCell(0);
            if (codeCell == null) continue;
            String code = getCellString(codeCell).trim();
            if (code.isEmpty()) continue;

            String sidoName = getCellString(r.getCell(1));
            String sigunguName = getCellString(r.getCell(2));
            String emdName = getCellString(r.getCell(3));
            String dongriName = r.getLastCellNum() > 4 ? getCellString(r.getCell(4)) : "";

            // 레벨 판별
            AreaLevel level = detectLevel(code);

            // 이름 선택: 가장 구체적인(emd > sigungu > sido)
            String name = "";
            if (level == AreaLevel.EMD) {
                name = !emdName.isEmpty() ? emdName : (!dongriName.isEmpty() ? dongriName : sigunguName);
            } else if (level == AreaLevel.SIGUNGU) {
                name = !sigunguName.isEmpty() ? sigunguName : sidoName;
            } else {
                name = sidoName;
            }

            Area a = Area.builder()
                    .code(code)
                    .name(name)
                    .level(level)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .createBy(systemUserId)
                    .updateBy(systemUserId)
                    .build();

            map.put(code, a);
        }

        // 3) 부모 연결 (map 조회로)
        for (Map.Entry<String, Area> e : map.entrySet()) {
            String code = e.getKey();
            Area a = e.getValue();
            String parentCode = computeParentCode(code);

            if (parentCode != null) {
                // 부모가 map에 있으면 연결 (같은 파일 내), 없으면 DB에서 찾아 연결
                Area parent = map.get(parentCode);
                if (parent == null) {
                    parent = areaRepository.findByCode(parentCode)
                            .orElse(null);
                }
                a.setParent(parent);
            }
        }

        // 4) DB에 저장 (순서를 보장하기 위해 SIDO -> SIGUNGU -> EMD 순으로 정렬해 저장 권장)
        List<Area> all = new ArrayList<>(map.values());
        all.sort(Comparator.comparing(Area::getLevel)); // enum 순서대로 (SIDO < SIGUNGU < EMD)로 정의되어있어야 함
        areaRepository.saveAll(all);
    }

    private static String getCellString(Cell c) {
        if (c == null) return "";
        if (c.getCellType() == CellType.NUMERIC) {
            // 코드가 숫자로 들어있을 수 있으므로 소수점 없애서 문자열로
            long v = (long) c.getNumericCellValue();
            return String.valueOf(v);
        } else {
            return c.getStringCellValue();
        }
    }

    private static AreaLevel detectLevel(String code) {
        // code는 10자리 문자열 (예: 1100000000, 1111000000, 1111010100)
        if (code.endsWith("00000000")) return AreaLevel.SIDO;
        if (code.endsWith("000000")) return AreaLevel.SIGUNGU;
        if (code.endsWith("00")) return AreaLevel.EMD;
        // 기본 fallback
        return AreaLevel.EMD;
    }

    private static String computeParentCode(String code) {
        if (code.endsWith("00000000")) {
            // SIDO: 최상위, 부모 없음
            return null;
        } else if (code.endsWith("000000")) {
            // SIGUNGU -> parent is SIDO: first 2 digits + 00000000
            if (code.length() >= 2) return code.substring(0, 2) + "00000000";
            return null;
        } else if (code.endsWith("00")) {
            // EMD -> parent is SIGUNGU: first 4 digits + 000000
            if (code.length() >= 4) return code.substring(0, 4) + "000000";
            return null;
        }
        return null;
    }
}
