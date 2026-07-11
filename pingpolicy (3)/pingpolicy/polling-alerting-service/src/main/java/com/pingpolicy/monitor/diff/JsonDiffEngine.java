package com.pingpolicy.monitor.diff;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Structural JSON diff engine.
 *
 * Compares an "expected" JSON tree (the registered contract) against an
 * "observed" JSON tree (the live poll response) and reports structural
 * drift: fields that were added, removed, or whose type changed.
 *
 * This is a *shape* diff, not a value diff — PingPolicy cares whether the
 * contract (field names + types) has silently drifted, not whether field
 * values differ between polls.
 */
@Component
public class JsonDiffEngine {

    public List<FieldDiff> diff(JsonNode expected, JsonNode observed) {
        List<FieldDiff> diffs = new ArrayList<>();
        walk("$", expected, observed, diffs);
        return diffs;
    }

    private void walk(String path, JsonNode expected, JsonNode observed, List<FieldDiff> diffs) {
        if (expected == null || expected.isMissingNode()) {
            return;
        }

        if (observed == null || observed.isMissingNode()) {
            diffs.add(FieldDiff.removed(path, typeOf(expected)));
            return;
        }

        JsonNodeType expectedType = typeOf(expected);
        JsonNodeType observedType = typeOf(observed);

        if (expectedType != observedType) {
            diffs.add(FieldDiff.typeChanged(path, expectedType, observedType));
            return; // don't recurse further into a structurally mismatched node
        }

        switch (expectedType) {
            case OBJECT -> walkObject(path, expected, observed, diffs);
            case ARRAY -> walkArray(path, expected, observed, diffs);
            default -> {
                // scalar leaf of matching type: nothing structural to compare
            }
        }
    }

    private void walkObject(String path, JsonNode expected, JsonNode observed, List<FieldDiff> diffs) {
        Set<String> expectedFields = new HashSet<>();
        expected.fieldNames().forEachRemaining(expectedFields::add);

        Set<String> observedFields = new HashSet<>();
        observed.fieldNames().forEachRemaining(observedFields::add);

        // fields present in contract but missing from the live response
        for (String field : expectedFields) {
            String childPath = path + "." + field;
            if (!observedFields.contains(field)) {
                diffs.add(FieldDiff.removed(childPath, typeOf(expected.get(field))));
            } else {
                walk(childPath, expected.get(field), observed.get(field), diffs);
            }
        }

        // fields present live but NOT in the contract == undocumented / new field
        for (String field : observedFields) {
            if (!expectedFields.contains(field)) {
                String childPath = path + "." + field;
                diffs.add(FieldDiff.added(childPath, typeOf(observed.get(field))));
            }
        }
    }

    private void walkArray(String path, JsonNode expected, JsonNode observed, List<FieldDiff> diffs) {
        ArrayNode expectedArr = (ArrayNode) expected;
        ArrayNode observedArr = (ArrayNode) observed;

        if (expectedArr.isEmpty() || observedArr.isEmpty()) {
            return; // nothing to structurally compare against
        }

        // Contracts describe array item *shape*, not length, so we only
        // compare the first element of each as a representative sample.
        walk(path + "[]", expectedArr.get(0), observedArr.get(0), diffs);
    }

    private JsonNodeType typeOf(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) return JsonNodeType.NULL;
        if (node.isObject()) return JsonNodeType.OBJECT;
        if (node.isArray()) return JsonNodeType.ARRAY;
        if (node.isTextual()) return JsonNodeType.STRING;
        if (node.isBoolean()) return JsonNodeType.BOOLEAN;
        if (node.isNumber()) return JsonNodeType.NUMBER;
        return JsonNodeType.UNKNOWN;
    }

    public enum JsonNodeType {
        OBJECT, ARRAY, STRING, NUMBER, BOOLEAN, NULL, UNKNOWN
    }

    /** A single structural difference found at a JSON path. */
    public record FieldDiff(String path, DiffType type, JsonNodeType expectedType, JsonNodeType observedType) {

        public static FieldDiff removed(String path, JsonNodeType expectedType) {
            return new FieldDiff(path, DiffType.FIELD_REMOVED, expectedType, null);
        }

        public static FieldDiff added(String path, JsonNodeType observedType) {
            return new FieldDiff(path, DiffType.FIELD_ADDED, null, observedType);
        }

        public static FieldDiff typeChanged(String path, JsonNodeType expectedType, JsonNodeType observedType) {
            return new FieldDiff(path, DiffType.TYPE_CHANGED, expectedType, observedType);
        }

        public enum DiffType {
            FIELD_ADDED, FIELD_REMOVED, TYPE_CHANGED
        }
    }

    /** Rolls a list of field diffs up into an overall severity. */
    public DriftSeverityResult classify(List<FieldDiff> diffs) {
        if (diffs.isEmpty()) {
            return new DriftSeverityResult(Severity.NONE, 0);
        }

        boolean hasHigh = diffs.stream().anyMatch(d ->
                d.type() == FieldDiff.DiffType.FIELD_REMOVED || d.type() == FieldDiff.DiffType.TYPE_CHANGED);
        boolean hasMedium = diffs.stream().anyMatch(d -> d.type() == FieldDiff.DiffType.FIELD_ADDED);

        Severity severity = hasHigh ? Severity.HIGH : (hasMedium ? Severity.LOW : Severity.LOW);
        return new DriftSeverityResult(severity, diffs.size());
    }

    public enum Severity { NONE, LOW, MEDIUM, HIGH }

    public record DriftSeverityResult(Severity severity, int changeCount) {}
}
