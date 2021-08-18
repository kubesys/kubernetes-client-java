/**
 * Copyright (2020, ) Institute of Software, Chinese Academy of Sciences
 */
package io.github.kubesys.kubeclient.testcases;

import com.fasterxml.jackson.databind.JsonNode;

import io.github.kubesys.kubeclient.AbstractKubernetesClientTest;
import io.github.kubesys.kubeclient.KubernetesClient;
import io.github.kubesys.kubeclient.KubernetesConstants;
import io.github.kubesys.kubeclient.KubernetesWatcher;


/**
 * @author wuheng09@gmail.com
 *
 */
public class WatchKindTest extends AbstractKubernetesClientTest {

	
	public static void main(String[] args) throws Exception {
		KubernetesClient client = createClient2(null);
		
		KubernetesWatcher watcher = new KubernetesWatcher(client) {
			
			@Override
			public void doModified(JsonNode node) {
				System.out.println(node);
			}
			
			@Override
			public void doDeleted(JsonNode node) {
				System.out.println(node);
			}
			
			@Override
			public void doAdded(JsonNode node) {
				System.out.println(node);
			}

			@Override
			public void doClose() {
				System.out.println("close");
			}

		};
		client.watchResources("Pod", KubernetesConstants.VALUE_ALL_NAMESPACES, watcher);
		// or
//		client.watchResources("apps.Deployment", KubernetesConstants.VALUE_ALL_NAMESPACES, watcher);
		
		System.out.println("Start...");
		Thread.sleep(10000);
		System.out.println("End...");
	}
}
