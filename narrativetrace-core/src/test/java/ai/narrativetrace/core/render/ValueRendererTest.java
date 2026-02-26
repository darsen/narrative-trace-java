package ai.narrativetrace.core.render;

import ai.narrativetrace.core.annotation.NarrativeSummary;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ValueRendererTest {

    private final ValueRenderer renderer = new ValueRenderer();

    static class Order {
        private final String orderId;
        private final int itemCount;

        Order(String orderId, int itemCount) {
            this.orderId = orderId;
            this.itemCount = itemCount;
        }

        @NarrativeSummary
        public String toNarrativeSummary() {
            return "Order(" + orderId + ", " + itemCount + " items)";
        }

        @Override
        public String toString() {
            return "Order{orderId=" + orderId + ", itemCount=" + itemCount + "}";
        }
    }

    @Test
    void usesNarrativeSummaryMethodWhenPresent() {
        var order = new Order("ORD-1", 3);
        var result = renderer.render(order);

        assertThat(result).isEqualTo("Order(ORD-1, 3 items)");
    }

    @Test
    void fallsBackToToStringWhenNoNarrativeSummary() {
        var result = renderer.render(42);
        assertThat(result).isEqualTo("42");
    }

    @Test
    void truncatesLongStringsAtConfiguredLimit() {
        var longString = "a".repeat(250);
        var result = renderer.render(longString);

        assertThat(result).hasSize(203); // 1 quote + 200 chars + … + 1 quote
        assertThat(result).startsWith("\"");
        assertThat(result).endsWith("…\"");
    }

    @Test
    void customStringLimitIsRespected() {
        var customRenderer = new ValueRenderer(10, 5, 5);
        var result = customRenderer.render("a".repeat(20));

        assertThat(result).hasSize(13); // 1 quote + 10 chars + … + 1 quote
        assertThat(result).endsWith("…\"");
    }

    @Test
    void limitsCollectionsToConfiguredMax() {
        var list = List.of("a", "b", "c", "d", "e", "f", "g");
        var result = renderer.render(list);

        assertThat(result).isEqualTo("[\"a\", \"b\", \"c\", \"d\", \"e\", … (7 total)]");
    }

    @Test
    void customCollectionLimitIsRespected() {
        var customRenderer = new ValueRenderer(200, 2, 5);
        var list = List.of("a", "b", "c", "d");
        var result = customRenderer.render(list);

        assertThat(result).isEqualTo("[\"a\", \"b\", … (4 total)]");
    }

    @Test
    void rendersSmallCollectionsFully() {
        var list = List.of("a", "b");
        var result = renderer.render(list);

        assertThat(result).isEqualTo("[\"a\", \"b\"]");
    }

    record LargeRecord(String a, String b, String c, String d, String e, String f) {}

    @Test
    void limitsRecordFieldsToConfiguredMax() {
        var record = new LargeRecord("1", "2", "3", "4", "5", "6");
        var result = renderer.render(record);

        assertThat(result).isEqualTo("LargeRecord(a: \"1\", b: \"2\", c: \"3\", d: \"4\", e: \"5\", …)");
    }

    @Test
    void customObjectFieldLimitIsRespected() {
        var customRenderer = new ValueRenderer(200, 5, 2);
        var record = new LargeRecord("1", "2", "3", "4", "5", "6");
        var result = customRenderer.render(record);

        assertThat(result).isEqualTo("LargeRecord(a: \"1\", b: \"2\", …)");
    }

    record SmallRecord(String name, int count) {}

    @Test
    void rendersSmallRecordFully() {
        var record = new SmallRecord("test", 42);
        var result = renderer.render(record);

        assertThat(result).isEqualTo("SmallRecord(name: \"test\", count: 42)");
    }

    @Test
    void rendersNullAsNull() {
        assertThat(renderer.render(null)).isEqualTo("null");
    }

    static class FailingSummary {
        @NarrativeSummary
        public String toNarrativeSummary() {
            throw new RuntimeException("oops");
        }

        @Override
        public String toString() {
            return "FailingSummary{}";
        }
    }

    @Test
    void narrativeSummaryThatThrowsFallsBackToToString() {
        var result = renderer.render(new FailingSummary());
        assertThat(result).isEqualTo("FailingSummary{}");
    }

    static class HasSummaryWithParams {
        @NarrativeSummary
        public String toNarrativeSummary(String format) {
            return "formatted";
        }

        @Override
        public String toString() {
            return "HasSummaryWithParams{}";
        }
    }

    record BrokenRecord(String name) {
        @Override
        public String name() {
            throw new RuntimeException("broken accessor");
        }
    }

    @Test
    void recordWithFailingAccessorRendersErrorFallback() {
        var result = renderer.render(new BrokenRecord("test"));

        assertThat(result).isEqualTo("BrokenRecord(name: <error>)");
    }

    @Test
    void narrativeSummaryWithParametersIsIgnored() {
        var result = renderer.render(new HasSummaryWithParams());
        assertThat(result).isEqualTo("HasSummaryWithParams{}");
    }

    @Test
    void rendersPrimitiveArray() {
        assertThat(renderer.render(new int[]{1, 2, 3})).isEqualTo("[1, 2, 3]");
    }

    @Test
    void rendersObjectArray() {
        assertThat(renderer.render(new String[]{"a", "b"})).isEqualTo("[\"a\", \"b\"]");
    }

    @Test
    void truncatesLargeArray() {
        var arr = new int[]{1, 2, 3, 4, 5, 6, 7};
        assertThat(renderer.render(arr)).isEqualTo("[1, 2, 3, 4, 5, ... (7 total)]");
    }

    @SuppressWarnings("unused")
    static class LegacyPojo {
        String name = "Alice";
        int age = 30;
    }

    @Test
    void rendersLegacyPojoWithReflectiveIntrospection() {
        assertThat(renderer.render(new LegacyPojo()))
                .isEqualTo("LegacyPojo{name: \"Alice\", age: 30}");
    }

    static class WithCustomToString {
        @SuppressWarnings("unused")
        String name = "ignored";

        @Override
        public String toString() {
            return "custom";
        }
    }

    @Test
    void objectWithCustomToStringUsesToString() {
        assertThat(renderer.render(new WithCustomToString())).isEqualTo("custom");
    }

    @SuppressWarnings("unused")
    static class ManyFields {
        String a = "1";
        String b = "2";
        String c = "3";
        String d = "4";
        String e = "5";
        String f = "6";
    }

    @Test
    void objectFieldTruncationRespectsMaxObjectFields() {
        assertThat(renderer.render(new ManyFields())).endsWith(", ...}");
    }

    @SuppressWarnings("unused")
    static class WithStaticField {
        static int COUNT = 0;
        String name = "test";
    }

    @Test
    void staticFieldsExcludedFromIntrospection() {
        assertThat(renderer.render(new WithStaticField()))
                .isEqualTo("WithStaticField{name: \"test\"}");
    }

    static class ThrowingToString {
        @Override
        public String toString() {
            throw new RuntimeException("boom");
        }
    }

    @Test
    void toStringFailureRendersClassName() {
        assertThat(renderer.render(new ThrowingToString()))
                .isEqualTo("<ThrowingToString>");
    }

    @SuppressWarnings("unused")
    static class SelfRef {
        String name = "root";
        SelfRef self;
    }

    @Test
    void cycleDetectionForReflectiveIntrospection() {
        var obj = new SelfRef();
        obj.self = obj;
        assertThat(renderer.render(obj)).contains("self: <SelfRef@");
    }

    @SuppressWarnings("unused")
    static class FieldWithThrowingGetter {
        String name = "ok";
        Object broken = new Object() {
            @Override
            public String toString() {
                throw new RuntimeException("field boom");
            }
        };
    }

    @Test
    void fieldWithThrowingToStringRendersClassName() {
        var result = renderer.render(new FieldWithThrowingGetter());
        assertThat(result).startsWith("FieldWithThrowingGetter{name: \"ok\"");
    }
}
