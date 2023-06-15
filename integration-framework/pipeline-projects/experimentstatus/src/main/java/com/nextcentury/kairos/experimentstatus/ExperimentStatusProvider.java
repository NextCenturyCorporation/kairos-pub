package com.nextcentury.kairos.experimentstatus;

import com.nextcentury.kairos.experimentstatus.tuple.ExperimentStatus;
import com.nextcentury.kairos.experimentstatus.tuple.EnclavePodStatus;
import com.nextcentury.kairos.experimentstatus.tuple.EnclaveServiceStatus;
import com.nextcentury.restclient.RestClient;
import com.nextcentury.restclient.RestClientResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

public class ExperimentStatusProvider {

    private static final Logger logger = LogManager.getLogger(ExperimentStatusProvider.class);

    private static final String ENCLAVE = "-enclave";

    private static final String SVC_CLUSTER_LOCAL = ".svc.cluster.local";
    private static final String PERFORMER_SERVICE_IMPL_KEY = "service-impl";
    private static final String PERFORMER_MAIN_SERVICE = "main";

    private static final String HTTP = "http://";

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.setSerializationInclusion(Inclusion.NON_NULL);
        mapper.setSerializationInclusion(Inclusion.NON_EMPTY);
    }

    public ExperimentStatusProvider() {
        super();
    }

    public List<ExperimentStatus> getAllExperimentsStatus() {

        // get list of experiments running in this cluster
        // get experiment identified
        // for each namespace, get the experiment status
        List<ExperimentStatus> experimentsStatus = ShellCmdRunner
            .runCommand("kubectl get namespaces | grep \"-enclave\"")
            .stream()
            .map(ns -> getExperimentStatus(ns))
            .collect(Collectors.toList());

        return experimentsStatus;
    }

    public ExperimentStatus getExperimentStatus(String namespaceInfo) {
        ExperimentStatus expStatus = new ExperimentStatus();

        String[] infoParts = namespaceInfo.trim().split("\\s+");
        String namespace = infoParts[0];
        expStatus.setNamespace(namespace);
        expStatus.setAge(infoParts[2]);

        logger.debug("Namespace - " + namespace);

        String[] parts = namespace.split("-");
        String experimentName = String.join("-", Arrays.copyOfRange(parts,1,parts.length-1));
        expStatus.setExperiment(experimentName);
        logger.debug("Experiment - " + experimentName);

        expStatus.setPods(getPodsStatus(namespace));
        expStatus.setServices(getServices(namespace));

        expStatus.setStatus("Unknown");

        logger.debug("Finding status of main performer pod");
        expStatus.getServices().forEach(service -> {
            if (service.getUri().contains("-main.")) {
                String fqdnUri = HTTP+service.getUri()+"/kairos/status";
                RestClientResponse response = new RestClient().get(fqdnUri);
                if (response.getStatusCode() == HttpStatus.SC_OK) {
                    expStatus.setStatus(response.getResponse());
                } else {
                    expStatus.setStatus("UNRESPONSIVE");
                }
            }
        });

        return expStatus;
    }

    private List<EnclaveServiceStatus> getServices(String namespace) {
        return ShellCmdRunner
            .runCommand("kubectl get svc -n "+namespace+" --no-headers | awk \'{print $1 \",\" $5}\'")
            .stream().map(info -> {
                EnclaveServiceStatus service = new EnclaveServiceStatus();
                String name = info.split(",")[0];
                String port = info.split(",")[1].split("/")[0];
                
                if (!StringUtils.isNumeric(port)) {
                    return null;
                }
                service.setUri(namespace,name,port);

                return service;
            })
            .filter(ess -> ess != null)
            .collect(Collectors.toList());
    }

    private Map<String, List<String>> getPodsStatus(String namespace) {
        Map<String, List<EnclavePodStatus>> statusMap = ShellCmdRunner
            .runCommand("kubectl get pods -n "+namespace+" --no-headers | awk \'{print $1 \" : \" $3}\'")
            .stream()
            .map(performer -> {
                String[] podStatusParts = performer.split(":");
                String podname = podStatusParts[0].trim();
                String k8spodStatus = podStatusParts[1].trim();

                EnclavePodStatus podStatus = new EnclavePodStatus();
                podStatus.setPodname(podname);
                podStatus.setPodstatus(k8spodStatus);
                return podStatus;
            })
            .collect(Collectors.groupingBy(EnclavePodStatus::getPodstatus));

        Map<String, List<String>> status = new HashMap<>();

        for (String key : statusMap.keySet() ) {
            List<String> podList = statusMap.get(key).stream().map(pod -> pod.getPodname()).collect(Collectors.toList());
            status.put(key, podList);
        }

        return status;
    }
}
