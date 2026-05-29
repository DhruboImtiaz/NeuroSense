enum RiskLevel {
  low,
  moderate,
  elevated,
}

class AnalysisResult {
  final double voiceStabilityScore;
  final double motorTremorScore;
  final double aiConfidence;
  final String assessmentReliabilityIndicator;
  final RiskLevel riskLevel;
  
  // Explainable AI (XAI) factors
  final List<String> contributingFactors;
  final Map<String, double> signalQualityIndicators;
  
  final List<String> observations;
  final String explanation;
  final String recommendation;
  
  // Specific detailed metrics
  final String vocalJitter;
  final String tremorAmplitude;

  AnalysisResult({
    required this.voiceStabilityScore,
    required this.motorTremorScore,
    required this.aiConfidence,
    required this.assessmentReliabilityIndicator,
    required this.riskLevel,
    required this.contributingFactors,
    required this.signalQualityIndicators,
    required this.observations,
    required this.explanation,
    required this.recommendation,
    required this.vocalJitter,
    required this.tremorAmplitude,
  });
}
