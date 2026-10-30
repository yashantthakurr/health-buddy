package com.project.healthbuddy.ai;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class PrescriptionAnalyzerTest {

    @Test
    public void testParseSingleLinePrescription() {
        String input = "1. Tab. Paracetamol 500mg 1-0-1 After Food";
        List<MedicineExtractionResult> results = PrescriptionAnalyzer.parseRawTextString(input);

        assertEquals(1, results.size());
        MedicineExtractionResult m = results.get(0);
        assertEquals("Paracetamol", m.getName());
        assertEquals("500mg", m.getDosage());
        assertEquals("Twice a day", m.getFrequency());
        assertEquals("After Food", m.getFoodRelation());
    }

    @Test
    public void testParseBeforeFoodAndOnceDaily() {
        String input = "Cap. Omeprazole 20mg OD before food";
        List<MedicineExtractionResult> results = PrescriptionAnalyzer.parseRawTextString(input);

        assertEquals(1, results.size());
        MedicineExtractionResult m = results.get(0);
        assertEquals("Omeprazole", m.getName());
        assertEquals("20mg", m.getDosage());
        assertEquals("Once a day", m.getFrequency());
        assertEquals("Before Food", m.getFoodRelation());
    }

    @Test
    public void testParseMultiLinePrescription() {
        String input = "Rx:\nTab. Metformin 500mg\n1-1-1 pc x 10 days";
        List<MedicineExtractionResult> results = PrescriptionAnalyzer.parseRawTextString(input);

        assertFalse(results.isEmpty());
        MedicineExtractionResult m = results.get(0);
        assertEquals("Metformin", m.getName());
        assertEquals("500mg", m.getDosage());
        assertEquals("Three times a day", m.getFrequency());
        assertEquals("After Food", m.getFoodRelation());
    }

    @Test
    public void testNonPrescriptionTextReturnsZeroResults() {
        String nonPrescriptionInput = "Dr. John Smith MD\nApollo Clinic Room 204\nPatient: Jane Doe\nDate: 29/10/2026\nPhone: +1 555-0199";
        List<MedicineExtractionResult> results = PrescriptionAnalyzer.parseRawTextString(nonPrescriptionInput);

        // Must not create fake medicines for non-prescription header text
        assertEquals(0, results.size());
    }
}
