package com.project.healthbuddy.ai;

import org.junit.Test;

import static org.junit.Assert.*;

public class MedicineAiKnowledgeTest {

    @Test
    public void testParacetamolSummary() {
        MedicineAiKnowledge.MedicineSummary summary = MedicineAiKnowledge.getSummary("Paracetamol 500mg");
        assertNotNull(summary);
        assertTrue(summary.isValidMedicine());
        assertTrue(summary.getWhyTaken().contains("pain"));
        assertTrue(summary.getWhoFor().contains("fever"));
    }

    @Test
    public void testMetforminSummary() {
        MedicineAiKnowledge.MedicineSummary summary = MedicineAiKnowledge.getSummary("Metformin 500mg");
        assertNotNull(summary);
        assertTrue(summary.isValidMedicine());
        assertTrue(summary.getWhyTaken().contains("blood glucose") || summary.getWhyTaken().contains("sugar"));
    }

    @Test
    public void testUnrecognizedMedicineSummary() {
        MedicineAiKnowledge.MedicineSummary summary = MedicineAiKnowledge.getSummary("asdfgh123");
        assertNotNull(summary);
        assertFalse(summary.isValidMedicine());
        assertTrue(summary.getFullSummary().contains("Cannot show AI insights"));
    }
}
