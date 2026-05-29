import '../../models/analysis_result.dart';

/// Core abstraction for AI inference services.
/// 
/// This interface allows seamless swapping between mock engines (for prototypes),
/// local on-device models (e.g., TensorFlow Lite), and cloud inference APIs.
abstract class IAiInferenceService {
  /// Computes a comprehensive neurological assessment.
  /// 
  /// [tremorVariance] represents raw motion instability derived from accelerometer.
  /// [voiceVariance] represents vocal pitch/jitter instability derived from audio processing.
  Future<AnalysisResult> computeAssessment({
    double? tremorVariance,
    double? voiceVariance,
  });
}
