package com.pingpolicy.monitor.diff;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonDiffEngineTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonDiffEngine engine = new JsonDiffEngine();

    @Test
    void identicalShapes_produceNoDiffs() throws Exception {
        JsonNode expected = mapper.readTree("{\"id\": 1, \"name\": \"foo\"}");
        JsonNode observed = mapper.readTree("{\"id\": 2, \"name\": \"bar\"}");

        List<JsonDiffEngine.FieldDiff> diffs = engine.diff(expected, observed);

        assertTrue(diffs.isEmpty(), "values differ but shape matches -> no drift");
    }

    @Test
    void removedField_isDetected() throws Exception {
        JsonNode expected = mapper.readTree("{\"id\": 1, \"email\": \"a@b.com\"}");
        JsonNode observed = mapper.readTree("{\"id\": 1}");

        List<JsonDiffEngine.FieldDiff> diffs = engine.diff(expected, observed);

        assertEquals(1, diffs.size());
        assertEquals(JsonDiffEngine.FieldDiff.DiffType.FIELD_REMOVED, diffs.get(0).type());
        assertEquals("$.email", diffs.get(0).path());
    }

    @Test
    void addedField_isDetected() throws Exception {
        JsonNode expected = mapper.readTree("{\"id\": 1}");
        JsonNode observed = mapper.readTree("{\"id\": 1, \"newField\": true}");

        List<JsonDiffEngine.FieldDiff> diffs = engine.diff(expected, observed);

        assertEquals(1, diffs.size());
        assertEquals(JsonDiffEngine.FieldDiff.DiffType.FIELD_ADDED, diffs.get(0).type());
    }

    @Test
    void typeChange_isDetected() throws Exception {
        JsonNode expected = mapper.readTree("{\"id\": 1}");
        JsonNode observed = mapper.readTree("{\"id\": \"1\"}");

        List<JsonDiffEngine.FieldDiff> diffs = engine.diff(expected, observed);

        assertEquals(1, diffs.size());
        assertEquals(JsonDiffEngine.FieldDiff.DiffType.TYPE_CHANGED, diffs.get(0).type());
        assertEquals(JsonDiffEngine.JsonNodeType.NUMBER, diffs.get(0).expectedType());
        assertEquals(JsonDiffEngine.JsonNodeType.STRING, diffs.get(0).observedType());
    }

    @Test
    void nestedObjectDrift_isDetectedWithFullPath() throws Exception {
        JsonNode expected = mapper.readTree("{\"user\": {\"id\": 1, \"address\": {\"zip\": \"12345\"}}}");
        JsonNode observed = mapper.readTree("{\"user\": {\"id\": 1, \"address\": {}}}");

        List<JsonDiffEngine.FieldDiff> diffs = engine.diff(expected, observed);

        assertEquals(1, diffs.size());
        assertEquals("$.user.address.zip", diffs.get(0).path());
    }

    @Test
    void classify_removedField_isHighSeverity() throws Exception {
        JsonNode expected = mapper.readTree("{\"id\": 1}");
        JsonNode observed = mapper.readTree("{}");

        List<JsonDiffEngine.FieldDiff> diffs = engine.diff(expected, observed);
        JsonDiffEngine.DriftSeverityResult result = engine.classify(diffs);

        assertEquals(JsonDiffEngine.Severity.HIGH, result.severity());
    }
}
