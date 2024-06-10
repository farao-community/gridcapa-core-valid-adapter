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
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import com.farao_community.farao.gridcapa_core_valid.api.exception.CoreValidInternalException;
import com.farao_community.farao.gridcapa_core_valid.api.resource.CoreValidRequest;
import com.farao_community.farao.gridcapa_core_valid.starter.CoreValidClient;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
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

    public TaskDto createTaskDtoWithStatus(TaskStatus status) {
        UUID id = UUID.randomUUID();
        OffsetDateTime timestamp = OffsetDateTime.parse("2021-12-07T14:30Z");
        List<ProcessFileDto> processFiles = new ArrayList<>();
        processFiles.add(new ProcessFileDto(cgmFilePath, cgmFileType, ProcessFileStatus.VALIDATED, cgmFileName, timestamp));
        processFiles.add(new ProcessFileDto(cbcoraFilePath, cbcoraFileType, ProcessFileStatus.VALIDATED, cbcoraFileName, timestamp));
        processFiles.add(new ProcessFileDto(glskFilePath, glskFileType, ProcessFileStatus.VALIDATED, glskFileName, timestamp));
        processFiles.add(new ProcessFileDto(studyPointsFilePath, studyPointsFileType, ProcessFileStatus.VALIDATED, studyPointsFileName, timestamp));
        processFiles.add(new ProcessFileDto(refprogFilePath, refprogFileType, ProcessFileStatus.VALIDATED, refprogFileName, timestamp));
        List<ProcessEventDto> processEvents = new ArrayList<>();
        return new TaskDto(id, timestamp, status, processFiles, null, Collections.emptyList(), processEvents, Collections.emptyList(), Collections.emptyList());
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
        processFiles.add(new ProcessFileDto(cgmFilePath, cgmFileType, ProcessFileStatus.VALIDATED, cgmFileName, timestamp));
        processFiles.add(new ProcessFileDto(cbcoraFilePath, cbcoraFileType, ProcessFileStatus.VALIDATED, cbcoraFileName, timestamp));
        processFiles.add(new ProcessFileDto(glskFilePath, glskFileType, ProcessFileStatus.VALIDATED, glskFileName, timestamp));
        processFiles.add(new ProcessFileDto(studyPointsFilePath, studyPointsFileType, ProcessFileStatus.VALIDATED, studyPointsFileName, timestamp));
        processFiles.add(new ProcessFileDto(refprogFilePath, wrongRefprogFileType, ProcessFileStatus.VALIDATED, refprogFileName, timestamp));
        List<ProcessEventDto> processEvents = new ArrayList<>();
        TaskDto taskDto = new TaskDto(id, timestamp, TaskStatus.READY, processFiles, null, Collections.emptyList(), processEvents, Collections.emptyList(), Collections.emptyList());
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
