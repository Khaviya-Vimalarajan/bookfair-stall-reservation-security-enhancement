package com.bookfair.Stall_Reservation;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "OIDC_ISSUER=https://mock-issuer.local"
})
@AutoConfigureMockMvc
public class FileUploadIntegrationTest {

    @MockBean
    private JwtDecoder jwtDecoder;

    @Autowired
    private MockMvc mockMvc;

    private byte[] createValidPngBytes() throws Exception {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    private byte[] createValidJpegBytes() throws Exception {
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpeg", baos);
        return baos.toByteArray();
    }

    @Test
    public void testImageUploadFlows() throws Exception {
        byte[] validPng = createValidPngBytes();
        byte[] validJpeg = createValidJpegBytes();

        // 1. Valid PNG upload -> Accepted
        MockMultipartFile validPngFile = new MockMultipartFile("file", "test.png", "image/png", validPng);
        mockMvc.perform(multipart("/api/admin/content/upload-image")
                        .file(validPngFile)
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_EXHIBITION_ORGANIZER"))
                                .jwt(j -> j.claim("sub", "admin-sub").claim("email", "admin@test.com"))
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Image uploaded successfully"))
                .andExpect(jsonPath("$.url").value(Matchers.containsString(".png")))
                .andExpect(jsonPath("$.url").value(Matchers.not(Matchers.containsString("test.png")))); // verified randomized filename

        // 2. Valid JPEG upload -> Accepted
        MockMultipartFile validJpegFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", validJpeg);
        mockMvc.perform(multipart("/api/admin/content/upload-image")
                        .file(validJpegFile)
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_EXHIBITION_ORGANIZER"))
                                .jwt(j -> j.claim("sub", "admin-sub").claim("email", "admin@test.com"))
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Image uploaded successfully"))
                .andExpect(jsonPath("$.url").value(Matchers.containsString(".jpg")));

        // 3. Spoofed text file (claiming to be image/jpeg) -> Rejected
        MockMultipartFile textFileSpoofed = new MockMultipartFile("file", "test.txt", "image/jpeg", "This is plain text".getBytes());
        mockMvc.perform(multipart("/api/admin/content/upload-image")
                        .file(textFileSpoofed)
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_EXHIBITION_ORGANIZER"))
                                .jwt(j -> j.claim("sub", "admin-sub").claim("email", "admin@test.com"))
                        ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unsupported file type."));

        // 4. Renamed text file (test.jpg with plain text content) -> Rejected
        MockMultipartFile renamedTextFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", "This is plain text".getBytes());
        mockMvc.perform(multipart("/api/admin/content/upload-image")
                        .file(renamedTextFile)
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_EXHIBITION_ORGANIZER"))
                                .jwt(j -> j.claim("sub", "admin-sub").claim("email", "admin@test.com"))
                        ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unsupported file type."));

        // 5. Empty file -> Rejected
        MockMultipartFile emptyFile = new MockMultipartFile("file", "test.png", "image/png", new byte[0]);
        mockMvc.perform(multipart("/api/admin/content/upload-image")
                        .file(emptyFile)
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_EXHIBITION_ORGANIZER"))
                                .jwt(j -> j.claim("sub", "admin-sub").claim("email", "admin@test.com"))
                        ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unsupported file type."));
    }

    @Test
    public void testVideoUploadFlows() throws Exception {
        byte[] validMp4 = new byte[] {0, 0, 0, 20, 'f', 't', 'y', 'p', 'm', 'p', '4', '2'};

        // 1. Valid MP4 upload -> Accepted
        MockMultipartFile validMp4File = new MockMultipartFile("file", "test.mp4", "video/mp4", validMp4);
        mockMvc.perform(multipart("/api/admin/content/upload-video")
                        .file(validMp4File)
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_EXHIBITION_ORGANIZER"))
                                .jwt(j -> j.claim("sub", "admin-sub").claim("email", "admin@test.com"))
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Video uploaded successfully"))
                .andExpect(jsonPath("$.videoUrl").value(Matchers.containsString(".mp4")));

        // 2. Spoofed text file (claiming to be video/mp4) -> Rejected
        MockMultipartFile textFileSpoofed = new MockMultipartFile("file", "test.txt", "video/mp4", "This is plain text".getBytes());
        mockMvc.perform(multipart("/api/admin/content/upload-video")
                        .file(textFileSpoofed)
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_EXHIBITION_ORGANIZER"))
                                .jwt(j -> j.claim("sub", "admin-sub").claim("email", "admin@test.com"))
                        ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unsupported file type."));

        // 3. Empty video -> Rejected
        MockMultipartFile emptyVideo = new MockMultipartFile("file", "test.mp4", "video/mp4", new byte[0]);
        mockMvc.perform(multipart("/api/admin/content/upload-video")
                        .file(emptyVideo)
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_EXHIBITION_ORGANIZER"))
                                .jwt(j -> j.claim("sub", "admin-sub").claim("email", "admin@test.com"))
                        ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unsupported file type."));
    }

    @Test
    public void testUploadPathTraversalProtection() throws Exception {
        byte[] validPng = createValidPngBytes();

        // Path traversal in filename: ../../exploit.png -> Expect randomized name under uploads/images (no traverse)
        MockMultipartFile traversalFile = new MockMultipartFile("file", "../../../exploit.png", "image/png", validPng);
        mockMvc.perform(multipart("/api/admin/content/upload-image")
                        .file(traversalFile)
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_EXHIBITION_ORGANIZER"))
                                .jwt(j -> j.claim("sub", "admin-sub").claim("email", "admin@test.com"))
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value(Matchers.not(Matchers.containsString("exploit"))))
                .andExpect(jsonPath("$.url").value(Matchers.not(Matchers.containsString(".."))));
    }

    @Test
    public void testRoleAccessControl() throws Exception {
        byte[] validPng = createValidPngBytes();
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", validPng);

        // STALL_VENDOR role -> Expect 403 Forbidden
        mockMvc.perform(multipart("/api/admin/content/upload-image")
                        .file(file)
                        .with(jwt()
                                .authorities(new SimpleGrantedAuthority("ROLE_STALL_VENDOR"))
                                .jwt(j -> j.claim("sub", "vendor-sub").claim("email", "vendor@test.com"))
                        ))
                .andExpect(status().isForbidden());
    }
}
