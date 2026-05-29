import 'package:flutter/material.dart';
import '../models/analysis_result.dart';
import '../utils/theme.dart';
import '../widgets/glass_card.dart';
import '../widgets/risk_gauge.dart';
import '../widgets/animated_gradient_background.dart';

class ResultsScreen extends StatefulWidget {
  final AnalysisResult result;

  const ResultsScreen({Key? key, required this.result}) : super(key: key);

  @override
  State<ResultsScreen> createState() => _ResultsScreenState();
}

class _ResultsScreenState extends State<ResultsScreen> with SingleTickerProviderStateMixin {
  late AnimationController _animController;

  @override
  void initState() {
    super.initState();
    _animController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 3500), 
    )..forward();
  }

  @override
  void dispose() {
    _animController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: AnimatedGradientBackground(
        child: SafeArea(
          child: CustomScrollView(
            slivers: [
              SliverAppBar(
                backgroundColor: Colors.transparent,
                title: Text(
                  'Clinical Assessment Report',
                  style: Theme.of(context).textTheme.displayMedium?.copyWith(
                    fontSize: 20,
                  ),
                ),
                automaticallyImplyLeading: false,
                centerTitle: true,
                actions: [
                  IconButton(
                    icon: const Icon(Icons.close),
                    onPressed: () {
                      Navigator.of(context).popUntil((route) => route.isFirst);
                    },
                  )
                ],
              ),
              SliverPadding(
                padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 10),
                sliver: SliverList(
                  delegate: SliverChildListDelegate([
                    
                    // SECTION 1: OVERVIEW
                    _buildAnimatedSection(
                      start: 0.0,
                      end: 0.25,
                      child: Center(
                        child: RiskGauge(
                          riskLevel: widget.result.riskLevel,
                          score: (widget.result.voiceStabilityScore + widget.result.motorTremorScore) / 2,
                        ),
                      ),
                    ),
                    const SizedBox(height: 35),
                    
                    _buildAnimatedSection(
                      start: 0.18,
                      end: 0.43,
                      child: Row(
                        children: [
                          Expanded(
                            child: _buildMetricCard(
                              context,
                              title: 'AI Confidence',
                              value: '${widget.result.aiConfidence.toStringAsFixed(1)}%',
                              icon: Icons.auto_awesome,
                            ),
                          ),
                          const SizedBox(width: 15),
                          Expanded(
                            child: _buildMetricCard(
                              context,
                              title: 'Reliability',
                              value: widget.result.assessmentReliabilityIndicator,
                              icon: Icons.shield,
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 25),
                    
                    // SECTION 2: SIGNAL QUALITY (Pre-Inference)
                    _buildAnimatedSection(
                      start: 0.32,
                      end: 0.57,
                      child: GlassCard(
                        padding: const EdgeInsets.all(24),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                const Icon(Icons.sensors, color: AppTheme.purpleAccent, size: 20),
                                const SizedBox(width: 10),
                                Text(
                                  'Pre-Inference Signal Quality',
                                  style: Theme.of(context).textTheme.titleLarge?.copyWith(color: AppTheme.purpleAccent),
                                ),
                              ],
                            ),
                            const Divider(color: AppTheme.glassBorder, height: 30),
                            ...widget.result.signalQualityIndicators.entries.map((e) => Padding(
                              padding: const EdgeInsets.only(bottom: 10.0),
                              child: _buildSubMetricRow(e.key, '${e.value.toStringAsFixed(1)} %', context),
                            )).toList(),
                          ],
                        ),
                      ),
                    ),
                    const SizedBox(height: 25),
                    
                    // SECTION 3: BIOMARKER METRICS
                    _buildAnimatedSection(
                      start: 0.48,
                      end: 0.73,
                      child: GlassCard(
                        padding: const EdgeInsets.all(24),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'Biomarker Metrics',
                              style: Theme.of(context).textTheme.titleLarge?.copyWith(color: AppTheme.cyanAccent),
                            ),
                            const Divider(color: AppTheme.glassBorder, height: 30),
                            _buildScoreRow('Vocal Biomarker Stability', widget.result.voiceStabilityScore, context),
                            const SizedBox(height: 8),
                            _buildSubMetricRow('Est. Vocal Jitter:', widget.result.vocalJitter, context),
                            const SizedBox(height: 25),
                            _buildScoreRow('Kinetic Tremor Analysis', widget.result.motorTremorScore, context),
                            const SizedBox(height: 8),
                            _buildSubMetricRow('Avg. Tremor Amplitude:', widget.result.tremorAmplitude, context),
                          ],
                        ),
                      ),
                    ),
                    const SizedBox(height: 25),
                    
                    // SECTION 4: EXPLAINABLE AI (XAI)
                    _buildAnimatedSection(
                      start: 0.65,
                      end: 0.85,
                      child: Container(
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(24),
                          boxShadow: [
                            BoxShadow(
                              color: Colors.amber.withOpacity(0.08),
                              blurRadius: 25,
                              offset: const Offset(0, 8),
                            )
                          ],
                        ),
                        child: GlassCard(
                          padding: const EdgeInsets.all(24),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(
                                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                                children: [
                                  Row(
                                    children: [
                                      const Icon(Icons.psychology, color: Colors.amber, size: 22),
                                      const SizedBox(width: 10),
                                      Text(
                                        'Explainable AI Factors (XAI)',
                                        style: Theme.of(context).textTheme.titleLarge?.copyWith(
                                          color: Colors.amber,
                                          fontWeight: FontWeight.bold,
                                        ),
                                      ),
                                    ],
                                  ),
                                  Container(
                                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                                    decoration: BoxDecoration(
                                      color: Colors.amber.withOpacity(0.1),
                                      borderRadius: BorderRadius.circular(10),
                                      border: Border.all(color: Colors.amber.withOpacity(0.3)),
                                    ),
                                    child: Text(
                                      'DECISION BOUNDARY WEIGHTS',
                                      style: Theme.of(context).textTheme.bodySmall?.copyWith(
                                        color: Colors.amber,
                                        fontSize: 9,
                                        fontWeight: FontWeight.bold,
                                        letterSpacing: 0.5,
                                      ),
                                    ),
                                  )
                                ],
                              ),
                              const Divider(color: AppTheme.glassBorder, height: 35),
                              Text(
                                'The following factors heavily weighted the neural network\'s decision boundary:',
                                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                                  fontStyle: FontStyle.italic,
                                  color: Colors.white70,
                                ),
                              ),
                              const SizedBox(height: 20),
                              ...widget.result.contributingFactors.map((factor) => Padding(
                                padding: const EdgeInsets.only(bottom: 12.0),
                                child: Row(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Container(
                                      margin: const EdgeInsets.only(top: 4),
                                      padding: const EdgeInsets.all(4),
                                      decoration: BoxDecoration(
                                        color: Colors.amber.withOpacity(0.15),
                                        shape: BoxShape.circle,
                                      ),
                                      child: const Icon(Icons.bolt, size: 14, color: Colors.amber),
                                    ),
                                    const SizedBox(width: 12),
                                    Expanded(
                                      child: Text(
                                        factor,
                                        style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                                          height: 1.5,
                                        ),
                                      ),
                                    ),
                                  ],
                                ),
                              )).toList(),
                            ],
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(height: 35),
                    
                    // SECTION 5: CLINICAL OBSERVATIONS
                    _buildAnimatedSection(
                      start: 0.78,
                      end: 0.92,
                      child: Text(
                        'Clinical Observations',
                        style: Theme.of(context).textTheme.titleLarge?.copyWith(
                          color: Colors.white,
                          letterSpacing: 1.0,
                        ),
                      ),
                    ),
                    const SizedBox(height: 15),
                    
                    ...widget.result.observations.asMap().entries.map((entry) {
                      final index = entry.key;
                      final start = 0.80 + (index * 0.05);
                      final end = (start + 0.10).clamp(0.0, 1.0);
                      
                      return _buildAnimatedSection(
                        start: start,
                        end: end,
                        child: Padding(
                          padding: const EdgeInsets.only(bottom: 12.0),
                          child: GlassCard(
                            padding: const EdgeInsets.all(18),
                            child: Row(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                const Icon(Icons.check_circle_outline, size: 22, color: AppTheme.cyanAccent),
                                const SizedBox(width: 12),
                                Expanded(
                                  child: Text(
                                    entry.value,
                                    style: Theme.of(context).textTheme.bodyMedium?.copyWith(height: 1.5),
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ),
                      );
                    }).toList(),
                    
                    const SizedBox(height: 25),
                    
                    // SECTION 6: RECOMMENDATION
                    _buildAnimatedSection(
                      start: 0.92,
                      end: 1.0,
                      child: Container(
                        padding: const EdgeInsets.all(24),
                        decoration: BoxDecoration(
                          color: AppTheme.glassWhite,
                          borderRadius: BorderRadius.circular(24),
                          border: Border.all(
                            color: widget.result.riskLevel == RiskLevel.low 
                                ? AppTheme.cyanAccent.withOpacity(0.5)
                                : AppTheme.riskElevated.withOpacity(0.5),
                            width: 2,
                          ),
                          boxShadow: [
                            BoxShadow(
                              color: widget.result.riskLevel == RiskLevel.low 
                                  ? AppTheme.cyanAccent.withOpacity(0.1)
                                  : AppTheme.riskElevated.withOpacity(0.1),
                              blurRadius: 30,
                            )
                          ]
                        ),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              children: [
                                Icon(
                                  Icons.medical_services, 
                                  color: widget.result.riskLevel == RiskLevel.low ? AppTheme.cyanAccent : AppTheme.riskElevated
                                ),
                                const SizedBox(width: 10),
                                Text(
                                  'AI Recommendation',
                                  style: Theme.of(context).textTheme.titleLarge?.copyWith(color: Colors.white),
                                ),
                              ],
                            ),
                            const SizedBox(height: 15),
                            Text(
                              widget.result.recommendation,
                              style: Theme.of(context).textTheme.bodyLarge?.copyWith(height: 1.6),
                            ),
                          ],
                        ),
                      ),
                    ),
                    const SizedBox(height: 40),
                  ]),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildAnimatedSection({required double start, required double end, required Widget child}) {
    final animation = CurvedAnimation(
      parent: _animController,
      curve: Interval(start, end, curve: Curves.easeOutCubic),
    );
    
    return FadeTransition(
      opacity: animation,
      child: SlideTransition(
        position: Tween<Offset>(begin: const Offset(0, 0.08), end: Offset.zero).animate(animation),
        child: child,
      ),
    );
  }

  Widget _buildMetricCard(BuildContext context, {required String title, required String value, required IconData icon}) {
    return GlassCard(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Icon(icon, color: AppTheme.cyanAccent, size: 24),
          const SizedBox(height: 12),
          Text(
            title,
            style: Theme.of(context).textTheme.bodyMedium,
          ),
          const SizedBox(height: 5),
          Text(
            value,
            style: Theme.of(context).textTheme.titleLarge?.copyWith(
              color: AppTheme.cyanAccent,
              fontWeight: FontWeight.bold,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildScoreRow(String label, double score, BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(label, style: Theme.of(context).textTheme.titleMedium),
        Text(
          '${score.toStringAsFixed(1)}',
          style: Theme.of(context).textTheme.titleLarge?.copyWith(
            fontWeight: FontWeight.bold,
            color: Colors.white,
          ),
        ),
      ],
    );
  }

  Widget _buildSubMetricRow(String label, String value, BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.spaceBetween,
      children: [
        Text(label, style: Theme.of(context).textTheme.bodyMedium?.copyWith(color: Colors.white54)),
        Text(
          value,
          style: Theme.of(context).textTheme.bodyMedium?.copyWith(
            color: AppTheme.cyanAccent,
            fontFamily: 'monospace',
          ),
        ),
      ],
    );
  }
}
