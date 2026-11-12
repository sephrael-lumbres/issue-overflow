package com.sephrael.issueoverflow.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryFactoryBean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Turns on the Spring Data repositories with Envers revision support (IssueRepository's change history).
 * <p>
 * It sits in its own class rather than on the application class: test slices read the application class's
 * annotations but skip @Configuration classes, so a @WebMvcTest no longer tries to build repositories without a
 * database. @DataJpaTest slices get the same repositories from Spring Boot's autoconfiguration, which also uses
 * the Envers factory when Spring Data Envers is on the classpath.
 */
@Configuration(proxyBeanMethods = false)
@EnableJpaRepositories(basePackages = "com.sephrael.issueoverflow.repository", repositoryFactoryBeanClass = EnversRevisionRepositoryFactoryBean.class)
public class JpaConfig {
}
