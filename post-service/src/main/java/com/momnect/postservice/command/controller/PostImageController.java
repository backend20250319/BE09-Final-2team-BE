package com.momnect.postservice.command.controller;

import com.momnect.postservice.command.client.FileServiceClient;
import com.momnect.postservice.command.dto.ImageFileDTO;
import com.momnect.postservice.common.ApiResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/posts/images")
public class PostImageController {

    private static final Logger log = LoggerFactory.getLogger(PostImageController.class);
    private final FileServiceClient fileServiceClient;

    @PostMapping(value = "/editor-upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadForEditor(@RequestPart("upload") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return bad(Map.of("reason", "empty file"));
        }

        try {
            ApiResponse<List<ImageFileDTO>> resp = fileServiceClient.upload(file);
            List<ImageFileDTO> data = (resp != null) ? resp.getData() : null;
            ImageFileDTO item = (data != null && !data.isEmpty()) ? data.get(0) : null;

            String url = (item != null) ? item.getUrl() : null;

            // url이 없으면 path + 서버 기본 URL로 조합
            if (isBlank(url) && item != null && !isBlank(item.getPath())) {
                ApiResponse<Map<String, String>> baseResp = fileServiceClient.getServerUrl();
                String base = (baseResp != null && baseResp.getData() != null)
                        ? baseResp.getData().get("fileServiceUrl") : null;
                if (!isBlank(base)) {
                    url = joinUrl(base, item.getPath());
                }
            }

            if (isBlank(url)) {
                return bad(Map.of("reason", "file-service returned no usable url"));
            }

            Map<String, Object> ok = new LinkedHashMap<>();
            ok.put("uploaded", 1);
            ok.put("url", url);
            return ResponseEntity.ok(ok);

        } catch (FeignException e) {
            String body = null;
            try { body = e.contentUTF8(); } catch (Exception ignore) {}
            return ResponseEntity.status(e.status()).body(resp(0, Map.of(
                    "message", body == null ? "" : body
            )));
        } catch (Exception e) {
            log.error("editor-upload failed", e);
            return ResponseEntity.internalServerError().body(resp(0, Map.of(
                    "error", e.getClass().getSimpleName(),
                    "message", String.valueOf(e.getMessage())
            )));
        }
    }

    // helpers
    private static ResponseEntity<Map<String, Object>> bad(Map<String, ?> extra) {
        return ResponseEntity.badRequest().body(resp(0, extra));
    }
    private static Map<String, Object> resp(int uploaded, Map<String, ?> extra) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("uploaded", uploaded);
        if (extra != null) m.putAll(extra);
        return m;
    }
    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
    private static String joinUrl(String base, String path) {
        String b = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        String p = path.startsWith("/") ? path : ("/" + path);
        return b + p;
    }
}
