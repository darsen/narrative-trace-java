package ai.narrativetrace.examples.minecraft;

import ai.narrativetrace.core.context.ThreadLocalNarrativeContext;
import ai.narrativetrace.core.render.IndentedTextRenderer;
import ai.narrativetrace.diagrams.MermaidSequenceDiagramRenderer;
import ai.narrativetrace.examples.minecraft.refactored.*;
import ai.narrativetrace.examples.minecraft.unrefactored.*;
import ai.narrativetrace.proxy.NarrativeTraceProxy;
import ai.narrativetrace.slf4j.Slf4jNarrativeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MinecraftExample {

  private static final Logger logger = LoggerFactory.getLogger(MinecraftExample.class);

  public static void main(String[] args) {
    var context = new Slf4jNarrativeContext(new ThreadLocalNarrativeContext());
    var renderer = new IndentedTextRenderer();
    var mermaidRenderer = new MermaidSequenceDiagramRenderer();

    // --- Refactored version: clean domain names ---
    logger.info("=== Refactored: Player Joins World ===\n");
    logger.info("  Domain-specific names make the trace self-documenting.\n");

    var worldGenerator =
        NarrativeTraceProxy.trace(new DefaultWorldGenerator(), WorldGenerator.class, context);
    var inventory =
        NarrativeTraceProxy.trace(new DefaultPlayerInventory(), PlayerInventory.class, context);
    var craftingTable =
        NarrativeTraceProxy.trace(new DefaultCraftingTable(), CraftingTable.class, context);
    var spawner =
        NarrativeTraceProxy.trace(new DefaultCreatureSpawner(), CreatureSpawner.class, context);
    var server =
        NarrativeTraceProxy.trace(
            new DefaultWorldServer(worldGenerator, inventory, craftingTable, spawner),
            WorldServer.class,
            context);

    server.playerJoined("Steve");
    var refactoredTrace = context.captureTrace();
    logger.info("\n{}", renderer.render(refactoredTrace));
    logger.info("\n--- Mermaid ---\n");
    logger.info("\n{}", mermaidRenderer.render(refactoredTrace));

    // --- Unrefactored version: generic names, same behavior ---
    context.reset();
    logger.info("\n=== Unrefactored: Player Joins World ===\n");
    logger.info("  Generic names — same behavior, but the trace tells you nothing.\n");

    var dataProcessor =
        NarrativeTraceProxy.trace(new DefaultDataProcessor(), DataProcessor.class, context);
    var stateManager =
        NarrativeTraceProxy.trace(new DefaultStateManager(), StateManager.class, context);
    var thingFactory =
        NarrativeTraceProxy.trace(new DefaultThingFactory(), ThingFactory.class, context);
    var entityHandler =
        NarrativeTraceProxy.trace(new DefaultEntityHandler(), EntityHandler.class, context);
    var manager =
        NarrativeTraceProxy.trace(
            new DefaultGameManager(dataProcessor, stateManager, thingFactory, entityHandler),
            GameManager.class,
            context);

    manager.handle("Steve");
    logger.info("\n{}", renderer.render(context.captureTrace()));

    logger.info("\n  ^ Same call graph. Same return values. Only names differ.");
    logger.info("  If your code can't tell its own story, it needs refactoring.");
  }
}
