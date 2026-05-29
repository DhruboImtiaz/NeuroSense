import 'package:flutter/material.dart';
import '../models/analysis_result.dart';
import '../utils/theme.dart';

class RiskGauge extends StatelessWidget {
  final RiskLevel riskLevel;
  final double score;

  const RiskGauge({
    Key? key,
    required this.riskLevel,
    required this.score,
  }) : super(key: key);

  Color get _gaugeColor {
    switch (riskLevel) {
      case RiskLevel.low:
        return AppTheme.riskLow;
      case RiskLevel.moderate:
        return AppTheme.riskModerate;
      case RiskLevel.elevated:
        return AppTheme.riskElevated;
    }
  }

  String get _riskText {
    switch (riskLevel) {
      case RiskLevel.low:
        return 'LOW RISK';
      case RiskLevel.moderate:
        return 'MODERATE RISK';
      case RiskLevel.elevated:
        return 'ELEVATED RISK';
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 200,
      height: 200,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        boxShadow: [
          BoxShadow(
            color: _gaugeColor.withOpacity(0.2),
            blurRadius: 30,
            spreadRadius: 10,
          ),
        ],
      ),
      child: Stack(
        fit: StackFit.expand,
        children: [
          CircularProgressIndicator(
            value: score / 100,
            strokeWidth: 12,
            backgroundColor: AppTheme.glassBorder,
            valueColor: AlwaysStoppedAnimation<Color>(_gaugeColor),
          ),
          Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  '${score.toStringAsFixed(1)}',
                  style: Theme.of(context).textTheme.displayMedium?.copyWith(
                    color: _gaugeColor,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(height: 5),
                Text(
                  _riskText,
                  style: Theme.of(context).textTheme.bodySmall?.copyWith(
                    color: _gaugeColor,
                    fontWeight: FontWeight.w600,
                    letterSpacing: 1.5,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}
