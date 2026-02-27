package ai.narrativetrace.core.config;

/** Thrown when conflicting configuration sources provide incompatible settings. */
public class DuplicateConfigurationException extends RuntimeException {

  public DuplicateConfigurationException(String message) {
    super(message);
  }
}
