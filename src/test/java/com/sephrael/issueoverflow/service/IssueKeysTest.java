package com.sephrael.issueoverflow.service;

import com.sephrael.issueoverflow.support.WebIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class IssueKeysTest extends WebIntegrationTestBase {

    @Autowired
    private IssueKeys issueKeys;

    // Requests at the same time each get their own number. The fixture's issue already has number 1.
    @Test
    void keysTakenAtTheSameTimeAreAllDifferent() throws Exception {
        Fixture fixture = createFixture();
        Callable<String> nextKey = () -> issueKeys.nextKey(fixture.project);
        List<Future<String>> futures = new ArrayList<>();

        try (ExecutorService executor = Executors.newFixedThreadPool(8)) {
            for (int i = 0; i < 40; i++)
                futures.add(executor.submit(nextKey));
        }

        Set<String> keys = new HashSet<>();
        for (Future<String> future : futures)
            keys.add(future.get());
        assertThat(keys).containsExactlyInAnyOrderElementsOf(
                IntStream.rangeClosed(2, 41).mapToObj(n -> fixture.project.getIdentifier() + "-" + n).toList());
    }
}
