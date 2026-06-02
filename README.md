# Logitech Sync Cloud Integration - Capabilities & Configuration
This document covers Logitech Sync Cloud Aggregator Capabilities and Configuration.

Symphony integrates with Logitech Sync Cloud to provide comprehensive monitoring of Logitech devices across an organization.
Main features are: real-time device health monitoring, peripheral tracking, place/room information, network details, and environmental sensor data.

## Main use cases
- **Monitor** device health status, online/offline state, and firmware versions
- **Track** peripheral counts per device (Camera, MicPod, DisplayHub, Speaker, TableHub)
- **Inventory** place and room information including occupancy and seat count
- **Inspect** network details (IP, MAC, hostname, wired/wireless config) and environmental sensor readings

## Prerequisites and where to start
Logi Sync Cloud API uses **mTLS (mutual TLS)** for authorization - this is different from standard OAuth or basic authentication.

Before configuring the Symphony Adapter:
- Ensure you have an active and valid **Logitech Select license** (required for Logi Sync Cloud API access). More info: https://www.logitech.com/en-eu/products/videoconferencing/room-solutions/select-comprehensive-service-plan.html
- On the Logi Sync portal, go to the **System** page and generate **Sync API client certificates**. You will need the contents of:
  - `certificate.pem` -> used as `apiCertificate`
  - `privateKey.pem` -> used as `apiKey`
- Make sure Symphony Cloud Communicator can reach `api.sync.logitech.com` on port 443 over HTTPS.

## Logitech Sync Cloud Device Configuration and Provisioning
Once mTLS certificates are obtained from the Logi Sync portal, use them to configure the Symphony device.

Once the Logi Sync Cloud device is created with Monitoring Service -> Advanced Monitoring, HTTP Secure management protocol must be selected:
- Management Address: `api.sync.logitech.com`
- Protocol: HTTP | Secure
- Port: 443
- Username / Password: leave blank (auth is handled via mTLS)

Since mTLS is used for authorization, the following adapter properties must also be provided:

| Property | Description |
|---|---|
| apiCertificate | Contents of `certificate.pem`, provided by Logi Sync |
| apiKey | Contents of `privateKey.pem`, provided by Logi Sync |
| organizationIds | CSV String of organization IDs to monitor |

When the device is configured, saved, and set active, the Logi Sync Cloud Aggregator will start communicating with the Logi Sync Cloud API to retrieve data about registered devices.

By default, unprovisioned devices will appear on Aggregated Devices -> Unprovisioned Devices tab.

To import a Poly Lens aggregated device for monitoring:
1. Open Aggregated Devices
2. Select unprovisioned devices
3. Fill required provisioning fields
4. Import devices into Symphony

Devices and available data can be tuned by adapter configuration properties:

| Property | Description |
|---|---|
| apiCertificate | mTLS certificate value provided by Logi Sync Cloud |
| apiKey | mTLS key value provided by Logi Sync Cloud |
| organizationIds | Organizations to include into monitoring |
| placeRetrievalPageSize | Number of entries to pull per individual request to Logi Sync Cloud API. Smaller values may increase rate of API request limit exhaustion, 1000 by default |
| placeRetrievalTimeout | Time to wait between individual organization information retrievals. Relevant for multi-organization configurations, 30000 ms by default |

For detailed information on aggregator and its configuration, please refer to our knowledgebase -> https://symphony.knowledgeowl.com/help/logitech-sync-cloud-technical-breakdown

## Available Monitored Data
Logi Sync Cloud Aggregator monitored data consists of 2 parts: Aggregator extended properties and Aggregated Device extended properties.

Aggregator properties include service/adapter metadata: AdapterBuildDate, AdapterVersion, AdapterUptime, AdapterUptime(min), LastMonitoringCycleDuration(s), MonitoredDevicesTotal, MonitoringCycleInterval(mins).

Aggregated Devices provide the following monitoring capabilities:

| Property Type | Description |
| --- | --- |
| Device Metadata | Basic device identity, status, model, version, and lifecycle information. |
| Peripheral Counts | Counts and availability status of connected peripherals and accessories. |
| Place Information | Room details including name, group, capacity, occupancy, and seat count. |
| Network Information | Wired and wireless network configuration, addressing, and connectivity details. |
| Sensors | Environmental measurements and presence data from supported sensor-enabled devices. For example: CO2(ppm), Humidity(%), Pressure(Pa), Temperature(C) |

## Troubleshooting
**Login Error / mTLS Authentication Failure**
- Verify that the `apiCertificate` and `apiKey` adapter properties contain the correct and complete PEM file contents (including BEGIN/END headers)
- Make sure the certificate is active and not expired on the Logi Sync portal (System -> Sync API client certificates)
- Confirm that the Select license is active and valid

**API Error**
- Check the API error description in the aggregator extended properties
- Verify `organizationIds` are correct and formatted as a CSV string
- Make sure `placeRetrievalPageSize` is within the valid range (1-1000)

**Link Error / Ping Timeout**
- Make sure your Cloud Communicator can reach `api.sync.logitech.com` on port 443
- Check Ping Protocol in the Logi Sync Cloud Aggregator Device configuration
- Try switching between ICMP/TCP modes, as certain protocols may be blocked by proxy settings

If none of the recommended steps help, please enter an SOS ticket at {https://avi-spl.atlassian.net/servicedesk/customer/portals}

## What AI Assistant can do with it:
- Find Logitech Sync Cloud Aggregated Devices (Logi Sync Cloud Aggregator as Monitoring Proxy)
- Verify Logi Sync Cloud Aggregator configuration

## What AI Assistant cannot do with it:
- Provision the devices
