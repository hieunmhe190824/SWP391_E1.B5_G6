package com.carrental.service;

import com.carrental.model.User;
import com.carrental.model.UserDocument;
import com.carrental.repository.UserDocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserDocumentService {

    @Autowired
    private UserDocumentRepository userDocumentRepository;

    @Value("${file.upload-dir:src/main/resources/static/uploads}")
    private String uploadDir;

    public List<UserDocument> getDocumentsByUser(User user) {
        return userDocumentRepository.findByUser(user);
    }

    public Optional<UserDocument> getDocumentByIdAndUser(Long id, User user) {
        return userDocumentRepository.findByIdAndUser(id, user);
    }

    public UserDocument createDocument(User user, UserDocument.DocumentType documentType, 
                                      String documentNumber, LocalDate expiryDate, 
                                      MultipartFile imageFile) throws IOException {
        UserDocument document = new UserDocument();
        document.setUser(user);
        document.setDocumentType(documentType);
        document.setDocumentNumber(documentNumber);
        document.setExpiryDate(expiryDate);
        document.setStatus(UserDocument.DocumentStatus.Pending);

        if (imageFile != null && !imageFile.isEmpty()) {
            String imageUrl = saveFile(imageFile, user.getId());
            document.setImageUrl(imageUrl);
        }

        return userDocumentRepository.save(document);
    }

    public UserDocument updateDocument(Long id, User user, String documentNumber, 
                                      LocalDate expiryDate, MultipartFile imageFile) throws IOException {
        UserDocument document = userDocumentRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        document.setDocumentNumber(documentNumber);
        document.setExpiryDate(expiryDate);
        
        // If status was rejected, reset to pending when updating
        if (document.getStatus() == UserDocument.DocumentStatus.Rejected) {
            document.setStatus(UserDocument.DocumentStatus.Pending);
            document.setVerifiedBy(null);
        }

        if (imageFile != null && !imageFile.isEmpty()) {
            // Delete old file if exists
            if (document.getImageUrl() != null) {
                deleteFile(document.getImageUrl());
            }
            String imageUrl = saveFile(imageFile, user.getId());
            document.setImageUrl(imageUrl);
        }

        return userDocumentRepository.save(document);
    }

    public void deleteDocument(Long id, User user) {
        UserDocument document = userDocumentRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        // Delete file if exists
        if (document.getImageUrl() != null) {
            deleteFile(document.getImageUrl());
        }

        userDocumentRepository.delete(document);
    }

    private String saveFile(MultipartFile file, Long userId) throws IOException {
        // Use same pattern as VehicleService - external uploads directory
        Path uploadPath = Paths.get("uploads/docs");
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".") 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : "";
        String filename = "doc_" + userId + "_" + UUID.randomUUID().toString() + extension;

        // Save file
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Return URL path (same pattern as vehicle - just the filename path)
        return "/uploads/docs/" + filename;
    }

    private void deleteFile(String imageUrl) {
        try {
            if (imageUrl != null && imageUrl.startsWith("/uploads/")) {
                // Extract filename from URL (same pattern as vehicle)
                String filename = imageUrl.substring("/uploads/docs/".length());
                Path filePath = Paths.get("uploads/docs").resolve(filename);
                Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            // Log error but don't throw - file might not exist
            System.err.println("Error deleting file: " + imageUrl);
        }
    }
}
