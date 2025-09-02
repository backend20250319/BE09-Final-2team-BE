package com.momnect.postservice.command.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.momnect.postservice.command.dto.ImageFileDTO;
import com.momnect.postservice.config.FtpUploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class FileServiceClient {

    private final RestTemplate restTemplate;
    private final FileServerUrlCache fileServerUrlCache;
    private final FtpUploader ftpUploader;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** HTTP 업로드 API 기본 설정 */
    @Value("${clients.file-service.enabled:true}")
    private boolean httpEnabled;

    @Value("${clients.file-service.base-url:}")
    private String baseUrl;
    /** FTP/정적 공개 URL 쪽 설정 */
    @Value("${ftp.path:/}")
    private String ftpRemotePath; // 예: /2

    public List<ImageFileDTO> upload(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) return Collections.emptyList();

        // 1) HTTP 업로드 시도 (enabled && baseUrl 존재)
        if (httpEnabled && hasText(baseUrl)) {
            try {
                List<ImageFileDTO> viaHttp = tryHttpUpload(files);
                if (!viaHttp.isEmpty()) {
                    log.info("[FileUpload] HTTP 업로드 성공 count={}", viaHttp.size());
                    return viaHttp;
                } else {
                    log.warn("[FileUpload] HTTP 응답 파싱 결과 비어있음 → FTP 폴백 진행");
                }
            } catch (Exception e) {
                log.warn("[FileUpload] HTTP 업로드 실패 → FTP 폴백 진행. err={}", e.getMessage());
            }
        } else {
            log.info("[FileUpload] HTTP 업로드 비활성화 혹은 base-url 미설정 → FTP 업로드로 진행");
        }

        // 2) FTP 폴백
        return uploadViaFtp(files);
    }

    public String getUrlById(Long id) {
        if (!httpEnabled || !hasText(baseUrl)) {
            log.info("[FileGetUrl] HTTP 비활성화 상태이므로 getUrlById는 지원하지 않음 (id={})", id);
            return null;
        }
        try {
            String url = baseUrl.endsWith("/") ? baseUrl + "files/" + id : baseUrl + "/files/" + id;
            ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) return null;

            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode obj = root.isObject() && root.has("data") ? root.get("data") : root;
            String u = obj.hasNonNull("url") ? obj.get("url").asText() : null;
            if (hasText(u)) return u;

            String path = obj.hasNonNull("path") ? obj.get("path").asText() : null;
            if (hasText(path)) {
                String base = safePublicBaseUrl();
                return path.startsWith("/") ? base + path : base + "/" + path;
            }
        } catch (Exception e) {
            log.warn("[FileGetUrl] id={}, err={}", id, e.toString());
        }
        return null;
    }


    private List<ImageFileDTO> tryHttpUpload(List<MultipartFile> files) throws Exception {
        List<String> paramCandidates = List.of(
                "imageFiles",           // 최우선
                "file", "files",
                "image", "images",
                "multipartFile",
                "uploadFile", "uploadFiles",
                "files[]", "fileList"
        );

        for (String paramName : paramCandidates) {
            try {
                List<ImageFileDTO> result = doHttpUpload(paramName, files);
                if (!result.isEmpty()) {
                    log.info("[FileUpload] HTTP 성공 paramName={}, count={}", paramName, result.size());
                    return result;
                } else {
                    log.warn("[FileUpload] HTTP 응답 파싱 비어있음 paramName={}", paramName);
                }
            } catch (RestClientException ex) {
                log.warn("[FileUpload] HTTP 4xx/5xx paramName={}, err={}", paramName, ex.getMessage());
            } catch (Exception ex) {
                log.warn("[FileUpload] HTTP 예외 paramName={}, err={}", paramName, ex.toString());
            }
        }
        return Collections.emptyList();
    }

    private List<ImageFileDTO> doHttpUpload(String paramName, List<MultipartFile> files) throws Exception {
        final String url = baseUrl.endsWith("/") ? baseUrl + "files" : baseUrl + "/files";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.setAcceptCharset(List.of(StandardCharsets.UTF_8));

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        for (MultipartFile mf : files) {
            String filename = Optional.ofNullable(mf.getOriginalFilename()).orElse("file");
            MediaType partType = MediaType.parseMediaType(guessContentType(filename, mf.getContentType()));

            ByteArrayResource resource = new ByteArrayResource(mf.getBytes()) {
                @Override public String getFilename() { return filename; }
            };

            HttpHeaders partHeaders = new HttpHeaders();
            partHeaders.setContentType(partType);
            ContentDisposition cd = ContentDisposition
                    .builder("form-data")
                    .name(paramName)
                    .filename(filename, StandardCharsets.UTF_8)
                    .build();
            partHeaders.setContentDisposition(cd);

            HttpEntity<ByteArrayResource> part = new HttpEntity<>(resource, partHeaders);
            body.add(paramName, part);
        }

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> resp = restTemplate.postForEntity(url, request, String.class);

        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new RestClientException("status=" + resp.getStatusCode() + ", body=" + resp.getBody());
        }
        return parseUploadResponse(resp.getBody());
    }

    private List<ImageFileDTO> parseUploadResponse(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        JsonNode arr = root;

        if (root.isObject()) {
            for (String key : List.of("data", "result", "results", "payload", "files")) {
                if (root.has(key) && root.get(key).isArray()) {
                    arr = root.get(key);
                    break;
                }
            }
        }

        if (!arr.isArray()) {
            if (arr.isObject()) return List.of(toDto(arr));
            return Collections.emptyList();
        }

        List<ImageFileDTO> list = new ArrayList<>();
        for (JsonNode node : arr) list.add(toDto(node));
        return list;
    }

    private ImageFileDTO toDto(JsonNode n) {
        ImageFileDTO dto = new ImageFileDTO();
        if (n.hasNonNull("id")) dto.setId(n.get("id").asLong());
        if (n.hasNonNull("filename")) dto.setFilename(n.get("filename").asText());
        if (n.hasNonNull("contentType")) dto.setContentType(n.get("contentType").asText());
        if (n.hasNonNull("size")) dto.setSize(n.get("size").asLong());
        if (n.hasNonNull("url")) dto.setUrl(n.get("url").asText());
        if (n.hasNonNull("path")) dto.setPath(n.get("path").asText());

        if (!hasText(dto.getUrl()) && hasText(dto.getPath())) {
            String base = safePublicBaseUrlOrHttpBase();
            String p = dto.getPath();
            dto.setUrl(p.startsWith("/") ? base + p : base + "/" + p);
        }
        return dto;
    }


    private List<ImageFileDTO> uploadViaFtp(List<MultipartFile> files) {
        List<ImageFileDTO> result = new ArrayList<>();
        String publicBase = safePublicBaseUrl();

        for (MultipartFile mf : files) {
            try {
                String filename = Optional.ofNullable(mf.getOriginalFilename()).orElse("file");
                String contentType = guessContentType(filename, mf.getContentType());

                String publicUrl = ftpUploader.uploadAndReturnPublicUrl(mf, ftpRemotePath);
                if (!hasText(publicUrl)) {
                    log.warn("[FileUpload][FTP] publicUrl null/blank: fn={}", filename);
                    continue;
                }

                String path = null;
                if (hasText(publicBase) && publicUrl.startsWith(publicBase)) {
                    path = publicUrl.substring(publicBase.length()); // 앞의 / 포함/미포함 상관없이 그대로 둠
                }

                ImageFileDTO dto = new ImageFileDTO();
                dto.setFilename(filename);
                dto.setContentType(contentType);
                dto.setSize(mf.getSize());
                dto.setUrl(publicUrl);
                dto.setPath(path);

                result.add(dto);
                log.info("[FileUpload][FTP] 성공: {}", publicUrl);
            } catch (Exception e) {
                log.warn("[FileUpload][FTP] 실패 err={}", e.getMessage());
            }
        }
        return result;
    }

    private String guessContentType(String filename, String originalContentType) {
        if (hasText(originalContentType)
                && !originalContentType.equalsIgnoreCase(MediaType.APPLICATION_OCTET_STREAM_VALUE)) {
            return originalContentType;
        }
        String ext = "";
        int dot = filename.lastIndexOf('.');
        if (dot >= 0) ext = filename.substring(dot + 1).toLowerCase(Locale.ROOT);

        return switch (ext) {
            case "jpg", "jpeg", "jfif", "pjpeg" -> MediaType.IMAGE_JPEG_VALUE;
            case "png" -> MediaType.IMAGE_PNG_VALUE;
            case "webp" -> "image/webp";
            case "gif" -> MediaType.IMAGE_GIF_VALUE;
            case "mp4" -> "video/mp4";
            case "pdf" -> MediaType.APPLICATION_PDF_VALUE;
            default -> MediaType.APPLICATION_OCTET_STREAM_VALUE;
        };
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private String safePublicBaseUrlOrHttpBase() {
        String cached = null;
        try { cached = (fileServerUrlCache != null) ? fileServerUrlCache.get() : null; } catch (Throwable ignore) {}
        if (hasText(cached)) return cached;
        if (hasText(baseUrl)) return baseUrl;
        return ""; // 최후
    }

    private String safePublicBaseUrl() {
        String cached = null;
        try { cached = (fileServerUrlCache != null) ? fileServerUrlCache.get() : null; } catch (Throwable ignore) {}
        if (hasText(cached)) return cached;
        return hasText(baseUrl) ? baseUrl : "";
    }
}
