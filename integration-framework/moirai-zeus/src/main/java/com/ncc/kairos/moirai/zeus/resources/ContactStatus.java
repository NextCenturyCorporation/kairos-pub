package com.ncc.kairos.moirai.zeus.resources;

public enum ContactStatus {
    /**
     * The initial state of a contact request before an admin can answer it.
     */
    UNANSWERED,
    /**
     * A contact request that has been answered but still needs further communication.
     */
    ANSWERED,
    /**
     * For when the request is closed and needs no further communication.
     */
    CLOSED
}
