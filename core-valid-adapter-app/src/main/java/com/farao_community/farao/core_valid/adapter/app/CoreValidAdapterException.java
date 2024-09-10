package com.farao_community.farao.core_valid.adapter.app;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public class CoreValidAdapterException extends RuntimeException {

    public CoreValidAdapterException(String message, Throwable throwable) {
        super(message, throwable);
    }

    public CoreValidAdapterException(String message) {
        super(message);
    }
}
