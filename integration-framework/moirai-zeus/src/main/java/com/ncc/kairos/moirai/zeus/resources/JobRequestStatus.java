package com.ncc.kairos.moirai.zeus.resources;

public enum JobRequestStatus {
    /**
     * REQUESTED is for a new job.
     */
    REQUESTED,
    /**
     * RUNNING is for active jobs.
     */
    RUNNING,
    /**
     * COMPLETED is for successfully finished jobs.
     */
    COMPLETED,
    /**
     * FAILED is for failed finished jobs that still had attempts left.
     */
    FAILED,
    /**
     * FAILED_FAIL is for jobs that have reached their max attempts.
     */
    FAILED_FINAL
}
