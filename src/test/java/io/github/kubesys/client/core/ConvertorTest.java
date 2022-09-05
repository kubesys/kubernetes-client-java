/**
 * Copyright (2022, ) Institute of Software, Chinese Academy of Sciences
 */
package io.github.kubesys.client.core;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author wuheng@iscas.ac.cn
 * @since  2.0.0
 *
 * get real Url from <code>KubernetesRuleBase</code>
 * 
 */
public class ConvertorTest {
	
	protected final static KubernetesRuleBase ruleBase  = new KubernetesRuleBase();
	
	protected final static KubernetesConvertor convertor = new KubernetesConvertor(ruleBase);
	
	protected final static ObjectNode podJson    = new ObjectMapper().createObjectNode();
	
	protected final static ObjectNode podMeta    = new ObjectMapper().createObjectNode();
	
	protected final static ObjectNode deployJson = new ObjectMapper().createObjectNode();
	
	protected final static ObjectNode deployMeta = new ObjectMapper().createObjectNode();

	protected final static ObjectNode nodeJson   = new ObjectMapper().createObjectNode();
	
	protected final static ObjectNode nodeMeta   = new ObjectMapper().createObjectNode();
	
	protected final static ObjectNode igrsJson   = new ObjectMapper().createObjectNode();
	
	protected final static ObjectNode igrsMeta   = new ObjectMapper().createObjectNode();
	
	static {
		ruleBase.fullKindToApiPrefixMapper.put("Pod",                            "https://39.100.71.73:6443/api/v1");
		ruleBase.fullKindToApiPrefixMapper.put("apps.Deployment",                "https://39.100.71.73:6443/apis/apps/v1");
		ruleBase.fullKindToApiPrefixMapper.put("Node",                           "https://39.100.71.73:6443/api/v1");
		ruleBase.fullKindToApiPrefixMapper.put("networking.k8s.io.IngressClass", "https://39.100.71.73:6443/apis/networking.k8s.io/v1");
		ruleBase.fullKindToNamespacedMapper.put("Pod",                            true);
		ruleBase.fullKindToNamespacedMapper.put("apps.Deployment",                true);
		ruleBase.fullKindToNamespacedMapper.put("Node",                           false);
		ruleBase.fullKindToNamespacedMapper.put("networking.k8s.io.IngressClass", false);
		ruleBase.fullKindToNameMapper.put("Pod",                                  "pods");
		ruleBase.fullKindToNameMapper.put("apps.Deployment",                      "deployments");
		ruleBase.fullKindToNameMapper.put("Node",                                 "nodes");
		ruleBase.fullKindToNameMapper.put("networking.k8s.io.IngressClass",       "ingressclasses");
		ruleBase.kindToFullKindMapper.put("Pod", Arrays.asList(new String[] {"Pod"}));
		ruleBase.kindToFullKindMapper.put("Deployment", Arrays.asList(new String[] {"apps.Deployment"}));
		ruleBase.kindToFullKindMapper.put("Node", Arrays.asList(new String[] {"Node"}));
		ruleBase.kindToFullKindMapper.put("IngressClass", Arrays.asList(new String[] {"networking.k8s.io.IngressClass"}));
		
		podJson.put("apiVersion", "v1");
		podJson.put("kind", "Pod");
		podMeta.put("name", "testPod");
		podMeta.put("namespace", "kube-system");
		podJson.set("metadata", podMeta);
		
		deployJson.put("apiVersion", "apps/v1");
		deployJson.put("kind", "Deployment");
		deployMeta.put("name", "testDeploy");
		deployMeta.put("namespace", "kube-system");
		deployJson.set("metadata", deployMeta);
		
		nodeJson.put("apiVersion", "v1");
		nodeJson.put("kind", "Node");
		nodeMeta.put("name", "testNode");
		nodeJson.set("metadata", nodeMeta);
		
		igrsJson.put("apiVersion", "networking.k8s.io/v1");
		igrsJson.put("kind", "IngressClass");
		igrsMeta.put("name", "testIngress");
		igrsJson.set("metadata", igrsMeta);
	}
	
	public static void main(String[] args) {
		System.out.println(convertor.createUrl(podJson));
	}
	
