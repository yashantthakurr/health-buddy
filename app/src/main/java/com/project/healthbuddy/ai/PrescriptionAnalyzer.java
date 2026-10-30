package com.project.healthbuddy.ai;

import android.graphics.Bitmap;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enhanced analyzer for doctor prescriptions using ML Kit Text Recognition and medical heuristics.
 */
public class PrescriptionAnalyzer {

    public interface AnalysisCallback {
        void onSuccess(List<MedicineExtractionResult> results);
        void onFailure(String errorMessage);
    }

    public static void analyze(Bitmap bitmap, AnalysisCallback callback) {
        if (bitmap == null) {
            callback.onFailure("No image provided. Please capture or select a clear prescription photo.");
            return;
        }

        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        recognizer.process(image)
                .addOnSuccessListener(visionText -> {
                    String fullText = visionText.getText().trim();
                    if (fullText.isEmpty()) {
                        callback.onFailure("We couldn't detect any text in this photo. Please ensure the image is bright and clearly focused.");
                        return;
                    }

                    List<MedicineExtractionResult> extractions = parsePrescriptionText(visionText);
                    if (extractions.isEmpty()) {
                        callback.onFailure("No prescription medicines detected in this image. Please ensure the image shows a valid doctor's prescription or enter details manually.");
                    } else {
                        callback.onSuccess(extractions);
                    }
                })
                .addOnFailureListener(e -> callback.onFailure("Failed to process image: " + e.getLocalizedMessage()));
    }

    // Regex patterns for medical extraction
    private static final Pattern DOSAGE_PATTERN = Pattern.compile(
            "(?i)\\b(\\d+(?:\\.\\d+)?\\s*(?:mg|g|mcg|µg|ml|iu|units?|tablets?|capsules?|tabs?|caps?|drops?|puffs?|sachet|%|mg/ml|w/v|w/w))\\b"
    );

    private static final Pattern FORM_PREFIX_PATTERN = Pattern.compile(
            "(?i)\\b(tab\\.?|tablet|cap\\.?|capsule|syr\\.?|syrup|inj\\.?|injection|oint\\.?|ointment|drops?|inhaler|suspension|gel|cream|patch|sol\\.?|solution)\\b"
    );

    private static final Pattern FOOD_PATTERN = Pattern.compile(
            "(?i)\\b(before\\s+food|before\\s+meals?|empty\\s+stomach|a\\.?c\\.?|after\\s+food|after\\s+meals?|p\\.?c\\.?|with\\s+food|with\\s+meals?|c\\.?c\\.?)\\b"
    );

    private static final Pattern FREQUENCY_PATTERN = Pattern.compile(
            "(?i)\\b(1-0-1|1-1-1|1-0-0|0-0-1|0-1-0|1-1-0|0-1-1|1-1-1-1|o\\.?d\\.?|b\\.?d\\.?|b\\.?i\\.?d\\.?|t\\.?d\\.?s\\.?|t\\.?i\\.?d\\.?|q\\.?d\\.?s\\.?|q\\.?i\\.?d\\.?|h\\.?s\\.?|s\\.?o\\.?s\\.?|p\\.?r\\.?n\\.?|once\\s+daily|twice\\s+daily|thrice\\s+daily|once\\s+a\\s+day|twice\\s+a\\s+day|three\\s+times\\s+a\\s+day|bedtime|at\\s+night)\\b"
    );

    private static final Pattern HEADER_FILTER = Pattern.compile(
            "(?i)\\b(dr\\.?|doctor|clinic|hospital|patient|date|age|gender|sex|address|phone|tel|mobile|email|m\\.?b\\.?b\\.?s\\.?|md|reg|registration|sl|no|bp|pulse|weight|height|temp|diagnosis|symptoms|signature|sub|total|amount|receipt|bill|rupees|rs|payment|vitals|rx)\\b"
    );

    public static List<MedicineExtractionResult> parsePrescriptionText(Text visionText) {
        List<MedicineExtractionResult> results = new ArrayList<>();
        Set<String> processedNames = new HashSet<>();

        if (visionText == null) return results;

        for (Text.TextBlock block : visionText.getTextBlocks()) {
            List<Text.Line> lines = block.getLines();
            for (int i = 0; i < lines.size(); i++) {
                String currentLine = lines.get(i).getText().trim();
                if (currentLine.length() < 3) continue;

                // Combine with next line in block if next line has dosage/instructions
                String combinedLine = currentLine;
                if (i + 1 < lines.size()) {
                    combinedLine = currentLine + " " + lines.get(i + 1).getText().trim();
                }

                MedicineExtractionResult extracted = extractMedicineFromLine(combinedLine, currentLine);
                if (extracted != null && !processedNames.contains(extracted.getName().toLowerCase(Locale.US))) {
                    processedNames.add(extracted.getName().toLowerCase(Locale.US));
                    results.add(extracted);
                }
            }
        }

        return results;
    }

    /**
     * Fallback parser for raw string input (useful for unit testing and direct parsing)
     */
    public static List<MedicineExtractionResult> parseRawTextString(String fullText) {
        List<MedicineExtractionResult> results = new ArrayList<>();
        Set<String> processedNames = new HashSet<>();

        if (fullText == null || fullText.trim().isEmpty()) return results;

        String[] lines = fullText.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String currentLine = lines[i].trim();
            if (currentLine.length() < 3) continue;

            String combinedLine = currentLine;
            if (i + 1 < lines.length) {
                combinedLine = currentLine + " " + lines[i + 1].trim();
            }

            MedicineExtractionResult extracted = extractMedicineFromLine(combinedLine, currentLine);
            if (extracted != null && !processedNames.contains(extracted.getName().toLowerCase(Locale.US))) {
                processedNames.add(extracted.getName().toLowerCase(Locale.US));
                results.add(extracted);
            }
        }

