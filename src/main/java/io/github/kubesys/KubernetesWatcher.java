/**
 * Copyright (2020, ) Institute of Software, Chinese Academy of Sciences
 */
package io.github.kubesys;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

/**
 * @author  wuheng09@gmail.com
 * 
 * 
 **/
public abstract class KubernetesWatcher extends WebSocketListener {


	@Override
	public void onMessage(WebSocket webSocket, String text) {
		super.onMessage(webSocket, text);
		try {
			JsonNode json = new ObjectMapper().readTree(text);
			String type = json.get(KubernetesConstants.KUBE_TYPE).asText();
			JsonNode obj = json.get(KubernetesConstants.KUBE_OBJECT);
			if (type.equals(KubernetesConstants.JSON_TYPE_ADDED)) {
				doAdded(obj);
			} else if (type.equals(KubernetesConstants.JSON_TYPE_MODIFIED)) {
				doModified(obj);
			} else if (type.equals(KubernetesConstants.JSON_TYPE_DELETED)) {
				doDeleted(obj);
			}
		} catch (Exception e) {
			throw new KubernetesException(e);
		}
		
	}
	
	@Override
	public void onClosed(WebSocket webSocket, int code, String reason) {
		doOnClose(new KubernetesException(reason));
	}

	/**
	 * @param node                  node
	 */
	public abstract void doAdded(JsonNode node);
	
	/**
	 * @param node                  node
	 */
	public abstract void doModified(JsonNode node);
	
	/**
	 * @param node                  node
	 */
	public abstract void doDeleted(JsonNode node);
	
	/**
	 * @param exception             exception
	 */
	public abstract void doOnClose(KubernetesException exception);
	
}
