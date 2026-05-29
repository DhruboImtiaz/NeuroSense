import 'dart:async';
import 'dart:math';
import 'package:flutter/material.dart';
import '../utils/theme.dart';
import '../utils/transitions.dart';
import '../widgets/animated_pulse.dart';
import '../widgets/glowing_button.dart';
import '../widgets/dynamic_waveform.dart';
import '../widgets/animated_gradient_background.dart';
import 'ai_analysis_screen.dart';

class VoiceAnalysisScreen extends StatefulWidget {
  const VoiceAnalysisScreen({Key? key}) : super(key: key);

  @override
  State<VoiceAnalysisScreen> createState() => _VoiceAnalysisScreenState();
}

class _VoiceAnalysisScreenState extends State<VoiceAnalysisScreen> {
  bool _isRecording = false;
  int _timeLeft = 10;
  Timer? _timer;

  void _startRecording() {
    setState(() {
      _isRecording = true;
      _timeLeft = 10;
    });

    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      if (_timeLeft > 0) {
        setState(() {
          _timeLeft--;
        });
      } else {
        _stopRecording();
      }
    });
  }

  void _stopRecording() {
    _timer?.cancel();
    setState(() {
      _isRecording = false;
    });
    
    final random = Random();
    double mockVoiceVariance = random.nextDouble() * 2.0; 
    
    Navigator.pushReplacement(
      context,
      FadePageRoute(page: AiAnalysisScreen(
        analysisType: 'Vocal Biomarkers',
        voiceVariance: mockVoiceVariance,
      )),
    );
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: AnimatedGradientBackground(
        child: Stack(
          children: [
            // Dark vignette overlay when recording to focus the user
            AnimatedContainer(
              duration: const Duration(seconds: 1),
              color: _isRecording ? Colors.black.withOpacity(0.6) : Colors.transparent,
            ),
            SafeArea(
              child: Column(
                children: [
                  AppBar(
                    backgroundColor: Colors.transparent,
                    title: const Text('Clinical Voice Screening'),
                    elevation: 0,
                  ),
                  Expanded(
                    child: Center(
                      child: Column(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          AnimatedOpacity(
                            duration: const Duration(seconds: 1),
                            opacity: _isRecording ? 0.3 : 1.0,
                            child: Column(
                              children: [
                                Text(
                                  'Read the phrase clearly:',
                                  style: Theme.of(context).textTheme.bodyLarge,
                                ),
                                const SizedBox(height: 25),
                                Padding(
                                  padding: const EdgeInsets.symmetric(horizontal: 40),
                                  child: Text(
                                    '"Today is a beautiful day, and the sun is shining brightly in the sky."',
                                    textAlign: TextAlign.center,
                                    style: Theme.of(context).textTheme.titleLarge?.copyWith(
                                      fontStyle: FontStyle.italic,
                                      color: AppTheme.cyanAccent,
                                      height: 1.5,
                                    ),
                                  ),
                                ),
                              ],
                            ),
                          ),
                          const SizedBox(height: 60),
                          DynamicWaveform(isRecording: _isRecording),
                          const SizedBox(height: 60),
                          AnimatedPulse(
                            isAnimating: _isRecording,
                            pulseColor: AppTheme.cyanAccent,
                            child: GestureDetector(
                              onTap: _isRecording ? null : _startRecording,
                              child: Container(
                                width: 130,
                                height: 130,
                                decoration: BoxDecoration(
                                  shape: BoxShape.circle,
                                  color: _isRecording ? AppTheme.cyanAccent.withOpacity(0.1) : AppTheme.glassWhite,
                                  border: Border.all(
                                    color: _isRecording ? AppTheme.cyanAccent : AppTheme.cyanAccent.withOpacity(0.5), 
                                    width: _isRecording ? 3 : 1
                                  ),
                                  boxShadow: _isRecording ? [
                                    BoxShadow(
                                      color: AppTheme.cyanAccent.withOpacity(0.6),
                                      blurRadius: 40,
                                      spreadRadius: 10,
                                    )
                                  ] : [],
                                ),
                                child: Icon(
                                  _isRecording ? Icons.mic : Icons.mic_none,
                                  size: 55,
                                  color: AppTheme.cyanAccent,
                                ),
                              ),
                            ),
                          ),
                          const SizedBox(height: 50),
                          if (_isRecording)
                            TweenAnimationBuilder<double>(
                              tween: Tween(begin: 0, end: 1),
                              duration: const Duration(milliseconds: 300),
                              builder: (context, value, child) {
                                return Opacity(
                                  opacity: value,
                                  child: Text(
                                    '00:${_timeLeft.toString().padLeft(2, '0')}',
                                    style: Theme.of(context).textTheme.displayLarge?.copyWith(
                                      color: AppTheme.cyanAccent,
                                      fontFeatures: const [FontFeature.tabularFigures()],
                                    ),
                                  ),
                                );
                              },
                            )
                          else
                            GlowingButton(
                              text: 'Begin Recording',
                              onPressed: _startRecording,
                            ),
                        ],
                      ),
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
