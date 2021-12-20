package com.farao_community.farao.corevalid.adapter;

import com.farao_community.farao.core_valid.api.resource.CoreValidFileResource;
import com.farao_community.farao.core_valid.api.resource.CoreValidRequest;
import com.farao_community.farao.gridcapa.task_manager.api.ProcessFileDto;
import com.farao_community.farao.gridcapa.task_manager.api.TaskDto;
import com.farao_community.farao.gridcapa_core_valid.starter.CoreValidClient;
import org.springframework.context.annotation.Bean;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Consumer;

/**
 * @author Alexandre Montigny {@literal <alexandre.montigny at rte-france.com>}
 */
public class CoreValidAdapterListener {

    private final CoreValidClient coreValidClient;

    public CoreValidAdapterListener(CoreValidClient coreValidClient) {
        this.coreValidClient = coreValidClient;
    }

    @Bean
    public Consumer<TaskDto> handleRun() {
        return taskDto -> {
            CoreValidRequest request = getCoreValidRequest(taskDto);
            coreValidClient.run(request);
        };
    }

    private CoreValidRequest getCoreValidRequest(TaskDto taskDto) {
        String id = taskDto.getId().toString();
        OffsetDateTime timeStamp = OffsetDateTime.from(taskDto.getTimestamp());
        List<ProcessFileDto> processFiles = taskDto.getProcessFiles();
        CoreValidFileResource cgm = null;
        CoreValidFileResource cbcora = null;
        CoreValidFileResource glsk = null;
        CoreValidFileResource refprog = null;
        CoreValidFileResource studyPoints = null;
        for (ProcessFileDto processFileDto : processFiles) {
            String filename = processFileDto.getFilename();
            switch (filename) {
                case "CGM":
                    cgm = new CoreValidFileResource(filename, processFileDto.getFileUrl());
                    break;
                case "CBCORA":
                    cbcora = new CoreValidFileResource(filename, processFileDto.getFileUrl());
                    break;
                case "GLSK":
                    glsk = new CoreValidFileResource(filename, processFileDto.getFileUrl());
                    break;
                case "REFPROG":
                    refprog = new CoreValidFileResource(filename, processFileDto.getFileUrl());
                    break;
                case "STUDY POINTS":
                    studyPoints = new CoreValidFileResource(filename, processFileDto.getFileUrl());
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + filename);
            }
        }
        return new CoreValidRequest(
                id,
                timeStamp,
                cgm,
                cbcora,
                refprog,
                glsk,
                studyPoints
        );
    }
}
