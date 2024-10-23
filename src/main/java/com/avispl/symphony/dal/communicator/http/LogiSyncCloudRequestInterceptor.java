/*
 * Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.communicator.http;

import com.avispl.symphony.api.dal.error.ResourceNotReachableException;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import javax.security.auth.login.FailedLoginException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Interceptor for RestTemplate that checks for the response headers populated for certain endpoints
 * such as metrics, to control the amount of requests left per day.
 *
 * @author Maksym.Rossiytsev
 * @since 1.0.0
 */
public class LogiSyncCloudRequestInterceptor implements ClientHttpRequestInterceptor {
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        ClientHttpResponse response = execution.execute(request, body);
        if (response.getRawStatusCode() == 429) {
            try {
                // If it's 429 - just retry in 1 second.
                TimeUnit.MILLISECONDS.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            response = execution.execute(request, body);
        }
        if (response.getRawStatusCode() == 401) {
            // Throw an error here, pick it up later in {@link LogiSyncCloudCommunicator} and propagate to UI.
            String errorMessage = "Unable to authorize, please check certificate and privateKey provided.";
            throw new ResourceNotReachableException(errorMessage, new FailedLoginException(errorMessage));
        } else if (response.getRawStatusCode() == 403) {
            // Throw an error here, pick it up later in {@link LogiSyncCloudCommunicator} and propagate to UI.
            String errorMessage = "Unable to authorize, your organization is not Logitech Select enabled or your client certificate is either expired or deactivated.";
            throw new ResourceNotReachableException(errorMessage, new FailedLoginException(errorMessage));
        }
        return response;
    }
}