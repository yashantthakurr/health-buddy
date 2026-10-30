# Health Buddy

---

## 1. What is Health Buddy and For Whom Is It?

**Health Buddy** is a modern, senior-friendly Android healthcare application designed to manage doctor-prescribed medications, prevent double-dosing or missed doses, track daily hydration, and facilitate communication between senior citizens, caregivers, and healthcare providers.

### Target Audience
- **Senior Citizens:** Elderly individuals managing multiple daily prescriptions who require large readable fonts, high-contrast Dark Mode, simple tap targets, and voice read-aloud assistance.
- **Caregivers & Family Members:** Individuals looking after elderly parents or relatives who need automatic low-stock alerts, adherence summaries, and shareable PDF reports.
- **Doctors & Healthcare Professionals:** Physicians who require clear, verified patient compliance logs and hydration tracking during routine checkups.

---

## 2. Why Health Buddy?

Accidental double-dosing, missed medications, and confusion over complex prescription schedules are among the leading causes of preventable emergency hospitalizations among senior citizens. Traditional medication reminder apps are often cluttered, overly complex, or lack critical senior-focused safety controls.

### Key Value Propositions
1. **Safety First:** Strict adherence filtering eliminates confusion by removing taken medicines from the Home dashboard for the day.
2. **Loud & Continuous Alarms:** Alarms ring loudly and repeat every 5 minutes in a loop until the medication is marked TAKEN, preventing ignored notifications.
3. **Accessibility:** Native Text-to-Speech (TTS) voice announcements and Multi-Language support (English & Hindi) cater to seniors with visual impairments or language preferences.
4. **Caregiver Empowerment:** One-touch Speed Dial and instant PDF report exports ensure caregivers stay informed without intrusive monitoring.

---

## 3. Features of Health Buddy

### 📱 A. Core Medication Dashboard
- **Dynamic Time Greetings:** Adapts greetings (*"Good Morning 👋"*, *"Good Afternoon 👋"*, *"Good Evening 👋"*, *"Good Night 👋"*) based on local time.
- **Senior Adherence Filter:** When a dose is marked TAKEN, it is logged to cloud history and automatically removed from "Today's Medicines" to avoid double-dosing.
- **Dynamic Silence Alarm Button:** A prominent `🔕 Silence Alarm` button appears on the Home dashboard *only* while an alarm is actively ringing and hides when silenced.

### 🔔 B. Advanced Alarm & Continuous Snooze Loop
- **High-Precision Exact Alarms:** Uses `AlarmManager.setAlarmClock()`, guaranteeing exact time delivery even in deep Doze mode.
- **Android 14/15 FGS Compliance:** Foreground service declared with `FOREGROUND_SERVICE_TYPE_SPECIAL_USE`.
- **Continuous 5-Minute Snooze Repeat:** If a dose is not taken, the alarm rings for 1 minute and repeats every 5 minutes in a loop until marked TAKEN.
- **Automatic 12 AM Boot Reschedule:** Automatically reschedules all active medicine alarms after phone reboots (`BOOT_COMPLETED`).

### 📷 C. On-Device AI / OCR Prescription Scanner
- **Full-Resolution Camera Processing:** Captures full-res photos via `FileProvider` (`res/xml/file_paths.xml`) for high-precision ML Kit text recognition.
- **Medical Shorthand Parser:** Automatically extracts dosage (`500mg`), form (`Tab`, `Cap`, `Syr`), frequency (`1-0-1`, `1-1-1`, `OD`, `BD`, `TDS`, `QID`, `HS`, `SOS`), and food relation (`AC`, `PC`, `Before Food`, `After Food`).
- **Smart Default Time Mapping:** Maps frequency shorthand to appropriate alarm times (`08:00 AM` for morning, `01:00 PM` for afternoon, `08:00 PM` for bedtime).
- **Safe Non-Prescription Filtering:** Detects non-prescription photos (like conversations or household items) and alerts the user instead of creating fake medicine entries.

### 💡 D. AI Medicine Insights (Why Taken & Who For)
- **Live Real-Time Preview:** As the user types a medicine name in manual entry or views a scanned prescription, a live AI card displays a 2–3 line non-diagnostic explanation answering **Why it is taken**, **Who it is for**, and **Key senior precautions**.
- **Recognized Drug Validation:** Validates drug names against a medical drug index (*Paracetamol, Metformin, Amoxicillin, Amlodipine, Omeprazole, Atorvastatin, Aspirin, Cetirizine, Vitamin D, etc.*). For random/unrecognized words, displays `"⚠️ Cannot show AI insights for this entry. Please verify the medicine name."`

### 💧 E. Daily Water Intake Tracker
- **Customizable Daily Goal:** Users can edit their daily target number of glasses (default 8 glasses).
- **Uncapped Increments:** Tapping `+1 Glass 💧` allows drinking more than the goal with encouraging toasts.
- **Automatic 12:00 AM Reset:** Automatically resets water intake count to 0 glasses at midnight based on date key verification (`water_last_date`).

