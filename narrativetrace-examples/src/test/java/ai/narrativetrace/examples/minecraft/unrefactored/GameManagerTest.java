package ai.narrativetrace.examples.minecraft.unrefactored;

import ai.narrativetrace.core.context.NarrativeContext;
import ai.narrativetrace.core.render.IndentedTextRenderer;
import ai.narrativetrace.junit5.NarrativeTraceExtension;
import ai.narrativetrace.proxy.NarrativeTraceProxy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(NarrativeTraceExtension.class)
class GameManagerTest {

    @Test
    void handleOrchestratesSameFlowWithGenericNames(NarrativeContext context) {
        var dataProcessor = NarrativeTraceProxy.trace(
                new DefaultDataProcessor(), DataProcessor.class, context);
        var stateManager = NarrativeTraceProxy.trace(
                new DefaultStateManager(), StateManager.class, context);
        var thingFactory = NarrativeTraceProxy.trace(
                new DefaultThingFactory(), ThingFactory.class, context);
        var entityHandler = NarrativeTraceProxy.trace(
                new DefaultEntityHandler(), EntityHandler.class, context);

        var manager = NarrativeTraceProxy.trace(
                new DefaultGameManager(dataProcessor, stateManager, thingFactory, entityHandler),
                GameManager.class, context);

        String result = manager.handle("Steve");

        assertThat(result).contains("Steve");

        var narrative = new IndentedTextRenderer().render(context.captureTrace());
        // Only generic names — impossible to understand the domain
        assertThat(narrative).contains("GameManager.handle");
        assertThat(narrative).contains("DataProcessor.process");
        assertThat(narrative).contains("StateManager.update");
        assertThat(narrative).contains("ThingFactory.create");
        assertThat(narrative).contains("EntityHandler.execute");
        // None of the domain-specific names appear
        assertThat(narrative).doesNotContain("WorldGenerator");
        assertThat(narrative).doesNotContain("PlayerInventory");
        assertThat(narrative).doesNotContain("CraftingTable");
        assertThat(narrative).doesNotContain("CreatureSpawner");
    }
}
