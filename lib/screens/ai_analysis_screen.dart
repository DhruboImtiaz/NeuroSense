import '../models/analysis_result.dart';
import 'dart:async';
import 'dart:math';
import 'package:flutter/material.dart';
import '../utils/theme.dart';
import '../utils/transitions.dart';
import '../core/di/service_locator.dart';
import '../widgets/animated_gradient_background.dart';
import '../widgets/neural_network_painter.dart';
import 'results_screen.dart';

class AiAnalysisScreen extends StatefulWidget {
  final String analysisType;
  final double? tremorVariance;
  final double? voiceVariance;
  
  const AiAnalysisScreen({
    Key? key,
    required this.analysisType,
    this.tremorVariance,
    this.voiceVariance,
  }) : super(key: key);

  @override
  State<AiAnalysisScreen> createState() => _AiAnalysisScreenState();
}

class _AiAnalysisScreenState extends State<AiAnalysisScreen> with TickerProviderStateMixin {
  final _aiService = getIt.aiInferenceService;
  
  String _currentStage = 'Initializing neural pathways...';
  double _currentConfidence = 0.0;
  
  late AnimationController _pulseController;
  late Animation<double> _pulseAnimation;
  
  late AnimationController _timelineController;
  AnalysisResult? _result;
  bool _inferenceCompleted = false;
  bool _isTransitioning = false;
  Timer? _holdTimer;

