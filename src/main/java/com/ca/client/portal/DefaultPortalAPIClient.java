package com.ca.client.portal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.ca.client.portal.ex.PortalAPIRuntimeException;
import com.ca.client.portal.util.PortalUtil;

@Component
public class DefaultPortalAPIClient {
	Logger log = LoggerFactory.getLogger(DefaultPortalAPIClient.class);

	@Autowired
	RestTemplate restTemplate;
	
	@Autowired
	PortalUtil portalUtil;

	/*	
	public String getAllAPIs(String url, String token) {
		String response = null;
		try {
			response = httpGet(url, token).getBody();
		} catch(HttpStatusCodeException e) {
			logAndThrowError(e);
		}
		return response;
	}
	
	public String getAllProxies(String url, String token) {
		String response = null;
		try {
			response = httpGet(url, token).getBody();
		} catch(HttpStatusCodeException e) {
			logAndThrowError(e);
		}
		return response;
	}
	*/
	
	public String getAPI(String url, String token) {
		String response = null;
		try {
			response = httpGet(url, token).getBody();
		} catch(HttpStatusCodeException e) {
			logAndThrowError(e);
		}
		return response;
	}
	
	public String getAPIMetaData(String url, String token) {
		String apiMetaData = null;
		try {
			String response = httpGet(url, token).getBody();
			apiMetaData = portalUtil.getAPIMetaData(response);
		} catch(HttpStatusCodeException e) {
			logAndThrowError(e);
		}
		return apiMetaData;
	}
	
	public String getAPIEulasMetaData(String url, String token) {
		String apiEulaMetaData = null;
		try {
			String response = httpGet(url, token).getBody();
			apiEulaMetaData = portalUtil.getAPIEulaMetaData(response);
		} catch(HttpStatusCodeException e) {
			logAndThrowError(e);
		}
		return apiEulaMetaData;
	}
	
	public String getProxyMetaData(String url, String token) {
		String proxyMetaData = null;
		try {
			String response = httpGet(url, token).getBody();
			proxyMetaData = portalUtil.getProxyMetaData(response);
		} catch(HttpStatusCodeException e) {
			logAndThrowError(e);
		}
		return proxyMetaData;
	}
		

	public String getAPISpec(String url, String token) {
		String response = null;
		try {
			response = httpGet(url, token).getBody();
		} catch(HttpStatusCodeException e) {
			logAndThrowError(e);
		}
		return response;
	}
	
	public String getOAuthAccessToken(String url) {
		String token = null;
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.set("Content-Type", "application/x-www-form-urlencoded");
			HttpEntity<String> entity = new HttpEntity<String>(headers);
			String response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class).getBody();
			token = portalUtil.getAccessToken(response);
		} catch(HttpStatusCodeException e) {
			logAndThrowError(e);
		}
		return token;
	}
	
	
	
	public boolean checkAPIExists(String url, String token) {
		boolean apiExists = true;
		try {
			ResponseEntity<String> response = httpGet(url, token);
		} catch(HttpStatusCodeException e) {
			if(e.getStatusCode() == HttpStatus.NOT_FOUND) {
				apiExists = false;
			} else {
				logAndThrowError(e);
			}
		}
		return apiExists;
	}

	public String postAPI(String url, String token, String requestPayload) {
		String response = null;
		try {
			response = httpCreateOrUpdate(url, token, requestPayload, HttpMethod.POST);
		} catch(HttpStatusCodeException e) {
			logAndThrowError(e);
		}
		return response;
	}
	
	public String updateAPI(String url, String token, String requestPayload) {
		String response = null;
		try {
			response = httpCreateOrUpdate(url, token, requestPayload, HttpMethod.PUT);
		} catch(HttpStatusCodeException e) {
			logAndThrowError(e);
		}
		return response;
	}

	public String createNewAPIDeployment(String url, String token, String requestPayload) {
		String response = null;
		try {
			response = httpCreateOrUpdate(url, token, requestPayload, HttpMethod.POST);
		} catch(HttpStatusCodeException e) {
			logAndThrowError(e);
		}
		return response;
	}

	public String updateAPIDeployment(String url, String token, String requestPayload) {
		String response = null;
		try {
			response = httpCreateOrUpdate(url, token, requestPayload, HttpMethod.PUT);
		} catch(HttpStatusCodeException e) {
			logAndThrowError(e);
		}
		return response;
	}

	private ResponseEntity<String> httpGet(String url, String token) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Bearer " + token);
		HttpEntity<String> entity = new HttpEntity<String>(headers);
		return restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
	}

	private String httpCreateOrUpdate(String url, String token, String requestPayload, HttpMethod httpMethod) {
		HttpHeaders headers = new HttpHeaders();
		headers.set("Authorization", "Bearer " + token);
		headers.set("Content-Type", "application/json");
		HttpEntity<String> entity = new HttpEntity<String>(requestPayload, headers);
		return restTemplate.exchange(url, httpMethod, entity, String.class).getBody();
	}
	
	private void logError(HttpStatusCodeException e) {
		log.error("Status: {} {}", e.getStatusCode(), e.getStatusText());
		log.error("Error: {}", e.getResponseBodyAsString());
	}
	
	private void logAndThrowError(HttpStatusCodeException e) {
		logError(e);
		throw new PortalAPIRuntimeException(e.getMessage());
	}

	/*
	private boolean checkError(ResponseEntity<String> response) {
		if (response.getStatusCode().series() == HttpStatus.Series.CLIENT_ERROR
				|| response.getStatusCode().series() == HttpStatus.Series.SERVER_ERROR) {
			return true;
		} else {
			return false;
		}
	}
	
	private boolean checkError(HttpStatus status) {
		if (status.series() == HttpStatus.Series.CLIENT_ERROR
				|| status.series() == HttpStatus.Series.SERVER_ERROR) {
			return true;
		} else {
			return false;
		}
	}*/
}
