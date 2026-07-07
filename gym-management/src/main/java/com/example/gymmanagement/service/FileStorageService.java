package com.example.gymmanagement.service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/** Lưu file đính kèm lên đĩa và trả về metadata để hiển thị / tải xuống. */
@Service
public class FileStorageService {

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    /** Thông tin file đã lưu. */
    @Getter
    @AllArgsConstructor
    public static class Stored {
        private final String url;    // /api/files/<category>/<uuid>.<ext>
        private final String name;   // tên gốc
        private final String type;   // MIME
        private final long   size;   // bytes
    }

    public Stored store(MultipartFile file, String category) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File không hợp lệ");
        }
        try {
            Path dir = Paths.get(uploadDir, category).toAbsolutePath().normalize();
            Files.createDirectories(dir);

            String original = StringUtils.cleanPath(
                    file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
            String ext = "";
            int dot = original.lastIndexOf('.');
            if (dot >= 0) ext = original.substring(dot); // gồm dấu '.'

            String stored = UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = dir.resolve(stored);
            Files.copy(file.getInputStream(), target);

            String type = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
            String url = "/api/files/" + category + "/" + stored;
            return new Stored(url, original, type, file.getSize());
        } catch (IOException e) {
            throw new RuntimeException("Không lưu được file: " + e.getMessage());
        }
    }
}
