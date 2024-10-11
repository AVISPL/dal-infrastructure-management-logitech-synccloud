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
                "MIIDGzCCAgMCFGGJLZHUZaQ3cnlAXsk6MTni9nJ3MA0GCSqGSIb3DQEBCwUAMBsx\n" +
                "GTAXBgNVBAMMEHB1YmxpYy1hcGktY2EtdjEwHhcNMjQwOTMwMTE1NDA3WhcNMjcw\n" +
                "OTMwMTE1NDA3WjB5MSkwJwYDVQQKDCBOU1hJM3VHV2ZnVFpkRjRXa3hJb083a2lr\n" +
                "cDJFczU1ZzExMC8GA1UECwwoUDBoVE52aU9TUzJuSnBtRTduUEcwMnM2a3JMUGVk\n" +
                "Wkw2Z0FDbDc0VDEZMBcGA1UEAwwQR1g5cnBKOThzekk0ZTB5MzCCASIwDQYJKoZI\n" +
                "hvcNAQEBBQADggEPADCCAQoCggEBAJcy2nzkDoR3pte4NnjCIhBI6GzEGPX0yDki\n" +
                "yYgcJL8Ve8r4PJNssRkBmXrHL9OsBjkxjkoBcGFoO9nM+h8+7c4Jv1PTfyLH3JQ1\n" +
                "19FgLff7EL6T6/6epayeWQHh2ycvjBZt8sB731B2MnLtxTLZHPrxFfYqPYawvWkw\n" +
                "Jm9Ci7kX2aMQnIgZgRr6NhmbWpqKTX+tVJLzsZaCFrN3wNYvEfBgsDlfsxDqRrZz\n" +
                "wJNKftV9EOhcW4jIjeQiM32hdmsb+vgrCsf1WRty/tLek6ga5/fYTJzjXriMqUcQ\n" +
                "P/5ahgizobSahptw4ywl1lt7uSzVy07ta5GjS7cGaCvERQmontkCAwEAATANBgkq\n" +
                "hkiG9w0BAQsFAAOCAQEAgex3gnKDpLBVUsw0Nbbb9hpeua2qpaJztfNgCwWT6p+n\n" +
                "oU/2nA7ktPbotJa5FC1bGwBetlCkHureaqZ68TaqNLhY8A1J72Yg8pXDdDcH1/mo\n" +
                "TOUauphYFwirKTC9yeIrhRco7Q+m/M5pZ0cPkuKf/EbuyksTAhQN58bpnwmsRKxF\n" +
                "dxIKWTKOACmXJ9I5PEpPmNWdchatkp40eUrw3V/hWTy4XQmcSeAFTBK1OOLn5XcL\n" +
                "971C0p8QBDa/9qbUr4HqVQA5G0D349HYI8U8fGqNpOOm5YNJZHDVZShe/y68qWfs\n" +
                "tFt5h0EX9diPfULF3yCFJi1C6Et7cfc7VhpAFudW3w==\n" +
                "-----END CERTIFICATE-----");

        communicator.setApiKey("-----BEGIN PRIVATE KEY-----\n" +
                "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCXMtp85A6Ed6bX\n" +
                "uDZ4wiIQSOhsxBj19Mg5IsmIHCS/FXvK+DyTbLEZAZl6xy/TrAY5MY5KAXBhaDvZ\n" +
                "zPofPu3OCb9T038ix9yUNdfRYC33+xC+k+v+nqWsnlkB4dsnL4wWbfLAe99QdjJy\n" +
                "7cUy2Rz68RX2Kj2GsL1pMCZvQou5F9mjEJyIGYEa+jYZm1qaik1/rVSS87GWghaz\n" +
                "d8DWLxHwYLA5X7MQ6ka2c8CTSn7VfRDoXFuIyI3kIjN9oXZrG/r4KwrH9Vkbcv7S\n" +
                "3pOoGuf32Eyc4164jKlHED/+WoYIs6G0moabcOMsJdZbe7ks1ctO7WuRo0u3Bmgr\n" +
                "xEUJqJ7ZAgMBAAECggEAAm+WIoigDT/05v53mNweerRZ/j/t2683tz1EEZZ2fGzx\n" +
                "rh7wBuYA4mjABWuGbeVpwkEgoY5CtmcHU9EgFHcfPgBdMzyx4dMQzth2jiUmXcY5\n" +
                "36rNagWPS2fPrPPruRwPKItZQQLzNujN6O5uDLgqZdvoISukCx1M0pAtMQIESJU/\n" +
                "RuF6QNezUkfN582YvnU7ZoOitaKUPScF7lYYBR0ZzM2rlUmTa1Et7WzVbe6dcF+2\n" +
                "eT1px6yKxobnMMZ5J1OQFrQ0uPVvyUPyR/eu76xLbAvLA6X4W0lO+PxkFW0OpFjH\n" +
                "I9vETkuugLQntQkkGMcfBfQO/O1ckVWA5IN4XXaKQQKBgQDHPWsxX3RvdhNxDh4b\n" +
                "VHTE0J0olcaZjjDRRDBeqYjjrUG8ldwzTxa1vmDReAGMZXjO5pu/FDyhYdiEEpz7\n" +
                "IZQQgUBat2PH8WEAAU6tdwCOSKnUD5zUyQpXkIEZoAxrZkwhztDBn7UMEOTsogtN\n" +
                "q0zropfolh0VXsruqFldsRGkUQKBgQDCRcgO560SIjldKf+j4emaCoDSfr4oTVsS\n" +
                "G2vUACsci5WAwx3RFr7GxTb5ltlntheHue4bxHDSGqwxiWgmorRjTsxwLSP+1GJc\n" +
                "RLMzaRihSi8krI6W4rMIIpv7x57y5NFrIk4dOXvhCOjMqdZNioijmvAOKNIGAbyc\n" +
                "oDkbaS5YCQKBgFnj2/jqmB2xqBPZyruJ3yTs+frVmESvYlcH7MrIsnoGMSJkenSr\n" +
                "uhNFUWkwO4KcRWUTpjEPcEtfWdA8lZa4D5ViuyYyl1IeFSVrcZPRnO6U2gpTAO7/\n" +
                "xSq8h4KIMxJBlRert4OkCornFGGuumrQXmPxd5f4IicCHYyPZ8JdoRgBAoGALi8g\n" +
                "rEpvXow8TuWZHICsZC5zCZeP5UzehaN3MuMHXLXiSMYZ5Icfu2lO9G7kKD+lwGJ3\n" +
                "NKqyl4A3x17/H5A2ihVFjLVuhTpAV+cNIv+tF0rngjRzgXNLVHfF1UlThDLZhjqV\n" +
                "j2UHCixwC5eklrKEYUCIKRWTOFJZYHLGoLmUhuECgYAWWYlfEcMBcHlMdIVEGat2\n" +
                "75TcJvoSiClUG5D30+LV5RxS+93Em6TkpJgiVUSjpjIuUXdI4WxgJWenQM5+zYhZ\n" +
                "d1joixKZoiIVAFdszFpL9UHeUXIZAyMwoqryH5sHTcWaeaRIEsgLSQK//HmVkRkC\n" +
                "UWtvfmPHIzz3IMO06pCktg==\n" +
                "-----END PRIVATE KEY-----");

        communicator.init();
    }

    @Test
    public void testGetMultipleStatistics() throws Exception {
        communicator.getMultipleStatistics();
        communicator.setOrganizationIds("NSXI3uGWfgTZdF4WkxIoO7kikp2Es55g");
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
        communicator.setOrganizationIds("NSXI3uGWfgTZdF4WkxIoO7kikp2Es55g");
        communicator.setPlaceRetrievalPageSize(15);
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
