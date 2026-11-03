package ch.bzz.controller;

import ch.bzz.Project;
import ch.bzz.ProjectRepository;
import ch.bzz.generated.api.ProjectApi;
import ch.bzz.generated.model.LoginProject200Response;
import ch.bzz.generated.model.LoginRequest;
import ch.bzz.security.JwtUtil;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProjectApiController implements ProjectApi {

    private final ProjectRepository projectRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final JwtUtil jwtUtil;

    public ProjectApiController(ProjectRepository projectRepository, JwtUtil jwtUtil) {
        this.projectRepository = projectRepository;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public ResponseEntity<Void> createProject(LoginRequest loginRequest) {
        if (!isValid(loginRequest)) {
            return ResponseEntity.badRequest().build();
        }

        if (projectRepository.existsById(loginRequest.getProjectName())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        Project project = new Project(
                loginRequest.getProjectName(),
                encoder.encode(loginRequest.getPassword()));
        try {
            projectRepository.save(project);
        } catch (DataIntegrityViolationException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Override
    public ResponseEntity<LoginProject200Response> loginProject(LoginRequest loginRequest) {
        if (!isValid(loginRequest)) {
            return ResponseEntity.badRequest().build();
        }

        return projectRepository.findById(loginRequest.getProjectName())
                .filter(project -> encoder.matches(
                        loginRequest.getPassword(),
                        project.getPasswordHash()))
                .map(project -> ResponseEntity.ok(
                        new LoginProject200Response(
                                jwtUtil.generateToken(project.getProjectName()))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    private boolean isValid(LoginRequest loginRequest) {
        return loginRequest != null
                && loginRequest.getProjectName() != null
                && !loginRequest.getProjectName().isBlank()
                && loginRequest.getProjectName().length() <= 255
                && loginRequest.getPassword() != null
                && !loginRequest.getPassword().isBlank();
    }
}
