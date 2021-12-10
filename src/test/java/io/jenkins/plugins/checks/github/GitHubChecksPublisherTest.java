package io.jenkins.plugins.checks.github;

import org.junit.jupiter.api.Test;

import io.jenkins.plugins.util.PluginLogger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GitHubChecksPublisherTest {
    @Test
    void shouldPublish() {
        GitHubChecksContext gitHubChecksContext = mock(GitHubChecksContext.class);
        PluginLogger logger = mock(PluginLogger.class);
        GitHubChecksPublisher publisher = new GitHubChecksPublisher(gitHubChecksContext, logger);
        // private repo
        assertThat(publisher.shouldPublish(true, true, false, true)).isTrue();

        // public repo with confluentinc member and publish non-confluent pr status
        assertThat(publisher.shouldPublish(false, true, true, false)).isFalse();

        // public repo with non-confluentinc member and publish confluent pr status
        assertThat(publisher.shouldPublish(false, false, false, true)).isFalse();

        // public repo with non-confluentinc member and publish non-confluent pr status
        assertThat(publisher.shouldPublish(false, false, true, false)).isTrue();

        // public repo with confluentinc member and publish confluent pr status
        assertThat(publisher.shouldPublish(false, true, false, true)).isTrue();

        // both properties are not set
        assertThat(publisher.shouldPublish(false, false, false, false)).isTrue();

    }
}
