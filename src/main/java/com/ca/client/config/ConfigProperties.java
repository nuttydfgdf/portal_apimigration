package com.ca.client.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

//@Validated
@ConfigurationProperties(prefix = "portal")
public class ConfigProperties {
	Logger log = LoggerFactory.getLogger(ConfigProperties.class);
	// @NotEmpty
	private String apiUuids;
	// @Valid
	private Src src = new Src();
	// @Valid
	private Dst dst = new Dst();

	public String getApiUuids() {
		return apiUuids;
	}

	public void setApiUuids(String apiUuids) {
		this.apiUuids = apiUuids;
	}

	public Src getSrc() {
		return src;
	}

	public void setSrc(Src src) {
		this.src = src;
	}

	public Dst getDst() {
		return dst;
	}

	public void setDst(Dst dst) {
		this.dst = dst;
	}

	public class Src {
		// @NotEmpty
		private String url;
		// @NotEmpty
		private String clientId;
		// @NotEmpty
		private String clientSecret;
		// @NotEmpty
		private String tokenUrl;

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public String getClientId() {
			return clientId;
		}

		public void setClientId(String clientId) {
			this.clientId = clientId;
		}

		public String getClientSecret() {
			return clientSecret;
		}

		public void setClientSecret(String clientSecret) {
			this.clientSecret = clientSecret;
		}

		public String getTokenUrl() {
			return tokenUrl;
		}

		public void setTokenUrl(String tokenUrl) {
			this.tokenUrl = tokenUrl;
		}

	}

	public class Dst {
		// @NotEmpty
		private String url;
		// @NotEmpty
		private String apiEulaUuid;
		// @NotEmpty
		private String proxyUuid;
		// @NotEmpty
		private String proxyUrl;
		// @NotEmpty
		private String backendAPIUrls;
		// @NotEmpty
		private String clientId;
		// @NotEmpty
		private String clientSecret;
		// @NotEmpty
		private String tokenUrl;

		private Map<String, String> backendAPIUrlMap = new HashMap<String, String>();

		public String getUrl() {
			return url;
		}

		public void setUrl(String url) {
			this.url = url;
		}

		public String getApiEulaUuid() {
			return apiEulaUuid;
		}

		public void setApiEulaUuid(String apiEulaUuid) {
			this.apiEulaUuid = apiEulaUuid;
		}

		public String getProxyUuid() {
			return proxyUuid;
		}

		public void setProxyUuid(String proxyUuid) {
			this.proxyUuid = proxyUuid;
		}

		public String getClientId() {
			return clientId;
		}

		public void setClientId(String clientId) {
			this.clientId = clientId;
		}

		public String getClientSecret() {
			return clientSecret;
		}

		public void setClientSecret(String clientSecret) {
			this.clientSecret = clientSecret;
		}

		public String getProxyUrl() {
			return proxyUrl;
		}

		public void setProxyUrl(String proxyUrl) {
			this.proxyUrl = proxyUrl;
		}

		public void setBackendAPIUrls(String backendAPIUrls) {
			backendAPIUrls = StringUtils.trimWhitespace(backendAPIUrls);
			if (StringUtils.hasLength(backendAPIUrls)) {
				if (backendAPIUrls.contains(",")) {
					Arrays.asList(backendAPIUrls.split(",")).forEach(splitBackendAPIUrl -> {
						splitBackendAPIUrlToMap(splitBackendAPIUrl);
					});
				} else {
					splitBackendAPIUrlToMap(backendAPIUrls);
				}
			}
			log.debug("BackendAPIUrlMap: {}", backendAPIUrlMap.values());
			this.backendAPIUrls = backendAPIUrls;
		}

		public Map<String, String> getBackendAPIUrlMap() {
			return backendAPIUrlMap;
		}

		public String getTokenUrl() {
			return tokenUrl;
		}

		public void setTokenUrl(String tokenUrl) {
			this.tokenUrl = tokenUrl;
		}

		private void splitBackendAPIUrlToMap(String splitBackendAPIUrl) {
			splitBackendAPIUrl = StringUtils.trimWhitespace(splitBackendAPIUrl);
			String[] splitBackendAPIUrlArray = splitBackendAPIUrl.split("\\|");
			if (splitBackendAPIUrlArray != null && splitBackendAPIUrlArray.length == 2) {
				backendAPIUrlMap.put(StringUtils.trimWhitespace(splitBackendAPIUrlArray[0]),
						StringUtils.trimWhitespace(splitBackendAPIUrlArray[1]));
			}
		}
	}

}
