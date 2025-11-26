/*
 * Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator;
import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.dto.monitor.aggregator.AggregatedDevice;
import com.avispl.symphony.api.dal.monitor.Monitorable;
import com.avispl.symphony.api.dal.monitor.aggregator.Aggregator;
import com.avispl.symphony.dal.aggregator.parser.AggregatedDeviceProcessor;
import com.avispl.symphony.dal.aggregator.parser.PropertiesMapping;
import com.avispl.symphony.dal.aggregator.parser.PropertiesMappingParser;
import com.avispl.symphony.dal.communicator.data.Constants;
import com.avispl.symphony.dal.communicator.http.LogiSyncCloudRequestInterceptor;
import com.avispl.symphony.dal.util.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.StandardCookieSpec;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.DefaultClientTlsStrategy;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.pool.PoolConcurrencyPolicy;
import org.apache.hc.core5.pool.PoolReusePolicy;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Logi Sync Cloud API communicator, it's communicating with {@link Constants.URI} endpoints to receive places and devices details.
 * Currently, the following data is monitored:
 * - Device metadata
 * - Device basic status information
 * - Device place (location)
 * - Device peripherals
 *
 * @author Maksym.Rossitsev/Symphony Team
 * @since 1.0.0
 */
public class LogiSyncCloudCommunicator extends RestCommunicator implements Aggregator, Monitorable {
    /** SSL Context for mTLS authentication handling */
    private SSLContext sslContext;

    /** Adapter metadata properties - adapter version and build date*/
    private Properties adapterProperties;

    /** Device property processor, that uses yml mapping to extract properties from json*/
    private AggregatedDeviceProcessor aggregatedDeviceProcessor;


    /** Aggregated devices cache */
    private Map<String, AggregatedDevice> aggregatedDevices = new ConcurrentHashMap<>();

    /** Latest error instance, that must be propagated to the caller */
    private Exception latestError;

    /**
     * Interceptor for RestTemplate that injects
     * authorization header and fixes malformed headers sent by XIO backend
     */
    private ClientHttpRequestInterceptor logiSyncCloudRequestInterceptor = new LogiSyncCloudRequestInterceptor();

    //********* Adapter Pacing section **********//
    /**
     * This parameter holds timestamp of when we need to stop performing API calls
     * It used when device stop retrieving statistic. Updated each time of called #retrieveMultipleStatistics
     */
    private volatile long validRetrieveStatisticsTimestamp;

    /**
     * Aggregator inactivity timeout. If the {@link LogiSyncCloudDeviceDataLoader#retrieveMultipleStatistics()}  method is not
     * called during this period of time - device is considered to be paused, thus the Cloud API
     * is not supposed to be called
     */
    private static final long retrieveStatisticsTimeOut = 180000;

    /** List of organizations to be monitored, provided by aggregator properties */
    private List<String> organizationIds = new ArrayList<>();

    /** Time period to wait in between of different {orgId}/place calls */
    private long placeRetrievalTimeout = 30000;

    /** Max number of entries in each {orgId}/place response */
    private int placeRetrievalPageSize = 1000;

    /**
     * Indicates whether a device is considered as paused.
     * True by default so if the system is rebooted and the actual value is lost -> the device won't start stats
     * collection unless the {@link #retrieveMultipleStatistics()} method is called which will change it
     * to a correct value
     */
    private volatile boolean devicePaused = true;

    /**
     * We don't want the statistics to be collected constantly, because if there's not a big list of devices -
     * new devices statistics loop will be launched before the next monitoring iteration. To avoid that -
     * this variable stores a timestamp which validates it, so when the devices statistics is done collecting, variable
     * is set to currentTime + 30s, at the same time, calling {@link #retrieveMultipleStatistics()} and updating the
     * {@link #aggregatedDevices} resets it to the currentTime timestamp, which will re-activate data collection.
     */
    private volatile long nextDevicesCollectionIterationTimestamp;

    /**
     * How much time last monitoring cycle took to finish
     * */
    private Long lastMonitoringCycleDuration;
    //********* END Adapter Pacing section **********//

    /**
     * Executor that runs all the async operations, that {@link #deviceDataLoader} is performing
     */
    private ExecutorService executorService;

    /**
     * Runner service responsible for collecting data
     */
    private LogiSyncCloudDeviceDataLoader deviceDataLoader;

    /**
     * Device adapter instantiation timestamp.
     */
    private long adapterInitializationTimestamp;

    private String apiCertificate;
    private String apiKey;

