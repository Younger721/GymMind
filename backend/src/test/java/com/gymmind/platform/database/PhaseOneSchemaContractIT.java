package com.gymmind.platform.database;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymmind.support.MySqlIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class PhaseOneSchemaContractIT extends MySqlIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void matchesThePhaseOneSchemaSnapshot() throws IOException {
        JsonNode snapshot = objectMapper.readTree(
                new ClassPathResource("schema/phase-one-schema.json").getInputStream());
        String database = snapshot.path("database").asText();
        assertThat(database).isEqualTo("gymmind");

        snapshot.path("tables").fields().forEachRemaining(entry -> assertTable(entry.getKey(), entry.getValue()));
    }

    private void assertTable(String table, JsonNode contract) {
        assertThat(jdbc.queryForObject("select count(*) from information_schema.tables "
                        + "where table_schema = database() and table_name = ?", Integer.class, table))
                .isEqualTo(1);

        Set<String> actualColumns = new LinkedHashSet<>(jdbc.queryForList(
                "select column_name from information_schema.columns "
                        + "where table_schema = database() and table_name = ? order by ordinal_position",
                String.class, table));
        contract.path("columns").forEach(column -> assertThat(actualColumns).contains(column.asText()));

        contract.path("required").forEach(column -> assertThat(jdbc.queryForObject(
                        "select is_nullable from information_schema.columns "
                                + "where table_schema = database() and table_name = ? and column_name = ?",
                        String.class, table, column.asText()))
                .isEqualTo("NO"));

        contract.path("unique").forEach(index -> {
            List<String> expected = new ArrayList<>();
            index.forEach(column -> expected.add(column.asText()));
            assertThat(uniqueIndexes(table)).contains(expected);
        });

        contract.path("foreignKeys").forEach(foreignKey -> assertThat(jdbc.queryForObject(
                        "select count(*) from information_schema.key_column_usage "
                                + "where table_schema = database() and table_name = ? and column_name = ? "
                                + "and referenced_table_name = ? and referenced_column_name = ?",
                        Integer.class, table, foreignKey.path("column").asText(),
                        foreignKey.path("table").asText(), foreignKey.path("target").asText()))
                .isGreaterThan(0));
    }

    private Set<List<String>> uniqueIndexes(String table) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select index_name AS INDEX_NAME, column_name AS COLUMN_NAME, seq_in_index AS SEQ_IN_INDEX "
                        + "from information_schema.statistics "
                        + "where table_schema = database() and table_name = ? and non_unique = 0 "
                        + "order by index_name, seq_in_index", table);
        Map<String, List<String>> grouped = rows.stream().collect(Collectors.groupingBy(
                        row -> (String) row.get("INDEX_NAME"), LinkedHashMap::new,
                        Collectors.mapping(row -> (String) row.get("COLUMN_NAME"), Collectors.toList())));
        return grouped.values().stream()
                .map(List::copyOf)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
