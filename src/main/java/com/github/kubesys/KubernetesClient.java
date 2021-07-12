/**
 * Copyright (2020, ) Institute of Software, Chinese Academy of Sciences
 */
package com.github.kubesys;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultClientConnectionReuseStrategy;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.DefaultServiceUnavailableRetryStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.kubesys.utils.HttpUtil;
import com.github.kubesys.utils.SSLUtil;
import com.github.kubesys.utils.URLUtil;

/**
 * @author wuheng@iscas.ac.cn
 *
 * Support create, update, delete, get and list [Kubernetes resources]
 * (https://kubernetes.io/docs/concepts/cluster-administration/manage-deployment/)
 * using [Kubernetes native API](https://kubernetes.io/docs/reference/generated/kubernetes-api/v1.21/)
 * 
 */
public class KubernetesClient {

	/**
	 * m_logger
	 */
	public static final Logger m_logger = Logger.getLogger(KubernetesClient.class.getName());

	
	/**
	 * caller
	 */
	protected final HttpCaller httpCaller;
	
	/**
	 * analyzer
	 */
	protected final KubernetesAnalyzer kubeAnalyzer;


	/**
	 * @param masterUrl masterUrl
	 * @throws Exception 
	 */
	public KubernetesClient(String masterUrl) throws Exception {
		this(masterUrl, null);
	}

	/**
	 * @param masterUrl masterUrl
	 * @param tokenInfo token
	 * @throws Exception 
	 */
	public KubernetesClient(String masterUrl, String tokenInfo) throws Exception {
		this.httpCaller = new HttpCaller(masterUrl, tokenInfo);
		this.kubeAnalyzer = new KubernetesAnalyzer(httpCaller);
	}

	/**
	 * @param masterUrl    masterUrl
	 * @param tokenInfo    token
	 * @param kubeAnalyzer analyzer
	 */
	public KubernetesClient(String masterUrl, String tokenInfo, KubernetesAnalyzer kubeAnalyzer) {
		super();
		this.httpCaller = new HttpCaller(masterUrl, tokenInfo);
		this.kubeAnalyzer = kubeAnalyzer;
	}


	/**********************************************************
	 * 
	 * Core
	 * 
	 **********************************************************/

	/**
	 * create a Kubernetes resource using JSON
	 * 
	 * @param jsonStr json
	 * @return json
	 * @throws Exception exception
	 */
	public JsonNode createResource(String jsonStr) throws Exception {
		return createResource(new ObjectMapper().readTree(jsonStr));
	}

	/**
	 * create a Kubernetes resource using JSON
	 * 
	 * @param json json
	 * @return json
	 * @throws Exception exception
	 */
	public JsonNode createResource(JsonNode json) throws Exception {

		final String uri = createUrl(json);
		
		HttpPost request = HttpUtil.post(
				httpCaller.getTokenInfo(), 
				uri, json.toString());
		
		return httpCaller.getResponse(request);
	}


	/**
	 * delete a Kubernetes resource using JSON
	 * 
	 * @param kind kind
	 * @param name name
	 * @return json
	 * @throws Exception exception
	 */
	public JsonNode deleteResource(String kind, String name) throws Exception {
		return deleteResource(kind, KubernetesConstants.VALUE_ALL_NAMESPACES, name);
	}

	/**
	 * delete a Kubernetes resource using JSON
	 * 
	 * @param json json
	 * @return json
	 * @throws Exception exception
	 */
	public JsonNode deleteResource(JsonNode json) throws Exception {
		return deleteResource(getFullKind(json), getNamespace(json), getName(json));
	}

	/**
	 * delete a Kubernetes resource using JSON
	 * 
	 * @param kind      kind
	 * @param namespace namespace
	 * @param name      name
	 * @return json
	 * @throws Exception exception
	 */
	public JsonNode deleteResource(String kind, String namespace, String name) throws Exception {

		final String uri = deleteUrl(kind, namespace, name);

		HttpDelete request = HttpUtil.delete(httpCaller.tokenInfo, uri);
		
		return httpCaller.getResponse(request);
	}

