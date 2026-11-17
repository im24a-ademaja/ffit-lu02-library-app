package ch.bzz.controller;

import ch.bzz.AccountRepository;
import ch.bzz.Project;
import ch.bzz.ProjectRepository;
import ch.bzz.generated.api.AccountApi;
import ch.bzz.security.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;

@RestController
public class AccountApiController implements AccountApi {

    private final AccountRepository accountRepository;
    private final ProjectRepository projectRepository;
    private final JwtUtil jwtUtil;

    public AccountApiController(
            AccountRepository accountRepository,
            ProjectRepository projectRepository,
            JwtUtil jwtUtil) {
        this.accountRepository = accountRepository;
        this.projectRepository = projectRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public ResponseEntity<List<ch.bzz.generated.model.Account>> getAccounts() {
        String projectName;
        try {
            projectName = jwtUtil.getProjectNameFromAuthorization(getAuthorizationHeader());
        } catch (JwtException | IllegalArgumentException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (projectName == null || projectName.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<Project> project = projectRepository.findById(projectName);
        if (project.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<ch.bzz.generated.model.Account> accounts =
                accountRepository.findByProject(project.get()).stream()
                        .map(account -> new ch.bzz.generated.model.Account().id(account.getId()))
                        .toList();
        return ResponseEntity.ok(accounts);
    }

    private String getAuthorizationHeader() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
            return null;
        }

        HttpServletRequest request = servletAttributes.getRequest();
        return request.getHeader("Authorization");
    }
}
