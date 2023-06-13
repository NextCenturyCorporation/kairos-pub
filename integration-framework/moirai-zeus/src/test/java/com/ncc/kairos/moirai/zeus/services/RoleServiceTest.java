package com.ncc.kairos.moirai.zeus.services;


import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ncc.kairos.moirai.zeus.dao.JwtRoleRepository;
import com.ncc.kairos.moirai.zeus.model.JwtRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

/**
 * Unit tests for RoleService class.
 * @author jake vanbramer
 * @version 0.1
 */
@TestPropertySource(locations = "classpath:test.properties")
class RoleServiceTest extends RoleService {

    @MockBean
    JwtRole jwtRole;

    @Autowired
    private JwtRoleRepository repository;

    @BeforeEach
    void setUp() {
        JwtRole role = new JwtRole().name("TA1").description("TA1 description.").id(UUID.randomUUID().toString());
        this.repository.save(role);
    }


    @Test
    void createRoleTest() {
        try {
            JwtRole role = createRole(getGroup("TA1"));
            assertEquals(role.getName(), "TA1");
        } catch (Exception e) {
            System.out.println("Enum failure.");
        }
    }

    @Test
    void getGroup() {
        assertEquals(getGroup("TA1").toString(), "TA1");
    }

    @Test
    void storeRole() {
        JwtRole role2 = this.repository.findByName("TA1");

        assertEquals("TA1", role2.getName());
        assertEquals("TA1 description.", role2.getDescription());

    }
}
