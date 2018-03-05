package com.ca.client.portal.ex;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;

public class CustomResponseErrorHandler extends DefaultResponseErrorHandler {
	Logger log = LoggerFactory.getLogger(CustomResponseErrorHandler.class);
	
	@Override
	public void handleError(ClientHttpResponse response) throws IOException {
		log.error("Status: {} {}", response.getStatusCode(), response.getStatusText());
		log.error("Response: {}", response.getBody());
	}

}
