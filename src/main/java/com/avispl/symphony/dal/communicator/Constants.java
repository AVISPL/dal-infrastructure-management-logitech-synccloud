/*
 * Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator;

/**
 * Adapter constants storage, grouped by type
 *
 * @author Maksym.Rossiytsev
 * @since 1.0.0
 */
public interface Constants {

    /**
     * URI constants, both for http operations and json navigation
     *
     * @author Maksym.Rossiytsev
     * @since 1.0.0
     */
    interface URI {
        String PLACES = "v1/org/%s/place?limit=%s&continuation=%s";
        String FIELD_CONTINUATION = "/continuation";
        String FIELD_PLACES = "/places";
    }

    /**
     * ExtendedProperties constants
     *
     * @author Maksym.Rossiytsev
     * @since 1.0.0
     */
    interface Properties {
        String CREATED_AT = "CreatedAt";
        String MONITORED_DEVICES_TOTAL = "MonitoredDevicesTotal";
        String MONITORING_CYCLE_DURATION = "LastMonitoringCycleDuration(s)";
        String ADAPTER_VERSION = "AdapterVersion";
        String ADAPTER_BUILD_DATE = "AdapterBuildDate";
        String ADAPTER_UPTIME_MIN = "AdapterUptime(min)";
        String ADAPTER_UPTIME = "AdapterUptime";
    }

    /**
     * YML mapping name constants
     *
     * @author Maksym.Rossiytsev
     * @since 1.0.0
     */
    interface MappingModels {
        String PLACE = "Place";
    }
}
