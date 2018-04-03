package com.ca.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

//@Validated
@ConfigurationProperties(prefix="portal")
public class ConfigProperties {
	//@NotEmpty
	private String apiUuids;
	//@Valid
	private Src src = new Src();
	//@Valid
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
		//@NotEmpty
		private String url;
		//@NotEmpty
		private String clientId;
		//@NotEmpty
		private String clientSecret;
		//@NotEmpty
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
		//@NotEmpty
		private String url;
		//@NotEmpty
		private String apiEulaUuid;
		//@NotEmpty
		private String proxyUuid;
		//@NotEmpty
		private String proxyUrl;
		//@NotEmpty
		private String clientId;
		//@NotEmpty
		private String clientSecret;
		//@NotEmpty
		private String tokenUrl;
		
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
		public String getTokenUrl() {
			return tokenUrl;
		}
		public void setTokenUrl(String tokenUrl) {
			this.tokenUrl = tokenUrl;
		}
		
	}
	
}