	/**
	 * update a Kubernetes resource using JSON
	 * 
	 * @param json json
	 * @return json
	 * @throws Exception exception
	 */
	public JsonNode updateResource(JsonNode json) throws Exception {

		final String uri = updateUrl(getFullKind(json), getNamespace(json), getName(json));

		ObjectNode node = json.deepCopy();

		if (json.has(KubernetesConstants.KUBE_STATUS)) {
			node.remove(KubernetesConstants.KUBE_STATUS);
		}

		HttpPut request = HttpUtil.put(
				httpCaller.getTokenInfo(), 
				uri, json.toString());
		
		return httpCaller.getResponse(request);
	}

	/**
	 * binding a Kubernetes resource using JSON
	 * 
	 * { "apiVersion": "v1", "kind": "Binding", "metadata": { "name": "podName" },
	 * "target": { "apiVersion": "v1", "kind": "Node", "name": "hostName" } }
	 * 
	 * @param pod pod
	 * @param host host
	 * @return json
	 * @throws Exception exception
	 */
	public JsonNode bindingResource(JsonNode pod, String host) throws Exception {

		ObjectNode binding = new ObjectMapper().createObjectNode();
		binding.put("apiVersion", "v1");
		binding.put("kind", "Binding");
		
		ObjectNode metadata = new ObjectMapper().createObjectNode();
		metadata.put("name", pod.get("metadata").get("name").asText());
		metadata.put("namespace", pod.get("metadata").get("namespace").asText());
		binding.set("metadata", metadata);
		
		ObjectNode target = new ObjectMapper().createObjectNode();
		target.put("apiVersion", "v1");
		target.put("kind", "Node");
		target.put("name", host);
		binding.set("target", target);
			
		final String uri = bindingUrl(binding);
		
		HttpPost request = HttpUtil.post(
					httpCaller.tokenInfo, 
					uri, binding.toString());
		
		return httpCaller.getResponse(request);
	}

	/**
	 * get a Kubernetes resource using kind, namespace and name
	 * 
	 * @param kind kind
	 * @param name name
	 * @return json
	 * @throws Exception exception
	 */
	public JsonNode getResource(String kind, String name) throws Exception {

		return getResource(kind, KubernetesConstants.VALUE_ALL_NAMESPACES, name);
	}

	/**
	 * get a Kubernetes resource using kind, namespace and name
	 * 
	 * @param kind      kind
	 * @param namespace namespace, if this kind unsupports namespace, it is null
	 * @param name      name
	 * @return json
	 * @throws Exception exception
	 */
	public JsonNode getResource(String kind, String namespace, String name) throws Exception {

		final String uri = getUrl(kind, namespace, name);

		HttpGet request = HttpUtil.get(httpCaller.getTokenInfo(), uri);
		
		return httpCaller.getResponse(request);
	}

	/**
	 * get a Kubernetes resource using kind, namespace and name
	 * 
	 * @param kind      kind
	 * @param namespace namespace, if this kind unsupports namespace, it is null
	 * @param name      name
	 * @return json
	 * @throws Exception exception
	 */
	public boolean hasResource(String kind, String namespace, String name) throws Exception {

		final String uri = getUrl(kind, namespace, name);

		try {
			HttpGet request = HttpUtil.get(httpCaller.getTokenInfo(), uri);
			httpCaller.getResponse(request);
			return true;
		} catch (Exception ex) {
			return false;
		}
	}

	/**
	 * list all Kubernetes resources using kind
	 * 
	 * @param kind kind
	 * @return json
	 * @throws Exception exception
	 */
	public JsonNode listResources(String kind) throws Exception {
		return listResources(kind, KubernetesConstants.VALUE_ALL_NAMESPACES, null, null, 0, null);
	}

	/**
	 * list all Kubernetes resources using kind and namespace
	 * 
	 * @param kind      kind
	 * @param namespace namespace
	 * @return json
	 * @throws Exception exception
	 */
	public JsonNode listResources(String kind, String namespace) throws Exception {
		return listResources(kind, namespace, null, null, 0, null);
	}

	/**
	 * list all Kubernetes resources using kind, namespace, fieldSelector and
	 * labelSelector
	 * 
	 * @param kind          kind
	 * @param namespace     namespace
	 * @param fieldSelector fieldSelector
	 * @param labelSelector labelSelector
	 * @return json
	 * @throws Exception exception
	 */
	public JsonNode listResources(String kind, String namespace, String fieldSelector, String labelSelector)
			throws Exception {
		return listResources(kind, namespace, fieldSelector, labelSelector, 0, null);
	}

