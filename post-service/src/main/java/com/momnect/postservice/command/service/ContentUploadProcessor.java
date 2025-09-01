package com.momnect.postservice.command.service;

import com.momnect.postservice.command.client.FileServerUrlCache;
import com.momnect.postservice.command.client.FileServiceClient;
import com.momnect.postservice.command.dto.ImageFileDTO;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Component
@RequiredArgsConstructor
public class ContentUploadProcessor {

    private final FileServiceClient fileServiceClient;
    private final FileServerUrlCache fileServerUrlCache;

    @Value("${post.upload.allowed-content-types}")
    private String allowedContentTypesCsv;

    @Value("${post.upload.max-size-bytes}")
    private long maxSizeBytes;

    public ProcessResult processHtml(String rawHtml) {
        // 0) sanitize
        String clean = Jsoup.clean(rawHtml, Safelist.relaxed()
                .addAttributes(":all", "style")
                .addTags("img", "figure", "figcaption", "video", "source"));

        Document doc = Jsoup.parse(clean);

        // 1) 업로드 대상 수집
        List<Element> targets = new ArrayList<>();
        for (Element img : doc.select("img[src]")) {
            String src = img.attr("src");
            if (isDataUri(src) || isTemp(src)) targets.add(img);
        }
        if (targets.isEmpty()) {
            return new ProcessResult(doc.body().html(), Collections.emptyList());
        }

        // 2) Multipart 준비
        List<MultipartFile> files = new ArrayList<>();
        List<String> oldSrcs = new ArrayList<>();
        for (Element el : targets) {
            String src = el.attr("src");
            if (isDataUri(src)) {
                MultipartFile mf = fromDataUri(src, guessFilenameFromDataUri(src));
                validate(mf);
                files.add(mf);
                oldSrcs.add(src);
            } else if (isTemp(src)) {
                throw new IllegalStateException("Temp 경로 업로드는 스토리지 연동 후 구현 필요: " + src);
            }
        }

        // 3) 파일서버 업로드 (체크예외 아님 → Exception으로 캐치)
        List<ImageFileDTO> uploaded = Collections.emptyList();
        try {
            uploaded = fileServiceClient.upload(files);
        } catch (Exception e) {
            // 업로드 실패시 본문은 그대로 두고, 메타는 빈 리스트로 반환
            uploaded = Collections.emptyList();
        }

        // 4) URL 치환
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < Math.min(uploaded.size(), oldSrcs.size()); i++) {
            map.put(oldSrcs.get(i), resolveUrl(uploaded.get(i)));
        }
        for (Element el : targets) {
            String old = el.attr("src");
            String nu = map.get(old);
            if (nu != null) el.attr("src", nu);
        }

        // 5) 결과/메타
        String finalHtml = doc.body().html();
        List<UploadedFileMeta> metas = new ArrayList<>();
        for (ImageFileDTO dto : uploaded) {
            metas.add(new UploadedFileMeta(
                    resolveUrl(dto),
                    dto.getContentType(),
                    dto.getSize(),
                    dto.getFilename()
            ));
        }
        return new ProcessResult(finalHtml, metas);
    }

    private boolean isDataUri(String src) { return src != null && src.startsWith("data:image/"); }
    private boolean isTemp(String src)    { return src != null && src.startsWith("/temp/"); }

    private MultipartFile fromDataUri(String dataUri, String filename) {
        int comma = dataUri.indexOf(',');
        String mime = dataUri.substring("data:".length(), dataUri.indexOf(';')); // image/png
        byte[] bytes = Base64.getDecoder().decode(dataUri.substring(comma + 1));
        return new InMemoryMultipartFile("imageFiles", filename, mime, bytes);
    }

    private void validate(MultipartFile mf) {
        Set<String> allowed = new HashSet<>(Arrays.asList(allowedContentTypesCsv.split(",")));
        if (!allowed.contains(mf.getContentType())) {
            throw new IllegalArgumentException("허용되지 않은 파일 형식: " + mf.getContentType());
        }
        if (mf.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException("파일이 너무 큼: " + mf.getSize());
        }
    }

    private String guessFilenameFromDataUri(String src) {
        // data:image/<ext>;base64,
        String ext = "png";
        int semi = src.indexOf(';');
        String mime = src.substring("data:".length(), semi); // image/png
        if (mime.endsWith("jpeg") || mime.endsWith("jpg")) ext = "jpg";
        else if (mime.endsWith("png")) ext = "png";
        else if (mime.endsWith("webp")) ext = "webp";
        else if (mime.endsWith("gif")) ext = "gif";
        return "image_" + System.nanoTime() + "." + ext;
    }

    private String resolveUrl(ImageFileDTO dto) {
        if (dto.getUrl() != null && !dto.getUrl().isBlank()) return dto.getUrl();
        String base = fileServerUrlCache.get();
        String path = Optional.ofNullable(dto.getPath()).orElse("");
        if (path.startsWith("/")) return base + path;
        return base + "/" + path;
    }

    /* ===== 결과/메타 DTO ===== */
    public record ProcessResult(String finalHtml, List<UploadedFileMeta> uploaded) {}
    public record UploadedFileMeta(String url, String contentType, Long size, String filename) {}

    /* ===== 런타임 전용 In-Memory MultipartFile ===== */
    static class InMemoryMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] bytes;

        InMemoryMultipartFile(String name, String originalFilename, String contentType, byte[] bytes) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.bytes = bytes;
        }

        @Override public String getName() { return name; }
        @Override public String getOriginalFilename() { return originalFilename; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return bytes.length == 0; }
        @Override public long getSize() { return bytes.length; }
        @Override public byte[] getBytes() { return bytes; }
        @Override public InputStream getInputStream() { return new java.io.ByteArrayInputStream(bytes); }
        @Override public void transferTo(java.io.File dest) throws IOException {
            java.nio.file.Files.write(dest.toPath(), bytes);
        }
    }
}
