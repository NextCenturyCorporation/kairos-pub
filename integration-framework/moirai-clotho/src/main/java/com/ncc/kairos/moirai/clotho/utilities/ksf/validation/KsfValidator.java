package com.ncc.kairos.moirai.clotho.utilities.ksf.validation;

import java.util.List;

public abstract class KsfValidator {

    /**
     * Label for errors.
     */
    public static final String FATAL = "FATAL";
    /**
     * Label for errors.
     */
    public static final String ERROR = "ERROR";
    /**
     * Label for warnings.
     */
    public static final String WARNING = "WARNING";

    protected boolean isTA2;

    protected KsfValidator() {
        isTA2 = true;
    }

    /**
     * Reset the validator with a new validation object.
     * @param obj an object model for validation
     * @throws UnsupportedOperationException if the validator implementation does not support models of the specified type
     */
    public abstract void setModel(Object obj) throws UnsupportedOperationException;

    /**
     * Validate SDF input.
     * Error if any required elements are missing:  @context, @id, sdfVersion, and version.
     * Error if sdfVersion does not equal {SDF_VERSION}.
     * (TA2): Error if additional required elements are missing: ceID, instances, provenanceData (Task1 only), task2.
     * (TA2): Error if events, entities, or relations is present.
     * (TA2): Error if @id does not follow naming convention.
     * (TA2): Error if there are any duplicate 5-digit IDs within the @ids of the array of instances objects.
     * @return a List of errors, one for each validation error or warning
     */
    public abstract List<String> validate();

    /**
     * Return whether this validator is processing TA2 input.
     * @return True if this validator is processing TA2 input
     */
    public boolean isTA2() {
        return isTA2;
    }

    /**
     * Return whether or not the input is valid.  Error messages cannot be retrieved without re-running validation.
     * @return true if the input is valid, otherwise false
     */
    public boolean isValid() {
        return validate().isEmpty();
    }

}
