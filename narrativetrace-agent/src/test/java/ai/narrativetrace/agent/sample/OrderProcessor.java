package ai.narrativetrace.agent.sample;

public class OrderProcessor {

  private final Calculator calculator = new Calculator();

  public int processOrder(int price, int quantity) {
    return calculator.add(price, quantity);
  }
}
