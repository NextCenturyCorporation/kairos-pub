package com.ncc.kairos.moirai.zeus.resources;

public enum EnvironmentTier {
    /**
     * Development is for local testing, does not run ansible or terraform.
     */
    DEVELOPMENT,
    /**
     * Testing is for testing individual branches with full capability.
     */
    TESTING,
    /**
     * Staging is for master branch outside of production.
     */
    STAGING,
    /**
     * For spinning up Zeus in production mode.
     */
    PRODUCTION,
    /**
     * Catch for if the environment tier property doesn't match anything else.
     */
    ERROR
}