        return results;
    }

    private static MedicineExtractionResult extractMedicineFromLine(String combinedLine, String primaryLine) {
        Matcher dosageMatcher = DOSAGE_PATTERN.matcher(combinedLine);
        Matcher formMatcher = FORM_PREFIX_PATTERN.matcher(combinedLine);

        boolean hasDosage = dosageMatcher.find();
        boolean hasFormPrefix = formMatcher.find();

        // Must have dosage or explicit medicine form prefix to be a valid medicine entry
        if (!hasDosage && !hasFormPrefix) {
            return null;
        }

        String dosage = hasDosage ? dosageMatcher.group(1) : "500mg";

        // Clean name candidate
        String nameSource = primaryLine;
        if (!hasFormPrefix && !hasDosageInLine(primaryLine)) {
            nameSource = combinedLine;
        }

        String rawName = cleanRawMedicineName(nameSource, dosage);
        if (rawName.length() < 2 || HEADER_FILTER.matcher(rawName).find()) {
            return null;
        }

        MedicineExtractionResult result = new MedicineExtractionResult();
        result.setName(formatMedicineName(rawName));
        result.setDosage(dosage);
        result.setQuantity("1 tablet");

        // Extract Food Relation
        Matcher foodMatcher = FOOD_PATTERN.matcher(combinedLine);
        if (foodMatcher.find() && foodMatcher.group(1) != null) {
            String foodStr = foodMatcher.group(1).toLowerCase(Locale.US);
            if (foodStr.contains("before") || foodStr.contains("a.c") || foodStr.contains("ac") || foodStr.contains("empty")) {
                result.setFoodRelation("Before Food");
            } else if (foodStr.contains("with") || foodStr.contains("c.c") || foodStr.contains("cc")) {
                result.setFoodRelation("With Food");
            } else {
                result.setFoodRelation("After Food");
            }
        } else {
            result.setFoodRelation("After Food");
        }

        // Extract Frequency & Smart Default Time
        Matcher freqMatcher = FREQUENCY_PATTERN.matcher(combinedLine);
        if (freqMatcher.find() && freqMatcher.group(1) != null) {
            String freqStr = freqMatcher.group(1).toLowerCase(Locale.US);
            parseFrequencyAndTime(result, freqStr);
        } else {
            result.setFrequency("Once a day");
            result.setMedicineTime("08:00 AM");
        }

        return result;
    }

    private static boolean hasDosageInLine(String line) {
        return DOSAGE_PATTERN.matcher(line).find();
    }

    private static void parseFrequencyAndTime(MedicineExtractionResult result, String freqStr) {
        if (freqStr.contains("twice") || freqStr.contains("b.d") || freqStr.contains("bd") || freqStr.contains("b.i.d") || freqStr.contains("bid") || freqStr.contains("1-0-1") || freqStr.contains("1-1-0") || freqStr.contains("0-1-1")) {
            result.setFrequency("Twice a day");
            result.setMedicineTime("08:00 AM");
        } else if (freqStr.contains("thrice") || freqStr.contains("t.d.s") || freqStr.contains("tds") || freqStr.contains("t.i.d") || freqStr.contains("tid") || freqStr.contains("1-1-1") || freqStr.contains("three times")) {
            result.setFrequency("Three times a day");
            result.setMedicineTime("08:00 AM");
        } else if (freqStr.contains("0-0-1") || freqStr.contains("h.s") || freqStr.contains("hs") || freqStr.contains("bedtime") || freqStr.contains("night")) {
            result.setFrequency("Once a day");
            result.setMedicineTime("08:00 PM");
        } else if (freqStr.contains("0-1-0")) {
            result.setFrequency("Once a day");
            result.setMedicineTime("01:00 PM");
        } else if (freqStr.contains("s.o.s") || freqStr.contains("sos") || freqStr.contains("p.r.n") || freqStr.contains("prn") || freqStr.contains("as needed") || freqStr.contains("when required")) {
            result.setFrequency("As needed (SOS)");
            result.setMedicineTime("10:00 AM");
        } else {
            result.setFrequency("Once a day");
            result.setMedicineTime("08:00 AM");
        }
    }

    private static String cleanRawMedicineName(String line, String dosage) {
        String cleaned = line;

        // Remove dosage
        if (dosage != null && !dosage.isEmpty()) {
            cleaned = cleaned.replaceAll("(?i)\\b" + Pattern.quote(dosage) + "\\b", "");
        }

        // Remove common form prefixes
        cleaned = cleaned.replaceAll("(?i)^[\\d\\.\\s\\-\\*#]+", "");
        cleaned = cleaned.replaceAll("(?i)\\b(tab\\.?|tablet|cap\\.?|capsule|syr\\.?|syrup|inj\\.?|injection|oint\\.?|ointment|drops?|inhaler|suspension|gel|cream|patch|sol\\.?|solution)\\b", "");

        // Remove frequency, food, duration, quantities
        cleaned = cleaned.replaceAll("(?i)\\b(1-0-1|1-1-1|1-0-0|0-0-1|0-1-0|bd|tds|tid|qid|od|hs|sos|ac|pc|before food|after food|once|twice|thrice|daily|days?|tabs?|caps?|x|for)\\b.*", "");

        // Remove non-word trailing symbols
        cleaned = cleaned.replaceAll("[^a-zA-Z0-9\\s]", " ").trim();
        cleaned = cleaned.replaceAll("\\s+", " ");

        return cleaned;
    }

    private static String formatMedicineName(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "";
        String[] words = raw.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (sb.length() > 0) sb.append(" ");
            sb.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                sb.append(word.substring(1).toLowerCase(Locale.US));
            }
        }
        return sb.toString();
    }
}
