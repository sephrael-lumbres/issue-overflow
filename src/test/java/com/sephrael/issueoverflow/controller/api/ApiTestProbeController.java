package com.sephrael.issueoverflow.controller.api;

import com.sephrael.issueoverflow.controller.OrganizationScope;
import com.sephrael.issueoverflow.entity.Issue;
import com.sephrael.issueoverflow.entity.User;
import com.sephrael.issueoverflow.repository.IssueRepository;
import com.sephrael.issueoverflow.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

/**
 * Test-only JSON endpoints that exercise the API's plumbing the way a real endpoint would: an organization- and
 * membership-scoped lookup returning a record (not the entity), a validated request body, and the error paths.
 * It lives under src/test, in the API package, so component scanning picks it up in the full-stack test contexts
 * only; the application itself has no /api endpoints yet.
 */
@RestController
@RequestMapping("/api/probe")
public class ApiTestProbeController {

    private final UserRepository userRepository;
    private final IssueRepository issueRepository;

    public ApiTestProbeController(UserRepository userRepository, IssueRepository issueRepository) {
        this.userRepository = userRepository;
        this.issueRepository = issueRepository;
    }

    public record ProbeIssue(Long id, String title) {
    }

    public record ProbeEcho(@NotBlank @Size(max = 20) String text) {
    }

    @GetMapping("/issues/{id}")
    public ProbeIssue issue(@PathVariable("id") long id, Principal principal) {
        User currentUser = userRepository.findByEmail(principal.getName());
        Issue issue = issueRepository.findByIdAndOrganization(id, currentUser.getOrganization());

        if(issue == null || !OrganizationScope.isMember(currentUser, issue.getProject()))
            throw OrganizationScope.notFound();

        return new ProbeIssue(issue.getId(), issue.getTitle());
    }

    @PostMapping("/echo")
    public ProbeEcho echo(@Valid @RequestBody ProbeEcho request) {
        return request;
    }

    @GetMapping("/forbidden")
    public void forbidden() {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    @GetMapping("/failure")
    public void failure() {
        throw new IllegalStateException("probe failure with internal detail");
    }
}
