package com.farao_community.farao.core_valid.adapter.app;

import com.farao_community.farao.core_valid.api.resource.CoreValidRequest;
import com.farao_community.farao.gridcapa.task_manager.api.*;
import com.farao_community.farao.gridcapa_core_valid.starter.CoreValidClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@SpringBootApplication
public class CoreValidAdapterListenerTest {

    @MockBean
    private CoreValidClient coreValidClient;

    @Autowired
    private CoreValidAdapterListener coreValidAdapterListener = new CoreValidAdapterListener(coreValidClient);

    @Test
    void testGetCoreValidRequest() {
        UUID id = UUID.randomUUID();
        LocalDateTime dateTime = LocalDateTime.parse("2021-12-07T14:30");
        OffsetDateTime timestamp = OffsetDateTime.of(dateTime, ZoneOffset.UTC);
        List<ProcessFileDto> processFiles = new ArrayList<>();
        processFiles.add(new ProcessFileDto("CGM", ProcessFileStatus.VALIDATED, "cgm", timestamp, "file://cgm.uct"));
        processFiles.add(new ProcessFileDto("CBCORA", ProcessFileStatus.VALIDATED, "cbcora", timestamp, "file://cbcora.xml"));
        processFiles.add(new ProcessFileDto("GLSK", ProcessFileStatus.VALIDATED, "glsk", timestamp, "file://glsk.xml"));
        processFiles.add(new ProcessFileDto("STUDY-POINTS", ProcessFileStatus.VALIDATED, "stydy points", timestamp, "file://study_points.csv"));
        processFiles.add(new ProcessFileDto("REFPROG", ProcessFileStatus.VALIDATED, "refprog", timestamp, "file://refprog.xml"));
        List<ProcessEventDto> processEvents = new ArrayList<>();
        TaskDto taskDto = new TaskDto(id, timestamp, TaskStatus.READY, processFiles, processEvents);
        CoreValidRequest coreValidRequest = coreValidAdapterListener.getCoreValidRequest(taskDto);
        assertEquals(id.toString(), coreValidRequest.getId());
        assertEquals("cgm", coreValidRequest.getCgm().getFilename());
        assertEquals("file://cgm.uct", coreValidRequest.getCgm().getUrl());

    }

    @Test
    void testGetCoreValidRequestWithIncorrectFiles() {
        UUID id = UUID.randomUUID();
        LocalDateTime dateTime = LocalDateTime.parse("2021-12-07T14:30");
        OffsetDateTime timestamp = OffsetDateTime.of(dateTime, ZoneOffset.UTC);
        List<ProcessFileDto> processFiles = new ArrayList<>();
        processFiles.add(new ProcessFileDto("CGM", ProcessFileStatus.VALIDATED, "cgm", timestamp, "file://cgm.uct"));
        processFiles.add(new ProcessFileDto("CBCORA", ProcessFileStatus.VALIDATED, "cbcora", timestamp, "file://cbcora.xml"));
        processFiles.add(new ProcessFileDto("GLSK", ProcessFileStatus.VALIDATED, "glsk", timestamp, "file://glsk.xml"));
        processFiles.add(new ProcessFileDto("STUDY-POINTS", ProcessFileStatus.VALIDATED, "stydy points", timestamp, "file://study_points.csv"));
        processFiles.add(new ProcessFileDto("REF-PROG", ProcessFileStatus.VALIDATED, "refprog", timestamp, "file://refprog.xml"));
        List<ProcessEventDto> processEvents = new ArrayList<>();
        TaskDto taskDto = new TaskDto(id, timestamp, TaskStatus.READY, processFiles, processEvents);
        assertThrows(IllegalStateException.class, () -> coreValidAdapterListener.getCoreValidRequest(taskDto));

    }
}