	/**
	 * list all Kubernetes resources using kind, namespace, fieldSelector,
	 * labelSelector, limit and nextId
	 * 
	 * @param kind          kind
	 * @param namespace     namespace
	 * @param fieldSelector fieldSelector
	 * @param labelSelector labelSelector
	 * @param limit         limit
	 * @param nextId        nextId
	 * @return json
	 * @throws Exception exception
	 */
	public JsonNode listResources(String kind, String namespace, String fieldSelector, String labelSelector, int limit,
			String nextId) throws Exception {
		StringBuilder uri = new StringBuilder();

		uri.append(listUrl(kind, namespace));

		uri.append(KubernetesConstants.HTTP_QUERY_KIND + kind);

		if (limit > 0) {
			uri.append(KubernetesConstants.HTTP_QUERY_PAGELIMIT).append(limit);
		}

		if (nextId != null) {
			uri.append(KubernetesConstants.HTTP_QUERY_NEXTID).append(nextId);
		}

		if (fieldSelector != null) {
			uri.append(KubernetesConstants.HTTP_QUERY_FIELDSELECTOR).append(fieldSelector);
		}

		if (labelSelector != null) {
			uri.append(KubernetesConstants.HTTP_QUERY_LABELSELECTOR).append(labelSelector);
		}

		HttpGet request = HttpUtil.get(httpCaller.getTokenInfo(), uri.toString());
		
		return httpCaller.getResponse(request);
	}

	/**
	 * update a Kubernetes resource status using JSON
	 * 
	 * @param json json
	 * @return json
	 * @throws Exception exception
	 */
	public JsonNode updateResourceStatus(JsonNode json) throws Exception {

		final String uri = updateStatusUrl(getKind(json), getNamespace(json), getName(json));

		HttpPut request = HttpUtil.put(
				httpCaller.getTokenInfo(), 
				uri, json.toString());
		
		return httpCaller.getResponse(request);
	}

	/**
	 * watch a Kubernetes resource using kind, namespace, name and WebSocketListener
	 * 
	 * @param kind    kind
	 * @param name    name
	 * @param watcher watcher
	 * @return thread
	 * @throws Exception exception
	 */
	public Thread watchResource(String kind, String name, KubernetesWatcher watcher) throws Exception {
		return watchResource(kind, KubernetesConstants.VALUE_ALL_NAMESPACES, name, watcher);
	}

	/**
	 * watch a Kubernetes resource using kind, namespace, name and WebSocketListener
	 * 
	 * @param kind      kind
	 * @param namespace namespace
	 * @param name      name
	 * @param watcher   watcher
	 * @return thread
	 * @throws Exception exception
	 */
	public Thread watchResource(String kind, String namespace, String name, KubernetesWatcher watcher)
			throws Exception {

		HttpCaller cloneHttpClient = new HttpCaller();
		watcher.setHttpClient(cloneHttpClient.getHttpClient());
		watcher.setRequest(HttpUtil.get(httpCaller.getTokenInfo(), watchOneUrl(kind, namespace, name)));
		Thread thread = new Thread(watcher, kind.toLowerCase() + "-" + namespace + "-" + name);
		thread.start();
		return thread;
	}

	/**
	 * watch a Kubernetes resources using kind, namespace, and WebSocketListener
	 * 
	 * @param kind    kind
	 * @param watcher watcher
	 * @return thread
	 * @throws Exception exception
	 */
	public Thread watchResources(String kind, KubernetesWatcher watcher) throws Exception {
		return watchResources(kind, KubernetesConstants.VALUE_ALL_NAMESPACES, watcher);
	}

	/**
	 * watch a Kubernetes resources using kind, namespace, and WebSocketListener
	 * 
	 * @param kind      kind
	 * @param namespace namespace
	 * @param watcher   watcher
	 * @return thread
	 * @throws Exception exception
	 */
	public Thread watchResources(String kind, String namespace, KubernetesWatcher watcher) throws Exception {
		HttpCaller cloneHttpClient = new HttpCaller();
		watcher.setHttpClient(cloneHttpClient.getHttpClient());
		watcher.setRequest(HttpUtil.get(httpCaller.getTokenInfo(), watchAllUrl(kind, namespace)));
		Thread thread = new Thread(watcher, kind.toLowerCase() + "-" + namespace);
		thread.start();
		return thread;
	}


