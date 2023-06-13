package com.ncc.kairos.moirai.zeus.jobs;

import com.ncc.kairos.moirai.zeus.dao.ExperimentRunRepository;
import com.ncc.kairos.moirai.zeus.services.ExperimentServices;
import com.ncc.kairos.moirai.zeus.resources.ExperimentRunStatus;
import com.ncc.kairos.moirai.zeus.model.ExperimentRun;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.json.JSONArray;
import org.json.JSONObject;
import com.ncc.kairos.moirai.zeus.resources.Constants;

import java.util.Arrays;
import java.util.List;
import java.io.*;
import java.time.OffsetDateTime;

@Component
public class ExperimentJobs {

    @Autowired
    ExperimentRunRepository experimentRunRepository;

    @Autowired
    ExperimentServices experimentServices;

    @Scheduled(cron = "0 */5 * * * *")
    public void provisionNodeJob() {

        // retrieve all current experimentRuns with status requested
        List<ExperimentRun> requestedRuns = this.experimentRunRepository.findAllByStatus(ExperimentRunStatus.REQUESTED.name());

        for (ExperimentRun run : requestedRuns) {
            run.setStatus(ExperimentRunStatus.PROVISIONING_NODE.name());
            this.experimentRunRepository.save(run);

            try {
                experimentServices.provisionNodeGroups(run);
                // success if ^ does not throw error
                run.setStatus(ExperimentRunStatus.NODES_PROVISIONED.name());
                run.setAttempt(0);
            } catch (Exception err) {
                run.setAttempt(run.getAttempt() + 1);
                if (run.getAttempt() >= 3) {
                    // after three attempts, mark as failed
                    run.setStatus(ExperimentRunStatus.FAILED.name());
                } else {
                    // set as requested to try again
                    run.setStatus(ExperimentRunStatus.REQUESTED.name());
                }
            } finally {
                // save experiment on fail or success
                this.experimentRunRepository.save(run);
            }
        }

    }

    @Scheduled(cron = "0 */5 * * * *")
    public void provisionEnclaveJob() {
        // retrieve all current experimentRuns with status nodes_provisioned
        List<ExperimentRun> runs = this.experimentRunRepository.findAllByStatus(ExperimentRunStatus.NODES_PROVISIONED.name());

        for (ExperimentRun run : runs) {
            // update status
            run.setStatus(ExperimentRunStatus.PROVISIONING_ENCLAVE.name());
            this.experimentRunRepository.save(run);

            try {
                experimentServices.addControlFingerprint();
                experimentServices.destoryEnclave(run);
                experimentServices.provisionEnclave(run);
                // success if ^ does not throw error
                run.setStatus(ExperimentRunStatus.ENCLAVE_PROVISIONED.name());
                run.setAttempt(0);
            } catch (Exception err) {
                run.setAttempt(run.getAttempt() + 1);
                if (run.getAttempt() >= 3) {
                    // after three attempts, mark as failed
                    run.setStatus(ExperimentRunStatus.FAILED.name());
                } else {
                    // set status back to try again
                    run.setStatus(ExperimentRunStatus.NODES_PROVISIONED.name());
                }
            } finally {
                this.experimentRunRepository.save(run);
            }
        }
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void runEnclaveJob() {
        // grab all experiments runs with status ENCLAVE_PROVISIONED
        List<ExperimentRun> runs = this.experimentRunRepository.findAllByStatus(ExperimentRunStatus.ENCLAVE_PROVISIONED.name());

        for (ExperimentRun run : runs) {
            run.setStatus(ExperimentRunStatus.CALLING_RUN.name());
            this.experimentRunRepository.save(run);
            try {
                experimentServices.runEnclave(run);
                // success if ^ does not throw error
                run.setStatus(ExperimentRunStatus.RUNNING.name());
                run.setAttempt(0);
            } catch (Exception err) {
                run.setAttempt(run.getAttempt() + 1);
                if (run.getAttempt() >= 3) {
                    // after three attempts, mark as failed
                    run.setStatus(ExperimentRunStatus.FAILED.name());
                } else {
                    // set status back to try again
                    run.setStatus(ExperimentRunStatus.ENCLAVE_PROVISIONED.name());
                }
            } finally {
                // save run
                this.experimentRunRepository.save(run);
            }
        }
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void watchEnclaveJob() throws IOException {

        // grab all experiemnt runs with status saving and change to run complete
        List<ExperimentRun> savingRuns = this.experimentRunRepository.findAllByStatus(ExperimentRunStatus.SAVING.name());
        for (ExperimentRun run : savingRuns) {
            run.setStatus(ExperimentRunStatus.RUN_COMPLETE.name());
            this.experimentRunRepository.save(run);
        }

        // grab all experiment runs with status RUNNING
        List<ExperimentRun> runs = this.experimentRunRepository.findAllByStatus(ExperimentRunStatus.RUNNING.name());

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/ld+json");
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        HttpEntity<String> entity = new HttpEntity<String>(headers);

        JSONObject jsonObj;
        JSONArray experiments = null;
        if (runs.size() > 0) {
            try {
                String jsonString = new RestTemplate().exchange(Constants.EXPERIMENT_STATUS_URL, HttpMethod.GET, entity, String.class).getBody();
                jsonObj = new JSONObject(jsonString);
                experiments = jsonObj.getJSONArray("experiments");
            } catch (Exception error) {
                // did not properly GET json
            } finally {
                if (experiments != null) {
                    for (ExperimentRun run : runs) {
                        experimentServices.watchEnclave(run, experiments);
                    }
                }
            }
        }
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void cleanUpJob() {
        // Grab all run_complete and failed runs
        List<ExperimentRun> runs = this.experimentRunRepository.findAllByStatus(ExperimentRunStatus.RUN_COMPLETE.name());
        runs.addAll(this.experimentRunRepository.findAllByStatus(ExperimentRunStatus.FAILED.name()));

        for (ExperimentRun run : runs) {
            String originalStatus = run.getStatus();
            try {
                experimentServices.destoryEnclave(run);
                experimentServices.destroyNodeGroups(run);

                if (originalStatus.equals(ExperimentRunStatus.RUN_COMPLETE.name())) {
                    run.setStatus(ExperimentRunStatus.COMPLETE.name());
                } else {
                    run.setStatus(ExperimentRunStatus.FAILED_COMPLETE.name());
                }
                run.setAttempt(0);
                run.setCompleteTime(OffsetDateTime.now());
            
            } catch (Exception error) {
                run.setStatus(originalStatus);
            } finally {
                this.experimentRunRepository.save(run);
            }
        }
    }
}
