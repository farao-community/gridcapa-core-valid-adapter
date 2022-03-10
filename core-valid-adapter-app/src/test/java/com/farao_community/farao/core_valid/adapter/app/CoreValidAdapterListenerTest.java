package com.farao_community.farao.core_valid.adapter.app;

import com.farao_community.farao.core_valid.api.resource.CoreValidRequest;
import com.farao_community.farao.gridcapa.task_manager.api.*;
import com.farao_community.farao.gridcapa_core_valid.starter.CoreValidClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
@SpringBootTest
class CoreValidAdapterListenerTest {

    @MockBean
    private CoreValidClient coreValidClient;

    @Captor
    ArgumentCaptor<CoreValidRequest> argumentCaptor;

    @Autowired
    private CoreValidAdapterListener coreValidAdapterListener;

    private TaskDto taskDto;

    @BeforeEach
    void setUp() {
        UUID id = UUID.randomUUID();
        OffsetDateTime timestamp = OffsetDateTime.parse("2021-12-07T14:30Z");
        List<ProcessFileDto> processFiles = new ArrayList<>();
        processFiles.add(new ProcessFileDto("CGM", ProcessFileStatus.VALIDATED, "cgm", timestamp, "file://cgm.uct"));
        processFiles.add(new ProcessFileDto("CBCORA", ProcessFileStatus.VALIDATED, "cbcora", timestamp, "file://cbcora.xml"));
        processFiles.add(new ProcessFileDto("GLSK", ProcessFileStatus.VALIDATED, "glsk", timestamp, "file://glsk.xml"));
        processFiles.add(new ProcessFileDto("STUDY-POINTS", ProcessFileStatus.VALIDATED, "stydy points", timestamp, "file://study_points.csv"));
        processFiles.add(new ProcessFileDto("REFPROG", ProcessFileStatus.VALIDATED, "refprog", timestamp, "file://refprog.xml"));
        List<ProcessEventDto> processEvents = new ArrayList<>();
        taskDto = new TaskDto(id, timestamp, TaskStatus.READY, processFiles, processEvents);
    }

    @Test
    void testGetManualCoreValidRequest() {
        CoreValidRequest coreValidRequest = coreValidAdapterListener.getManualCoreValidRequest(taskDto);
        assertEquals(taskDto.getId().toString(), coreValidRequest.getId());
        assertEquals("cgm", coreValidRequest.getCgm().getFilename());
        assertEquals("file://cgm.uct", coreValidRequest.getCgm().getUrl());
        assertFalse(coreValidRequest.getLaunchedAutomatically());
    }

    @Test
    void testGetAutomaticCoreValidRequest() {
        CoreValidRequest coreValidRequest = coreValidAdapterListener.getAutomaticCoreValidRequest(taskDto);
        assert coreValidRequest.getLaunchedAutomatically();
    }

    @Test
    void testGetCoreValidRequestWithIncorrectFiles() {
        UUID id = UUID.randomUUID();
        OffsetDateTime timestamp = OffsetDateTime.parse("2021-12-07T14:30Z");
        List<ProcessFileDto> processFiles = new ArrayList<>();
        processFiles.add(new ProcessFileDto("CGM", ProcessFileStatus.VALIDATED, "cgm", timestamp, "file://cgm.uct"));
        processFiles.add(new ProcessFileDto("CBCORA", ProcessFileStatus.VALIDATED, "cbcora", timestamp, "file://cbcora.xml"));
        processFiles.add(new ProcessFileDto("GLSK", ProcessFileStatus.VALIDATED, "glsk", timestamp, "file://glsk.xml"));
        processFiles.add(new ProcessFileDto("STUDY-POINTS", ProcessFileStatus.VALIDATED, "stydy points", timestamp, "file://study_points.csv"));
        processFiles.add(new ProcessFileDto("REF-PROG", ProcessFileStatus.VALIDATED, "refprog", timestamp, "file://refprog.xml"));
        List<ProcessEventDto> processEvents = new ArrayList<>();
        TaskDto taskDto = new TaskDto(id, timestamp, TaskStatus.READY, processFiles, processEvents);
        assertThrows(IllegalStateException.class, () -> coreValidAdapterListener.getManualCoreValidRequest(taskDto));

    }

    @Test
    void consumeAutoTask() {
        coreValidAdapterListener.consumeAutoTask().accept(taskDto);
        Mockito.verify(coreValidClient).run(argumentCaptor.capture());
        CoreValidRequest coreValidRequest = argumentCaptor.getValue();
        assert coreValidRequest.getLaunchedAutomatically();
    }

    @Test
    void consumeTask() {
        coreValidAdapterListener.consumeTask().accept(taskDto);
        Mockito.verify(coreValidClient).run(argumentCaptor.capture());
        CoreValidRequest coreValidRequest = argumentCaptor.getValue();
        assertFalse(coreValidRequest.getLaunchedAutomatically());
    }
}
