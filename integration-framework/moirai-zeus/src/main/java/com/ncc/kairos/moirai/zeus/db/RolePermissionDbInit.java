package com.ncc.kairos.moirai.zeus.db;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncc.kairos.moirai.zeus.dao.JwtPermissionRepository;
import com.ncc.kairos.moirai.zeus.dao.JwtRoleRepository;
import com.ncc.kairos.moirai.zeus.model.JwtPermission;
import com.ncc.kairos.moirai.zeus.model.JwtRole;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RolePermissionDbInit implements InitializingBean {

    @Autowired
    private JwtPermissionRepository jwtPermissionRepository;

    @Autowired
    private JwtRoleRepository jwtRoleRepository;


    private List<JwtPermission> permissions = new ArrayList<>();

    private static final Logger LOGGER = Logger.getLogger(RolePermissionDbInit.class.getName());
    
    @Override
    @Transactional
    public void afterPropertiesSet() throws Exception {
        this.bootStrap();
    }
    
    public void bootStrap() {
        try {
            readFile();
            List<JwtPermission> records = this.jwtPermissionRepository.findAll();
            // Change to java lambda to remove duplicates and save the rest
            for (JwtPermission permission : this.permissions) {
                boolean exists = false;
                for (JwtPermission jp : records) {
                    if (permission.getName().equals(jp.getName())) {
                        exists = true;
                        break;
                    }
                }       
                if (!exists) {
                    this.jwtPermissionRepository.save(permission);
                }
            }
            // Save Admin role
            if (this.jwtRoleRepository.findByName("ADMIN") == null) {
                this.jwtRoleRepository.save(new JwtRole().description("Administrative Role").name("ADMIN").permissions(this.jwtPermissionRepository.findAll()));
            }
        } catch (IOException e) {
            LOGGER.warning(e.getMessage());
        }
    }

    protected void readFile() throws IOException {
        ObjectMapper om = new ObjectMapper();
        InputStream inputStream = RolePermissionDbInit.class.getClassLoader().getResourceAsStream("permissions.json");
        List<JwtPermission> container = om.readValue(inputStream, new TypeReference<List<JwtPermission>>() { });
        this.permissions.addAll(container);
    }
}
