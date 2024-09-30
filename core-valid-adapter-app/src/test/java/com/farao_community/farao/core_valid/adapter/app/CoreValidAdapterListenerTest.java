/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.core_valid.adapter.app;

import com.farao_community.farao.gridcapa.task_manager.api.ProcessEventDto;
import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileDto;
import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileStatus;
import com.farao_community.farao.gridcapa.task_manager.api.ProcessRunDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import com.farao_community.farao.gridcapa_core_valid.api.resource.CoreValidRequest;
import com.farao_community.farao.gridcapa_core_valid.starter.CoreValidClient;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
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

    @MockBean
    private MinioAdapter minioAdapter;

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
    private String glskFileUrl;
    private String studyPointsFileUrl;
    private String refprogFileUrl;
    private String cgmFilePath;
    private String cbcoraFilePath;
    private String glskFilePath;
    private String studyPointsFilePath;
    private String refprogFilePath;

    public TaskDto createTaskDtoWithStatus(final TaskStatus status) {
        final UUID id = UUID.randomUUID();
        final OffsetDateTime timestamp = OffsetDateTime.parse("2021-12-07T14:30Z");
        final List<ProcessFileDto> processFiles = new ArrayList<>();
        processFiles.add(new ProcessFileDto(cgmFilePath, cgmFileType, ProcessFileStatus.VALIDATED, cgmFileName, "docId1", timestamp));
        processFiles.add(new ProcessFileDto(cbcoraFilePath, cbcoraFileType, ProcessFileStatus.VALIDATED, cbcoraFileName, "docId2", timestamp));
        processFiles.add(new ProcessFileDto(glskFilePath, glskFileType, ProcessFileStatus.VALIDATED, glskFileName, "docId3", timestamp));
        processFiles.add(new ProcessFileDto(studyPointsFilePath, studyPointsFileType, ProcessFileStatus.VALIDATED, studyPointsFileName, "docId4", timestamp));
        processFiles.add(new ProcessFileDto(refprogFilePath, refprogFileType, ProcessFileStatus.VALIDATED, refprogFileName, "docId5", timestamp));
        final List<ProcessEventDto> processEvents = new ArrayList<>();
        final List<ProcessRunDto> runHistory = new ArrayList<>();
        runHistory.add(new ProcessRunDto(UUID.randomUUID(), OffsetDateTime.now(), processFiles));
        return new TaskDto(id, timestamp, status, processFiles, null, Collections.emptyList(), processEvents, runHistory, Collections.emptyList());
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

        cgmFilePath = "/CGM";
        cbcoraFilePath = "/CBCORA";
        glskFilePath = "/GLSK";
        studyPointsFilePath = "/STUDYPOINTS";
        refprogFilePath = "/REFPROG";

        cgmFileUrl = "file://CGM/cgm.uct";
        cbcoraFileUrl = "file://CBCORA/cbcora.xml";
        glskFileUrl = "file://GLSK/glsk.xml";
        studyPointsFileUrl = "file://STUDYPOINTS/study_points.csv";
        refprogFileUrl = "file://REFPROG/refprog.xml";

        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(cgmFilePath, 1)).thenReturn(cgmFileUrl);
        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(cbcoraFilePath, 1)).thenReturn(cbcoraFileUrl);
        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(glskFilePath, 1)).thenReturn(glskFileUrl);
        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(studyPointsFilePath, 1)).thenReturn(studyPointsFileUrl);
        Mockito.when(minioAdapter.generatePreSignedUrlFromFullMinioPath(refprogFilePath, 1)).thenReturn(refprogFileUrl);
    }

    @Test
    void testGetManualCoreValidRequest() {
        final TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.READY);
        final CoreValidRequest coreValidRequest = coreValidAdapterListener.getManualCoreValidRequest(taskDto);
        Assertions.assertEquals(taskDto.getId().toString(), coreValidRequest.getId());
        Assertions.assertEquals(cgmFileName, coreValidRequest.getCgm().getFilename());
        Assertions.assertEquals(cgmFileUrl, coreValidRequest.getCgm().getUrl());
        Assertions.assertFalse(coreValidRequest.getLaunchedAutomatically());
    }

    @Test
    void testGetManualCoreValidRequestThrowsException() {
        final TaskDto taskDto = new TaskDto(UUID.randomUUID(), OffsetDateTime.now(), TaskStatus.READY, List.of(), null, List.of(), List.of(), List.of(), List.of());

        Assertions.assertThrows(
                CoreValidAdapterException.class,
                () -> coreValidAdapterListener.getManualCoreValidRequest(taskDto),
                "Failed to handle manual run request on timestamp because it has no run history");
    }

    @Test
    void testGetAutomaticCoreValidRequest() {
        final TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.READY);
        final CoreValidRequest coreValidRequest = coreValidAdapterListener.getAutomaticCoreValidRequest(taskDto);
        Assertions.assertTrue(coreValidRequest.getLaunchedAutomatically());
    }

    @Test
    void testGetCoreValidRequestWithIncorrectFiles() {
        final String wrongRefprogFileType = "REF-PROG";
        final UUID id = UUID.randomUUID();
        final OffsetDateTime timestamp = OffsetDateTime.parse("2021-12-07T14:30Z");
        final List<ProcessFileDto> processFiles = new ArrayList<>();
        processFiles.add(new ProcessFileDto(cgmFilePath, cgmFileType, ProcessFileStatus.VALIDATED, cgmFileName, "docId11", timestamp));
        processFiles.add(new ProcessFileDto(cbcoraFilePath, cbcoraFileType, ProcessFileStatus.VALIDATED, cbcoraFileName, "docId12", timestamp));
        processFiles.add(new ProcessFileDto(glskFilePath, glskFileType, ProcessFileStatus.VALIDATED, glskFileName, "docId13", timestamp));
        processFiles.add(new ProcessFileDto(studyPointsFilePath, studyPointsFileType, ProcessFileStatus.VALIDATED, studyPointsFileName, "docId14", timestamp));
        processFiles.add(new ProcessFileDto(refprogFilePath, wrongRefprogFileType, ProcessFileStatus.VALIDATED, refprogFileName, "docId15", timestamp));
        final List<ProcessEventDto> processEvents = new ArrayList<>();
        final TaskDto taskDto = new TaskDto(id, timestamp, TaskStatus.READY, processFiles, null, Collections.emptyList(), processEvents, Collections.emptyList(), Collections.emptyList());
        Assertions.assertThrows(IllegalStateException.class, () -> coreValidAdapterListener.getManualCoreValidRequest(taskDto));

    }

    @Test
    void consumeReadyAutoTask() {
        final TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.READY);
        coreValidAdapterListener.consumeAutoTask().accept(taskDto);
        Mockito.verify(coreValidClient).run(argumentCaptor.capture());
        final CoreValidRequest coreValidRequest = argumentCaptor.getValue();
        assert coreValidRequest.getLaunchedAutomatically();
    }

    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, names = {"READY", "SUCCESS", "ERROR"})
    void consumeReadyTask(final TaskStatus taskStatus) {
        final TaskDto taskDto = createTaskDtoWithStatus(taskStatus);
        coreValidAdapterListener.consumeTask().accept(taskDto);
        Mockito.verify(coreValidClient).run(argumentCaptor.capture());
        final CoreValidRequest coreValidRequest = argumentCaptor.getValue();
        Assertions.assertFalse(coreValidRequest.getLaunchedAutomatically());
    }

    @Test
    void consumeCreatedTask() {
        final TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.CREATED);
        coreValidAdapterListener.consumeTask().accept(taskDto);
        Mockito.verify(coreValidClient, Mockito.never()).run(argumentCaptor.capture());
    }

    @Test
    void consumeTaskThrowsException() {
        final TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.READY);
        Mockito.doThrow(RuntimeException.class).when(coreValidClient).run(Mockito.any());
        final Consumer<TaskDto> taskDtoConsumer = coreValidAdapterListener.consumeTask();
        Assertions.assertThrows(
                CoreValidAdapterException.class,
                () -> taskDtoConsumer.accept(taskDto),
                "Error during handling manual run request on TS 2021-12-07T14:30Z");
    }

    @ParameterizedTest
    @EnumSource(value = TaskStatus.class, names = {"READY", "SUCCESS", "ERROR"})
    void consumeSuccessAutoTask(final TaskStatus taskStatus) {
        final TaskDto taskDto = createTaskDtoWithStatus(taskStatus);
        coreValidAdapterListener.consumeAutoTask().accept(taskDto);
        Mockito.verify(coreValidClient).run(argumentCaptor.capture());
        final CoreValidRequest coreValidRequest = argumentCaptor.getValue();
        assert coreValidRequest.getLaunchedAutomatically();
    }

    @Test
    void consumeCreatedAutoTask() {
        final TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.CREATED);
        coreValidAdapterListener.consumeAutoTask().accept(taskDto);
        Mockito.verify(coreValidClient, Mockito.never()).run(argumentCaptor.capture());
    }

    @Test
    void consumeAutoTaskThrowsException() {
        final TaskDto taskDto = createTaskDtoWithStatus(TaskStatus.READY);
        Mockito.doThrow(RuntimeException.class).when(coreValidClient).run(Mockito.any());
        final Consumer<TaskDto> taskDtoConsumer = coreValidAdapterListener.consumeAutoTask();
        Assertions.assertThrows(
                CoreValidAdapterException.class,
                () -> taskDtoConsumer.accept(taskDto),
                "Error during handling manual run request on TS 2021-12-07T14:30Z");
    }
}
