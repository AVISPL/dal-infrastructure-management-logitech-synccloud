/*
 * Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Adapter constants storage, grouped by type
 *
 * @author Maksym.Rossiytsev
 * @since 1.0.0
 */
public interface Constants {
    Log logger = LogFactory.getLog(Constants.class);

    @JsonIgnoreProperties(ignoreUnknown = true)
    class CatalogEntry {
        private String model;
        private String type;
        private String category;
        private String manufacturer;

        public CatalogEntry() {}

        public CatalogEntry(String type, String category, String manufacturer, String model) {
            this.type = type;
            this.category = category;
            this.model = model;
            this.manufacturer = manufacturer;
        }

        /**
         * Retrieves {@link #manufacturer}
         *
         * @return value of {@link #manufacturer}
         */
        public String getManufacturer() {
            return manufacturer;
        }

        /**
         * Retrieves {@link #model}
         *
         * @return value of {@link #model}
         */
        public String getModel() {
            return model;
        }

        /**
         * Retrieves {@link #type}
         *
         * @return value of {@link #type}
         */
        public String getType() {
            return type;
        }

        /**
         * Retrieves {@link #category}
         *
         * @return value of {@link #category}
         */
        public String getCategory() {
            return category;
        }

        /**
         * Sets {@link #model} value
         *
         * @param model new value of {@link #model}
         */
        public void setModel(String model) {
            this.model = model;
        }

        /**
         * Sets {@link #type} value
         *
         * @param type new value of {@link #type}
         */
        public void setType(String type) {
            this.type = type;
        }

        /**
         * Sets {@link #category} value
         *
         * @param category new value of {@link #category}
         */
        public void setCategory(String category) {
            this.category = category;
        }

        /**
         * Sets {@link #manufacturer} value
         *
         * @param manufacturer new value of {@link #manufacturer}
         */
        public void setManufacturer(String manufacturer) {
            this.manufacturer = manufacturer;
        }
    }

    /**
     * Manager provides catalog mapping to match Logi Sync Cloud API devices to Symphony devices
     *
     * @author Maksym.Rossiytsev
     * @since 1.0.0
     */
    class CatalogManager {
        private static Map<String, Map<String, CatalogEntry>> catalog = new HashMap<>();
        static {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                Map<String, Map<String, CatalogEntry>> data = objectMapper.readValue(CatalogEntry.class.getResourceAsStream("/catalog.json"),
                        new TypeReference<Map<String, Map<String, CatalogEntry>>>() {});
                catalog.putAll(data);
            } catch (IOException e) {
                logger.error("Unable to resolve catalog content. Skipping.");
            }
        }

        public static Map<String, Map<String, CatalogEntry>> getCatalog() {
            return catalog;
        }
    }
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

    interface Catalog {
        String COMPUTER_SECTION = "Computer";
        String GENERIC_SECTION = "Generic";
        String LOGI_SECTION = "Logitech";

        Map<String, Map<String, CatalogEntry>> CATALOG = CatalogManager.getCatalog();
    }
}