	/**
	 * @return analyzer
	 */
	public KubernetesAnalyzer getAnalyzer() {
		return kubeAnalyzer;
	}

	/**********************************************************
	 * 
	 * Getter
	 * 
	 **********************************************************/

	/**
	 * @param json json
	 * @return kind
	 */
	public String getKind(JsonNode json) {
		return json.get(KubernetesConstants.KUBE_KIND).asText();
	}

	public String getFullKind(JsonNode json) {
		String apiVersion = json.get(KubernetesConstants.KUBE_APIVERSION).asText();
		String kind = json.get(KubernetesConstants.KUBE_KIND).asText();
		if(apiVersion.indexOf("/") > 0) {
			return apiVersion.substring(0, apiVersion.indexOf("/"))+ "." + kind;
		}
		return kind;

	}

	/**
	 * @param json json
	 * @return name
	 */
	public String getName(JsonNode json) {
		return json.get(KubernetesConstants.KUBE_METADATA).get(KubernetesConstants.KUBE_METADATA_NAME).asText();
	}

	/**
	 * @param json json
	 * @return full path
	 */
	public String getNamespace(JsonNode json) {
		JsonNode meta = json.get(KubernetesConstants.KUBE_METADATA);
		return meta.has(KubernetesConstants.KUBE_METADATA_NAMESPACE)
				? meta.get(KubernetesConstants.KUBE_METADATA_NAMESPACE).asText()
				: KubernetesConstants.VALUE_DEFAULT_NAMESPACE;

	}

	/**
	 * @param json json
	 * @return full path
	 */
	public String getApiVersion(JsonNode json) {
		return json.get("apiVersion").asText();

	}

	/**
	 * @return kinds           kinds
	 * @throws Exception       exception
	 */
	public JsonNode getKinds() throws Exception {
		return new ObjectMapper().readTree(
				new ObjectMapper().writeValueAsString(
						getAnalyzer().fullKindToKindMapper.values()));
	}
	
	/**
	 * @return fullkinds       fullkinds
	 * @throws Exception       execption
	 */
	public JsonNode getFullKinds() throws Exception {
		return new ObjectMapper().readTree(
				new ObjectMapper().writeValueAsString(
						getAnalyzer().fullKindToKindMapper.keySet()));
	}


	/*******************************************
	 * 
	 * knowledge-based Url
	 * 
	 ********************************************/
	/**
	 * @param json json
	 * @return Url
	 * @throws Exception exception
	 */
	protected String createUrl(JsonNode json) throws Exception {

		String version = getApiVersion(json);
		String uri = (version.indexOf("/") == -1) ? "api/" + version : "apis/" + version;

		String kind = getKind(json);
		String fullKind = version.indexOf("/") == -1 ? kind : version.substring(0, version.indexOf("/")) + "." + kind;
		return URLUtil.join(httpCaller.getMasterUrl(), uri, getNamespace(kubeAnalyzer.isNamespaced(fullKind), getNamespace(json)),
				kubeAnalyzer.getName(fullKind));
	}

	/**
	 * @param json json
	 * @return Url
	 * @throws Exception exception
	 */
	protected String bindingUrl(JsonNode json) throws Exception {

		String version = getApiVersion(json);
		String uri = (version.indexOf("/") == -1) ? "api/" + version : "apis/" + version;

		String kind = getKind(json);
		String fullKind = version.indexOf("/") == -1 ? kind : version.substring(0, version.indexOf("/")) + "." + kind;
		return URLUtil.join(httpCaller.getMasterUrl(), uri, getNamespace(kubeAnalyzer.isNamespaced(fullKind), getNamespace(json)),
				"pods", json.get("metadata").get("name").asText(), "binding");
	}

