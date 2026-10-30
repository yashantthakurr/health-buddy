package com.project.healthbuddy.notification;

import com.project.healthbuddy.data.Medicine;

import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.*;

public class AlarmSchedulerTest {

    @Test
    public void testGetTimeInMillis_ValidFormats() {
        // Test 10:00 AM
        long time10AM = AlarmScheduler.getTimeInMillis("10:00 AM");
        Calendar cal10AM = Calendar.getInstance();
        cal10AM.setTimeInMillis(time10AM);
        assertEquals(10, cal10AM.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal10AM.get(Calendar.MINUTE));

        // Test 08:30 PM (20:30)
        long time830PM = AlarmScheduler.getTimeInMillis("08:30 PM");
        Calendar cal830PM = Calendar.getInstance();
        cal830PM.setTimeInMillis(time830PM);
        assertEquals(20, cal830PM.get(Calendar.HOUR_OF_DAY));
        assertEquals(30, cal830PM.get(Calendar.MINUTE));

        // Test single digit hour without leading zero: 8:15 AM
        long time815AM = AlarmScheduler.getTimeInMillis("8:15 AM");
        Calendar cal815AM = Calendar.getInstance();
        cal815AM.setTimeInMillis(time815AM);
        assertEquals(8, cal815AM.get(Calendar.HOUR_OF_DAY));
        assertEquals(15, cal815AM.get(Calendar.MINUTE));

        // Test 24-hour format: 14:45
        long time1445 = AlarmScheduler.getTimeInMillis("14:45");
        Calendar cal1445 = Calendar.getInstance();
        cal1445.setTimeInMillis(time1445);
        assertEquals(14, cal1445.get(Calendar.HOUR_OF_DAY));
        assertEquals(45, cal1445.get(Calendar.MINUTE));

        // Test no-space AM/PM: 9:00PM
        long time9PM = AlarmScheduler.getTimeInMillis("9:00PM");
        Calendar cal9PM = Calendar.getInstance();
        cal9PM.setTimeInMillis(time9PM);
        assertEquals(21, cal9PM.get(Calendar.HOUR_OF_DAY));
        assertEquals(0, cal9PM.get(Calendar.MINUTE));
    }

    @Test
    public void testGetNotificationId_NullAndNonNull() {
        Medicine medicine = new Medicine();
        medicine.setId("doc_12345");
        int id1 = AlarmScheduler.getNotificationId(medicine);
        assertTrue(id1 > 0);

        // Null medicine ID
        Medicine nullIdMed = new Medicine();
        nullIdMed.setName("Aspirin");
        int id2 = AlarmScheduler.getNotificationId(nullIdMed);
        assertTrue(id2 > 0);

        // Null medicine object
        int id3 = AlarmScheduler.getNotificationId(null);
        assertEquals(10001, id3);
    }
}
