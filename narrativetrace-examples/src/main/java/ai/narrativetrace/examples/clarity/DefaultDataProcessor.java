package ai.narrativetrace.examples.clarity;

public class DefaultDataProcessor implements DataProcessor {

  @Override
  public String execute(String data, int val) {
    return "processed:" + data + ":" + val;
  }
}
