package com.ncc.kairos.moirai.zeus.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.Date;

import javax.transaction.Transactional;

import com.ncc.kairos.moirai.zeus.utililty.TimeUtil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@TestPropertySource(locations = "classpath:test.properties")
@Transactional
public class TimeUtilTest {

    @Test
    public void getDatabaseTimeTest() throws ParseException {
        SimpleDateFormat dateformat2 = new SimpleDateFormat("dd-M-yyyy hh:mm:ssZ");
        Date date = dateformat2.parse("02-04-2013 11:35:42-0400");
        OffsetDateTime odt = TimeUtil.getDatabaseTime(date);
        // 4 hour offset from est to UTC check hour to be 4 off and minute / day to remain the same for this time
        assertEquals(true, odt.getHour() == 15 && odt.getMinute() == 35 && odt.getDayOfMonth() == 2);
    }
    
}
