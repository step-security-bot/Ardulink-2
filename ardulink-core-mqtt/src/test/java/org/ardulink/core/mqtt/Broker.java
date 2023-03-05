/**
Copyright 2013 project Ardulink http://www.ardulink.org/
 
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
 
    http://www.apache.org/licenses/LICENSE-2.0
 
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package org.ardulink.core.mqtt;

import static io.moquette.BrokerConstants.ALLOW_ANONYMOUS_PROPERTY_NAME;
import static io.moquette.BrokerConstants.HOST_PROPERTY_NAME;
import static io.moquette.BrokerConstants.PERSISTENT_STORE_PROPERTY_NAME;
import static io.moquette.BrokerConstants.PORT_PROPERTY_NAME;
import static io.moquette.BrokerConstants.WEB_SOCKET_PORT_PROPERTY_NAME;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import org.ardulink.core.mqtt.duplicated.Message;
import org.ardulink.util.Lists;
import org.ardulink.util.Objects;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.moquette.broker.Server;
import io.moquette.broker.config.MemoryConfig;
import io.moquette.broker.security.IAuthenticator;
import io.moquette.interception.AbstractInterceptHandler;
import io.moquette.interception.InterceptHandler;
import io.moquette.interception.messages.InterceptPublishMessage;

/**
 * [ardulinktitle] [ardulinkversion]
 * 
 * project Ardulink http://www.ardulink.org/
 * 
 * [adsense]
 *
 */
public class Broker implements BeforeEachCallback, AfterEachCallback {

	private Server mqttServer;
	private IAuthenticator authenticator;
	private String host = "localhost";
	private int port = MqttLinkConfig.DEFAULT_PORT;
	private final List<InterceptHandler> listeners = Lists.newArrayList();
	private final List<Message> messages = new CopyOnWriteArrayList<>();

	private Broker() {
		super();
	}

	public static Broker newBroker() {
		return new Broker();
	}

	public static Broker newBroker(Collection<? extends InterceptHandler> listeners) {
		Broker newBroker = newBroker();
		newBroker.listeners.addAll(listeners);
		return newBroker;
	}

	@Override
	public void beforeEach(ExtensionContext context) throws IOException {
		this.mqttServer = new Server();
		start();
	}

	@Override
	public void afterEach(ExtensionContext context) {
		stop();
	}

	public void start() throws IOException {
		MemoryConfig memoryConfig = new MemoryConfig(properties());
		this.mqttServer.startServer(memoryConfig, listeners, null, authenticator, null);
	}

	private Properties properties() {
		Properties properties = new Properties();
		properties.put(HOST_PROPERTY_NAME, host);
		properties.put(PORT_PROPERTY_NAME, String.valueOf(port));
		if (this.authenticator != null) {
			properties.setProperty(ALLOW_ANONYMOUS_PROPERTY_NAME, Boolean.FALSE.toString());
		}
		properties.put(PERSISTENT_STORE_PROPERTY_NAME, "");
		properties.put(WEB_SOCKET_PORT_PROPERTY_NAME, "0");
		return properties;
	}

	public void stop() {
		this.mqttServer.stopServer();
	}

	public Broker recordMessages() {
		listeners.add(new AbstractInterceptHandler() {
			@Override
			public void onPublish(InterceptPublishMessage message) {
				messages.add(new Message(message.getTopicName(), new String(message.getPayload().array())));
			}

			@Override
			public String getID() {
				throw new IllegalStateException("not implemented");
			}
		});
		return this;
	}

	public Broker host(String host) {
		this.host = host;
		return this;
	}

	public Broker port(int port) {
		this.port = port;
		return this;
	}

	public int getPort() {
		return port;
	}

	public Broker authentication(String username, byte[] password) {
		this.authenticator = (c, u, p) -> Objects.equals(username, u) && Arrays.equals(password, p);
		return this;
	}

	public List<Message> getMessages() {
		return Lists.newArrayList(messages);
	}

}
