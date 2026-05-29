import 'dart:math';
import 'package:flutter/material.dart';
import '../utils/theme.dart';

class DynamicWaveform extends StatefulWidget {
  final bool isRecording;

  const DynamicWaveform({Key? key, required this.isRecording}) : super(key: key);

  @override
  State<DynamicWaveform> createState() => _DynamicWaveformState();
}

class _DynamicWaveformState extends State<DynamicWaveform> with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  final Random _random = Random();
  late List<double> _heights;

  @override
  void initState() {
    super.initState();
    _heights = List.generate(30, (index) => _random.nextDouble());
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 100),
    )..addListener(() {
        if (widget.isRecording) {
          setState(() {
            for (int i = 0; i < _heights.length; i++) {
              _heights[i] = _random.nextDouble();
            }
          });
        }
      });
      
    if (widget.isRecording) {
      _controller.repeat();
    }
  }

  @override
  void didUpdateWidget(covariant DynamicWaveform oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.isRecording && !oldWidget.isRecording) {
      _controller.repeat();
    } else if (!widget.isRecording && oldWidget.isRecording) {
      _controller.stop();
      setState(() {
        _heights = List.generate(30, (index) => 0.1);
      });
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: 60,
      child: Row(
        mainAxisAlignment: MainAxisAlignment.center,
        crossAxisAlignment: CrossAxisAlignment.center,
        children: List.generate(30, (index) {
          final height = widget.isRecording ? 10 + (_heights[index] * 50) : 10.0;
          return AnimatedContainer(
            duration: const Duration(milliseconds: 100),
            margin: const EdgeInsets.symmetric(horizontal: 2),
            width: 4,
            height: height,
            decoration: BoxDecoration(
              color: AppTheme.cyanAccent.withOpacity(0.8),
              borderRadius: BorderRadius.circular(2),
              boxShadow: [
                BoxShadow(
                  color: AppTheme.cyanAccent.withOpacity(0.5),
                  blurRadius: 5,
                  spreadRadius: 1,
                ),
              ],
            ),
          );
        }),
      ),
    );
  }
}
