package ai.narrativetrace.examples.minecraft.refactored;

public class DefaultPlayerInventory implements PlayerInventory {

    @Override
    public boolean addItem(Item item, int quantity) {
        return true;
    }
}