	@Test(expected = NullPointerException.class)
	public void testNullCreateURL() {
		convertor.createUrl(null);
	}
	
	@Test(expected = NullPointerException.class)
	public void testInvalidJsonCreateURL() {
		convertor.createUrl(new ObjectMapper().createObjectNode());
	}
	
	@Test(expected = NullPointerException.class)
	public void testNullBindingURL() {
		convertor.bindingUrl(null);
	}
	
	@Test(expected = NullPointerException.class)
	public void testInvalidJsonBindingURL() {
		convertor.bindingUrl(new ObjectMapper().createObjectNode());
	}
	
	@Test(expected = NullPointerException.class)
	public void testNullDeleteKindURL() {
		convertor.deleteUrl(null, "abc", "abc");
	}
	
	@Test(expected = NullPointerException.class)
	public void testNullDeleteNameURL1() {
		convertor.deleteUrl("abc", "abc", null);
	}
	
	@Test(expected = NullPointerException.class)
	public void testNullDeleteNameURL2() {
		convertor.deleteUrl("abc", "abc", "");
	}
	
	@Test(expected = NullPointerException.class)
	public void testNullListKindURL() {
		convertor.listUrl(null, "abc");
	}
	
	@Test
	public void testValidCreateURL() {
		assertEquals("https://39.100.71.73:6443/api/v1/namespaces/kube-system/pods", convertor.createUrl(podJson));
		assertEquals("https://39.100.71.73:6443/apis/apps/v1/namespaces/kube-system/deployments", convertor.createUrl(deployJson));
		assertEquals("https://39.100.71.73:6443/api/v1/nodes", convertor.createUrl(nodeJson));
		assertEquals("https://39.100.71.73:6443/apis/networking.k8s.io/v1/ingressclasses", convertor.createUrl(igrsJson));
	}
	
	@Test
	public void testValidDeleteURL() {
		assertEquals("https://39.100.71.73:6443/api/v1/namespaces/kube-system/pods/testPod", convertor.deleteUrl("Pod", "kube-system", "testPod"));
		assertEquals("https://39.100.71.73:6443/apis/apps/v1/namespaces/kube-system/deployments/testDeploy", convertor.deleteUrl("apps.Deployment", "kube-system", "testDeploy"));
		assertEquals("https://39.100.71.73:6443/api/v1/nodes/testNode", convertor.deleteUrl("Node", "", "testNode"));
		assertEquals("https://39.100.71.73:6443/apis/networking.k8s.io/v1/ingressclasses/testIngress", convertor.deleteUrl("networking.k8s.io.IngressClass", "", "testIngress"));
	}
	
	@Test
	public void testValidUpdateURL() {
		assertEquals("https://39.100.71.73:6443/api/v1/namespaces/kube-system/pods/testPod", convertor.updateUrl("Pod", "kube-system", "testPod"));
		assertEquals("https://39.100.71.73:6443/apis/apps/v1/namespaces/kube-system/deployments/testDeploy", convertor.updateUrl("apps.Deployment", "kube-system", "testDeploy"));
		assertEquals("https://39.100.71.73:6443/api/v1/nodes/testNode", convertor.updateUrl("Node", "", "testNode"));
		assertEquals("https://39.100.71.73:6443/apis/networking.k8s.io/v1/ingressclasses/testIngress", convertor.updateUrl("networking.k8s.io.IngressClass", "", "testIngress"));
	}
	
	@Test
	public void testValidGetURL() {
		assertEquals("https://39.100.71.73:6443/api/v1/namespaces/kube-system/pods/testPod", convertor.getUrl("Pod", "kube-system", "testPod"));
		assertEquals("https://39.100.71.73:6443/apis/apps/v1/namespaces/kube-system/deployments/testDeploy", convertor.getUrl("apps.Deployment", "kube-system", "testDeploy"));
		assertEquals("https://39.100.71.73:6443/api/v1/nodes/testNode", convertor.getUrl("Node", "", "testNode"));
		assertEquals("https://39.100.71.73:6443/apis/networking.k8s.io/v1/ingressclasses/testIngress", convertor.getUrl("networking.k8s.io.IngressClass", "", "testIngress"));
	}
	
