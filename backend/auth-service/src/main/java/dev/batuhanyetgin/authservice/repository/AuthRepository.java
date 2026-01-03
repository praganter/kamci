package dev.batuhanyetgin.authservice.repository;

import dev.batuhanyetgin.authservice.document.AuthDocument;
import dev.batuhanyetgin.commonmodule.repository.BaseRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AuthRepository extends BaseRepository<AuthDocument, String> {

    Optional<AuthDocument> findByEMail(String eMail);
}