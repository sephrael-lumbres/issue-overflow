package com.sephrael.issueoverflow.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The prod profile's settings, read from the file: the profile can't start here without Railway's variables.
 */
class ProductionProfileTest {

    @Test
    void theDatabaseIsRailwaysPrivatePostgres() throws IOException {
        Properties prod = prodProfile();

        assertThat(prod.getProperty("spring.datasource.url")).isEqualTo("jdbc:postgresql://${PGHOST}:${PGPORT}/${PGDATABASE}");
        assertThat(prod.getProperty("spring.datasource.username")).isEqualTo("${PGUSER}");
        assertThat(prod.getProperty("spring.datasource.password")).isEqualTo("${PGPASSWORD}");
        // the public TCP proxy is for connecting from outside Railway, not for the app
        assertThat(prod.values()).noneMatch(value -> value.toString().contains("proxy.rlwy.net"));
    }

    @Test
    void httpsClientAddressesAndMailFollowRailwaysEdge() throws IOException {
        Properties prod = prodProfile();

        assertThat(prod.getProperty("server.forward-headers-strategy")).isEqualTo("native");
        assertThat(prod.getProperty("server.tomcat.remoteip.internal-proxies")).isEqualTo(".*");
        assertThat(prod.getProperty("server.servlet.session.cookie.secure")).isEqualTo("true");
        assertThat(prod.getProperty("app.rate-limit.client-ip-header")).isEqualTo("X-Real-IP");
        assertThat(prod.getProperty("app.mail.transport")).isEqualTo("resend");
    }

    private static Properties prodProfile() throws IOException {
        return PropertiesLoaderUtils.loadProperties(new ClassPathResource("application-prod.properties"));
    }
}
