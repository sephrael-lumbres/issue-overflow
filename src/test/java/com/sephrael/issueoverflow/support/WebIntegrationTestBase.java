package com.sephrael.issueoverflow.support;

import com.sephrael.issueoverflow.entity.*;
import com.sephrael.issueoverflow.repository.*;
import com.sephrael.issueoverflow.service.AWSFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

/**
 * Base class for the full-stack regression tests.
 * <p>
 * These tests commit their data (nothing rolls back), so they use their own
 * Postgres container, separate from the rolled-back repository tests in
 * PostgresTestBase, and neither suite can see the other's rows. Fixtures use
 * unique random suffixes for the same reason. Subclasses must not add Spring
 * test annotations, so that they all share one Spring context.
 * <p>
 * S3 calls go to an S3Mock container (path-style, signatures not checked)
 * that starts with the bucket named in application-test.properties.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class WebIntegrationTestBase {

    protected static final String PASSWORD = "test-password";
    protected static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine");
    static final GenericContainer<?> S3_MOCK = new GenericContainer<>(DockerImageName.parse("adobe/s3mock:5.2.3"))
            .withExposedPorts(9090)
            .withEnv("COM_ADOBE_TESTING_S3MOCK_STORE_INITIAL_BUCKETS", "test-bucket")
            .waitingFor(Wait.forHttp("/favicon.ico").forPort(9090));
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    static {
        POSTGRES.start();
        S3_MOCK.start();
    }

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected OrganizationRepository organizationRepository;
    @Autowired
    protected UserRepository userRepository;
    @Autowired
    protected RoleRepository roleRepository;
    @Autowired
    protected ProjectRepository projectRepository;
    @Autowired
    protected IssueRepository issueRepository;
    @Autowired
    protected AWSFileRepository awsFileRepository;
    @Autowired
    protected AWSFileService awsFileService;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("app.s3.endpoint",
                () -> "http://" + S3_MOCK.getHost() + ":" + S3_MOCK.getMappedPort(9090));
    }

    protected static String uniqueSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    protected void inTransaction(Runnable work) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> work.run());
    }

    protected Organization createOrganization() {
        String suffix = uniqueSuffix();
        Organization organization = new Organization();
        organization.setName("Org " + suffix);
        organization.setAccessKey("K" + suffix);
        return organizationRepository.save(organization);
    }

    // Creates an enabled user with PASSWORD; organization may be null.
    protected User createUser(Organization organization, String roleName) {
        Role role = roleRepository.findByName(roleName);
        assertThat(role).as("role '%s' seeded by migration V2", roleName).isNotNull();
        String suffix = uniqueSuffix();
        User user = new User();
        user.setEmail("user-" + suffix.toLowerCase() + "@example.test");
        user.setPassword(ENCODER.encode(PASSWORD));
        user.setFirstName("Test");
        user.setLastName("User " + suffix);
        user.setEnabled(true);
        user.setRole(role);
        user.setOrganization(organization);
        return userRepository.save(user);
    }

    protected Project createProject(Organization organization, User member) {
        String suffix = uniqueSuffix();
        Project project = new Project();
        project.setName("Project " + suffix);
        project.setIdentifier("P" + suffix);
        project.setColor("primary");
        project.setOrganization(organization);
        project.addUser(member);
        return projectRepository.save(project);
    }

    // assignee may be null. The issue key is "<identifier>-<number>".
    protected Issue createIssue(Project project, User author, User assignee,
                                String type, String status, String priority, int number) {
        Issue issue = new Issue();
        issue.setTitle("Search returns stale results " + uniqueSuffix());
        issue.setDescription("Steps to reproduce: search twice for the same term.");
        issue.setType(type);
        issue.setStatus(status);
        issue.setPriority(priority);
        issue.setEstimatedHours(3);
        issue.setDueDate(LocalDate.now().plusDays(7));
        issue.setIssueKey(project.getIdentifier() + "-" + number);
        issue.setProject(project);
        issue.setUser(author);
        issue.setOrganization(project.getOrganization());
        issue.setAssignedTo(assignee);
        return issueRepository.save(issue);
    }

    // An organization with one Project Manager, one project, and one Open/High Bug assigned to that manager.
    protected Fixture createFixture() {
        Organization organization = createOrganization();
        User projectManager = createUser(organization, "Project Manager");
        Project project = createProject(organization, projectManager);
        Issue issue = createIssue(project, projectManager, projectManager, "Bug", "Open", "High", 1);
        return new Fixture(organization, projectManager, project, issue);
    }

    // Commits a title change the way the edit form does, so Envers records a revision.
    protected void editIssueTitle(Long issueId, String newTitle, User editor) {
        inTransaction(() -> {
            Issue issue = issueRepository.findById(issueId).orElseThrow();
            issue.setTitle(newTitle);
            issue.setUpdatedBy(editor);
        });
    }

    // Uploads one attachment to the fixture's issue the way the issue page does, and returns its record.
    protected AWSFile uploadAttachment(Fixture fixture, String fileName, String contentType, byte[] content) {
        MockMultipartFile file = new MockMultipartFile("files", fileName, contentType, content);
        inTransaction(() -> {
            User author = userRepository.findById(fixture.projectManager.getId()).orElseThrow();
            Issue issue = issueRepository.findById(fixture.issue.getId()).orElseThrow();
            awsFileService.uploadFile(file, author, issue, false);
        });
        List<AWSFile> files = awsFileRepository.findByIssue(fixture.issue);
        assertThat(files).hasSize(1);
        return files.get(0);
    }

    // Logs in through the real login form and returns the authenticated session.
    protected MockHttpSession login(String email) throws Exception {
        MvcResult result = mockMvc.perform(formLogin("/login").userParameter("email").user(email).password(PASSWORD))
                .andExpect(redirectedUrl("/dashboard"))
                .andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    protected static final class Fixture {
        public final Organization organization;
        public final User projectManager;
        public final Project project;
        public final Issue issue;

        Fixture(Organization organization, User projectManager, Project project, Issue issue) {
            this.organization = organization;
            this.projectManager = projectManager;
            this.project = project;
            this.issue = issue;
        }
    }
}
