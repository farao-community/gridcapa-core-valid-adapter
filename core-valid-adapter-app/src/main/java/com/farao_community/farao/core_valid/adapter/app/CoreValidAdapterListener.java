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
import java.util.function.Function;

import static com.farao_community.farao.gridcapa.task_manager.api.TaskStatus.ERROR;
import static com.farao_community.farao.gridcapa.task_manager.api.TaskStatus.READY;
import static com.farao_community.farao.gridcapa.task_manager.api.TaskStatus.SUCCESS;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
@Component
public class CoreValidAdapterListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(CoreValidAdapterListener.class);
    private final CoreValidClient coreValidClient;
    private final MinioAdapter minioAdapter;

    public CoreValidAdapterListener(final CoreValidClient coreValidClient, final MinioAdapter minioAdapter) {
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

    private void handleAutoTask(final TaskDto taskDto) {
        handleTask(taskDto, this::getAutomaticCoreValidRequest, "automatic");
    }

    private void handleManualTask(final TaskDto taskDto) {
        handleTask(taskDto, this::getManualCoreValidRequest, "manual");
    }

    private void handleTask(final TaskDto taskDto,
                            final Function<TaskDto, CoreValidRequest> coreValidReqMapper,
                            final String launchType) {
        try {
            if (isReadyOrFinished(taskDto)) {
                LOGGER.info("Handling {} run request on TS {} ", launchType, taskDto.getTimestamp());
                final CoreValidRequest request = coreValidReqMapper.apply(taskDto);
                coreValidClient.run(request);
            } else {
                LOGGER.warn("Failed to handle {} run request on timestamp {} because it is not ready yet",
                            launchType,
                            taskDto.getTimestamp());
            }
        } catch (final Exception e) {
            throw new CoreValidAdapterException(String.format("Error during handling of %s run request on TS %s",
                                                              launchType, taskDto.getTimestamp()), e);
        }

    }

    private static boolean isReadyOrFinished(final TaskDto taskDto) {
        final TaskStatus status = taskDto.getStatus();
        return status == READY || status == SUCCESS || status == ERROR;
    }

    CoreValidRequest getManualCoreValidRequest(final TaskDto taskDto) {
        return getCoreValidRequest(taskDto, false);
    }

    CoreValidRequest getAutomaticCoreValidRequest(final TaskDto taskDto) {
        return getCoreValidRequest(taskDto, true);
    }

    CoreValidRequest getCoreValidRequest(final TaskDto taskDto,
                                         final boolean isLaunchedAutomatically) {
        final String id = taskDto.getId().toString();
        final OffsetDateTime offsetDateTime = taskDto.getTimestamp();
        final List<ProcessFileDto> processFiles = taskDto.getInputs();
        CoreValidFileResource cgm = null;
        CoreValidFileResource cbcora = null;
        CoreValidFileResource glsk = null;
        CoreValidFileResource refprog = null;
        CoreValidFileResource studyPoints = null;
        for (final ProcessFileDto processFileDto : processFiles) {
            final String fileType = processFileDto.getFileType();
            final String fileUrl = minioAdapter.generatePreSignedUrlFromFullMinioPath(processFileDto.getFilePath(), 1);
            final String fileName = processFileDto.getFilename();

            switch (fileType) {
                case "CGM" -> cgm = new CoreValidFileResource(fileName, fileUrl);
                case "CBCORA" -> cbcora = new CoreValidFileResource(fileName, fileUrl);
                case "GLSK" -> glsk = new CoreValidFileResource(fileName, fileUrl);
                case "REFPROG" -> refprog = new CoreValidFileResource(fileName, fileUrl);
                case "STUDY-POINTS" -> studyPoints = new CoreValidFileResource(fileName, fileUrl);
                default -> throw new IllegalStateException("Unexpected value: " + fileType);
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

    private String getCurrentRunId(final TaskDto taskDto) {
        List<ProcessRunDto> runHistory = taskDto.getRunHistory();
        if (runHistory == null || runHistory.isEmpty()) {
            LOGGER.warn("Failed to handle manual run request on timestamp {} because it has no run history", taskDto.getTimestamp());
            throw new CoreValidAdapterException("Failed to handle manual run request on timestamp because it has no run history");
        }
        runHistory.sort((o1, o2) -> o2.getExecutionDate().compareTo(o1.getExecutionDate()));
        return runHistory.getFirst().getId().toString();
    }
}
