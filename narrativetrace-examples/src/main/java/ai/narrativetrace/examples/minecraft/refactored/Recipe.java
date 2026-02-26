package ai.narrativetrace.examples.minecraft.refactored;

public enum Recipe {
    WOODEN_PICKAXE(Item.WOODEN_PICKAXE),
    OAK_PLANKS(Item.OAK_PLANKS),
    STICK(Item.STICK);

    private final Item result;

    Recipe(Item result) {
        this.result = result;
    }

    public Item result() {
        return result;
    }
}
