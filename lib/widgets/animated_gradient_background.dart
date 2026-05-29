import 'package:flutter/material.dart';
import '../utils/theme.dart';

class AnimatedGradientBackground extends StatefulWidget {
  final Widget child;
  const AnimatedGradientBackground({Key? key, required this.child}) : super(key: key);

  @override
  State<AnimatedGradientBackground> createState() => _AnimatedGradientBackgroundState();
}

class _AnimatedGradientBackgroundState extends State<AnimatedGradientBackground> with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<Color?> _color1;
  late Animation<Color?> _color2;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 10),
    )..repeat(reverse: true);

    _color1 = ColorTween(
      begin: AppTheme.backgroundDeepNavy,
      end: AppTheme.backgroundBlack,
    ).animate(_controller);

    _color2 = ColorTween(
      begin: AppTheme.backgroundBlack,
      end: AppTheme.backgroundDeepNavy,
    ).animate(_controller);
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
        return Container(
          decoration: BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
              colors: [
                _color1.value ?? AppTheme.backgroundDeepNavy,
                _color2.value ?? AppTheme.backgroundBlack,
              ],
            ),
          ),
          child: child,
        );
      },
      child: widget.child,
    );
  }
}
