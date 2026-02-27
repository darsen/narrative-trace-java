package ai.narrativetrace.examples.minecraft.refactored;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PlayerInventoryTest {

  @Test
  void addItemReturnsTrue() {
    PlayerInventory inventory = new DefaultPlayerInventory();

    boolean added = inventory.addItem(Item.WOODEN_PICKAXE, 1);

    assertThat(added).isTrue();
  }
}
