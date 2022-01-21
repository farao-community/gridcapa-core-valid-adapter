/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.farao_community.farao.core_valid.adapter.app;

import com.farao_community.farao.core_valid.api.resource.CoreValidFileResource;
import com.farao_community.farao.core_valid.api.resource.CoreValidRequest;
import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskStatus;
import com.farao_community.farao.gridcapa_core_valid.starter.CoreValidClient;
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

    public CoreValidAdapterListener(CoreValidClient coreValidClient) {
        this.coreValidClient = coreValidClient;
    }

    @Bean
    public Consumer<TaskDto> handleRun() {
        return taskDto -> {
            try {
                if (taskDto.getStatus() == TaskStatus.READY
                        || taskDto.getStatus() == TaskStatus.SUCCESS
                        || taskDto.getStatus() == TaskStatus.ERROR) {
                    LOGGER.info(String.format("Handling run request on TS %s ", taskDto.getTimestamp()));
                    CoreValidRequest request = getCoreValidRequest(taskDto);
                    coreValidClient.run(request);
                } else {
                    LOGGER.warn("Failed to handle run request on timestamp {} because it is not ready yet", taskDto.getTimestamp());
                }
            } catch (Exception e) {
                throw new CoreValidAdapterException(String.format("Error during handling run request %s on TS ", taskDto.getTimestamp()), e);
            }
        };
    }

    CoreValidRequest getCoreValidRequest(TaskDto taskDto) {
        String id = taskDto.getId().toString();
        OffsetDateTime offsetDateTime = taskDto.getTimestamp();
        List<ProcessFileDto> processFiles = taskDto.getProcessFiles();
        CoreValidFileResource cgm = null;
        CoreValidFileResource cbcora = null;
        CoreValidFileResource glsk = null;
        CoreValidFileResource refprog = null;
        CoreValidFileResource studyPoints = null;
        for (ProcessFileDto processFileDto : processFiles) {
            String fileType = processFileDto.getFileType();
            switch (fileType) {
                case "CGM":
                    cgm = new CoreValidFileResource(processFileDto.getFilename(), processFileDto.getFileUrl());
                    break;
                case "CBCORA":
                    cbcora = new CoreValidFileResource(processFileDto.getFilename(), processFileDto.getFileUrl());
                    break;
                case "GLSK":
                    glsk = new CoreValidFileResource(processFileDto.getFilename(), processFileDto.getFileUrl());
                    break;
                case "REFPROG":
                    refprog = new CoreValidFileResource(processFileDto.getFilename(), processFileDto.getFileUrl());
                    break;
                case "STUDY-POINTS":
                    studyPoints = new CoreValidFileResource(processFileDto.getFilename(), processFileDto.getFileUrl());
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + processFileDto.getFileType());
            }
        }
        return new CoreValidRequest(
                id,
                offsetDateTime,
                cgm,
                cbcora,
                glsk,
                refprog,
                studyPoints
        );
    }
}
