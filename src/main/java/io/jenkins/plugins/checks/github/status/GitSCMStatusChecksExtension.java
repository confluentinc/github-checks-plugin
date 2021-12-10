package io.jenkins.plugins.checks.github.status;

import hudson.Extension;
import hudson.plugins.git.extensions.GitSCMExtension;
import hudson.plugins.git.extensions.GitSCMExtensionDescriptor;
import hudson.util.FormValidation;
import io.jenkins.plugins.checks.status.AbstractStatusChecksProperties;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;

/**
 * Git Extension that controls {@link AbstractStatusChecksProperties} for freestyle jobs using {@link hudson.plugins.git.GitSCM}.
 */
@SuppressWarnings("PMD.DataClass")
public class GitSCMStatusChecksExtension extends GitSCMExtension implements GitHubStatusChecksConfigurations {
    private boolean skip = false;
    private boolean unstableBuildNeutral = false;
    private String name = "Jenkins";
    private boolean suppressLogs;
    private boolean skipProgressUpdates = false;
    private boolean publishConfluentIncPR = false;
    private boolean publishNonConfluentIncPR = false;

    /**
     * Constructor for stapler.
     */
    @DataBoundConstructor
    public GitSCMStatusChecksExtension() {
        super();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isSkip() {
        return skip;
    }

    @Override
    public boolean isUnstableBuildNeutral() {
        return unstableBuildNeutral;
    }

    @Override
    public boolean isSuppressLogs() {
        return suppressLogs;
    }

    @Override
    public boolean isSkipProgressUpdates() {
        return skipProgressUpdates;
    }

    @Override
    public boolean isPublishConfluentIncPR() {
        return publishConfluentIncPR;
    }

    @Override
    public boolean isPublishNonConfluentIncPR() {
        return publishNonConfluentIncPR;
    }

    @DataBoundSetter
    public void setSkipProgressUpdates(boolean skipProgressUpdates) {
        this.skipProgressUpdates = skipProgressUpdates;
    }

    /**
     * Set the name of the status checks.
     *
     * @param name
     *         name of the checks
     */
    @DataBoundSetter
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Set if skip publishing status checks.
     *
     * @param skip
     *         true if skip
     */
    @DataBoundSetter
    public void setSkip(final boolean skip) {
        this.skip = skip;
    }

    @DataBoundSetter
    public void setUnstableBuildNeutral(final boolean unstableBuildNeutral) {
        this.unstableBuildNeutral = unstableBuildNeutral;
    }

    @DataBoundSetter
    public void setSuppressLogs(final boolean suppressLogs) {
        this.suppressLogs = suppressLogs;
    }

    @DataBoundSetter
    public void setPublishConfluentIncPR(final boolean publishConfluentIncPR) {
        this.publishConfluentIncPR = publishConfluentIncPR;
    }

    @DataBoundSetter
    public void setPublishNonConfluentIncPR(final boolean publishNonConfluentIncPR) {
        this.publishNonConfluentIncPR = publishNonConfluentIncPR;
    }

    /**
     * Descriptor implementation for {@link GitSCMStatusChecksExtension}.
     */
    @Extension
    public static class DescriptorImpl extends GitSCMExtensionDescriptor {
        /**
         * Returns the display name.
         *
         * @return "Status Checks Properties"
         */
        @Override
        public String getDisplayName() {
            return "Status Checks Properties";
        }

        /**
         * Checks if the name of status checks is valid.
         *
         * @param name
         *         name of status checks
         * @return ok if the name is not empty
         */
        public FormValidation doCheckName(@QueryParameter final String name) {
            if (StringUtils.isBlank(name)) {
                return FormValidation.error("Name should not be empty!");
            }

            return FormValidation.ok();
        }
    }
}
