import 'package:flutter/material.dart';
import 'utils/theme.dart';
import 'core/di/service_locator.dart';
import 'screens/splash_screen.dart';

void main() {
  // Initialize dependency injection / service locator
  getIt.setup();
  
  runApp(const NeuroSenseApp());
}

class NeuroSenseApp extends StatelessWidget {
  const NeuroSenseApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'NeuroSense',
      theme: AppTheme.darkTheme,
      home: const SplashScreen(),
      debugShowCheckedModeBanner: false,
    );
  }
}
