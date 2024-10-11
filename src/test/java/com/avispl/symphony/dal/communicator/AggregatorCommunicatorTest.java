/*
 * Copyright (c) 2023 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator;

import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.dto.monitor.aggregator.AggregatedDevice;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;

import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class AggregatorCommunicatorTest {

    LogiSyncCloudCommunicator communicator;

    @BeforeEach
    public void setUp() throws Exception {
        communicator = new LogiSyncCloudCommunicator();
        communicator.setHost("api.sync.logitech.com");
        communicator.setProtocol("https");
        communicator.setPingProtocol("TCP");
        communicator.setPingTimeout(1000);
        communicator.setPort(443);

        communicator.setApiCertificate("-----BEGIN CERTIFICATE-----\n" +
                "-----END CERTIFICATE-----");

        communicator.setApiKey("-----BEGIN PRIVATE KEY-----\n" +
                "-----END PRIVATE KEY-----");

        communicator.init();
    }

    @Test
    public void testGetMultipleStatistics() throws Exception {
        communicator.getMultipleStatistics();
        communicator.setOrganizationIds("...");
        communicator.retrieveMultipleStatistics();
        Thread.sleep(30000);
        List<Statistics> statistics = communicator.getMultipleStatistics();
        ExtendedStatistics es = (ExtendedStatistics) statistics.get(0);
        Map<String, String> esMap = es.getStatistics();
        Map<String, String> dsMap = es.getDynamicStatistics();

        Assertions.assertNotNull(statistics);
        Assertions.assertNotNull(es);
        Assertions.assertNotNull(esMap.get("AdapterBuildDate"));
        Assertions.assertNotNull(esMap.get("AdapterUptime"));
        Assertions.assertNotNull(esMap.get("AdapterUptime(min)"));
        Assertions.assertNotNull(esMap.get("AdapterVersion"));
        Assertions.assertNotNull(dsMap.get("LastMonitoringCycleDuration(s)"));
        Assertions.assertNotNull(dsMap.get("MonitoredDevicesTotal"));
    }

    @Test
    public void testRetrieveMultipleStatistics() throws Exception {
        communicator.setOrganizationIds("...");
        communicator.retrieveMultipleStatistics();
        Thread.sleep(30000);
        communicator.retrieveMultipleStatistics();
        Thread.sleep(30000);
        communicator.retrieveMultipleStatistics();
        Thread.sleep(30000);
        communicator.retrieveMultipleStatistics();
        Thread.sleep(30000);
        communicator.retrieveMultipleStatistics();
        Thread.sleep(30000);
        List<AggregatedDevice> statistics = communicator.retrieveMultipleStatistics();
        Assertions.assertNotNull(statistics);
        Assertions.assertEquals(14, statistics.size());
        for (AggregatedDevice device: statistics) {
            Assertions.assertTrue(StringUtils.isNotBlank(device.getDeviceId()));
            Assertions.assertTrue(StringUtils.isNotBlank(device.getDeviceMake()));
            Assertions.assertTrue(StringUtils.isNotBlank(device.getDeviceName()));
            Assertions.assertTrue(StringUtils.isNotBlank(device.getDeviceModel()));
            Assertions.assertTrue(StringUtils.isNotBlank(device.getCategory()));
        }
    }

    @Test
    public void testRetrieveMultipleStatisticsWithoutOrganizationId() throws Exception {
        assertThrows(IllegalArgumentException.class, ()->communicator.retrieveMultipleStatistics());
    }
}
