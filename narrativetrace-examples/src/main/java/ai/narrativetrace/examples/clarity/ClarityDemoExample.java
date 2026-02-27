package ai.narrativetrace.examples.clarity;

import ai.narrativetrace.clarity.ClarityAnalyzer;
import ai.narrativetrace.clarity.ClarityReportRenderer;
import ai.narrativetrace.core.context.ThreadLocalNarrativeContext;
import ai.narrativetrace.core.render.IndentedTextRenderer;
import ai.narrativetrace.core.tree.TraceTree;
import ai.narrativetrace.proxy.NarrativeTraceProxy;
import ai.narrativetrace.slf4j.Slf4jNarrativeContext;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClarityDemoExample {

  private static final Logger logger = LoggerFactory.getLogger(ClarityDemoExample.class);

  public static void main(String[] args) {
    var context = new Slf4jNarrativeContext(new ThreadLocalNarrativeContext());
    var renderer = new IndentedTextRenderer();
    var analyzer = new ClarityAnalyzer();
    var reportRenderer = new ClarityReportRenderer();
    var scenarioResults = new LinkedHashMap<String, TraceTree>();

    // --- Scenario 1: Excellent naming — guest books a room ---
    logger.info("=== Scenario 1: Guest Books a Room (Excellent Naming) ===\n");

    var availabilityChecker =
        NarrativeTraceProxy.trace(
            new DefaultAvailabilityChecker(), AvailabilityChecker.class, context);
    var paymentGateway =
        NarrativeTraceProxy.trace(new DefaultPaymentGateway(), PaymentGateway.class, context);
    var reservationService =
        NarrativeTraceProxy.trace(
            new DefaultReservationService(availabilityChecker, paymentGateway),
            ReservationService.class,
            context);

    reservationService.confirmReservation(
        "G-1001", "deluxe", LocalDate.of(2025, 6, 15), LocalDate.of(2025, 6, 18));

    scenarioResults.put("Guest books a room", context.captureTrace());
    logger.info("\n{}", renderer.render(context.captureTrace()));

    // --- Scenario 2: Adequate naming — booking manager ---
    context.reset();
    logger.info("\n=== Scenario 2: Booking via Manager (Adequate Naming) ===\n");

    var bookingManager =
        NarrativeTraceProxy.trace(new DefaultBookingManager(), BookingManager.class, context);

    bookingManager.handleBooking("Jane Smith", "suite", "2025-07-01", "2025-07-05");

    scenarioResults.put("Booking via manager", context.captureTrace());
    logger.info("\n{}", renderer.render(context.captureTrace()));

    // --- Scenario 3: Poor naming — legacy data processing ---
    context.reset();
    logger.info("\n=== Scenario 3: Legacy Data Processing (Poor Naming) ===\n");

    var dataProcessor =
        NarrativeTraceProxy.trace(new DefaultDataProcessor(), DataProcessor.class, context);

    dataProcessor.execute("room-data", 42);

    scenarioResults.put("Legacy data processing", context.captureTrace());
    logger.info("\n{}", renderer.render(context.captureTrace()));

    // --- Scenario 4: Cohesion violation — guest repository ---
    context.reset();
    logger.info("\n=== Scenario 4: Guest Repository (Cohesion Mismatch) ===\n");

    var guestRepository =
        NarrativeTraceProxy.trace(new DefaultGuestRepository(), GuestRepository.class, context);

    guestRepository.findGuestById("G-1001");
    guestRepository.renderReport();
    guestRepository.dispatchEmail("G-1001", "Your reservation is confirmed");

    scenarioResults.put("Guest repository operations", context.captureTrace());
    logger.info("\n{}", renderer.render(context.captureTrace()));

    // --- Clarity Report ---
    logger.info("\n\n========================================");
    logger.info("         CLARITY ANALYSIS REPORT");
    logger.info("========================================\n");

    var combinedResults = new LinkedHashMap<String, ai.narrativetrace.clarity.ClarityResult>();
    for (var entry : scenarioResults.entrySet()) {
      var result = analyzer.analyze(entry.getValue());
      combinedResults.put(entry.getKey(), result);
      logger.info("\n{}", reportRenderer.render(entry.getKey(), result));
    }

    logger.info("\n\n{}", reportRenderer.renderSuiteReport(combinedResults));
  }
}
