import 'dart:math';
import 'package:flutter/material.dart';
import '../utils/theme.dart';

class ParticleBackground extends StatefulWidget {
  const ParticleBackground({Key? key}) : super(key: key);

  @override
  State<ParticleBackground> createState() => _ParticleBackgroundState();
}

class _ParticleBackgroundState extends State<ParticleBackground> with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  final List<_Particle> _particles = [];
  final int _particleCount = 50;

  @override
  void initState() {
    super.initState();
    final random = Random();
    for (int i = 0; i < _particleCount; i++) {
      _particles.add(_Particle(
        x: random.nextDouble(),
        y: random.nextDouble(),
        speedX: (random.nextDouble() - 0.5) * 0.005,
        speedY: (random.nextDouble() - 0.5) * 0.005,
        radius: random.nextDouble() * 3 + 1,
        color: random.nextBool() ? AppTheme.cyanAccent : AppTheme.purpleAccent,
      ));
    }

    _controller = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 10),
    )..addListener(() {
        setState(() {
          for (var particle in _particles) {
            particle.update();
          }
        });
      })
      ..repeat();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return CustomPaint(
      painter: _ParticlePainter(_particles),
      size: Size.infinite,
    );
  }
}

class _Particle {
  double x;
  double y;
  double speedX;
  double speedY;
  double radius;
  Color color;

  _Particle({
    required this.x,
    required this.y,
    required this.speedX,
    required this.speedY,
    required this.radius,
    required this.color,
  });

  void update() {
    x += speedX;
    y += speedY;

    if (x < 0 || x > 1) speedX *= -1;
    if (y < 0 || y > 1) speedY *= -1;
  }
}

class _ParticlePainter extends CustomPainter {
  final List<_Particle> particles;

  _ParticlePainter(this.particles);

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint();

    for (var particle in particles) {
      paint.color = particle.color.withOpacity(0.5);
      canvas.drawCircle(
        Offset(particle.x * size.width, particle.y * size.height),
        particle.radius,
        paint,
      );
    }
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => true;
}
