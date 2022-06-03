package com.farao_community.farao.core_valid.adapter.app;

import com.farao_community.farao.gridcapa.task_manager.api.*;
import com.farao_community.farao.gridcapa_core_valid.api.exception.CoreValidInternalException;
import com.farao_community.farao.gridcapa_core_valid.api.resource.CoreValidRequest;
import com.farao_community.farao.gridcapa_core_valid.starter.CoreValidClient;
import org.junit.jupiter.api.Assertions;
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
import java.util.function.Consumer;


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
    private String cgmFileType;
    private String cbcoraFileType;
    private String glskFileType;
    private String studyPointsFileType;
    private String refprogFileType;
    private String cgmFileName;
    private String cbcoraFileName;
    private String glskFileName;
    private String studyPointsFileName;
    private String refprogFileName;
    private String cgmFileUrl;
    private String cbcoraFileUrl;
    private String glksFileUrl;
    private String studyPointsFileUrl;
    private String refprogFileUrl;

    public TaskDto createTaskDtoWithStatus(TaskStatus status) {
        UUID id = UUID.randomUUID();
        OffsetDateTime timestamp = OffsetDateTime.parse("2021-12-07T14:30Z");
        List<ProcessFileDto> processFiles = new ArrayList<>();
        processFiles.add(new ProcessFileDto(cgmFileType, ProcessFileStatus.VALIDATED, cgmFileName, timestamp, cgmFileUrl));
        processFiles.add(new ProcessFileDto(cbcoraFileType, ProcessFileStatus.VALIDATED, cbcoraFileName, timestamp, cbcoraFileUrl));
        processFiles.add(new ProcessFileDto(glskFileType, ProcessFileStatus.VALIDATED, glskFileName, timestamp, glksFileUrl));
        processFiles.add(new ProcessFileDto(studyPointsFileType, ProcessFileStatus.VALIDATED, studyPointsFileName, timestamp, studyPointsFileUrl));
        processFiles.add(new ProcessFileDto(refprogFileType, ProcessFileStatus.VALIDATED, refprogFileName, timestamp, refprogFileUrl));
        List<ProcessEventDto> processEvents = new ArrayList<>();
        return new TaskDto(id, timestamp, status, null, processFiles, null, processEvents);
    }

    @BeforeEach
    void setUp() {
        cgmFileType = "CGM";
        cbcoraFileType = "CBCORA";
        glskFileType = "GLSK";
        studyPointsFileType = "STUDY-POINTS";
        refprogFileType = "REFPROG";
        cgmFileName = "cgm";
        cbcoraFileName = "cbcora";
        glskFileName = "glsk";
        studyPointsFileName = "study points";
        refprogFileName = "refprog";
        cgmFileUrl = "file://cgm.uct";
        cbcoraFileUrl = "file://cbcora.xml";
        glksFileUrl = "file://glsk.xml";
        studyPointsFileUrl = "file://study_points.csv";
        refprogFileUrl = "file://refprog.xml";
    }

    @Test
    void testGetManualCoreValidRequest() {
        TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.READY);
        CoreValidRequest coreValidRequest = coreValidAdapterListener.getManualCoreValidRequest(taskDto);
        Assertions.assertEquals(taskDto.getId().toString(), coreValidRequest.getId());
        Assertions.assertEquals(cgmFileName, coreValidRequest.getCgm().getFilename());
        Assertions.assertEquals(cgmFileUrl, coreValidRequest.getCgm().getUrl());
        Assertions.assertFalse(coreValidRequest.getLaunchedAutomatically());
    }

    @Test
    void testGetAutomaticCoreValidRequest() {
        TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.READY);
        CoreValidRequest coreValidRequest = coreValidAdapterListener.getAutomaticCoreValidRequest(taskDto);
        Assertions.assertTrue(coreValidRequest.getLaunchedAutomatically());
    }

    @Test
    void testGetCoreValidRequestWithIncorrectFiles() {
        String wrongRefprogFileType = "REF-PROG";
        UUID id = UUID.randomUUID();
        OffsetDateTime timestamp = OffsetDateTime.parse("2021-12-07T14:30Z");
        List<ProcessFileDto> processFiles = new ArrayList<>();
        processFiles.add(new ProcessFileDto(cgmFileType, ProcessFileStatus.VALIDATED, cgmFileName, timestamp, cgmFileUrl));
        processFiles.add(new ProcessFileDto(cbcoraFileType, ProcessFileStatus.VALIDATED, cbcoraFileName, timestamp, cbcoraFileUrl));
        processFiles.add(new ProcessFileDto(glskFileType, ProcessFileStatus.VALIDATED, glskFileName, timestamp, glksFileUrl));
        processFiles.add(new ProcessFileDto(studyPointsFileType, ProcessFileStatus.VALIDATED, studyPointsFileName, timestamp, studyPointsFileUrl));
        processFiles.add(new ProcessFileDto(wrongRefprogFileType, ProcessFileStatus.VALIDATED, refprogFileName, timestamp, refprogFileUrl));
        List<ProcessEventDto> processEvents = new ArrayList<>();
        TaskDto taskDto = new TaskDto(id, timestamp, TaskStatus.READY, null, processFiles, null, processEvents);
        Assertions.assertThrows(IllegalStateException.class, () -> coreValidAdapterListener.getManualCoreValidRequest(taskDto));

    }

    @Test
    void consumeReadyAutoTask() {
        TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.READY);
        coreValidAdapterListener.consumeAutoTask().accept(taskDto);
        Mockito.verify(coreValidClient).run(argumentCaptor.capture());
        CoreValidRequest coreValidRequest = argumentCaptor.getValue();
        assert coreValidRequest.getLaunchedAutomatically();
    }

    @Test
    void consumeReadyTask() {
        TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.READY);
        coreValidAdapterListener.consumeTask().accept(taskDto);
        Mockito.verify(coreValidClient).run(argumentCaptor.capture());
        CoreValidRequest coreValidRequest = argumentCaptor.getValue();
        Assertions.assertFalse(coreValidRequest.getLaunchedAutomatically());
    }

    @Test
    void consumeSuccessAutoTask() {
        TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.SUCCESS);
        coreValidAdapterListener.consumeAutoTask().accept(taskDto);
        Mockito.verify(coreValidClient).run(argumentCaptor.capture());
        CoreValidRequest coreValidRequest = argumentCaptor.getValue();
        assert coreValidRequest.getLaunchedAutomatically();
    }

    @Test
    void consumeSuccessTask() {
        TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.SUCCESS);
        coreValidAdapterListener.consumeTask().accept(taskDto);
        Mockito.verify(coreValidClient).run(argumentCaptor.capture());
        CoreValidRequest coreValidRequest = argumentCaptor.getValue();
        Assertions.assertFalse(coreValidRequest.getLaunchedAutomatically());
    }

    @Test
    void consumeErrorAutoTask() {
        TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.ERROR);
        coreValidAdapterListener.consumeAutoTask().accept(taskDto);
        Mockito.verify(coreValidClient).run(argumentCaptor.capture());
        CoreValidRequest coreValidRequest = argumentCaptor.getValue();
        assert coreValidRequest.getLaunchedAutomatically();
    }

    @Test
    void consumeErrorTask() {
        TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.ERROR);
        coreValidAdapterListener.consumeTask().accept(taskDto);
        Mockito.verify(coreValidClient).run(argumentCaptor.capture());
        CoreValidRequest coreValidRequest = argumentCaptor.getValue();
        Assertions.assertFalse(coreValidRequest.getLaunchedAutomatically());
    }

    @Test
    void consumeCreatedAutoTask() {
        TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.CREATED);
        coreValidAdapterListener.consumeAutoTask().accept(taskDto);
        Mockito.verify(coreValidClient, Mockito.never()).run(argumentCaptor.capture());
    }

    @Test
    void consumeCreatedTask() {
        TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.CREATED);
        coreValidAdapterListener.consumeTask().accept(taskDto);
        Mockito.verify(coreValidClient, Mockito.never()).run(argumentCaptor.capture());
    }

    @Test
    void consumeAutoTaskThrowingError() {
        Mockito.when(coreValidClient.run(Mockito.any())).thenThrow(new CoreValidInternalException("message"));
        TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.ERROR);
        Consumer<TaskDto> taskDtoConsumer = coreValidAdapterListener.consumeAutoTask();
        Assertions.assertThrows(CoreValidAdapterException.class, () -> taskDtoConsumer.accept(taskDto));
    }

    @Test
    void consumeTaskThrowingError() {
        Mockito.when(coreValidClient.run(Mockito.any())).thenThrow(new CoreValidInternalException("message"));
        TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.ERROR);
        Consumer<TaskDto> taskDtoConsumer = coreValidAdapterListener.consumeTask();
        Assertions.assertThrows(CoreValidAdapterException.class, () -> taskDtoConsumer.accept(taskDto));
    }
}
