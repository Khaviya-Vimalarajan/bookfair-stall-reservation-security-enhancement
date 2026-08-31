package com.bookfair.Stall_Reservation.controller.admin;

import com.bookfair.Stall_Reservation.service.ContentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/content")
public class AdminContentController {

    private static final List<String> EDITABLE_KEYS = List.of(
            "home.title",
            "home.description",
            "home.videoUrl",
            "contact.email",
            "contact.phone",
            "contact.address",
            "contact.content");

    private static final List<String> ALLOWED_VIDEO_TYPES = List.of(
            "video/mp4", "video/webm", "video/ogg");

    private final ContentService contentService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public AdminContentController(ContentService contentService) {
        this.contentService = contentService;
    }

    /** Get all editable site content */
    @GetMapping
    public ResponseEntity<Map<String, String>> getAll() {
        return ResponseEntity.ok(contentService.getAll(EDITABLE_KEYS));
    }

    /** Update site content — accepts a map of key→value pairs */
    @PutMapping
    public ResponseEntity<Map<String, String>> update(@RequestBody Map<String, String> body) {
        for (Map.Entry<String, String> entry : body.entrySet()) {
            if (EDITABLE_KEYS.contains(entry.getKey())) {
                contentService.set(entry.getKey(), entry.getValue());
            }
        }
        return ResponseEntity.ok(contentService.getAll(EDITABLE_KEYS));
    }

    /** Upload a video file and store its URL in home.videoUrl */
    @PostMapping("/upload-video")
    public ResponseEntity<?> uploadVideo(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Unsupported file type."));
        }

        try (java.io.InputStream is = new java.io.BufferedInputStream(file.getInputStream())) {
            is.mark(12);
            byte[] header = new byte[12];
            int bytesRead = is.read(header);
            is.reset();

            boolean isMp4 = bytesRead >= 8 && header[4] == 'f' && header[5] == 't' && header[6] == 'y' && header[7] == 'p';
            boolean isWebm = bytesRead >= 4 && (header[0] & 0xFF) == 0x1A && (header[1] & 0xFF) == 0x45 && (header[2] & 0xFF) == 0xDF && (header[3] & 0xFF) == 0xA3;
            boolean isOgg = bytesRead >= 4 && header[0] == 'O' && header[1] == 'g' && header[2] == 'g' && header[3] == 'S';

            if (!isMp4 && !isWebm && !isOgg) {
                return ResponseEntity.badRequest().body(Map.of("message", "Unsupported file type."));
            }

            String extension = isMp4 ? ".mp4" : (isWebm ? ".webm" : ".ogg");
            String filename = "home-video-" + UUID.randomUUID().toString().substring(0, 8) + extension;

            Path uploadPath = Paths.get(uploadDir, "videos");
            Files.createDirectories(uploadPath);

            Path filePath = uploadPath.resolve(filename);
            Files.copy(is, filePath, StandardCopyOption.REPLACE_EXISTING);

            String videoUrl = "/uploads/videos/" + filename;
            contentService.set("home.videoUrl", videoUrl);

            return ResponseEntity.ok(Map.of(
                    "message", "Video uploaded successfully",
                    "videoUrl", videoUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid video file."));
        }
    }

    /** Upload an image file and return its URL */
    @PostMapping("/upload-image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Unsupported file type."));
        }

        try (java.io.InputStream is = new java.io.BufferedInputStream(file.getInputStream())) {
            is.mark(12);
            byte[] header = new byte[12];
            int bytesRead = is.read(header);
            is.reset();

            boolean isJpeg = bytesRead >= 3 && (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF;
            boolean isPng = bytesRead >= 8 && (header[0] & 0xFF) == 0x89 && (header[1] & 0xFF) == 0x50 && (header[2] & 0xFF) == 0x4E && (header[3] & 0xFF) == 0x47
                    && (header[4] & 0xFF) == 0x0D && (header[5] & 0xFF) == 0x0A && (header[6] & 0xFF) == 0x1A && (header[7] & 0xFF) == 0x0A;
            boolean isGif = bytesRead >= 6 && header[0] == 'G' && header[1] == 'I' && header[2] == 'F' && header[3] == '8' && (header[4] == '7' || header[4] == '9') && header[5] == 'a';

            if (!isJpeg && !isPng && !isGif) {
                return ResponseEntity.badRequest().body(Map.of("message", "Unsupported file type."));
            }

            // Decode image using ImageIO (safe check)
            try {
                java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(is);
                is.reset();
                if (img == null) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Invalid image file."));
                }
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid image file."));
            }

            String extension = isJpeg ? ".jpg" : (isPng ? ".png" : ".gif");
            String filename = "event-" + UUID.randomUUID().toString().substring(0, 8) + extension;

            Path uploadPath = Paths.get(uploadDir, "images");
            Files.createDirectories(uploadPath);

            Path filePath = uploadPath.resolve(filename);
            Files.copy(is, filePath, StandardCopyOption.REPLACE_EXISTING);

            String imageUrl = "/uploads/images/" + filename;
            return ResponseEntity.ok(Map.of(
                    "message", "Image uploaded successfully",
                    "url", imageUrl));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid image file."));
        }
    }

    /** Delete about page content */
    @DeleteMapping("/about")
    public ResponseEntity<Void> deleteAbout() {
        contentService.delete("about.content");
        return ResponseEntity.noContent().build();
    }
}