	/**
	 * @param kind kind
	 * @param ns   ns
	 * @param name name
	 * @return Url
	 * @throws Exception exception
	 */
	protected String deleteUrl(String kind, String ns, String name) throws Exception {
		String fullKind = kind.indexOf(".") == -1 ? kubeAnalyzer.getFullKind(kind) : kind;
		return URLUtil.join(kubeAnalyzer.getApiPrefix(fullKind), getNamespace(kubeAnalyzer.isNamespaced(fullKind), ns),
				kubeAnalyzer.getName(fullKind), name);
	}

	/**
	 * @param kind kind
	 * @param ns   ns
	 * @param name name
	 * @return Url
	 * @throws Exception exception
	 */
	protected String updateUrl(String kind, String ns, String name) throws Exception {
		String fullKind = kind.indexOf(".") == -1 ? kubeAnalyzer.getFullKind(kind) : kind;
		return URLUtil.join(kubeAnalyzer.getApiPrefix(fullKind), getNamespace(kubeAnalyzer.isNamespaced(fullKind), ns),
				kubeAnalyzer.getName(fullKind), name);
	}

	/**
	 * @param kind kind
	 * @param ns   ns
	 * @param name name
	 * @return Url
	 * @throws Exception exception
	 */
	protected String getUrl(String kind, String ns, String name) throws Exception {
		String fullKind = kind.indexOf(".") == -1 ? kubeAnalyzer.getFullKind(kind) : kind;
		return URLUtil.join(kubeAnalyzer.getApiPrefix(fullKind), getNamespace(kubeAnalyzer.isNamespaced(fullKind), ns),
				kubeAnalyzer.getName(fullKind), name);
	}

	/**
	 * @param kind kind
	 * @param ns   ns
	 * @return Url
	 * @throws Exception exception
	 */
	protected String listUrl(String kind, String ns) throws Exception {
		String fullKind = kind.indexOf(".") == -1 ? kubeAnalyzer.getFullKind(kind) : kind;
		return URLUtil.join(kubeAnalyzer.getApiPrefix(fullKind), getNamespace(kubeAnalyzer.isNamespaced(fullKind), ns),
				kubeAnalyzer.getName(fullKind));
	}

	/**
	 * @param kind kind
	 * @param ns   ns
	 * @param name name
	 * @return Url
	 * @throws Exception exception
	 */
	protected String updateStatusUrl(String kind, String ns, String name) throws Exception {
		String fullKind = kind.indexOf(".") == -1 ? kubeAnalyzer.getFullKind(kind) : kind;
		return URLUtil.join(kubeAnalyzer.getApiPrefix(fullKind), getNamespace(kubeAnalyzer.isNamespaced(fullKind), ns),
				kubeAnalyzer.getName(fullKind), name, KubernetesConstants.HTTP_RESPONSE_STATUS);
	}

	/**
	 * @param kind kind
	 * @param ns   ns
	 * @param name name
	 * @return Url
	 * @throws Exception exception
	 */
	protected String watchOneUrl(String kind, String ns, String name) throws Exception {
		String fullKind = kind.indexOf(".") == -1 ? kubeAnalyzer.getFullKind(kind) : kind;
		return URLUtil.join(kubeAnalyzer.getApiPrefix(fullKind), KubernetesConstants.KUBEAPI_WATCHER_PATTERN,
				getNamespace(kubeAnalyzer.isNamespaced(fullKind), ns), kubeAnalyzer.getName(fullKind), name,
				KubernetesConstants.HTTP_QUERY_WATCHER_ENABLE);
	}

	/**
	 * @param kind kind
	 * @param ns   ns
	 * @return Url
	 * @throws Exception exception
	 */
	protected String watchAllUrl(String kind, String ns) throws Exception {
		String fullKind = kind.indexOf(".") == -1 ? kubeAnalyzer.getFullKind(kind) : kind;
		return URLUtil.join(kubeAnalyzer.getApiPrefix(fullKind), KubernetesConstants.KUBEAPI_WATCHER_PATTERN,
				getNamespace(kubeAnalyzer.isNamespaced(fullKind), ns), kubeAnalyzer.getName(fullKind),
				KubernetesConstants.HTTP_QUERY_WATCHER_ENABLE);
	}

