/**
 * Copyright (2020, ) Institute of Software, Chinese Academy of Sciences
 */
package io.github.kubesys.kubeclient;

import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;

import io.github.kubesys.kubeclient.core.KubernetesRuleBase;
import io.github.kubesys.kubeclient.utils.URLUtil;

/**
 * @author wuheng09@gmail.com
 * @since  2.0.3
 *
 */
public class KubernetesCRDWacther extends KubernetesWatcher {

	/**
	 * m_logger
	 */
	public static final Logger m_logger = Logger.getLogger(KubernetesCRDWacther.class.getName());
	
	
	public KubernetesCRDWacther(KubernetesClient client) {
		super(client);
	}

	@Override
	public void doAdded(JsonNode node) {
		
		JsonNode spec = node.get(KubernetesConstants.KUBE_SPEC);
		
		String apiGroup  = spec.get(KubernetesConstants.KUBE_SPEC_GROUP).asText();
		
		String version = spec.get(KubernetesConstants.KUBE_SPEC_VERSIONS)
							.iterator().next().get(KubernetesConstants
									.KUBE_SPEC_VERSIONS_NAME).asText();
		String url = URLUtil.join(KubernetesConstants
							.VALUE_APIS, apiGroup, version);
		
		try {
			client.getAnalyzer().registry.registerKinds(client.getHttpCaller(), url);
		} catch (Exception e) {
			m_logger.warning(e.getMessage());
		}
		
	}


	@Override
	public void doDeleted(JsonNode node) {
		
		JsonNode spec = node.get(KubernetesConstants.KUBE_SPEC);
		JsonNode names = spec.get(KubernetesConstants.KUBE_SPEC_NAMES);
		
		String shortKind = names.get(KubernetesConstants.KUBE_SPEC_NAMES_KIND).asText();
		String apiGroup  = spec.get(KubernetesConstants.KUBE_SPEC_GROUP).asText();
		String fullKind  = apiGroup + "." + shortKind;
		
		KubernetesRuleBase ruleBase = client.getAnalyzer().convertor.getRuleBase();
		ruleBase.removeFullKind(shortKind, fullKind);
		
		ruleBase.removeKindBy(fullKind);
		ruleBase.removeNameBy(fullKind);
		ruleBase.removeGroupBy(fullKind);
		ruleBase.removeVersionBy(fullKind);
		ruleBase.removeNamespacedBy(fullKind);
		ruleBase.removeApiPrefixBy(fullKind);
		ruleBase.removeVerbsBy(fullKind);
		
		m_logger.info("unregister " + shortKind);
	}

	@Override
	public void doModified(JsonNode node) {
		// ignore here
	}

	@Override
	public void doClose() {
		try {
			client.watchResources("apiextensions.k8s.io.CustomResourceDefinition", 
					KubernetesConstants.VALUE_ALL_NAMESPACES, 
					new KubernetesCRDWacther(client));
		} catch (Exception e) {
			try {
				Thread.sleep(5000);
			} catch (Exception e1) {
				doClose();
			}
		}
	}

}