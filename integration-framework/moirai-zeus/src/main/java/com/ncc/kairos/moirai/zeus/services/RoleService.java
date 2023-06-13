package com.ncc.kairos.moirai.zeus.services;

import com.ncc.kairos.moirai.zeus.dao.JwtRoleRepository;
import com.ncc.kairos.moirai.zeus.model.JwtRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service for storing and creating roles.
 * 
 * @author jake vanbramer
 * @version 0.1
 */
@Service
public class RoleService {

    /**
     * The repository for storing JwtRoles.
     */
    @Autowired
    private JwtRoleRepository repository;

    /**
     * Possible groups used to create roles.
     */
    public enum Group {
        /**
         * Group TA1.
         */
        TA1,
        /**
         * Group TA2.
         */
        TA2,
        /**
         * Group TA3.
         */
        TA3,
        /**
         * Group TA4.
         */
        TA4
    }

    public JwtRole createRole(Group roleGroup) throws Exception {
        JwtRole role = new JwtRole();
        role.setName(roleGroup.name());
        String description = getStandardGroupDescription(roleGroup);
        role.setDescription(description);
        role.setId(UUID.randomUUID().toString());
        return role;
    }

    public Group getGroup(String group) {
        switch (group) {
            case "TA1":
                return Group.TA1;
            case "TA2":
                return Group.TA2;
            case "TA3":
                return Group.TA3;
            case "TA4":
                return Group.TA4;
            default:
                throw new IllegalArgumentException("Given argument is not an existing group value: " + group);
        }
    }

    /**
     * Takes a jwtRole and adds it to the known roles list.
     *
     * @param jwtRole The new jwtRole.
     */
    public void storeRole(JwtRole jwtRole) {
        this.repository.save(jwtRole);
    }

    private String getStandardGroupDescription(Group group) throws Exception {
        String description;
        switch (group) {
            case TA1:
                description = "TA1 description.";
                return description;
            case TA2:
                description = "TA2 description.";
                return description;
            case TA3:
                description = "TA3 description.";
                return description;
            case TA4:
                description = "TA4 description.";
                return description;
            default:
                throw new Exception("Invalid Enum Encountered " + group);
        }
    }
}
