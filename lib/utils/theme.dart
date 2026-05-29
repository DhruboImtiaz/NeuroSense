import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class AppTheme {
  static const Color backgroundDeepNavy = Color(0xFF070B14);
  static const Color backgroundBlack = Color(0xFF030406);
  static const Color cyanAccent = Color(0xFF00E5FF);
  static const Color purpleAccent = Color(0xFF9D00FF);
  
  // More premium frosted glass colors
  static const Color glassWhite = Color(0x0AFFFFFF); 
  static const Color glassBorder = Color(0x1AFFFFFF);
  
  static const Color riskLow = Color(0xFF00FF9D);
  static const Color riskModerate = Color(0xFFFFB800);
  static const Color riskElevated = Color(0xFFFF3366);

  static ThemeData get darkTheme {
    return ThemeData(
      brightness: Brightness.dark,
      scaffoldBackgroundColor: backgroundDeepNavy,
      primaryColor: cyanAccent,
      colorScheme: const ColorScheme.dark(
        primary: cyanAccent,
        secondary: purpleAccent,
        background: backgroundDeepNavy,
        surface: backgroundBlack,
      ),
      textTheme: GoogleFonts.interTextTheme(
        ThemeData.dark().textTheme,
      ).copyWith(
        displayLarge: GoogleFonts.outfit(
          fontSize: 34,
          fontWeight: FontWeight.bold,
          color: Colors.white,
          letterSpacing: -0.5,
          height: 1.2,
        ),
        displayMedium: GoogleFonts.outfit(
          fontSize: 26,
          fontWeight: FontWeight.bold,
          color: Colors.white,
          letterSpacing: -0.2,
          height: 1.3,
        ),
        titleLarge: GoogleFonts.outfit(
          fontSize: 20,
          fontWeight: FontWeight.w600,
          color: Colors.white,
          letterSpacing: 0.1,
          height: 1.4,
        ),
        titleMedium: GoogleFonts.outfit(
          fontSize: 16,
          fontWeight: FontWeight.w600,
          color: Colors.white,
          letterSpacing: 0.1,
          height: 1.4,
        ),
        bodyLarge: GoogleFonts.inter(
          fontSize: 15,
          fontWeight: FontWeight.w400,
          color: Colors.white.withOpacity(0.85),
          letterSpacing: 0.1,
          height: 1.6,
        ),
        bodyMedium: GoogleFonts.inter(
          fontSize: 13,
          fontWeight: FontWeight.w400,
          color: Colors.white.withOpacity(0.65),
          letterSpacing: 0.1,
          height: 1.5,
        ),
        bodySmall: GoogleFonts.inter(
          fontSize: 11,
          fontWeight: FontWeight.w400,
          color: Colors.white.withOpacity(0.5),
          letterSpacing: 0.05,
          height: 1.4,
        ),
      ),
      appBarTheme: AppBarTheme(
        backgroundColor: Colors.transparent,
        elevation: 0,
        centerTitle: true,
        titleTextStyle: GoogleFonts.outfit(
          fontSize: 18,
          fontWeight: FontWeight.w600,
          color: Colors.white,
          letterSpacing: 1.5,
        ),
      ),
    );
  }
}
