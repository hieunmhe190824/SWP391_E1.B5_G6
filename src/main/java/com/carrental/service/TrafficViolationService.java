package com.carrental.service;

import com.carrental.config.FileUploadConfig;
import com.carrental.model.DepositHold;
import com.carrental.model.TrafficViolation;
import com.carrental.model.TrafficViolation.ViolationStatus;
import com.carrental.repository.DepositHoldRepository;
import com.carrental.repository.TrafficViolationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class TrafficViolationService {

    @Autowired
    private TrafficViolationRepository violationRepository;

    @Autowired
    private DepositHoldRepository depositHoldRepository;

    @Autowired
    private FileUploadConfig fileUploadConfig;

    /**
     * Get all violations for a deposit hold
     */
    public List<TrafficViolation> getViolationsByDepositHold(Long holdId) {
        return violationRepository.findByDepositHoldId(holdId);
    }

    /**
     * Create a new traffic violation
     */
    @Transactional
    public TrafficViolation createViolation(Long holdId, String violationType, LocalDateTime violationDate,
                                             BigDecimal fineAmount, String description, MultipartFile evidenceFile) 
            throws IOException {
        
        // Validate input
        if (violationType == null || violationType.trim().isEmpty()) {
            throw new IllegalArgumentException("Violation type is required");
        }
        if (violationDate == null) {
            throw new IllegalArgumentException("Violation date is required");
        }
        if (fineAmount == null || fineAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Fine amount must be greater than 0");
        }

        // Get deposit hold
        DepositHold depositHold = depositHoldRepository.findById(holdId)
                .orElseThrow(() -> new RuntimeException("Deposit hold not found"));

        // Upload evidence if provided
        String evidenceUrl = null;
        if (evidenceFile != null && !evidenceFile.isEmpty()) {
            evidenceUrl = uploadEvidenceImage(evidenceFile, holdId);
        }

        // Create violation
        TrafficViolation violation = new TrafficViolation();
        violation.setDepositHold(depositHold);
        violation.setContract(depositHold.getContract());
        violation.setViolationType(violationType);
        violation.setViolationDate(violationDate);
        violation.setFineAmount(fineAmount);
        violation.setDescription(description);
        violation.setEvidenceUrl(evidenceUrl);
        violation.setStatus(ViolationStatus.PENDING);

        return violationRepository.save(violation);
    }

    /**
     * Confirm a violation (change status to CONFIRMED)
     */
    @Transactional
    public TrafficViolation confirmViolation(Long violationId) {
        TrafficViolation violation = violationRepository.findById(violationId)
                .orElseThrow(() -> new RuntimeException("Violation not found"));
        
        violation.setStatus(ViolationStatus.CONFIRMED);
        return violationRepository.save(violation);
    }

    /**
     * Delete a violation
     */
    @Transactional
    public void deleteViolation(Long violationId) {
        TrafficViolation violation = violationRepository.findById(violationId)
                .orElseThrow(() -> new RuntimeException("Violation not found"));
        
        // Delete evidence file if exists
        if (violation.getEvidenceUrl() != null) {
            try {
                String uploadDir = fileUploadConfig.getUploadDir();
                Path filePath = Paths.get(uploadDir + violation.getEvidenceUrl());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                // Log error but don't fail the deletion
                System.err.println("Failed to delete evidence file: " + e.getMessage());
            }
        }

        violationRepository.delete(violation);
    }

    /**
     * Get total fines for a deposit hold
     */
    public BigDecimal getTotalFinesByHold(Long holdId) {
        return violationRepository.sumFineAmountByDepositHoldId(holdId);
    }

    /**
     * Upload evidence image
     */
    private String uploadEvidenceImage(MultipartFile file, Long holdId) throws IOException {
        validateFile(file);

        String uploadDir = fileUploadConfig.getUploadDir() + "/violations/" + holdId;
        Path uploadPath = Paths.get(uploadDir);
        
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String filename = UUID.randomUUID().toString() + extension;
        
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        return "/uploads/violations/" + holdId + "/" + filename;
    }

    /**
     * Validate uploaded file
     */
    private void validateFile(MultipartFile file) {
        // Check file size (max 5MB)
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 5MB");
        }

        // Check file type
        String contentType = file.getContentType();
        List<String> allowedTypes = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp");
        if (contentType == null || !allowedTypes.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Invalid file type. Only JPG, PNG, and WEBP are allowed");
        }
    }

    /**
     * Get file extension
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}
