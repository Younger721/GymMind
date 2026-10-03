package com.gymmind.shared.async;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

class MySqlPersistentJobQueueTest {

    @Test
    void enqueueReturnsExistingActiveJobInsteadOfCreatingDuplicate() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(contains("get_lock"), eq(Boolean.class), anyString())).thenReturn(true);
        when(jdbc.queryForObject(contains("select id from async_job"), eq(Long.class), anyString(), anyLong(), anyString(), anyString())).thenReturn(42L);
        when(jdbc.queryForObject(contains("release_lock"), eq(Integer.class), anyString())).thenReturn(1);

        Long id = new MySqlPersistentJobQueue(jdbc).enqueue("KNOWLEDGE_INGEST:7", 3L);

        assertThat(id).isEqualTo(42L);
        verify(jdbc).queryForObject(contains("select id from async_job"), eq(Long.class), anyString(), anyLong(), anyString(), anyString());
    }
}
