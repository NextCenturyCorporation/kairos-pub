package com.ncc.kairos.moirai.zeus.utility;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javax.transaction.Transactional;

import com.ncc.kairos.moirai.zeus.utililty.PasswordUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(SpringExtension.class)
@TestPropertySource(locations = "classpath:test.properties")
@Transactional
public class PasswordUtilTest {

    @BeforeEach
    public void setup() {
    }

    @Test
    public void generateAccessCodeTest() {
        String result = PasswordUtil.generateAccessCode();
        assertEquals(true, !result.isBlank());
    }

    @Test
    public void getSecurePasswordTest() {
        String rawPassword = "Password123414";
        String result = PasswordUtil.getSecurePassword(rawPassword);
        assertEquals(true, !rawPassword.equals(result));
    }

    @Test
    public void passwordValidationTest() {
        String validPassword = "LengthLArgeWithCaps1231";
        String invalidPassword = "bad";
        // Bad password
        assertThrows(ResponseStatusException.class, () -> {
            PasswordUtil.passwordValidation(invalidPassword);
        });
        // Null password
        assertThrows(ResponseStatusException.class, () -> {
            PasswordUtil.passwordValidation(null);
        });
        // Valid password
        assertDoesNotThrow(() -> {
            PasswordUtil.passwordValidation(validPassword);
        });
    }

    @Test
    public void encodeAndDecodeTest() {
        String test = "something";
        assertEquals(true, test.equals(PasswordUtil.decode(PasswordUtil.encode(test))));
    }
    
    @Test
    public void generatePasswordSaltTest() {
        byte[] result = PasswordUtil.createPasswordSalt();
        assertEquals(true, result != null && result.length > 0);
    }
}
