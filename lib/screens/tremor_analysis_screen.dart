import 'dart:async';
import 'package:flutter/material.dart';
import 'package:sensors_plus/sensors_plus.dart';
import 'package:fl_chart/fl_chart.dart';
import '../utils/theme.dart';
import '../utils/transitions.dart';
import '../widgets/animated_pulse.dart';
import '../widgets/glowing_button.dart';
import '../widgets/animated_gradient_background.dart';
import 'ai_analysis_screen.dart';

class TremorAnalysisScreen extends StatefulWidget {
  const TremorAnalysisScreen({Key? key}) : super(key: key);

  @override
  State<TremorAnalysisScreen> createState() => _TremorAnalysisScreenState();
}

class _TremorAnalysisScreenState extends State<TremorAnalysisScreen> {
  bool _isRecording = false;
  int _timeLeft = 10;
  Timer? _timer;
  
  double _x = 0, _y = 0, _z = 0;
  StreamSubscription? _accelSubscription;
  final List<FlSpot> _xSpots = [];
  final List<FlSpot> _ySpots = [];
  final List<FlSpot> _zSpots = [];
  double _timeCounter = 0;
  final List<double> _magnitudeHistory = [];

  void _startRecording() {
    setState(() {
      _isRecording = true;
      _timeLeft = 10;
      _xSpots.clear();
      _ySpots.clear();
      _zSpots.clear();
      _timeCounter = 0;
      _magnitudeHistory.clear();
    });

    _accelSubscription = accelerometerEventStream().listen((AccelerometerEvent event) {
      if (mounted && _isRecording) {
        setState(() {
          _x = event.x;
          _y = event.y;
          _z = event.z;
          
          double magnitude = event.x.abs() + event.y.abs() + event.z.abs();
          _magnitudeHistory.add(magnitude);
          
          _timeCounter += 0.1;
          if (_xSpots.length > 50) {
            _xSpots.removeAt(0);
            _ySpots.removeAt(0);
            _zSpots.removeAt(0);
          }
          _xSpots.add(FlSpot(_timeCounter, _x));
          _ySpots.add(FlSpot(_timeCounter, _y));
          _zSpots.add(FlSpot(_timeCounter, _z));
        });
      }
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
    _accelSubscription?.cancel();
    setState(() {
      _isRecording = false;
    });
    
    double variance = 0.0;
    if (_magnitudeHistory.isNotEmpty) {
      double mean = _magnitudeHistory.reduce((a, b) => a + b) / _magnitudeHistory.length;
      double sumSquareDiff = _magnitudeHistory.map((val) => (val - mean) * (val - mean)).reduce((a, b) => a + b);
      variance = sumSquareDiff / _magnitudeHistory.length;
    }
    
    Navigator.pushReplacement(
      context,
      FadePageRoute(page: AiAnalysisScreen(
        analysisType: 'Kinetic Tremor Signatures',
        tremorVariance: variance,
      )),
    );
  }

  @override
  void dispose() {
    _timer?.cancel();
    _accelSubscription?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: AnimatedGradientBackground(
        child: Stack(
          children: [
            AnimatedContainer(
              duration: const Duration(seconds: 1),
              color: _isRecording ? Colors.black.withOpacity(0.6) : Colors.transparent,
            ),
            SafeArea(
              child: Column(
                children: [
                  AppBar(
                    backgroundColor: Colors.transparent,
                    title: const Text('Clinical Tremor Screening'),
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
                                  'Hold your phone steadily',
                                  style: Theme.of(context).textTheme.displayMedium,
                                ),
                                const SizedBox(height: 10),
                                Text(
                                  'Extend your arm and hold for 10 seconds.',
                                  style: Theme.of(context).textTheme.bodyLarge?.copyWith(
                                    color: AppTheme.purpleAccent,
                                  ),
                                ),
                              ],
                            ),
                          ),
                          const SizedBox(height: 40),
                          
                          Container(
                            height: 120,
                            padding: const EdgeInsets.symmetric(horizontal: 20),
                            child: _isRecording && _xSpots.isNotEmpty ? LineChart(
                              LineChartData(
                                gridData: const FlGridData(show: false),
                                titlesData: const FlTitlesData(show: false),
                                borderData: FlBorderData(show: false),
                                minY: -20,
                                maxY: 20,
                                lineBarsData: [
                                  LineChartBarData(
                                    spots: _xSpots,
                                    isCurved: true,
                                    color: AppTheme.cyanAccent,
                                    dotData: const FlDotData(show: false),
                                    belowBarData: BarAreaData(show: true, color: AppTheme.cyanAccent.withOpacity(0.1)),
                                  ),
                                  LineChartBarData(
                                    spots: _ySpots,
                                    isCurved: true,
                                    color: AppTheme.purpleAccent,
                                    dotData: const FlDotData(show: false),
                                  ),
                                ],
                              ),
                            ) : Center(
                              child: Text(
                                'Awaiting Sensor Activation',
                                style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                                  fontStyle: FontStyle.italic,
                                ),
                              ),
                            ),
                          ),
                          
                          const SizedBox(height: 40),
                          AnimatedPulse(
                            isAnimating: _isRecording,
                            pulseColor: AppTheme.purpleAccent,
                            child: Container(
                              width: 190,
                              height: 190,
                              decoration: BoxDecoration(
                                shape: BoxShape.circle,
                                color: AppTheme.glassWhite,
                                border: Border.all(
                                  color: _isRecording ? AppTheme.purpleAccent : AppTheme.purpleAccent.withOpacity(0.5), 
                                  width: _isRecording ? 3 : 1
                                ),
                                boxShadow: _isRecording ? [
                                  BoxShadow(
                                    color: AppTheme.purpleAccent.withOpacity(0.5),
                                    blurRadius: 40,
                                    spreadRadius: 10,
                                  )
                                ] : [],
                              ),
                              child: Center(
                                child: AnimatedContainer(
                                  duration: const Duration(milliseconds: 100),
                                  width: 20 + (_x.abs() + _y.abs() + _z.abs()).clamp(0, 50) * 3,
                                  height: 20 + (_x.abs() + _y.abs() + _z.abs()).clamp(0, 50) * 3,
                                  decoration: BoxDecoration(
                                    shape: BoxShape.circle,
                                    color: AppTheme.purpleAccent,
                                    boxShadow: [
                                      BoxShadow(
                                        color: AppTheme.purpleAccent,
                                        blurRadius: 15,
                                        spreadRadius: 3,
                                      )
                                    ],
                                  ),
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
                                      color: AppTheme.purpleAccent,
                                      fontFeatures: const [FontFeature.tabularFigures()],
                                    ),
                                  ),
                                );
                              },
                            )
                          else
                            GlowingButton(
                              text: 'Begin Assessment',
                              glowColor: AppTheme.purpleAccent,
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
