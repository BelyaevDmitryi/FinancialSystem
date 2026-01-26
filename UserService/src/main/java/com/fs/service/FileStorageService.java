package com.fs.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    @Value("${file.upload-dir:uploads/avatars}")
    private String uploadDir;

    /**
     * Сохраняет загруженный файл аватара
     *
     * @param file загруженный файл
     * @return путь к сохраненному файлу относительно корня приложения
     * @throws IOException если произошла ошибка при сохранении файла
     * @throws IllegalArgumentException если файл невалиден
     */
    public String saveAvatar(MultipartFile file) throws IOException {
        // Валидация файла
        validateFile(file);

        // Создаем директорию, если её нет
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("Создана директория для загрузки файлов: {}", uploadPath.toAbsolutePath());
        }

        // Генерируем уникальное имя файла
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString() + extension;

        // Сохраняем файл
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Возвращаем относительный путь для URL
        String relativePath = "/uploads/avatars/" + filename;
        log.info("Файл успешно сохранен: {}", filePath.toAbsolutePath());
        return relativePath;
    }

    /**
     * Удаляет файл аватара
     *
     * @param filePath путь к файлу
     */
    public void deleteAvatar(String filePath) {
        if (filePath == null || filePath.isEmpty() || filePath.startsWith("/images/")) {
            // Не удаляем дефолтные аватары или файлы из classpath
            return;
        }

        try {
            // Удаляем префикс /uploads/avatars/ для получения имени файла
            String filename = filePath.replace("/uploads/avatars/", "");
            Path fileToDelete = Paths.get(uploadDir, filename);
            
            if (Files.exists(fileToDelete)) {
                Files.delete(fileToDelete);
                log.info("Файл успешно удален: {}", fileToDelete.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Ошибка при удалении файла: {}", filePath, e);
        }
    }

    /**
     * Валидирует загруженный файл
     *
     * @param file файл для валидации
     * @throws IllegalArgumentException если файл невалиден
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл не может быть пустым");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Размер файла не должен превышать 5 МБ");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Неподдерживаемый тип файла. Разрешены только изображения: JPEG, PNG, GIF, WEBP"
            );
        }
    }
}