	/**
	 * @param namespaced bool
	 * @param namespace  ns
	 * @return full path
	 */
	protected String getNamespace(boolean namespaced, String namespace) {
		return (namespaced && namespace != null && namespace.length() != 0)
				? KubernetesConstants.KUBEAPI_NAMESPACES_PATTERN + namespace
				: KubernetesConstants.VALUE_ALL_NAMESPACES;
	}

	/**
	 * @return json
	 */
	public JsonNode getKindDesc() {

		ObjectNode map = new ObjectMapper().createObjectNode();


		for (String kind : kubeAnalyzer.getNamespacedMapping().keySet()) {
			ObjectNode node = new ObjectMapper().createObjectNode();
			node.put("apiVersion", kubeAnalyzer.fullKindToVersionMapper.get(kind));
			node.put("kind", kubeAnalyzer.fullKindToKindMapper.get(kind));
			node.put("plural", kubeAnalyzer.fullKindToNameMapper.get(kind));
			node.set("verbs", kubeAnalyzer.fullKindToVerbsMapper.get(kind));

			map.set(kind, node);
		}

		return map;
	}
	
	
	public HttpCaller getHttpCaller() {
		return httpCaller;
	}



	public static class HttpCaller {
		
		/**
		 * master IP
		 */
		protected String masterUrl;

		/**
		 * token
		 */
		protected String tokenInfo;

		/**
		 * client
		 */
		protected final CloseableHttpClient httpClient;

		/**
		 */
		public HttpCaller() {
			super();
			this.httpClient = createDefaultHttpClient();
		}
		
		/**
		 * @param masterUrl
		 * @param tokenInfo
		 */
		public HttpCaller(String masterUrl, String tokenInfo) {
			super();
			this.masterUrl = masterUrl;
			this.tokenInfo = tokenInfo;
			this.httpClient = createDefaultHttpClient();
		}
		
		/**
		 * @return httpClient
		 */
		protected CloseableHttpClient createDefaultHttpClient() {

			SocketConfig socketConfig = SocketConfig.custom().setSoKeepAlive(true).setSoTimeout(0).setSoReuseAddress(true)
					.build();

			RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(0).setConnectionRequestTimeout(0)
					.setSocketTimeout(0).build();

			return createDefaultHttpClientBuilder().setConnectionTimeToLive(0, TimeUnit.SECONDS)
					.setDefaultSocketConfig(socketConfig).setDefaultRequestConfig(requestConfig)
					.setKeepAliveStrategy(new DefaultConnectionKeepAliveStrategy())
					.setConnectionReuseStrategy(new DefaultClientConnectionReuseStrategy())
					.setServiceUnavailableRetryStrategy(new DefaultServiceUnavailableRetryStrategy()).build();
		}

		/**
		 * @return builder
		 */
		protected HttpClientBuilder createDefaultHttpClientBuilder() {
			HttpClientBuilder builder = HttpClients.custom();

			if (this.tokenInfo != null) {
				builder.setSSLHostnameVerifier(SSLUtil.createHostnameVerifier())
						.setSSLSocketFactory(SSLUtil.createSocketFactory());
			}

			return builder;
		}

		/**
		 * @param response response
		 * @return response
		 */
		public synchronized JsonNode getResponse(CloseableHttpResponse response) {

			try {
				JsonNode result = new ObjectMapper().readTree(response.getEntity().getContent());
				if (result.has("status") && result.get("status").asText().equals("Failure")) {
					throw new Exception(result.toPrettyString());
				}
				return result;
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			} finally {
				if (response != null) {
					try {
						response.close();
					} catch (IOException e) {
						m_logger.severe(e.toString());
					}
				}
			}
		}
		
		/**
		 * @param  caller caller
		 * @return response
		 * @throws Exception 
		 */
		public synchronized JsonNode getResponse(HttpRequestBase req) throws Exception {
			return getResponse(httpClient.execute(req)); 
		}

		/**
		 * 
		 */
		protected void close() {
			if (httpClient != null) {
				try {
					httpClient.close();
				} catch (IOException e) {
				}
			}
		}
		
		public String getMasterUrl() {
			return masterUrl;
		}

		public String getTokenInfo() {
			return tokenInfo;
		}

		public CloseableHttpClient getHttpClient() {
			return httpClient;
		}
		
	}
}
