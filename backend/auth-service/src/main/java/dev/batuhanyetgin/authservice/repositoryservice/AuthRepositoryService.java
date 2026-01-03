package dev.batuhanyetgin.authservice.repositoryservice;

import dev.batuhanyetgin.authservice.document.AuthDocument;
import dev.batuhanyetgin.authservice.repository.AuthRepository;
import dev.batuhanyetgin.commonmodule.repository.BaseRepositoryService;
import org.springframework.stereotype.Service;

@Service
public class AuthRepositoryService extends BaseRepositoryService<AuthDocument> {

    private final AuthRepository authRepository;

    public AuthRepositoryService(final AuthRepository authRepository) {
        super(authRepository);
        this.authRepository = authRepository;
    }

    public AuthDocument findByEMail(final String eMail) {
        return authRepository.findByEMail(eMail)
                .orElseThrow(() -> new IllegalArgumentException("Could not find user with eMail: " + eMail));
    }
}