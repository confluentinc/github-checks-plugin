package io.jenkins.plugins.checks.github;

import edu.hm.hafner.util.VisibleForTesting;
import io.jenkins.plugins.checks.api.ChecksDetails;
import io.jenkins.plugins.checks.api.ChecksPublisher;
import io.jenkins.plugins.checks.github.status.GitHubStatusChecksProperties;
import io.jenkins.plugins.util.PluginLogger;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.github_branch_source.Connector;
import org.jenkinsci.plugins.github_branch_source.GitHubAppCredentials;
import org.kohsuke.github.GHCheckRun;
import org.kohsuke.github.GHUser;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHCheckRunBuilder;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;

/**
 * A publisher which publishes GitHub check runs.
 */
public class GitHubChecksPublisher extends ChecksPublisher {
    private static final String GITHUB_URL = "https://api.github.com";
    private static final String GITHUB_ORG = "confluentinc";
    private static final Logger SYSTEM_LOGGER = Logger.getLogger(GitHubChecksPublisher.class.getName());

    private final GitHubChecksContext context;
    private final PluginLogger buildLogger;
    private final String gitHubUrl;

    /**
     * {@inheritDoc}.
     *
     * @param context
     *         a context which contains SCM properties
     */
    public GitHubChecksPublisher(final GitHubChecksContext context, final PluginLogger buildLogger) {
        this(context, buildLogger, GITHUB_URL);
    }

    GitHubChecksPublisher(final GitHubChecksContext context, final PluginLogger buildLogger, final String gitHubUrl) {
        super();

        this.context = context;
        this.buildLogger = buildLogger;
        this.gitHubUrl = gitHubUrl;
    }

    /**
     * Publishes a GitHub check run.
     *
     * @param details
     *         the details of a check run
     */
    @Override
    public void publish(final ChecksDetails details) {
        try {
            GitHubAppCredentials credentials = context.getCredentials();
            GitHub gitHub = Connector.connect(StringUtils.defaultIfBlank(credentials.getApiUri(), gitHubUrl),
                    credentials);
            boolean shouldPublish = true;
            String contributor = context.getContributor();
            if (!contributor.isEmpty()) {
                buildLogger.log("contributor name is " + contributor);
                String repository = context.getRepository();
                boolean isPrivate = gitHub.getRepository(repository).isPrivate();
                GitHubStatusChecksProperties gitHubStatusChecksProperties = new GitHubStatusChecksProperties();
                boolean publishNonConfluentIncPR = gitHubStatusChecksProperties.isPublishNonConfluentIncPR(context.getJob());
                boolean publishConfluentIncPR = gitHubStatusChecksProperties.isPublishConfluentIncPR(context.getJob());
                buildLogger.log("publishNonConfluentIncPR is " + publishNonConfluentIncPR);
                buildLogger.log("publishConfluentIncPR is " + publishConfluentIncPR);
                boolean inGithubOrg = orgCheck(contributor, gitHub, isPrivate);
                shouldPublish = shouldPublish(isPrivate, inGithubOrg, publishNonConfluentIncPR, publishConfluentIncPR);
                buildLogger.log("publish github status : " + shouldPublish);
            }
            if (shouldPublish) {
                GitHubChecksDetails gitHubDetails = new GitHubChecksDetails(details);

                Optional<Long> existingId = context.getId(gitHubDetails.getName());

                final GHCheckRun run;

                if (existingId.isPresent()) {
                    run = getUpdater(gitHub, gitHubDetails, existingId.get()).create();
                }
                else {
                    run = getCreator(gitHub, gitHubDetails).create();
                }

                context.addActionIfMissing(run.getId(), gitHubDetails.getName());

                buildLogger.log("GitHub check (name: %s, status: %s) has been published.", gitHubDetails.getName(),
                                gitHubDetails.getStatus());
                SYSTEM_LOGGER.fine(format("Published check for repo: %s, sha: %s, job name: %s, name: %s, status: %s",
                                context.getRepository(),
                                context.getHeadSha(),
                                context.getJob().getFullName(),
                                gitHubDetails.getName(),
                                gitHubDetails.getStatus()).replaceAll("[\r\n]", ""));
            }
        }
        catch (IOException e) {
            String message = "Failed Publishing GitHub checks: ";
            SYSTEM_LOGGER.log(Level.WARNING, (message + details).replaceAll("[\r\n]", ""), e);
            buildLogger.log(message + e);
        }
    }

    @VisibleForTesting
    GHCheckRunBuilder getUpdater(final GitHub github, final GitHubChecksDetails details, final long checkId) throws IOException {
        GHCheckRunBuilder builder = github.getRepository(context.getRepository())
                .updateCheckRun(checkId);

        return applyDetails(builder, details);
    }

    @VisibleForTesting
    GHCheckRunBuilder getCreator(final GitHub gitHub, final GitHubChecksDetails details) throws IOException {
        GHCheckRunBuilder builder = gitHub.getRepository(context.getRepository())
                .createCheckRun(details.getName(), context.getHeadSha())
                .withStartedAt(details.getStartedAt().orElse(Date.from(Instant.now())));

        return applyDetails(builder, details);
    }

    private GHCheckRunBuilder applyDetails(final GHCheckRunBuilder builder, final GitHubChecksDetails details) {
        builder
                .withStatus(details.getStatus())
                .withExternalID(context.getJob().getFullName())
                .withDetailsURL(details.getDetailsURL().orElse(context.getURL()));

        if (details.getConclusion().isPresent()) {
            builder.withConclusion(details.getConclusion().get())
                    .withCompletedAt(details.getCompletedAt().orElse(Date.from(Instant.now())));
        }

        details.getOutput().ifPresent(builder::add);
        details.getActions().forEach(builder::add);

        return builder;
    }

    private boolean orgCheck(String username, GitHub github, boolean isPrivate) throws IOException {
        // skip org check if the repo is private
        if (isPrivate) {
            return true;
        }
        try {
            GHUser user = github.getUser(username);
            GHOrganization org = github.getOrganization(GITHUB_ORG);
            boolean inGithubOrg = user.isMemberOf(org);
            buildLogger.log("inGithubOrg: " + inGithubOrg);
            return inGithubOrg;
        }
        catch (IOException e) {
            buildLogger.log("Failed to connect to GitHub " + e);
            return false;
        }
    }

    /**
     * Whether to publish github status.
     *
     * @param isPrivate
     *         whether repo is private
     * @param inGithubOrg
     *         whether contributor is member of confluentinc
     * @param publishNonConfluentIncPR
     *         publish non-confluentinc PR status only
     * @param publishConfluentIncPR
     *         publish confluentinc PR status only
     * @return true if we should publish github status
     */
    @VisibleForTesting
    boolean shouldPublish(boolean isPrivate,
                         boolean inGithubOrg,
                         boolean publishNonConfluentIncPR,
                         boolean publishConfluentIncPR) {
        if (isPrivate) {
            // if repo is private, we should publish
            return true;
        }
        else if (!publishConfluentIncPR && !publishNonConfluentIncPR) {
            return true;
        }
        else {
            return (inGithubOrg && publishConfluentIncPR) || (!inGithubOrg && publishNonConfluentIncPR);
        }
    }

}