	@Test
	public void testValidListURL() {
		assertEquals("https://39.100.71.73:6443/api/v1/namespaces/kube-system/pods", convertor.listUrl("Pod", "kube-system"));
		assertEquals("https://39.100.71.73:6443/api/v1/pods", convertor.listUrl("Pod", ""));
		assertEquals("https://39.100.71.73:6443/apis/apps/v1/namespaces/kube-system/deployments", convertor.listUrl("apps.Deployment", "kube-system"));
		assertEquals("https://39.100.71.73:6443/apis/apps/v1/deployments", convertor.listUrl("apps.Deployment", ""));
		assertEquals("https://39.100.71.73:6443/api/v1/nodes", convertor.listUrl("Node", ""));
		assertEquals("https://39.100.71.73:6443/apis/networking.k8s.io/v1/ingressclasses", convertor.listUrl("networking.k8s.io.IngressClass", ""));
	}
	
	@Test
	public void testValidUpdateStatusUrlURL() {
		assertEquals("https://39.100.71.73:6443/api/v1/namespaces/kube-system/pods/testPod/status", convertor.updateStatusUrl("Pod", "kube-system", "testPod"));
		assertEquals("https://39.100.71.73:6443/apis/apps/v1/namespaces/kube-system/deployments/testDeploy/status", convertor.updateStatusUrl("apps.Deployment", "kube-system", "testDeploy"));
		assertEquals("https://39.100.71.73:6443/api/v1/nodes/testNode/status", convertor.updateStatusUrl("Node", "", "testNode"));
		assertEquals("https://39.100.71.73:6443/apis/networking.k8s.io/v1/ingressclasses/testIngress/status", convertor.updateStatusUrl("networking.k8s.io.IngressClass", "", "testIngress"));
	}
	
	@Test
	public void testValidWatchOneUrlURL() {
		assertEquals("https://39.100.71.73:6443/api/v1/watch/namespaces/kube-system/pods/testPod?watch=true&timeoutSeconds=315360000", convertor.watchOneUrl("Pod", "kube-system", "testPod"));
		assertEquals("https://39.100.71.73:6443/apis/apps/v1/watch/namespaces/kube-system/deployments/testDeploy?watch=true&timeoutSeconds=315360000", convertor.watchOneUrl("apps.Deployment", "kube-system", "testDeploy"));
		assertEquals("https://39.100.71.73:6443/api/v1/watch/nodes/testNode?watch=true&timeoutSeconds=315360000", convertor.watchOneUrl("Node", "", "testNode"));
		assertEquals("https://39.100.71.73:6443/apis/networking.k8s.io/v1/watch/ingressclasses/testIngress?watch=true&timeoutSeconds=315360000", convertor.watchOneUrl("networking.k8s.io.IngressClass", "", "testIngress"));
	}
	
	@Test
	public void testValidWatchAllUrlURL() {
		assertEquals("https://39.100.71.73:6443/api/v1/watch/namespaces/kube-system/pods?watch=true&timeoutSeconds=315360000", convertor.watchAllUrl("Pod", "kube-system"));
		assertEquals("https://39.100.71.73:6443/api/v1/watch/pods?watch=true&timeoutSeconds=315360000", convertor.watchAllUrl("Pod", ""));
		assertEquals("https://39.100.71.73:6443/apis/apps/v1/watch/namespaces/kube-system/deployments?watch=true&timeoutSeconds=315360000", convertor.watchAllUrl("apps.Deployment", "kube-system"));
		assertEquals("https://39.100.71.73:6443/apis/apps/v1/watch/deployments?watch=true&timeoutSeconds=315360000", convertor.watchAllUrl("apps.Deployment", ""));
		assertEquals("https://39.100.71.73:6443/api/v1/watch/nodes?watch=true&timeoutSeconds=315360000", convertor.watchAllUrl("Node", ""));
		assertEquals("https://39.100.71.73:6443/apis/networking.k8s.io/v1/watch/ingressclasses?watch=true&timeoutSeconds=315360000", convertor.watchAllUrl("networking.k8s.io.IngressClass", ""));
	}
	
}