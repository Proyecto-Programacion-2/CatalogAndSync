package com.programacion2.catalogandsync.sync.infrastructure.catedra.dto;

import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Fija el punto de fallo de la decision 3.C: un {@code dayOfWeek} desconocido se
 * rechaza en la deserializacion, antes del mapper. No habla con Redis ni con la
 * catedra: solo verifica el contrato del DTO.
 */
class WeeklyScheduleResponseJsonTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void readsCanonicalDayOfWeek() {
        WeeklyScheduleResponse response = mapper.readValue(json("MONDAY"), WeeklyScheduleResponse.class);

        assertEquals(CatedraDayOfWeek.MONDAY, response.dayOfWeek());
        assertEquals(LocalTime.of(9, 0), response.startTime());
        assertEquals(30, response.slotDurationMinutes());
    }

    @Test
    void rejectsUnknownDayOfWeekAtDeserialization() {
        assertThrows(JacksonException.class,
                () -> mapper.readValue(json("FUNDAY"), WeeklyScheduleResponse.class));
    }

    private static String json(String dayOfWeek) {
        return """
                {
                  "id": 21,
                  "professionalId": 15,
                  "dayOfWeek": "%s",
                  "startTime": "09:00:00",
                  "endTime": "12:00:00",
                  "slotDurationMinutes": 30,
                  "enabled": true,
                  "createdAt": "2026-07-01T12:00:00Z",
                  "updatedAt": "2026-09-10T13:40:00Z"
                }
                """.formatted(dayOfWeek);
    }
}
