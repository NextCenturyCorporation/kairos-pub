package com.ncc.kairos.moirai.zeus.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import javax.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(locations = "classpath:test.properties")
@Transactional
public class ApiUtilTest {

    @Test
    public void checkValidIdTest() {
        String validId = "11reg32rq2f2qss";
        String invalidId = "-1";
        assertEquals(true, ApiUtil.checkForValidId(validId) == true);
        assertEquals(true, ApiUtil.checkForValidId(invalidId) == false);
    }
    
}
