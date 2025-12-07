package com.carrental.repository;

import com.carrental.model.User;
import com.carrental.model.UserDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserDocumentRepository extends JpaRepository<UserDocument, Long> {
    List<UserDocument> findByUser(User user);
    List<UserDocument> findByUserAndDocumentType(User user, UserDocument.DocumentType documentType);
    Optional<UserDocument> findByIdAndUser(Long id, User user);
}
