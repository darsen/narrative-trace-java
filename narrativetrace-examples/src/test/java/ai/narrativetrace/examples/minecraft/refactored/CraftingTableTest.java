package ai.narrativetrace.examples.minecraft.refactored;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CraftingTableTest {

  @Test
  void craftReturnsCraftedItem() {
    CraftingTable table = new DefaultCraftingTable();

    Item result = table.craft(Recipe.WOODEN_PICKAXE);

    assertThat(result).isEqualTo(Item.WOODEN_PICKAXE);
  }
}
