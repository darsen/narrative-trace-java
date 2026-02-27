package ai.narrativetrace.examples.minecraft.refactored;

public class DefaultCraftingTable implements CraftingTable {

  @Override
  public Item craft(Recipe recipe) {
    return recipe.result();
  }
}