### 📞 F. One-Touch Speed Dial Contacts
- Dedicated `📞 Call Caregiver` and `🩺 Call Doctor` buttons on the Home dashboard reserved exclusively for Caregiver and Doctor contacts (no family members).
- Integrated with Android's native phone dialer (`Intent.ACTION_DIAL`).
- Contact settings in Settings tab feature an **Edit lock/unlock** toggle with Save and Cancel buttons.

### 📄 G. Caregiver / Doctor PDF Report Export
- Generates an official, beautifully formatted **Medication & Hydration Adherence Report** using Android's native `android.graphics.pdf.PdfDocument` API.
- Includes total recorded doses, taken count, missed count, compliance rate %, daily hydration statistics, and structured log table.
- Opens Android's native Share Sheet (`FileProvider` PDF Uri) to email, print, or share via WhatsApp/Drive.

### 🌐 H. Multi-Language (English & Hindi) & Voice Switcher
- **Complete Hindi Translations:** Full Hindi translation resource file (`res/values-hi/strings.xml`).
- **Dynamic Language Switcher:** In-app toggle in Settings switching between **English (अंग्रेज़ी)** and **हिंदी (Hindi)**.
- **Hindi Text-to-Speech Engine:** `TextToSpeech` automatically voices spoken instructions in Hindi (`new Locale("hi", "IN")`) when Hindi is selected.

---

## 4. What Unique Feature(s) Make Health Buddy Different?

| Feature | Generic Medicine Tracker Apps | Health Buddy |
| :--- | :--- | :--- |
| **Senior Adherence Filtering** | Shows all medicines all day; causes double-dosing confusion. | **Removes taken medicines** from Home screen for the day immediately. |
| **Alarm Snooze Behavior** | Fires once or twice then stops; easily missed. | **Continuous 5-minute loop** that repeats endlessly until marked TAKEN. |
| **Dynamic Silence Control** | Cluttered or buried in notifications. | **Dynamic Silence Alarm button** appears on Home screen *only* when ringing. |
| **Prescription OCR Safety** | Creates fake entries on non-prescription photos. | **Strict safety filter** alerts user on non-prescription photos. |
| **AI Medicine Insights** | Requires manual search or 3rd party ads. | **Instant 2–3 line summary** (Why Taken & Who For) with validation warning for unknown words. |
| **Hydration & Adherence PDF** | Basic screenshot or paid premium export. | **Native offline PDF report** with hydration + adherence data for doctor reviews. |
| **Voice & Language** | English only. | **Native English & Hindi Text-to-Speech** reading instructions aloud. |

---

## 5. Technologies Used While Developing Health Buddy

### Core Framework & Architecture
- **Language:** Java (Android Views)
- **Build System:** Gradle Kotlin DSL (`build.gradle.kts`)
- **Target SDK:** Android 15 (API Level 36) | Min SDK: Android 10 (API Level 29)
- **Architecture Pattern:** Modular UI View Architecture with Real-Time Event Listeners

### Cloud & Backend Services
- **Firebase Authentication:** Email/Password & Google Sign-In (`play-services-auth:21.0.0`).
- **Firebase Firestore:** Live multi-device cloud database under `users/{uid}/medicines` and `users/{uid}/medicationHistory`.

### Machine Learning & On-Device Processing
- **Google ML Kit Text Recognition:** `com.google.mlkit:text-recognition:16.0.1` for fast, offline OCR prescription scanning.

### Native Android System APIs
- **AlarmManager API:** Exact alarms via `setAlarmClock()` and Doze mode exemptions.
- **Foreground Service API:** Special-use service (`FOREGROUND_SERVICE_TYPE_SPECIAL_USE`) for continuous ringtone playback.
- **TextToSpeech API:** `android.speech.tts.TextToSpeech` for English and Hindi voice assistance.
- **PdfDocument API:** `android.graphics.pdf.PdfDocument` for native PDF report generation.
- **FileProvider API:** `androidx.core.content.FileProvider` for secure full-res camera captures and PDF file sharing.
- **SharedPreferences API:** Persistent local storage for theme, water counts, speed dial contacts, and language choice.

---

## 6. Additional Architectural & Security Highlights

- **Medical Safety Compliance:** The app does NOT diagnose diseases or change prescribed dosages; AI only assists with reading prescriptions and providing non-diagnostic information.
- **100% Offline Core Functionality:** Alarm triggering, Text-to-Speech voice instructions, PDF report generation, and ML Kit OCR run completely on-device without requiring continuous internet connection.
- **Automated Unit Test Coverage:** Comprehensive test suite (`:app:testDebugUnitTest`) verifying time parsing, notification ID generation, medical regex extractions, and AI knowledge validation.
