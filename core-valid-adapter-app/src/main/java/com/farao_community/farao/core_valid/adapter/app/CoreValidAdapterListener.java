/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.core_valid.adapter.app;

import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileDto;
import com.farao_community.farao.gridcapa.task_manager.api.ProcessRunDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import com.farao_community.farao.gridcapa_core_valid.api.resource.CoreValidFileResource;
import com.farao_community.farao.gridcapa_core_valid.api.resource.CoreValidRequest;
import com.farao_community.farao.gridcapa_core_valid.starter.CoreValidClient;
import com.farao_community.farao.minio_adapter.starter.MinioAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
@Component
public class CoreValidAdapterListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreValidAdapterListener.class);
    private final CoreValidClient coreValidClient;
    private final MinioAdapter minioAdapter;

    public CoreValidAdapterListener(CoreValidClient coreValidClient, MinioAdapter minioAdapter) {
        this.coreValidClient = coreValidClient;
        this.minioAdapter = minioAdapter;
    }

    @Bean
    public Consumer<TaskDto> consumeTask() {
        return this::handleManualTask;
    }

    @Bean
    public Consumer<TaskDto> consumeAutoTask() {
        return this::handleAutoTask;
    }

    private void handleAutoTask(TaskDto taskDto) {
        try {
            if (taskDto.getStatus() == TaskStatus.READY
                    || taskDto.getStatus() == TaskStatus.SUCCESS
                    || taskDto.getStatus() == TaskStatus.ERROR) {
                LOGGER.info("Handling automatic run request on TS {} ", taskDto.getTimestamp());
                CoreValidRequest request = getAutomaticCoreValidRequest(taskDto);
                coreValidClient.run(request);
            } else {
                LOGGER.warn("Failed to handle automatic run request on timestamp {} because it is not ready yet", taskDto.getTimestamp());
            }
        } catch (Exception e) {
            throw new CoreValidAdapterException(String.format("Error during handling automatic run request on TS %s", taskDto.getTimestamp()), e);
        }
    }

    private void handleManualTask(TaskDto taskDto) {
        try {
            if (taskDto.getStatus() == TaskStatus.READY
                    || taskDto.getStatus() == TaskStatus.SUCCESS
                    || taskDto.getStatus() == TaskStatus.ERROR) {
                LOGGER.info("Handling manual run request on TS {} ", taskDto.getTimestamp());
                CoreValidRequest request = getManualCoreValidRequest(taskDto);
                coreValidClient.run(request);
            } else {
                LOGGER.warn("Failed to handle manual run request on timestamp {} because it is not ready yet", taskDto.getTimestamp());
            }
        } catch (Exception e) {
            throw new CoreValidAdapterException(String.format("Error during handling manual run request on TS %s", taskDto.getTimestamp()), e);
        }

    }

    CoreValidRequest getManualCoreValidRequest(TaskDto taskDto) {
        return getCoreValidRequest(taskDto, false);
    }

    CoreValidRequest getAutomaticCoreValidRequest(TaskDto taskDto) {
        return getCoreValidRequest(taskDto, true);
    }

    CoreValidRequest getCoreValidRequest(TaskDto taskDto, boolean isLaunchedAutomatically) {
        String id = taskDto.getId().toString();
        OffsetDateTime offsetDateTime = taskDto.getTimestamp();
        List<ProcessFileDto> processFiles = taskDto.getInputs();
        CoreValidFileResource cgm = null;
        CoreValidFileResource cbcora = null;
        CoreValidFileResource glsk = null;
        CoreValidFileResource refprog = null;
        CoreValidFileResource studyPoints = null;
        for (ProcessFileDto processFileDto : processFiles) {
            String fileType = processFileDto.getFileType();
            String fileUrl = minioAdapter.generatePreSignedUrlFromFullMinioPath(processFileDto.getFilePath(), 1);
            switch (fileType) {
                case "CGM" -> cgm = new CoreValidFileResource(processFileDto.getFilename(), fileUrl);
                case "CBCORA" -> cbcora = new CoreValidFileResource(processFileDto.getFilename(), fileUrl);
                case "GLSK" -> glsk = new CoreValidFileResource(processFileDto.getFilename(), fileUrl);
                case "REFPROG" -> refprog = new CoreValidFileResource(processFileDto.getFilename(), fileUrl);
                case "STUDY-POINTS" -> studyPoints = new CoreValidFileResource(processFileDto.getFilename(), fileUrl);
                default -> throw new IllegalStateException("Unexpected value: " + processFileDto.getFileType());
            }
        }
        return new CoreValidRequest(
                id,
                getCurrentRunId(taskDto),
                offsetDateTime,
                cgm,
                cbcora,
                glsk,
                refprog,
                studyPoints,
                isLaunchedAutomatically
        );
    }

    private String getCurrentRunId(TaskDto taskDto) {
        List<ProcessRunDto> runHistory = taskDto.getRunHistory();
        if (runHistory == null || runHistory.isEmpty()) {
            LOGGER.warn("Failed to handle manual run request on timestamp {} because it has no run history", taskDto.getTimestamp());
            throw new CoreValidAdapterException("Failed to handle manual run request on timestamp because it has no run history");
        }
        runHistory.sort((o1, o2) -> o2.getExecutionDate().compareTo(o1.getExecutionDate()));
        return runHistory.get(0).getId().toString();
    }
}
