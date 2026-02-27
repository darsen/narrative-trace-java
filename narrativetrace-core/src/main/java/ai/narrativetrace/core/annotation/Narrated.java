package ai.narrativetrace.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Customizes the narrative template for a traced method.
 *
 * <p>The template replaces the auto-generated description with a hand-written sentence.
 * Placeholders reference method parameters by name and support property access:
 *
 * <pre>{@code
 * @Narrated("place order for {item} with quantity {quantity}")
 * OrderResult placeOrder(String item, int quantity);
 *
 * @Narrated("transfer {amount} from {source.accountId} to {target.accountId}")
 * TransferResult transfer(Account source, Account target, Money amount);
 * }</pre>
 *
 * <p>Placeholder syntax:
 *
 * <ul>
 *   <li>{@code {paramName}} — renders the parameter value via {@code ValueRenderer}
 *   <li>{@code {param.property}} — calls the getter on the raw object before serialization
 * </ul>
 *
 * <p>Templates are resolved at capture time (in the proxy or agent), before eager serialization.
 * This ensures {@code {param.property}} access works on live objects.
 *
 * @see OnError
 * @see ai.narrativetrace.core.template.TemplateParser
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Narrated {
  /**
   * The narrative template with {@code {paramName}} placeholders.
   *
   * @return the template string
   */
  String value();
}
