/**
 * Copyright (2023, ) Institute of Software, Chinese Academy of Sciences
 */
package io.github.kubesys.client.writers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author wuheng@iscas.ac.cn
 * @since  2023/07/26
 * @version 1.0.2
 *
 */
public class SecretWriter extends KindWriter {

	static final String TEMPLATE = "apiVersion: v1\r\n"
			+ "kind: Secret\r\n"
			+ "metadata:\r\n"
			+ "  name: #NAME#\r\n"
			+ "  namespace: #NAMESPACE#\r\n"
			+ "type: Opaque";
	
	public SecretWriter(String name, String namespace) throws Exception {
		super(name, namespace);
	}

	public SecretWriter withData(String key, String value) {
		if (!json.has("data")) {
			json.set("data", new ObjectMapper().createObjectNode());
		}
		
		ObjectNode data = (ObjectNode) json.get("data");
		data.put(key, value);
		return this;
	}
	
	@Override
	public String getTemplate() {
		return TEMPLATE;
	}
	
	public static void main(String[] args) throws Exception {
		SecretWriter writer = new SecretWriter("kube-database", "kube-system");
		writer.withData("username", "onceas").withData("password", "onceas").stream(System.out);
	}
}