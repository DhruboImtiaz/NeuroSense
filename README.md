# NeuroSense

A Flutter-based, AI-powered neurological screening assistant designed to help patients conduct preliminary clinical screenings for neurological anomalies right from their mobile device.

### Core Functionality
- Vocal Biomarker Analysis - Analyzes micro-fluctuations and pitch instability in the user's voice for early signs of neurological irregularities
- Kinetic Tremor Analysis - Uses the device's accelerometer to monitor high-precision variance, detecting resting and postural motor tremors
- Explainable AI (XAI) Dashboard - Provides detailed breakdowns of AI inference results, contributing factors, signal quality indicators (SNR, Jitter), and overall confidence levels
- Stunning UI/UX - Built with modern, responsive design using glassmorphism, animated gradient backgrounds, and glowing UI components

### Technical Excellence
- Flutter Framework - Cross-platform UI toolkit for visually attractive native applications
- Dart Programming Language - Robust, typed language optimized for UI building
- Provider State Management - Efficient and scalable state management pattern
- GetIt Dependency Injection - Service locator pattern for clean architecture
- Fl_Chart - High-quality, interactive data visualization library
- Sensors Plus - Hardware integration for accessing device accelerometer data

### Prerequisites
Before you begin, ensure you have the following installed:
- Flutter SDK (>=3.0.0) - Mobile development
- Dart SDK - Core language
- Android Studio or Xcode - For iOS/Android emulators and building

### Setup
```bash
# Clone the repository
git clone <repository-url>
cd NeuroSense
```

#### 1. Application Configuration
Fetch all required dependencies:
```bash
flutter pub get
```

#### 2. Run the Application
Run the application on an emulator or physical device:
```bash
flutter run
```

### Project Architecture
```text
NeuroSense/
├── lib/
│   ├── core/               # Core configurations and interfaces
│   │   ├── di/                 # Dependency injection (GetIt setup)
│   │   └── interfaces/         # Service interfaces (IAiInferenceService)
│   ├── models/             # Data models (AnalysisResult)
│   ├── screens/            # UI Screens (Home, Tremor Analysis, Voice Analysis)
│   ├── services/           # Business logic and external service mocks
│   ├── utils/              # Theming, animations, and transitions
│   ├── widgets/            # Reusable UI components (GlassCard, GlowingButton)
│   └── main.dart           # Application entry point
├── android/              # Native Android configuration
├── ios/                  # Native iOS configuration
└── pubspec.yaml          # Project metadata and dependencies
```

### Architectural Highlights
- Interface-Driven Services - The AI inference logic sits behind an interface (`IAiInferenceService`), making it easy to swap implementations
- Service Locator - `get_it` is used to register and retrieve services globally
- Component Reusability - Complex UI elements like glassmorphism cards (`GlassCard`) are extracted into the `widgets/` folder for consistency

### Disclaimer
**NeuroSense** is a conceptual application. Its simulated AI inference engine provides mocked diagnostic results intended for demonstration and UI/UX testing purposes only. It is not a substitute for professional medical advice, diagnosis, or treatment. Always seek the advice of a qualified healthcare provider with any questions regarding a medical condition.