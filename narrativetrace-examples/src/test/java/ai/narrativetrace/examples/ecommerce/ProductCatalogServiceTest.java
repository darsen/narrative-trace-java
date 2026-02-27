package ai.narrativetrace.examples.ecommerce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProductCatalogServiceTest {

  @Test
  void lookupPriceReturnsUnitPrice() {
    ProductCatalogService service = new InMemoryProductCatalogService();

    double price = service.lookupPrice("SKU-MECHANICAL-KB");

    assertThat(price).isEqualTo(89.99);
  }

  @Test
  void lookupPriceThrowsForUnknownProduct() {
    ProductCatalogService service = new InMemoryProductCatalogService();

    assertThatThrownBy(() -> service.lookupPrice("UNKNOWN"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("UNKNOWN");
  }
}
