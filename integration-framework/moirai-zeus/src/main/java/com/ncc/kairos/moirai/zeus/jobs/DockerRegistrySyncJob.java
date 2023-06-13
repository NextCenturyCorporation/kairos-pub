package com.ncc.kairos.moirai.zeus.jobs;

import com.ncc.kairos.moirai.zeus.services.DockerService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

@Component
public class DockerRegistrySyncJob {

    private static final Logger LOGGER = LoggerFactory.getLogger(DockerRegistrySyncJob.class);

    @Autowired
    DockerService dockerService;


    @Scheduled(cron = "0 0 */6 * * *")
    public void mainCronJob() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        Date now = new Date();
        String strDate = sdf.format(now);
        LOGGER.info("Java cron job expression:: " + strDate);
        dockerService.discoverRegistries();
        dockerService.refreshAllRegistries();
    }
}
