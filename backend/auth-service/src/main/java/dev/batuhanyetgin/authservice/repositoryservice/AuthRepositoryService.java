package dev.batuhanyetgin.authservice.repositoryservice;

import dev.batuhanyetgin.authservice.document.AuthDocument;
import dev.batuhanyetgin.authservice.repository.AuthRepository;
import dev.batuhanyetgin.commonmodule.exception.BaseException;
import dev.batuhanyetgin.commonmodule.repository.BaseRepositoryService;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthRepositoryService extends BaseRepositoryService<AuthDocument> {

    private final AuthRepository authRepository;

    public AuthRepositoryService(final AuthRepository authRepository) {
        super(authRepository);
        this.authRepository = authRepository;
    }

    public AuthDocument findByEMail(final String email) {
        return authRepository.findByEmail(email)
                .orElseThrow(() -> BaseException.notFound("error.auth.user_not_found"));
    }

    public Optional<AuthDocument> findByEmailOptional(final String email) {
        return authRepository.findByEmail(email);
    }

    public boolean existsByEmail(final String email) {
        return authRepository.existsByEmail(email);
    }

    public boolean existsById(final String id) {
        return authRepository.existsById(id);
    }

    public void delete(final String id) {
        authRepository.deleteById(id);
    }
}