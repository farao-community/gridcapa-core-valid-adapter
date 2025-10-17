package com.farao_community.farao.core_valid.adapter.app;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public class CoreValidAdapterException extends RuntimeException {

    public CoreValidAdapterException(final String message, final Throwable throwable) {
        super(message, throwable);
    }

    public CoreValidAdapterException(final String message) {
        super(message);
    }
}