    /**
     * Process that is running constantly and triggers collecting data from Logi Sync Cloud API endpoints, based on the given timeouts and thresholds.
     *
     * @author Maksym.Rossiytsev
     * @since 1.0.0
     */
    public class LogiSyncCloudDeviceDataLoader implements Runnable {
        private volatile boolean inProgress;
        public LogiSyncCloudDeviceDataLoader() {
        logDebugMessage("Creating new device data loader.");

            inProgress = true;
        }

        @Override
        public void run() {
        logDebugMessage("Entering device data loader active stage.");
            mainloop:
            while (inProgress) {
                long startCycle = System.currentTimeMillis();
                try {
                    try {
                        TimeUnit.MILLISECONDS.sleep(500);
                    } catch (InterruptedException e) {
                        // Ignore for now
                    }

                    if (!inProgress) {
                        logDebugMessage("Main data collection thread is not in progress, breaking.");
                        break mainloop;
                    }

                    updateAggregatorStatus();
                    // next line will determine whether Logi Sync Cloud monitoring was paused
                    if (devicePaused) {
                        logDebugMessage("The device communicator is paused, data collector is not active.");
                        continue mainloop;
                    }
                    try {
                        logDebugMessage("Fetching devices list.");
                        fetchDevicesList();
                        latestError = null;
                    } catch (Exception e) {
                        Throwable cause = e.getCause();
                        if (cause != null) {
                            latestError = (Exception) cause;
                        } else {
                            latestError = e;
                        }
                        logger.error("Error occurred during device list retrieval: " + e.getMessage(), e);
                    }

                    if (!inProgress) {
                        logDebugMessage("The data collection thread is not in progress. Breaking the loop.");
                        break mainloop;
                    }

                    int aggregatedDevicesCount = aggregatedDevices.size();
                    if (aggregatedDevicesCount == 0) {
                        logDebugMessage("No devices collected in the main data collection thread so far. Continuing.");
                        // We shouldn't just continue here because we'll exhaust 14000 daily requests too quickly.
                        // Need to continue with a normal pace.
                        //continue mainloop;
                    }

                    while (nextDevicesCollectionIterationTimestamp > System.currentTimeMillis()) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(1000);
                        } catch (InterruptedException e) {
                            //
                        }
                    }
                    // We don't want to fetch devices statuses too often, so by default it's currentTime + 30s
                    // otherwise - the variable is reset by the retrieveMultipleStatistics() call, which
                    // launches devices detailed statistics collection
                    nextDevicesCollectionIterationTimestamp = System.currentTimeMillis() + 30000;

                    lastMonitoringCycleDuration = (System.currentTimeMillis() - startCycle) / 1000;
                    logDebugMessage("Finished collecting devices statistics cycle at " + new Date() + ", total duration: " + lastMonitoringCycleDuration);
                } catch (Exception e) {
                    logger.error("Unexpected error occurred during main device collection cycle", e);
                }
            }
            logDebugMessage("Main device collection loop is completed, in progress marker: " + inProgress);
            // Finished collecting
        }

        /**
         * Triggers main loop to stop
         */
        public void stop() {
            logDebugMessage("Main device details collection loop is stopped!");
            inProgress = false;
        }

        /**
         * Retrieves {@link #inProgress}
         *
         * @return value of {@link #inProgress}
         */
        public boolean isInProgress() {
            return inProgress;
        }
    }


    /**
     * LogiSyncCloudCommunicator constructor. Initializes properties processor, device metadata and device data loader
     * */
    public LogiSyncCloudCommunicator() throws IOException {
        Map<String, PropertiesMapping> mapping = new PropertiesMappingParser().loadYML("mapping/model-mapping.yml", getClass());
        aggregatedDeviceProcessor = new AggregatedDeviceProcessor(mapping);
        adapterProperties = new Properties();
        adapterProperties.load(getClass().getResourceAsStream("/version.properties"));

        executorService = Executors.newFixedThreadPool(1);
        executorService.submit(deviceDataLoader = new LogiSyncCloudDeviceDataLoader());
    }

    /**
     * Load private key from {@link #apiKey} for local keystore instantiation
     *
     * @return {@link PrivateKey} instance of an apiKey
     * @throws Exception if an error occurs during key loading
     * */
    private PrivateKey loadPrivateKey() throws Exception {
        try (PEMParser pemParser = new PEMParser(new StringReader(apiKey))) {
            Object object = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
            return converter.getPrivateKey((PrivateKeyInfo) object);
        }
    }

    /**
     * Load mTLS certificate from {@link #apiCertificate} for local keystore instantiation
     *
     * @return {@link X509Certificate} instance of an apiCertificate
     * @throws Exception if an error occurs during certificate load
     * */
    private X509Certificate loadCertificate() throws Exception {
        try (PEMParser pemParser = new PEMParser(new StringReader(apiCertificate))) {
            Object object = pemParser.readObject();
            JcaX509CertificateConverter converter = new JcaX509CertificateConverter();
            return converter.getCertificate((X509CertificateHolder) object);
        }
    }

    /**
     * Retrieves {@link #apiCertificate}
     *
     * @return value of {@link #apiCertificate}
     */
    public String getApiCertificate() {
        return apiCertificate;
    }

    /**
     * Sets {@link #apiCertificate} value
     *
     * @param apiCertificate new value of {@link #apiCertificate}
     */
    public void setApiCertificate(String apiCertificate) {
        this.apiCertificate = apiCertificate;
    }

    /**
     * Retrieves {@link #apiKey}
     *
     * @return value of {@link #apiKey}
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Sets {@link #apiKey} value
     *
     * @param apiKey new value of {@link #apiKey}
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Retrieves {@link #placeRetrievalTimeout}
     *
     * @return value of {@link #placeRetrievalTimeout}
     */
    public long getPlaceRetrievalTimeout() {
        return placeRetrievalTimeout;
    }

    /**
     * Sets {@link #placeRetrievalTimeout} value
     *
     * @param placeRetrievalTimeout new value of {@link #placeRetrievalTimeout}
     */
    public void setPlaceRetrievalTimeout(long placeRetrievalTimeout) {
        this.placeRetrievalTimeout = placeRetrievalTimeout;
    }

    /**
     * Retrieves {@link #placeRetrievalPageSize}
     *
     * @return value of {@link #placeRetrievalPageSize}
     */
    public int getPlaceRetrievalPageSize() {
        return placeRetrievalPageSize;
    }

    /**
     * Sets {@link #placeRetrievalPageSize} value
     *
     * @param placeRetrievalPageSize new value of {@link #placeRetrievalPageSize}
     */
    public void setPlaceRetrievalPageSize(int placeRetrievalPageSize) {
        this.placeRetrievalPageSize = placeRetrievalPageSize;
    }

    /**
     * Retrieves {@link #organizationIds}
     *
     * @return value of {@link #organizationIds}
     */
    public String getOrganizationIds() {
        return String.join(",", organizationIds);
    }

    /**
     * Sets {@link #organizationIds} value
     *
     * @param organizationIds new value of {@link #organizationIds}
     */
    public void setOrganizationIds(String organizationIds) {
        this.organizationIds = Arrays.stream(organizationIds.split(",")).map(String::trim).collect(Collectors.toList());
    }

    @Override
    protected void internalInit() throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("Internal init is called.");
        }
        adapterInitializationTimestamp = System.currentTimeMillis();

        try {
            // Add Bouncy Castle as a security provider
            Security.addProvider(new BouncyCastleProvider());
            // Load the private key
            PrivateKey privateKey = loadPrivateKey();
            // Load the certificate
            X509Certificate certificate = loadCertificate();

            String devicePassword = getPassword();
            char[] keystorePassword = StringUtils.isNullOrEmpty(devicePassword) ? "".toCharArray() : devicePassword.toCharArray();
            // Create a new PKCS#12 keystore in memory
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null); // Initialize an empty keystore
            keyStore.setKeyEntry("alias", privateKey, keystorePassword, new X509Certificate[]{certificate});

            // Save the keystore to a ByteArrayOutputStream
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            keyStore.store(baos, keystorePassword);
            byte[] keystoreBytes = baos.toByteArray();

            // Load the keystore from the byte array
            ByteArrayInputStream bais = new ByteArrayInputStream(keystoreBytes);
            KeyStore loadedKeyStore = KeyStore.getInstance("PKCS12");
            loadedKeyStore.load(bais, keystorePassword);

            // Set up KeyManagerFactory and TrustManagerFactory
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(loadedKeyStore, keystorePassword);

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(loadedKeyStore);
            sslContext = SSLContext.getInstance("TLS");

            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                        public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                    }
            };
            sslContext.init(keyManagerFactory.getKeyManagers(), trustAllCerts, null);
            if (logger.isInfoEnabled()) {
                logger.info("mTLS is configured with provided certificate and privateKey.");
            }
        } catch (Exception e) {
            logger.error("An error occurred during mTLS configuration.", e);
        }

        super.internalInit();
    }

    @Override
    protected void internalDestroy() {
        deviceDataLoader.stop();
        aggregatedDevices.clear();

        super.internalDestroy();
    }

    @Override
    public int ping() throws Exception {
        if (!isInitialized()) {
            throw new IllegalStateException("Cannot use device class without calling init() first");
        }
        if ("TCP".equals(getPingProtocol())) {
            long pingResultTotal = 0L;

            for (int i = 0; i < this.getPingAttempts(); i++) {
                long startTime = System.currentTimeMillis();

                try (Socket puSocketConnection = new Socket(this.getHost(), this.getPort())) {
                    puSocketConnection.setSoTimeout(this.getPingTimeout());

                    if (puSocketConnection.isConnected()) {
                        long pingResult = System.currentTimeMillis() - startTime;
                        pingResultTotal += pingResult;
                        if (this.logger.isTraceEnabled()) {
                            this.logger.trace(String.format("PING OK: Attempt #%s to connect to %s on port %s succeeded in %s ms", i + 1, this.getHost(), this.getPort(), pingResult));
                        }
                    } else {
                        logDebugMessage(String.format("PING DISCONNECTED: Connection to %s did not succeed within the timeout period of %sms", this.getHost(), this.getPingTimeout()));
                        return this.getPingTimeout();
                    }
                } catch (SocketTimeoutException tex) {
                    logDebugMessage(String.format("PING TIMEOUT: Connection to %s did not succeed within the timeout period of %sms", this.getHost(), this.getPingTimeout()));
                    return this.getPingTimeout();
                }
            }
            return Math.max(1, Math.toIntExact(pingResultTotal / this.getPingAttempts()));
        } else {
            return super.ping();
        }
    }

    @Override
    protected RestTemplate obtainRestTemplate() throws Exception {
        RestTemplate restTemplate = super.obtainRestTemplate();
        DefaultClientTlsStrategy tlsStrategy = new DefaultClientTlsStrategy(sslContext);
        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setTlsSocketStrategy(tlsStrategy)
                .setDefaultSocketConfig(SocketConfig.custom()
                        .setSoTimeout(Timeout.ofMinutes(1))
                        .build())
                .setPoolConcurrencyPolicy(PoolConcurrencyPolicy.STRICT)
                .setConnPoolPolicy(PoolReusePolicy.LIFO)
                .setDefaultConnectionConfig(ConnectionConfig.custom()
                        .setSocketTimeout(Timeout.ofMinutes(1))
                        .setConnectTimeout(Timeout.ofMinutes(1))
                        .setTimeToLive(TimeValue.ofMinutes(10))
                        .build())
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setCookieSpec(StandardCookieSpec.STRICT)
                        .build())
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        restTemplate.setRequestFactory(requestFactory);

        List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
        if (!interceptors.contains(logiSyncCloudRequestInterceptor))
            interceptors.add(logiSyncCloudRequestInterceptor);

        return restTemplate;
    }

    @Override
    public List<Statistics> getMultipleStatistics() throws Exception {
        ExtendedStatistics extendedStatistics = new ExtendedStatistics();
        Map<String, String> properties = new HashMap<>();
        extendedStatistics.setStatistics(properties);

        Map<String, String> statistics = new HashMap<>();
        Map<String, String> dynamicStatistics = new HashMap<>();

        dynamicStatistics.put(Constants.Properties.MONITORED_DEVICES_TOTAL, String.valueOf(aggregatedDevices.size()));
        if (lastMonitoringCycleDuration != null) {
            dynamicStatistics.put(Constants.Properties.MONITORING_CYCLE_DURATION, String.valueOf(lastMonitoringCycleDuration));
        }

        statistics.put(Constants.Properties.ADAPTER_VERSION, adapterProperties.getProperty("aggregator.version"));
        statistics.put(Constants.Properties.ADAPTER_BUILD_DATE, adapterProperties.getProperty("aggregator.build.date"));
        long adapterUptime = System.currentTimeMillis() - adapterInitializationTimestamp;
        statistics.put(Constants.Properties.ADAPTER_UPTIME_MIN, String.valueOf(adapterUptime / (1000*60)));
        statistics.put(Constants.Properties.ADAPTER_UPTIME, normalizeUptime(adapterUptime/1000));

        extendedStatistics.setStatistics(statistics);
        extendedStatistics.setDynamicStatistics(dynamicStatistics);
        return Collections.singletonList(extendedStatistics);
    }

    @Override
    public List<AggregatedDevice> retrieveMultipleStatistics() throws Exception {
        if (organizationIds.isEmpty()) {
            throw new IllegalArgumentException("Monitoring Error: organization id is missing, please check organizationIds configuration parameter");
        }
        nextDevicesCollectionIterationTimestamp = System.currentTimeMillis();
        updateValidRetrieveStatisticsTimestamp();

        List<AggregatedDevice> aggregatedDeviceList = new ArrayList<>(aggregatedDevices.values());
        for(AggregatedDevice aggregatedDevice: aggregatedDeviceList) {
            aggregatedDevice.setTimestamp(System.currentTimeMillis());
        }

        if (latestError != null) {
            throw latestError;
        }
        return aggregatedDeviceList;
    }

    @Override
    public List<AggregatedDevice> retrieveMultipleStatistics(List<String> list) throws Exception {
        return retrieveMultipleStatistics()
                .stream()
                .filter(aggregatedDevice -> list.contains(aggregatedDevice.getDeviceId()))
                .collect(Collectors.toList());
    }

    @Override
    protected void authenticate() throws Exception {
        //Authentication is performed during the init() stage
    }

    /**
     * Fetching logi devices list from {@link Constants.URI#PLACES} endpoint.
     * The page size is specified with {@link #placeRetrievalPageSize}, data is pulled until all information is retrieved.
     * If multiple organizations are in play - requests are paced by {@link #placeRetrievalTimeout} value
     *
     * @throws Exception if any error occurs
     * */
    private synchronized void fetchDevicesList() throws Exception {
        String continuationToken = "";

        // To keep track of all the relevant devices collected this cycle
        List<String> retrievedDeviceIds = new ArrayList<>();
        int orgCounter = 1;
        for (String organizationId: organizationIds) {
            do {
                String urlTemplate = String.format(Constants.URI.PLACES, organizationId, placeRetrievalPageSize, continuationToken);

                JsonNode response = doGet(urlTemplate, JsonNode.class);
                continuationToken = response.at(Constants.URI.FIELD_CONTINUATION).asText();

                List<AggregatedDevice> placeDevices = null;
                for (JsonNode room : response.at(Constants.URI.FIELD_PLACES)) {
                    Map<String, String> placeProperties = new HashMap<>();
                    placeProperties.put(Constants.Properties.PLACE_ID, room.at("/id").asText());
                    placeProperties.put(Constants.Properties.PLACE_TYPE, room.at("/type").asText());
                    placeProperties.put(Constants.Properties.PLACE_NAME, room.at("/name").asText());
                    placeProperties.put(Constants.Properties.PLACE_GROUP, room.at("/group").asText());
                    placeProperties.put(Constants.Properties.PLACE_OCCUPANCY, room.at("/occupancy").asText());
                    placeProperties.put(Constants.Properties.PLACE_CREATED_AT, room.at("/createdAt").asText());
                    placeProperties.put(Constants.Properties.PLACE_SEAT_COUNT, room.at("/seatCount").asText());

                    placeDevices = aggregatedDeviceProcessor.extractDevices(room);
                    placeDevices.forEach(aggregatedDevice -> {
                        String deviceId = aggregatedDevice.getDeviceId();
                        Map<String, String> deviceProperties = aggregatedDevice.getProperties();
                        deviceProperties.putAll(placeProperties);
                        formatProperties(deviceProperties);
                        applyCatalog(aggregatedDevice);

                        retrievedDeviceIds.add(deviceId);
                        aggregatedDevices.put(deviceId, aggregatedDevice);
                    });
                }
                // Default timeout between requests, according to Logi Sync Cloud Limitation:
                // Maximum allowed sustained rate is 1 request per second.
                //
                // Only need to do this if this iteration isn't the last one. If there are more organizations to come -
                // the timeout will fallback to #placeRetrievalTimeout
                if (StringUtils.isNotNullOrEmpty(continuationToken)) {
                    TimeUnit.MILLISECONDS.sleep(1000);
                }
            } while (StringUtils.isNotNullOrEmpty(continuationToken));
            if(orgCounter < organizationIds.size()) {
                TimeUnit.MILLISECONDS.sleep(placeRetrievalTimeout);
            }
            orgCounter++;
        }

        // Remove cached devices that are not in the latest list of devices
        aggregatedDevices.entrySet().removeIf(deviceEntry -> !retrievedDeviceIds.contains(deviceEntry.getKey()));
    }

    /**
     * Uptime is received in seconds, need to normalize it and make it human-readable, like
     * 1 day(s) 5 hour(s) 12 minute(s) 55 minute(s)
     * Incoming parameter is may have a decimal point, so in order to safely process this - it's rounded first.
     * We don't need to add a segment of time if it's 0.
     *
     * @param uptimeSeconds value in seconds
     * @return string value of format 'x day(s) x hour(s) x minute(s) x minute(s)'
     */
    private String normalizeUptime(long uptimeSeconds) {
        StringBuilder normalizedUptime = new StringBuilder();

        long seconds = uptimeSeconds % 60;
        long minutes = uptimeSeconds % 3600 / 60;
        long hours = uptimeSeconds % 86400 / 3600;
        long days = uptimeSeconds / 86400;

        if (days > 0) {
            normalizedUptime.append(days).append(" day(s) ");
        }
        if (hours > 0) {
            normalizedUptime.append(hours).append(" hour(s) ");
        }
        if (minutes > 0) {
            normalizedUptime.append(minutes).append(" minute(s) ");
        }
        if (seconds > 0) {
            normalizedUptime.append(seconds).append(" second(s)");
        }
        return normalizedUptime.toString().trim();
    }

    /**
     * Format properties after retrieval, if needed.
     * This later can be extended to a yml mapping, for now it's a single property that we need to format, so consider
     * this a functional placeholder.
     *
     * @param properties map to format properties in
     * */
    private void formatProperties(Map<String, String> properties) {
        properties.forEach((name, value) -> {
            if (name.endsWith(Constants.Properties.CREATED_AT)) {
                properties.put(name, String.valueOf(new Date(Long.parseLong(value))));
            }
        });
    }

    /**
     * Logi Sync does not provide full information on any given models, so we need to do this manually through
     * statically defined catalog.
     *
     * @param device device to set catalog information for
     * */
    private void applyCatalog(AggregatedDevice device) {
        String deviceType = device.getType();
        String deviceName = device.getDeviceName();

        Map<String, Constants.CatalogEntry> catalogSection = Constants.Catalog.CATALOG.get(deviceType);
        Optional<Constants.CatalogEntry> catalogEntry;
        if (deviceType.equals(Constants.Catalog.COMPUTER_SECTION) || deviceType.equals(Constants.Catalog.GENERIC_SECTION)) {
            catalogEntry = Optional.of(catalogSection.get(""));
        } else {
            catalogEntry = catalogSection.entrySet().stream().filter(entry -> entry.getKey().equalsIgnoreCase(deviceName)).findFirst().map(Map.Entry::getValue);
        }

        if (!catalogEntry.isPresent()) {
            if (logger.isInfoEnabled()) {
                logger.info(String.format("Unable to find catalog entry for type %s and name %s, skipping.", deviceType, deviceName));
            }
            device.setDeviceName(deviceName + " " + device.getSerialNumber());
            return;
        }
        catalogEntry.ifPresent(entry -> {
            device.setType(entry.getCategory());
            device.setDeviceMake(entry.getManufacturer());
            device.setDeviceModel(entry.getModel());
            device.setCategory(entry.getType());
            device.setDeviceName(entry.getModel() + " " + device.getSerialNumber());
        });
    }
    /**
     * Update the status of the device.
     * The device is considered as paused if did not receive any retrieveMultipleStatistics()
     * calls during {@link LogiSyncCloudCommunicator#validRetrieveStatisticsTimestamp}
     */
    private synchronized void updateAggregatorStatus() {
        // If the adapter is destroyed out of order, we need to make sure the device isn't paused here
        if (validRetrieveStatisticsTimestamp > 0L) {
            devicePaused = validRetrieveStatisticsTimestamp < System.currentTimeMillis();
        } else {
            devicePaused = false;
        }
    }

    /**
     * Update statistics retrieval timestamp
     * */
    private synchronized void updateValidRetrieveStatisticsTimestamp() {
        validRetrieveStatisticsTimestamp = System.currentTimeMillis() + retrieveStatisticsTimeOut;
        updateAggregatorStatus();
    }

    /**
     * Logging debug message with checking if it's enabled first
     *
     * @param message to log
     * */
    private void logDebugMessage(String message) {
        if (logger.isDebugEnabled()) {
            logger.debug(message);
        }
    }
}
