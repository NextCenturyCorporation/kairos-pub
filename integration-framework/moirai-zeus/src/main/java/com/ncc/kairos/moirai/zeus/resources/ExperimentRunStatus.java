package com.ncc.kairos.moirai.zeus.resources;
public enum ExperimentRunStatus {
    /**
     * Run Requested.
     */
    REQUESTED,
    /**
     * Creating Node Groups.
     */
    PROVISIONING_NODE,
    /**
     * Node groups done being set up.
     */
    NODES_PROVISIONED,
    /**
     * Setting up Enclave.
     */
    PROVISIONING_ENCLAVE,
    /**
     * Enclave done being set up.
     */
    ENCLAVE_PROVISIONED,
    /**
     * Called Running.
     */
    CALLING_RUN,
    /**
    * Run in progress.
    */
    RUNNING,
    /**
     * Saving.
     */
    SAVING,
    /**
     * Post Run.
     */
    RUN_COMPLETE,
    /**
     * Tearing down enclave.
     */
    DESTROYING_ENCLAVE,
    /**
     * Tearing down nodegroups.
     */
    DESTROYING_NODEGROUPS,
    /**
     * Run Failed 3 Times.
     */
    FAILED,
    /**
     * Run was successful and cleaned up.
     */
    COMPLETE,
    /**
     * Run failed and cleaned up.
     */
    FAILED_COMPLETE
}
