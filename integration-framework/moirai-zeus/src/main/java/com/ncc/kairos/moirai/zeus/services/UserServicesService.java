package com.ncc.kairos.moirai.zeus.services;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import com.ncc.kairos.moirai.zeus.dao.JwtUserRepository;
import com.ncc.kairos.moirai.zeus.dao.UserServiceRepository;
import com.ncc.kairos.moirai.zeus.model.JwtUser;
import com.ncc.kairos.moirai.zeus.model.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import static com.ncc.kairos.moirai.zeus.resources.Constants.OPEN_ACCESS_KEYWORD;


/**
 * Service layer for storing and accessing user and admin services.
 * @author a developer
 * @version 0.1
 */
@org.springframework.stereotype.Service
public class UserServicesService {

    @Autowired
    private UserServiceRepository userServiceRepository;

    @Autowired
    private JwtUserRepository jwtUserRepository;

    @Autowired
    private KairosUserService kairosUserService;

    /**
     * Obtain all Services the User should be able to access, except for the ones that don't
     * match the filters passed in as parameters.
     * @param name of the services to return
     * @param jwtUser user that should be able to access the services
     * @return list of services
     */
    public List<Service> getFilteredServiceList(String name, JwtUser jwtUser) {
        jwtUser = this.kairosUserService.findUserByUsername(jwtUser.getUsername());
        List<Service> combinedList = getFilteredPrivateServicesForUser(name, jwtUser);
        if (StringUtils.isEmpty(name)) {
            combinedList.addAll(this.userServiceRepository.findByAccess(OPEN_ACCESS_KEYWORD));
        } else {
            combinedList.addAll(this.userServiceRepository.findByNameAndAccess(name, OPEN_ACCESS_KEYWORD));
        }
        return combinedList.stream().collect(Collectors.toCollection(() ->
                new TreeSet<>(Comparator.comparing(Service::getId)))).stream().collect(Collectors.toList());
    }

    public void updateServiceStatus(Service updatedService) {
        try {
            Service service = this.userServiceRepository.findById(updatedService.getId()).get();
            if (!StringUtils.isEmpty(updatedService.getStatus())) {
                service.setStatus(updatedService.getStatus());
            }

            //TODO determine if this should really be adding updatedServices's instance list instead of replacing it
            if (!CollectionUtils.isEmpty(updatedService.getAwsInstances())) {
                service.setAwsInstances(updatedService.getAwsInstances());
            }

            if (!CollectionUtils.isEmpty(updatedService.getEndpoints())) {
                service.setEndpoints(updatedService.getEndpoints());
            }

            if (!CollectionUtils.isEmpty(updatedService.getDownloads())) {
                service.setDownloads(updatedService.getDownloads());
            }

            if (!StringUtils.isEmpty(updatedService.getTeamName())) {
                service.setTeamName(updatedService.getTeamName());
            }

            this.userServiceRepository.save(service);
        }  catch (NoSuchElementException e) {
            System.out.println("No such element to update");
            throw e;
        }
    }

    // Saves modified details and access status of a service
    public void updateServiceDetails(Service updatedService) {
        try {
            Service service = this.userServiceRepository.findById(updatedService.getId()).get();
            service.setDetails(updatedService.getDetails());
            service.setAccess(updatedService.getAccess());

            this.userServiceRepository.save(service);
        } catch (NoSuchElementException e) {
            System.out.println("Cannot find element to update");
        }
    }

    /**
     * Persist Services.
     */
    public Service saveNewService(Service service, JwtUser user) {
        user = this.kairosUserService.findUserByUsername(user.getUsername());
        Service servicePersisted = new Service();
         // The request may include values that exist in the model but dont make sense
        // in this context because they couldn't possibly be populated yet.
        // I'm setting these to null
        service.setId(null);
        service.setDateReceived(LocalDate.now());
        service.setAwsInstances(null);
        service.setEndpoints(null);
        service.setDownloads(null);
        
        // replace spaces with underscores for AWS
        service.name(service.getName().replaceAll(" ", "_"));
        try {
            List<Service> usersServices = user.getServices();
            System.out.println("Saving Service: " + service.toString());
            usersServices.add(service);
            user.services(usersServices);
            JwtUser updatedUser = this.jwtUserRepository.save(user);
            servicePersisted = updatedUser.getServices().get(updatedUser.getServices().size() - 1);
        } catch (Exception e) {
            servicePersisted.id("-1");
            servicePersisted.error("Error: Could not persist Service: " + service.toString() + "   " + e.toString());
        }
        return servicePersisted;
    }

    public List<Service> getServiceByName(String name) {
        List<Service> returnServiceList = new ArrayList<>();
        try {
            returnServiceList = this.userServiceRepository.findByName(name);
        } catch (Exception e) {
            returnServiceList =  new ArrayList<>();
        }
        return returnServiceList;
    }

    public List<Service> getServicesByTeamName(String teamName) {
        List<Service> returnServiceList = new ArrayList<>();
        try {
            returnServiceList = this.userServiceRepository.findAllByTeamName(teamName);
        } catch (Exception e) {
            returnServiceList =  new ArrayList<>();
        }

        return returnServiceList; 
    }

    private List<Service> getFilteredPrivateServicesForUser(String name, JwtUser jwtUser) {
        return StringUtils.isEmpty(name) ?
                jwtUser.getServices() :
                jwtUser.getServices().stream()
                        .filter(service -> service.getName().equals(name))
                        .collect(Collectors.toList());
    }
}
