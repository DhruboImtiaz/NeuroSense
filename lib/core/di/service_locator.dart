import '../interfaces/ai_inference_interface.dart';
import '../../services/mock_ai_inference_service.dart';

/// Simple service locator for dependency injection.
/// This prepares the architecture for large scale production use.
class ServiceLocator {
  static final ServiceLocator _instance = ServiceLocator._internal();
  
  factory ServiceLocator() {
    return _instance;
  }
  
  ServiceLocator._internal();

  late final IAiInferenceService aiInferenceService;

  void setup() {
    // In production, this can conditionally inject different implementations
    // e.g., if (env == 'prod') aiInferenceService = CloudAiInferenceService();
    // else aiInferenceService = MockAiInferenceService();
    
    aiInferenceService = MockAiInferenceService();
  }
}

final getIt = ServiceLocator();
