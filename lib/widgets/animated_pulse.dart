import 'package:flutter/material.dart';
import '../utils/theme.dart';

class AnimatedPulse extends StatefulWidget {
  final Widget child;
  final Color pulseColor;
  final bool isAnimating;

  const AnimatedPulse({
    Key? key,
    required this.child,
    this.pulseColor = AppTheme.purpleAccent,
    this.isAnimating = true,
  }) : super(key: key);

  @override
  State<AnimatedPulse> createState() => _AnimatedPulseState();
}

class _AnimatedPulseState extends State<AnimatedPulse> with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _animation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 2),
    );
    
    _animation = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(parent: _controller, curve: Curves.easeOutCubic),
    );

    if (widget.isAnimating) {
      _controller.repeat();
    }
  }

  @override
  void didUpdateWidget(covariant AnimatedPulse oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (widget.isAnimating != oldWidget.isAnimating) {
      if (widget.isAnimating) {
        _controller.repeat();
      } else {
        _controller.stop();
        _controller.reset();
      }
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: _animation,
      builder: (context, child) {
        return Stack(
          alignment: Alignment.center,
          children: [
            if (widget.isAnimating)
              Container(
                width: 150 + (_animation.value * 100),
                height: 150 + (_animation.value * 100),
                decoration: BoxDecoration(
                  shape: BoxShape.circle,
                  color: widget.pulseColor.withOpacity(
                    (1.0 - _animation.value) * 0.5,
                  ),
                ),
              ),
            widget.child,
          ],
        );
      },
    );
  }
}
