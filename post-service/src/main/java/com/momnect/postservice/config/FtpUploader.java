package com.momnect.postservice.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Component
public class FtpUploader {

    @Value("${ftp.server}")
    private String server;

    @Value("${ftp.port:21}")
    private int port;

    @Value("${ftp.user}")
    private String user;

    @Value("${ftp.password}")
    private String password;

    @Value("${ftp.file-server-url}")
    private String publicBaseUrl;

    @Value("${ftp.path:/}")
    private String defaultRemotePath;

    public String uploadAndReturnPublicUrl(MultipartFile file, String remotePath) throws IOException {
        String dir = normalizeDir(hasText(remotePath) ? remotePath : defaultRemotePath);

        String ext = extFromOriginalOrContentType(file.getOriginalFilename(), file.getContentType());
        String storedName = System.currentTimeMillis() + "_" + UUID.randomUUID() + ext; // 예) 1756698..._uuid.png

        FTPClient ftp = new FTPClient();
        try (InputStream in = file.getInputStream()) {
            ftp.setControlEncoding("UTF-8");
            ftp.connect(server, port);
            int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                throw new IOException("FTP connect failed: " + ftp.getReplyString());
            }
            if (!ftp.login(user, password)) {
                throw new IOException("FTP login failed: " + ftp.getReplyString());
            }
            ftp.enterLocalPassiveMode();
            ftp.setFileType(FTP.BINARY_FILE_TYPE);

            createDirectories(ftp, dir);

            String remoteFilePath = dir + storedName;
            if (!ftp.storeFile(remoteFilePath, in)) {
                throw new IOException("FTP storeFile failed: " + ftp.getReplyString());
            }

            String publicUrl = buildPublicUrl(dir, storedName);
            log.info("[FTP] 업로드 성공(랜덤명): {}", publicUrl);
            return publicUrl;
        } finally {
            safeClose(ftp);
        }
    }

    public void upload(InputStream in, String remotePath) throws IOException {
        String normalized = normalizePath(remotePath);
        String dir = normalized.substring(0, normalized.lastIndexOf('/') + 1); // 디렉터리 part
        FTPClient ftp = new FTPClient();
        try {
            ftp.setControlEncoding("UTF-8");
            ftp.connect(server, port);
            int reply = ftp.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                throw new IOException("FTP connect failed: " + ftp.getReplyString());
            }
            if (!ftp.login(user, password)) {
                throw new IOException("FTP login failed: " + ftp.getReplyString());
            }
            ftp.enterLocalPassiveMode();
            ftp.setFileType(FTP.BINARY_FILE_TYPE);

            createDirectories(ftp, dir);

            if (!ftp.storeFile(normalized, in)) {
                throw new IOException("FTP storeFile failed: " + ftp.getReplyString());
            }
            log.info("[FTP] 업로드(레거시 시그니처) 성공: {}", normalized);
        } finally {
            safeClose(ftp);
            try { if (in != null) in.close(); } catch (Exception ignore) {}
        }
    }

    private void createDirectories(FTPClient ftp, String path) throws IOException {
        String dir = normalizeDir(path);
        String[] parts = dir.split("/");
        String current = "";
        for (String p : parts) {
            if (p.isBlank()) continue;
            current += "/" + p;
            if (!ftp.changeWorkingDirectory(current)) {
                if (!ftp.makeDirectory(current)) {
                    throw new IOException("Could not create dir " + current + ": " + ftp.getReplyString());
                }
            }
        }
    }

    private String buildPublicUrl(String dir, String filename) {
        String base = (publicBaseUrl == null) ? "" : publicBaseUrl.replaceAll("/+$", "");
        String path = dir + filename;
        if (!path.startsWith("/")) path = "/" + path;
        return base + path;
    }

    private String normalizeDir(String p) {
        if (!hasText(p)) return "/";
        String s = p.replace("\\", "/");
        if (!s.startsWith("/")) s = "/" + s;
        if (!s.endsWith("/")) s = s + "/";
        return s;
    }

    private String normalizePath(String p) {
        String s = (p == null) ? "" : p.replace("\\", "/");
        if (!s.startsWith("/")) s = "/" + s;
        return s;
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private String extFromOriginalOrContentType(String originalName, String ct) {
        String ext = "";
        if (originalName != null) {
            int dot = originalName.lastIndexOf('.');
            if (dot >= 0 && dot < originalName.length() - 1) {
                ext = originalName.substring(dot); // ".png"
            }
        }
        if (!ext.isEmpty()) return ext;

        if (ct == null) return "";
        String low = ct.toLowerCase(Locale.ROOT);
        if (low.contains("jpeg")) return ".jpg";
        if (low.contains("png"))  return ".png";
        if (low.contains("webp")) return ".webp";
        if (low.contains("gif"))  return ".gif";
        if (low.contains("mp4"))  return ".mp4";
        if (low.contains("pdf"))  return ".pdf";
        if (low.contains("jfif")) return ".jfif";
        return "";
    }

    private void safeClose(FTPClient ftp) {
        if (ftp != null && ftp.isConnected()) {
            try { ftp.logout(); } catch (Exception ignore) {}
            try { ftp.disconnect(); } catch (Exception ignore) {}
        }
    }
}
