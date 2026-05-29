import 'dart:math';
import 'package:flutter/material.dart';
import '../utils/theme.dart';

class NeuralNetworkPainter extends StatefulWidget {
  final double intensity;

  const NeuralNetworkPainter({Key? key, this.intensity = 1.0}) : super(key: key);

  @override
  State<NeuralNetworkPainter> createState() => _NeuralNetworkPainterState();
}

class _NeuralNetworkPainterState extends State<NeuralNetworkPainter> with SingleTickerProviderStateMixin {
  late AnimationController _controller;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 3),
    )..repeat();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _controller,
      builder: (context, child) {
        return CustomPaint(
          painter: _NetworkPainter(_controller.value, widget.intensity),
          size: Size.infinite,
        );
      },
    );
  }
}

class _NetworkPainter extends CustomPainter {
  final double progress;
  final double intensity;

  _NetworkPainter(this.progress, this.intensity);

  @override
  void paint(Canvas canvas, Size size) {
    // 1. Draw Background Layer (Distant depth)
    final bgPaint = Paint()
      ..strokeWidth = 1.0
      ..style = PaintingStyle.stroke;

    final bgDotPaint = Paint()
      ..style = PaintingStyle.fill;

    // Use a different seed for background nodes to decouple positions
    final bgRandom = Random(101);
    final bgNodes = <Offset>[];
    for (int i = 0; i < 18; i++) {
      bgNodes.add(Offset(
        bgRandom.nextDouble() * size.width,
        bgRandom.nextDouble() * size.height,
      ));
    }

    // Draw background lines
    for (int i = 0; i < bgNodes.length; i++) {
      for (int j = i + 1; j < bgNodes.length; j++) {
        final distance = (bgNodes[i] - bgNodes[j]).distance;
        if (distance < size.width * 0.4) {
          final opacity = (1.0 - (distance / (size.width * 0.4))) * 0.15;
          // Background lines use purple accent and animate at half speed
          bgPaint.color = AppTheme.purpleAccent.withOpacity(
            opacity * intensity * (0.3 + 0.7 * sin(progress * pi + i)),
          );
          canvas.drawLine(bgNodes[i], bgNodes[j], bgPaint);
        }
      }
      
      // Draw background dots
      final bgGlow = 0.5 + 0.5 * sin(progress * pi + i * 0.5);
      bgDotPaint.color = AppTheme.purpleAccent.withOpacity(0.3 * intensity);
      canvas.drawCircle(bgNodes[i], 1.5 + (bgGlow * 1.0 * intensity), bgDotPaint);
    }

    // 2. Draw Foreground Layer (Primary network)
    final paint = Paint()
      ..strokeWidth = 1.5
      ..style = PaintingStyle.stroke;

    final dotPaint = Paint()
      ..style = PaintingStyle.fill;

    final random = Random(42); 
    final nodes = <Offset>[];
    for (int i = 0; i < 24; i++) {
      nodes.add(Offset(
        random.nextDouble() * size.width,
        random.nextDouble() * size.height,
      ));
    }

    // Primary lines
    for (int i = 0; i < nodes.length; i++) {
      // Draw lines
      for (int j = i + 1; j < nodes.length; j++) {
        final distance = (nodes[i] - nodes[j]).distance;
        if (distance < size.width * 0.35) {
          final opacity = (1.0 - (distance / (size.width * 0.35))) * 0.6;
          paint.color = AppTheme.cyanAccent.withOpacity(opacity * intensity * (0.4 + 0.6 * sin(progress * 2 * pi + i)));
          canvas.drawLine(nodes[i], nodes[j], paint);
        }
      }
      
      // Draw glowing nodes
      final nodeGlow = 0.5 + 0.5 * sin(progress * 2 * pi + i * 0.5);
      dotPaint.color = AppTheme.cyanAccent.withOpacity(0.8 * intensity);
      canvas.drawCircle(nodes[i], 3.0 + (nodeGlow * 2.0 * intensity), dotPaint);
      
      dotPaint.color = AppTheme.cyanAccent.withOpacity(0.2 * nodeGlow * intensity);
      canvas.drawCircle(nodes[i], 7.0 + (nodeGlow * 4.0 * intensity), dotPaint);
    }

    // 3. Draw Aggressive Pulsing Central Core
    final center = Offset(size.width * 0.5, size.height * 0.45);
    
    // Aggressive speed scale: as intensity increases, the core pulses extremely fast!
    final coreSpeedMultiplier = 1.0 + (intensity - 0.5) * 8.0; 
    final corePulse = sin(progress * 2 * pi * coreSpeedMultiplier);
    
    // Core Paint
    final corePaint = Paint()..style = PaintingStyle.fill;
    
    // Outer radial glow that pulses and expands
    final outerGlowRadius = 40.0 + (corePulse * 20.0 * intensity) + (intensity * 25.0);
    final outerGlowPaint = Paint()
      ..shader = RadialGradient(
        colors: [
          AppTheme.purpleAccent.withOpacity(0.55 * intensity),
          AppTheme.cyanAccent.withOpacity(0.15 * intensity),
          Colors.transparent,
        ],
      ).createShader(Rect.fromCircle(center: center, radius: outerGlowRadius));
    
    canvas.drawCircle(center, outerGlowRadius, outerGlowPaint);

    // Mid-core pulsing solid ring
    final midGlowRadius = 15.0 + (corePulse * 8.0 * intensity) + (intensity * 10.0);
    corePaint.color = AppTheme.cyanAccent.withOpacity(0.7 * intensity);
    canvas.drawCircle(center, midGlowRadius, corePaint);
    
    // Solid white hot center
    final innerCoreRadius = 6.0 + (corePulse * 3.0 * intensity);
    corePaint.color = Colors.white.withOpacity(0.9 * intensity);
    canvas.drawCircle(center, innerCoreRadius, corePaint);

    // 4. Energy beams connecting the core to the closest 6 primary nodes
    final beamPaint = Paint()
      ..strokeWidth = 2.0 * intensity
      ..style = PaintingStyle.stroke;

    final sortedNodes = List<Offset>.from(nodes);
    sortedNodes.sort((a, b) => (a - center).distanceSquared.compareTo((b - center).distanceSquared));

    for (int k = 0; k < min(6, sortedNodes.length); k++) {
      final node = sortedNodes[k];
      final dist = (node - center).distance;
      if (dist < size.width * 0.45) {
        final beamPulse = 0.5 + 0.5 * sin(progress * 4 * pi * coreSpeedMultiplier - k);
        // Energy beams flash aggressively between cyan and purple
        beamPaint.color = Color.lerp(
          AppTheme.cyanAccent, 
          AppTheme.purpleAccent, 
          beamPulse
        )!.withOpacity(0.7 * beamPulse * intensity);
        
        canvas.drawLine(center, node, beamPaint);
        
        // Draw energy packets travelling down the beam
        final packetProgress = (progress * 2.0 * coreSpeedMultiplier + k * 0.25) % 1.0;
        final packetPos = Offset.lerp(center, node, packetProgress)!;
        final packetPaint = Paint()
          ..color = Colors.white.withOpacity(0.9 * intensity)
          ..style = PaintingStyle.fill;
        canvas.drawCircle(packetPos, 2.5 + (1.5 * intensity), packetPaint);
      }
    }
  }

  @override
  bool shouldRepaint(covariant _NetworkPainter oldDelegate) {
    return oldDelegate.progress != progress || oldDelegate.intensity != intensity;
  }
}