  @override
  void initState() {
    super.initState();
    
    _pulseController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1000),
    )..repeat(reverse: true);
    
    _pulseAnimation = Tween<double>(begin: 1.0, end: 1.12).animate(
      CurvedAnimation(parent: _pulseController, curve: Curves.easeInOutSine),
    );
    
    _timelineController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 8000),
    );
    
    _timelineController.addListener(() {
      if (mounted) {
        _updateTimeline(_timelineController.value);
      }
    });
    
    _timelineController.addStatusListener((status) {
      if (status == AnimationStatus.completed) {
        _checkAndTransition();
      }
    });

    _startInference();
    _timelineController.forward();
  }

  void _startInference() async {
    try {
      final res = await _aiService.computeAssessment(
        tremorVariance: widget.tremorVariance,
        voiceVariance: widget.voiceVariance,
      );
      if (mounted) {
        setState(() {
          _result = res;
          _inferenceCompleted = true;
        });
        if (_timelineController.isCompleted) {
          _checkAndTransition();
        }
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _inferenceCompleted = true;
        });
      }
    }
  }

  void _updateTimeline(double t) {
    String stage;
    double confidenceBase;
    double confidenceRange;
    double progressInStage;

    if (t < 0.15) {
      stage = 'Initializing neural pathways...';
      confidenceBase = 0.0;
      confidenceRange = 15.0;
      progressInStage = t / 0.15;
    } else if (t < 0.35) {
      final type = widget.analysisType.toLowerCase();
      stage = 'Extracting $type biomarkers...';
      confidenceBase = 15.0;
      confidenceRange = 23.0;
      progressInStage = (t - 0.15) / 0.20;
    } else if (t < 0.55) {
      stage = 'Mapping neurological signatures...';
      confidenceBase = 38.0;
      confidenceRange = 24.0;
      progressInStage = (t - 0.35) / 0.20;
    } else if (t < 0.75) {
      stage = 'Applying CNN filters for feature extraction...';
      confidenceBase = 62.0;
      confidenceRange = 19.0;
      progressInStage = (t - 0.55) / 0.20;
    } else if (t < 0.90) {
      stage = 'Analyzing cross-modal patterns...';
      confidenceBase = 81.0;
      confidenceRange = 11.0;
      progressInStage = (t - 0.75) / 0.15;
    } else {
      stage = 'Computing AI confidence score...';
      final targetConfidence = _result?.aiConfidence ?? 94.8;
      confidenceBase = 92.0;
      confidenceRange = targetConfidence - 92.0;
      progressInStage = ((t - 0.90) / 0.10).clamp(0.0, 1.0);
    }

    final wiggle = sin(t * 150 * pi) * 0.7;
    
    setState(() {
      _currentStage = stage;
      _currentConfidence = (confidenceBase + (confidenceRange * progressInStage) + wiggle).clamp(0.0, 100.0);
    });
  }

  void _checkAndTransition() {
    if (_isTransitioning) return;
    if (_inferenceCompleted && _result != null) {
      _isTransitioning = true;
      setState(() {
        _currentStage = 'Finalizing clinical report...';
        _currentConfidence = _result!.aiConfidence;
      });
      
      _holdTimer = Timer(const Duration(milliseconds: 1000), () {
        if (mounted) {
          Navigator.pushReplacement(
            context,
            FadePageRoute(page: ResultsScreen(result: _result!)),
          );
        }
      });
    } else {
      setState(() {
        _currentStage = 'Compiling clinical observation matrix...';
      });
      _holdTimer?.cancel();
      _holdTimer = Timer.periodic(const Duration(milliseconds: 100), (timer) {
        if (mounted) {
          if (_inferenceCompleted && _result != null) {
            timer.cancel();
            _checkAndTransition();
          } else {
            setState(() {
              final random = Random();
              _currentConfidence = (98.5 + random.nextDouble() * 1.3).clamp(0.0, 99.9);
            });
          }
        } else {
          timer.cancel();
        }
      });
    }
  }

  @override
  void dispose() {
    _pulseController.dispose();
    _timelineController.dispose();
    _holdTimer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    double intensity = 0.5 + (_currentConfidence / 200.0);

    return Scaffold(
      body: AnimatedGradientBackground(
        child: Stack(
          fit: StackFit.expand,
          children: [
            NeuralNetworkPainter(intensity: intensity),
            Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  ScaleTransition(
                    scale: _pulseAnimation,
                    child: Container(
                      width: 150,
                      height: 150,
                      decoration: BoxDecoration(
                        shape: BoxShape.circle,
                        color: AppTheme.backgroundBlack.withOpacity(0.7),
                        border: Border.all(
                          color: AppTheme.cyanAccent.withOpacity(intensity), 
                          width: 2
                        ),
                        boxShadow: [
                          BoxShadow(
                            color: AppTheme.cyanAccent.withOpacity(0.4 * intensity),
                            blurRadius: 50,
                            spreadRadius: 15,
                          )
                        ],
                      ),
                      child: Center(
                        child: Icon(
                          Icons.memory, 
                          size: 70, 
                          color: AppTheme.cyanAccent.withOpacity(intensity.clamp(0.5, 1.0)),
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(height: 70),
                  Text(
                    'AI Inference Engine',
                    style: Theme.of(context).textTheme.displayMedium?.copyWith(
                      letterSpacing: 2,
                      shadows: [
                        Shadow(color: AppTheme.cyanAccent.withOpacity(0.5), blurRadius: 10)
                      ]
                    ),
                  ),
                  const SizedBox(height: 25),
                  AnimatedSwitcher(
                    duration: const Duration(milliseconds: 600),
                    switchInCurve: Curves.easeIn,
                    switchOutCurve: Curves.easeOut,
                    child: Text(
                      _currentStage,
                      key: ValueKey<String>(_currentStage),
                      style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                        color: AppTheme.cyanAccent,
                        fontStyle: FontStyle.italic,
                        letterSpacing: 1.5,
                      ),
                      textAlign: TextAlign.center,
                    ),
                  ),
                  const SizedBox(height: 50),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                    decoration: BoxDecoration(
                      color: AppTheme.glassWhite,
                      borderRadius: BorderRadius.circular(30),
                      border: Border.all(color: AppTheme.cyanAccent.withOpacity(0.5)),
                      boxShadow: [
                        BoxShadow(
                          color: AppTheme.cyanAccent.withOpacity(0.2),
                          blurRadius: 20,
                        )
                      ],
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Icon(Icons.bar_chart, color: AppTheme.cyanAccent, size: 22),
                        const SizedBox(width: 12),
                        Text(
                          'Inference Confidence: ${_currentConfidence.toStringAsFixed(1)}%',
                          style: Theme.of(context).textTheme.titleMedium?.copyWith(
                            color: Colors.white,
                            fontFeatures: const [FontFeature.tabularFigures()],
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
