package ai.narrativetrace.examples.minecraft.refactored;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CraftingTableTest {

    @Test
    void craftReturnsCraftedItem() {
        CraftingTable table = new DefaultCraftingTable();

        Item result = table.craft(Recipe.WOODEN_PICKAXE);

        assertThat(result).isEqualTo(Item.WOODEN_PICKAXE);
    }
}
