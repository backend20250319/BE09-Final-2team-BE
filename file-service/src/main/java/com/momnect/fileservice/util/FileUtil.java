package com.momnect.fileservice.util;

public class FileUtil {

    /***
     * 파일 확장자 추출
     * @param filename
     * @return 확장자
     */
    public static String getExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf('.') + 1);
        }
        return "";
    }

    /***
     * 고유한 파일명 생성
     * @param extension 확장자
     * @return 파일명
     */
    public static String generateStoredFileName(String extension) {
        return System.currentTimeMillis() + "_" + java.util.UUID.randomUUID() + "." + extension;
    }
}