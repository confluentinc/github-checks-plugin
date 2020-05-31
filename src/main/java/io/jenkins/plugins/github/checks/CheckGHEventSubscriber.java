package io.jenkins.plugins.github.checks;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import edu.umd.cs.findbugs.annotations.Nullable;

import org.kohsuke.github.GHEvent;
import org.jenkinsci.plugins.github.extension.GHEventsSubscriber;
import org.jenkinsci.plugins.github.extension.GHSubscriberEvent;
import hudson.Extension;
import hudson.model.Item;


/**
 * This subscriber manages the {@link org.kohsuke.github.GHEvent} CHECK_RUN and CHECK_SUITE.
 */
@Extension
public class CheckGHEventSubscriber extends GHEventsSubscriber {

    private static final Logger LOGGER = Logger.getLogger(CheckGHEventSubscriber.class.getName());

    public static CheckGHEventSubscriber getInstance() {
        return GHEventsSubscriber.all().get(CheckGHEventSubscriber.class);
    }

    /**
     * {@inheritDoc}
     *
     * @return false since this class will not be used util supporting the re-run request
     */
    @Override
    protected boolean isApplicable(@Nullable Item project) {
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * @return set with CHECK_SUITE and CHECK_RUN event
     */
    @Override
    protected Set<GHEvent> events() {
        return new HashSet<>(Arrays.asList(GHEvent.CHECK_RUN, GHEvent.CHECK_SUITE));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onEvent(GHSubscriberEvent event) {
    }
}
