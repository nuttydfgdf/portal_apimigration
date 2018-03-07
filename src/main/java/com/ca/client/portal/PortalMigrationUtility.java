package com.ca.client.portal;

import java.io.Console;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.ca.client.config.ConfigProperties;
import com.ca.client.portal.util.CryptoUtil;
import com.ca.client.portal.util.PortalUtil;

@Component
public class PortalMigrationUtility {
	Logger log = LoggerFactory.getLogger(PortalMigrationUtility.class);

	@Autowired
	private DefaultPortalAPIClient portalClient;
	
	@Autowired
	private ConfigProperties props;
	
	@Autowired
	private PortalUtil portalUtil;
	
	private String srcToken;
	private String dstToken;
	private boolean isInitialized = false;
	
	private void initializeTokens() {
		String baseTokenUrl = props.getTokenUrl();
		String srcTokenUrl = baseTokenUrl + "&client_id=" + props.getSrc().getClientId() + "&client_secret=" + CryptoUtil.decrypt(props.getSrc().getClientSecret());
		String dstTokenUrl = baseTokenUrl + "&client_id=" + props.getDst().getClientId() + "&client_secret=" + CryptoUtil.decrypt(props.getDst().getClientSecret());

		log.debug("Started retreiving OAuth token for Source Portal");
		this.srcToken = portalClient.getOAuthAccessToken(srcTokenUrl);
		log.debug("Finished retrieving OAuth {} token for Source Portal", srcToken);

		log.debug("Started retreiving OAuth token for Destination Portal");
		this.dstToken = portalClient.getOAuthAccessToken(dstTokenUrl);
		log.debug("Finished retrieving OAuth {} token for Destination Portal", dstToken);
		this.isInitialized = true;
	}
	
	public void encodePassword() {
		Console console = System.console();
		System.out.println("Enter the password to encode:");
		char[] password = console.readPassword();
		while(password.length == 0) {
			System.out.println("Invalid password, enter the password to encode:");
			password = console.readPassword();
		}
		String encryptedPassword = CryptoUtil.encrypt(String.valueOf(password));
		System.out.println(encryptedPassword);
		//System.out.println("Decrypted: " + CryptoUtil.decrypt(encryptedPassword, key));
	}

	public void listAPIMetaData() {
		String srcApisUrl = props.getSrc().getUrl() + "/2.0/Apis";
		if(!isInitialized) {
			initializeTokens();
		}
		String apiMetaData = portalClient.getAPIMetaData(srcApisUrl, this.srcToken);
		System.out.println(apiMetaData);
	}
	
	public void runMigration() {
		try {
			if(!isInitialized) {
				initializeTokens();
			}
			Arrays.asList(props.getApiUuids().split(",")).forEach(apiUuid -> {
				log.info("==================Starting Migration of API Uuid: {} ====================", apiUuid);
				migrateApi(StringUtils.trimWhitespace(apiUuid));
				log.info("==================Finished Migration of API Uuid: {} ====================", apiUuid);
			});
		} catch(Exception e) {
			log.error("Process terminated, Exception: {}", e.getMessage());
		}
	}
	
	private void migrateApi(String apiUuid) {
		String srcBaseUrl = props.getSrc().getUrl();
		String destBaseUrl = props.getDst().getUrl();
		String proxyUuid = props.getDst().getProxyUuid();
		String apiEulaUuid = props.getDst().getApiEulaUuid();
		
		String srcApiUrl = srcBaseUrl + "/2.0/Apis('"+ apiUuid + "')";
		String srcApiSpecUrl = srcBaseUrl + "/2.0/Apis('" + apiUuid + "')/SpecContent";
		String dstApiUrl = destBaseUrl + "/2.0/Apis('"+ apiUuid + "')";
		String dstPostApiUrl = destBaseUrl + "/2.0/Apis";
		String dstPostApiNewDeploymentUrl = destBaseUrl + "/deployments/1.0/apis/" + apiUuid + "/proxies";
		String dstPostApiUpdateDeploymentStatusUrl = destBaseUrl + "/deployments/1.0/apis/" + apiUuid + "/proxies/" + proxyUuid;
		String dstProxyUrl = destBaseUrl + "/deployments/1.0/proxies/" + props.getDst().getProxyUuid();
		
		String postAPIResponse = "";
		log.info("Started retreiving API data for uuid: {}", apiUuid);
		String api = portalClient.getAPI(srcApiUrl, this.srcToken);
		log.info("Finished retrieving API data for uuid: {}", apiUuid);
		
		log.info("Started retrieving API Swagger Specs for uuid: {}", apiUuid);
		String apiSpec = portalClient.getAPISpec(srcApiSpecUrl, this.srcToken);
		log.info("Finished retrieving API Swagger Specs for uuid: {}", apiUuid);

		
		log.info("Started creating/updating API for uuid: {}", apiUuid);
		// Check if API exists on destination portal
		boolean destAPIExists = portalClient.checkAPIExists(dstApiUrl, this.dstToken);
		String postAPIPayload = portalUtil.prepareCreateOrUpdateAPIPayLoad(api, apiSpec, apiEulaUuid, destAPIExists);
		log.info("API with uuid {} exists on destination portal: {}", apiUuid, destAPIExists);
		
		if(destAPIExists) {
			postAPIResponse = portalClient.updateAPI(dstApiUrl, this.dstToken, postAPIPayload);
		} else {
			postAPIResponse = portalClient.postAPI(dstPostApiUrl, this.dstToken, postAPIPayload);
		}
		log.info("Finished creating/updating API for uuid: {}", apiUuid);
		
		/*
		if(!destAPIExists) {
			log.info("Started creating new deployment for uuid: {}", apiUuid);
			String postNewAPIDeploymentPayload = portalUtil.preparePOSTForNewAPIDeployment(proxyUuid);
			String postNewAPIDeploymentResponse = portalClient.createNewAPIDeployment(dstPostApiNewDeploymentUrl, dstToken, postNewAPIDeploymentPayload);
			log.info("Finished creating new deployment for uuid: {}", apiUuid);
		}
		
		log.info("Started updating deployment status to DEPLOYED for uuid: {}", apiUuid);
		String postApiUpdateDeploymentPayload = portalUtil.preparePOSTForUpdateDeploymentStatus("DEPLOYED");
		String postApiUpdateDeploymentResponse = portalClient.updateAPIDeployment(dstPostApiUpdateDeploymentStatusUrl, dstToken, postApiUpdateDeploymentPayload);
		log.info("Finished updating deployment status to DEPLOYED for uuid: {}", apiUuid);
		*/
	}
}
