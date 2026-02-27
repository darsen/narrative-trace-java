package ai.narrativetrace.agent.sample;

public class StaticInit {

  static {
    System.getProperty("test.static.init");
  }

  public int value() {
    return 99;
  }
}
