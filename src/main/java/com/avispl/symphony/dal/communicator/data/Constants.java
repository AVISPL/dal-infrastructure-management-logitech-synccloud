/*
 * Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Adapter constants storage, grouped by type
 *
 * @author Maksym.Rossiytsev
 * @since 1.0.0
 */
public interface Constants {
    class CatalogEntry {
        private String model;
        private String type;
        private String category;
        private String manufacturer;

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
    }

    /**
     * Manager provides catalog mapping to match Logi Sync Cloud API devices to Symphony devices
     *
     * @author Maksym.Rossiytsev
     * @since 1.0.0
     */
    class CatalogManager {
        private static Map<String, CatalogEntry> genericCatalogSection = new HashMap<>();
        private static Map<String, CatalogEntry> computerCatalogSection = new HashMap<>();
        private static Map<String, CatalogEntry> logiCatalogSection = new HashMap<>();

        private static Map<String, Map<String, CatalogEntry>> catalog = new HashMap<>();
        static {
            catalog.put(Catalog.LOGI_SECTION, logiCatalogSection);
            catalog.put(Catalog.COMPUTER_SECTION, computerCatalogSection);
            catalog.put(Catalog.GENERIC_SECTION, genericCatalogSection);

            computerCatalogSection.put("", new CatalogEntry("Computer", "Desktop", "Logitech", "Unknown"));
            genericCatalogSection.put("", new CatalogEntry("AV Devices", "Generic", "Other", "Generic AV Device"));

            logiCatalogSection.put("MeetUp", new CatalogEntry("Computer", "MTR", "Logitech", "Logitech MeetUp MTR"));
            logiCatalogSection.put("Rally", new CatalogEntry("Codecs", "Single Codecs", "Logitech", "Rally"));
            logiCatalogSection.put("Rally Bar", new CatalogEntry("Codecs", "Single Codecs", "Logitech", "Rally Bar"));
            logiCatalogSection.put("Rally Bar Huddle", new CatalogEntry("Codecs", "Single Codecs", "Logitech", "Rally Bar Huddle"));
            logiCatalogSection.put("Rally Bar Mini", new CatalogEntry("Codecs", "Single Codecs", "Logitech", "Rally Bar Mini"));
            logiCatalogSection.put("Rally Camera", new CatalogEntry("Codecs", "Single Codecs", "Logitech", "Rally Camera"));
            logiCatalogSection.put("Rally Plus", new CatalogEntry("Codecs", "Single Codecs", "Logitech", "Rally Plus"));
            logiCatalogSection.put("RoomMate", new CatalogEntry("Computer", "Desktop", "Logitech", "RoomMate"));
            logiCatalogSection.put("Sight", new CatalogEntry("AV Devices", "Camera", "Logitech", "Sight"));
            logiCatalogSection.put("Scribe", new CatalogEntry("AV Devices", "Camera", "Logitech", "Scribe"));
            logiCatalogSection.put("Swytch", new CatalogEntry("AV Devices", "Switchers", "Logitech", "Swytch"));
            logiCatalogSection.put("Tap", new CatalogEntry("AV Devices", "Touch Screens", "Logitech", "Tap"));
            logiCatalogSection.put("Tap IP", new CatalogEntry("AV Devices", "Touch Screens", "Logitech", "Tap IP"));
            logiCatalogSection.put("Tap Scheduler", new CatalogEntry("AV Devices", "Touch Screens", "Logitech", "Tap Scheduler"));
            logiCatalogSection.put("BRIO", new CatalogEntry("AV Devices", "Camera", "Logitech", "BRIO"));
            logiCatalogSection.put("C920c", new CatalogEntry("AV Devices", "Camera", "Logitech", "C920c"));
            logiCatalogSection.put("C920e", new CatalogEntry("AV Devices", "Camera", "Logitech", "C920e"));
            logiCatalogSection.put("C925e", new CatalogEntry("AV Devices", "Camera", "Logitech", "C925e"));
            logiCatalogSection.put("C930c", new CatalogEntry("AV Devices", "Camera", "Logitech", "C930c"));
            logiCatalogSection.put("C930e", new CatalogEntry("AV Devices", "Camera", "Logitech", "C930e"));
            logiCatalogSection.put("MX Brio 705 for Business", new CatalogEntry("AV Devices", "Camera", "Logitech", "MX Brio 705 for Business"));
            logiCatalogSection.put("BRIO 505", new CatalogEntry("AV Devices", "Camera", "Logitech", "BRIO 505"));
            logiCatalogSection.put("BRIO 305", new CatalogEntry("AV Devices", "Camera", "Logitech", "BRIO 305"));
            logiCatalogSection.put("Brio 105", new CatalogEntry("AV Devices", "Camera", "Logitech", "Brio 105"));
            logiCatalogSection.put("Zone Wireless", new CatalogEntry("AV Devices", "Headset", "Logitech", "Zone Wireless"));
            logiCatalogSection.put("Zone Wireless Plus", new CatalogEntry("AV Devices", "Headset", "Logitech", "Zone Wireless Plus"));
            logiCatalogSection.put("Zone Wireless 2", new CatalogEntry("AV Devices", "Headset", "Logitech", "Zone Wireless 2"));
            logiCatalogSection.put("Zone Wired", new CatalogEntry("AV Devices", "Headset", "Logitech", "Zone Wired"));
            logiCatalogSection.put("Zone Wired Earbuds", new CatalogEntry("AV Devices", "Headset", "Logitech", "Zone Wired Earbuds"));
            logiCatalogSection.put("Zone True Wireless", new CatalogEntry("AV Devices", "Headset", "Logitech", "Zone True Wireless"));
            logiCatalogSection.put("Zone Vibe Wireless", new CatalogEntry("AV Devices", "Headset", "Logitech", "Zone Vibe Wireless"));
            logiCatalogSection.put("Logi Dock", new CatalogEntry("AV Devices", "Dock", "Logitech", "Logi Dock"));
            logiCatalogSection.put("BCC950e", new CatalogEntry("AV Devices", "Camera", "Logitech", "BCC950e"));
            logiCatalogSection.put("PTZ Pro 2", new CatalogEntry("AV Devices", "Camera", "Logitech", "PTZ Pro 2"));
            logiCatalogSection.put("ConferenceCam Connect", new CatalogEntry("AV Devices", "Camera", "Logitech", "ConferenceCam Connect"));
            logiCatalogSection.put("GROUP", new CatalogEntry("AV Devices", "Camera", "Logitech", "GROUP"));
            logiCatalogSection.put("CC3000e", new CatalogEntry("AV Devices", "Camera", "Logitech", "CC3000e"));
            logiCatalogSection.put("Smartdock", new CatalogEntry("AV Devices", "Power", "Logitech", "Logi Dock"));

            logiCatalogSection.put("MX Keys for Business", new CatalogEntry("AV Devices", "Generic", "Logitech", "MX Keys for Business"));
            logiCatalogSection.put("MX Keys Mini for Business", new CatalogEntry("AV Devices", "Generic", "Logitech", "MX Keys Mini for Business"));
            logiCatalogSection.put("ERGO K860 for Business", new CatalogEntry("AV Devices", "Generic", "Logitech", "ERGO K860 for Business"));
            logiCatalogSection.put("Signature K650 for Business", new CatalogEntry("AV Devices", "Generic", "Logitech", "Signature K650 for Business"));
            logiCatalogSection.put("MX Master 3S for Business", new CatalogEntry("AV Devices", "Generic", "Logitech", "MX Master 3S for Business"));
            logiCatalogSection.put("MX Master 3 for Business", new CatalogEntry("AV Devices", "Generic", "Logitech", "MX Master 3 for Business"));
            logiCatalogSection.put("MX Anywhere 3 for Business", new CatalogEntry("AV Devices", "Generic", "Logitech", "MX Anywhere 3 for Business"));
            logiCatalogSection.put("Lift Vertical Mouse for Business", new CatalogEntry("AV Devices", "Generic", "Logitech", "Lift Vertical Mouse for Business"));
            logiCatalogSection.put("Signature M650 for Business", new CatalogEntry("AV Devices", "Generic", "Logitech", "Signature M650 for Business"));
            logiCatalogSection.put("ERGO M575 for Business", new CatalogEntry("AV Devices", "Generic", "Logitech", "ERGO M575 for Business"));
            logiCatalogSection.put("MX ERGO", new CatalogEntry("AV Devices", "Generic", "Logitech", "MX ERGO"));
            logiCatalogSection.put("MX Vertical", new CatalogEntry("AV Devices", "Generic", "Logitech", "MX Vertical"));

            logiCatalogSection.put("AVer CAM540", new CatalogEntry("AV Devices", "Camera", "AVer", "CAM540"));
            logiCatalogSection.put("AVer CAM520 Pro", new CatalogEntry("AV Devices", "Camera", "AVer", "CAM520 Pro"));
            logiCatalogSection.put("AVer VC520+", new CatalogEntry("AV Devices", "Camera", "AVer", "VC520+"));
            logiCatalogSection.put("AVer VB342", new CatalogEntry("AV Devices", "Camera", "AVer", "VB342"));

            logiCatalogSection.put("Crestron-UC-Soundbar", new CatalogEntry("AV Devices", "Speaker", "Crestron", "UC Soundbar"));
            logiCatalogSection.put("Huddly IQ", new CatalogEntry("AV Devices", "Camera", "Huddly", "IQ"));
            logiCatalogSection.put("Jabra Panacast 50", new CatalogEntry("AV Devices", "Camera", "Jabra", "Panacast 50"));
            logiCatalogSection.put("Poly Studio", new CatalogEntry("Codecs", "Single Codecs", "Poly", "Poly Studio"));
            logiCatalogSection.put("Polycom EagleEye Director II", new CatalogEntry("AV Devices", "Camera", "Poly", "EagleEye Director II"));
            logiCatalogSection.put("Polycom EagleEye IV USB", new CatalogEntry("AV Devices", "Camera", "Poly", "EagleEye Director IV USB"));
            logiCatalogSection.put("Polycom MSR Dock", new CatalogEntry("AV Devices", "Power", "Poly", "MSR Dock"));
            logiCatalogSection.put("Shure P300", new CatalogEntry("AV Devices", "Audio DSP", "Shure", "P300"));
            logiCatalogSection.put("Yamaha CS-700", new CatalogEntry("AV Devices", "Speaker", "Yamaha", "CS-700"));
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
