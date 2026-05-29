import 'package:flutter/material.dart';
import '../utils/theme.dart';
import '../utils/transitions.dart';
import '../widgets/glass_card.dart';
import '../widgets/glowing_button.dart';
import '../widgets/animated_gradient_background.dart';
import 'voice_analysis_screen.dart';
import 'tremor_analysis_screen.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({Key? key}) : super(key: key);

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> with SingleTickerProviderStateMixin {
  late AnimationController _animController;

  @override
  void initState() {
    super.initState();
    _animController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1500),
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
                  'NeuroSense Ecosystem',
                  style: Theme.of(context).textTheme.displayMedium?.copyWith(
                    letterSpacing: 2,
                    fontSize: 22,
                  ),
                ),
                centerTitle: true,
                floating: true,
                actions: [
                  IconButton(
                    icon: const Icon(Icons.person, color: Colors.white70),
                    onPressed: () {},
                  )
                ],
              ),
              SliverPadding(
                padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 10),
                sliver: SliverList(
                  delegate: SliverChildListDelegate([
                    const SizedBox(height: 10),
                    _buildAnimatedSection(
                      delay: 0.1,
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Patient Dashboard',
                            style: Theme.of(context).textTheme.displayLarge,
                          ),
                          const SizedBox(height: 10),
                          Text(
                            'Conduct a clinical AI screening module or review your longitudinal neurological health trends.',
                            style: Theme.of(context).textTheme.bodyLarge,
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 40),
                    _buildAnimatedSection(
                      delay: 0.2,
                      child: Text(
                        'Active Screening Modules',
                        style: Theme.of(context).textTheme.titleLarge?.copyWith(color: AppTheme.cyanAccent),
                      ),
                    ),
                    const SizedBox(height: 20),
                    _buildAnimatedSection(
                      delay: 0.3,
                      child: _buildModuleCard(
                        context: context,
                        title: 'Vocal Biomarker Analysis',
                        description: 'Analyze micro-fluctuations and pitch instability in your voice for early signs of neurological irregularities.',
                        icon: Icons.mic,
                        color: AppTheme.cyanAccent,
                        onTap: () => Navigator.push(context, FadePageRoute(page: const VoiceAnalysisScreen())),
                      ),
                    ),
                    const SizedBox(height: 25),
                    _buildAnimatedSection(
                      delay: 0.4,
                      child: _buildModuleCard(
                        context: context,
                        title: 'Kinetic Tremor Analysis',
                        description: 'Monitor high-precision accelerometer variance to detect resting and postural motor tremors.',
                        icon: Icons.waving_hand,
                        color: AppTheme.purpleAccent,
                        onTap: () => Navigator.push(context, FadePageRoute(page: const TremorAnalysisScreen())),
                      ),
                    ),
                    const SizedBox(height: 40),
                    
                    // Future Feature: Longitudinal Tracking
                    _buildAnimatedSection(
                      delay: 0.5,
                      child: Text(
                        'Longitudinal Neurological Trends',
                        style: Theme.of(context).textTheme.titleLarge?.copyWith(color: Colors.white),
                      ),
                    ),
                    const SizedBox(height: 20),
                    _buildAnimatedSection(
                      delay: 0.6,
                      child: GlassCard(
                        padding: const EdgeInsets.all(20),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: [
                                Row(
                                  children: [
                                    const Icon(Icons.timeline, color: Colors.white70, size: 20),
                                    const SizedBox(width: 10),
                                    Text('Historical Variance', style: Theme.of(context).textTheme.bodyMedium),
                                  ],
                                ),
                                Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                                  decoration: BoxDecoration(
                                    color: Colors.white.withOpacity(0.1),
                                    borderRadius: BorderRadius.circular(10),
                                  ),
                                  child: const Text('Beta Feature', style: TextStyle(fontSize: 10, color: Colors.white54)),
                                )
                              ],
                            ),
                            const SizedBox(height: 20),
                            Center(
                              child: Container(
                                height: 80,
                                decoration: BoxDecoration(
                                  border: Border(bottom: BorderSide(color: AppTheme.glassBorder), left: BorderSide(color: AppTheme.glassBorder)),
                                ),
                                child: Stack(
                                  alignment: Alignment.center,
                                  children: [
                                    Opacity(
                                      opacity: 0.3,
                                      child: CustomPaint(
                                        size: const Size(double.infinity, 60),
                                        painter: _MockChartPainter(),
                                      ),
                                    ),
                                    Text(
                                      'Awaiting Sufficient Screening Data',
                                      style: Theme.of(context).textTheme.bodySmall?.copyWith(fontStyle: FontStyle.italic),
                                    )
                                  ],
                                ),
                              ),
                            ),
                            const SizedBox(height: 15),
                            SizedBox(
                              width: double.infinity,
                              child: OutlinedButton(
                                onPressed: () {},
                                style: OutlinedButton.styleFrom(
                                  side: const BorderSide(color: AppTheme.glassBorder),
                                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
                                ),
                                child: const Text('Connect Wearable Device (Coming Soon)', style: TextStyle(color: Colors.white54)),
                              ),
                            )
                          ],
                        ),
                      ),
                    ),
                    const SizedBox(height: 40),
                    
                    _buildAnimatedSection(
                      delay: 0.8,
                      child: _buildDisclaimer(context),
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

  Widget _buildAnimatedSection({required double delay, required Widget child}) {
    final animation = CurvedAnimation(
      parent: _animController,
      curve: Interval(delay, 1.0, curve: Curves.easeOutCubic),
    );
    
    return FadeTransition(
      opacity: animation,
      child: SlideTransition(
        position: Tween<Offset>(begin: const Offset(0, 0.1), end: Offset.zero).animate(animation),
        child: child,
      ),
    );
  }

  Widget _buildModuleCard({
    required BuildContext context,
    required String title,
    required String description,
    required IconData icon,
    required Color color,
    required VoidCallback onTap,
  }) {
    return GlassCard(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: color.withOpacity(0.15),
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(color: color.withOpacity(0.5)),
                ),
                child: Icon(icon, color: color, size: 28),
              ),
              const SizedBox(width: 15),
              Expanded(
                child: Text(
                  title,
                  style: Theme.of(context).textTheme.titleLarge,
                ),
              ),
            ],
          ),
          const SizedBox(height: 15),
          Text(
            description,
            style: Theme.of(context).textTheme.bodyMedium,
          ),
          const SizedBox(height: 25),
          Center(
            child: GlowingButton(
              text: 'Start Assessment',
              icon: Icons.play_arrow,
              glowColor: color,
              onPressed: onTap,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildDisclaimer(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: Colors.amber.withOpacity(0.04),
            blurRadius: 30,
            offset: const Offset(0, 10),
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
                    Icon(
                      Icons.gavel_outlined, 
                      color: Colors.amber.withOpacity(0.9), 
                      size: 20
                    ),
                    const SizedBox(width: 10),
                    Text(
                      'Regulatory & Safety Assurance',
                      style: Theme.of(context).textTheme.titleMedium?.copyWith(
                        color: Colors.amber,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                  ],
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(
                    color: Colors.amber.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(20),
                    border: Border.all(color: Colors.amber.withOpacity(0.3), width: 1),
                  ),
                  child: Text(
                    'NON-DIAGNOSTIC SCREENING',
                    style: Theme.of(context).textTheme.bodySmall?.copyWith(
                      color: Colors.amber,
                      fontSize: 8,
                      fontWeight: FontWeight.bold,
                      letterSpacing: 0.5,
                    ),
                  ),
                )
              ],
            ),
            const Divider(color: AppTheme.glassBorder, height: 35),
            Text(
              'NeuroSense is an investigative software tool designed solely for early neurological biomarker screening assistance. Under clinical safety protocols, all generated indexes represent relative statistical variances and do not constitute, nor replace, a professional medical diagnosis or clinical evaluation. Users must consult board-certified physicians or neurologists for comprehensive diagnostic assessment and medical treatment planning.',
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                color: Colors.white.withOpacity(0.55),
                height: 1.6,
                letterSpacing: 0.1,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// Simple mock painter for the trendline UI concept
class _MockChartPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = AppTheme.cyanAccent
      ..strokeWidth = 2
      ..style = PaintingStyle.stroke;
      
    final path = Path();
    path.moveTo(0, size.height * 0.8);
    path.quadraticBezierTo(size.width * 0.2, size.height * 0.9, size.width * 0.4, size.height * 0.5);
    path.quadraticBezierTo(size.width * 0.6, size.height * 0.1, size.width * 0.8, size.height * 0.4);
    path.quadraticBezierTo(size.width * 0.9, size.height * 0.5, size.width, size.height * 0.3);
    
    canvas.drawPath(path, paint);
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}
