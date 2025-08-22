package com.momnect.fileservice.command.service;

import com.momnect.fileservice.command.dto.ImageFileDTO;
import com.momnect.fileservice.command.entity.ImageFile;
import com.momnect.fileservice.command.repository.ImageFileRepository;
import com.momnect.fileservice.util.FileUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageFileService {

    private final ImageFileRepository imageFileRepository;

    @Value("${ftp.server}")
    private String ftpServer;

    @Value("${ftp.port}")
    private int ftpPort;

    @Value("${ftp.user}")
    private String ftpUser;

    @Value("${ftp.password}")
    private String ftpPassword;

    @Value("${ftp.path}")
    private String ftpPath;

    /***
     * 파일 업로드
     * @param files 업로드할 이미지 파일 리스트
     * @return 업로드된 이미지 파일 dto 리스트
     */
    @Transactional
    public List<ImageFileDTO> upload(List<MultipartFile> files) throws IOException {
        List<ImageFileDTO> uploadedFiles = new ArrayList<>();

        // FTP 접속
        FTPClient ftpClient = new FTPClient();
        ftpClient.connect(ftpServer, ftpPort);
        boolean login = ftpClient.login(ftpUser, ftpPassword);
        if (!login) {
            ftpClient.disconnect();
            throw new IOException("FTP 로그인 실패");
        }

        ftpClient.enterLocalPassiveMode();
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

        // 디렉토리 이동
        if (!ftpClient.changeWorkingDirectory(ftpPath)) {
            throw new IOException("FTP 디렉토리 이동 실패: " + ftpPath);
        }

        // 파일 업로드
        for (MultipartFile file : files) {
            String extension = FileUtil.getExtension(file.getOriginalFilename());
            String storedName = FileUtil.generateStoredFileName(extension);

            try (InputStream inputStream = file.getInputStream()) {
                boolean done = ftpClient.storeFile(storedName, inputStream);
                if (done) {
                    // DB에 엔티티 저장
                    ImageFile entity = ImageFile.builder()
                            .originalName(file.getOriginalFilename())
                            .storedName(storedName)
                            .path(ftpPath + "/" + storedName)
                            .size(file.getSize())
                            .extension(extension)
                            .isDeleted(false)
                            .createdAt(LocalDateTime.now())
                            .build();

                    imageFileRepository.save(entity);

                    // DTO 변환 후 리스트에 추가
                    uploadedFiles.add(ImageFileDTO.fromEntity(entity));
                }
            }
        }

        // FTP 로그아웃
        ftpClient.logout();
        ftpClient.disconnect();

        return uploadedFiles;
    }
}

