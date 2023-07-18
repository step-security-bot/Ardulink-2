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

package org.ardulink.core.convenience;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.stream.IntStream.range;
import static org.ardulink.core.Pin.analogPin;
import static org.ardulink.core.Pin.digitalPin;
import static org.ardulink.core.linkmanager.LinkConfig.NO_ATTRIBUTES;
import static org.ardulink.core.linkmanager.providers.DynamicLinkFactoriesProvider.withRegistered;
import static org.ardulink.testsupport.mock.TestSupport.extractDelegated;
import static org.ardulink.util.Strings.swapUpperLower;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

import java.io.IOException;
import java.util.Arrays;

import org.ardulink.core.ConnectionBasedLink;
import org.ardulink.core.Link;
import org.ardulink.core.Pin;
import org.ardulink.core.linkmanager.AliasUsingLinkFactory;
import org.ardulink.core.linkmanager.DummyConnection;
import org.ardulink.core.linkmanager.DummyLinkConfig;
import org.ardulink.core.linkmanager.DummyLinkFactory;
import org.ardulink.core.linkmanager.LinkConfig;
import org.ardulink.core.linkmanager.LinkFactory;
import org.ardulink.core.proto.impl.ArdulinkProtocol2;
import org.ardulink.util.Throwables;
import org.junit.jupiter.api.Test;

/**
 * [ardulinktitle] [ardulinkversion]
 * 
 * project Ardulink http://www.ardulink.org/
 * 
 * [adsense]
 *
 */
class LinksTest {

	@Test
	void whenRequestingDefaultLinkReturnsFirstAvailableConnectionIfSerialNotAvailable() throws IOException {
		try (Link link = Links.getDefault()) {
			assertThat(getConnection(link)).isInstanceOf(DummyConnection.class);
		}
	}

	@Test
	void whenRequestingDefaultLinkSerialHasPriorityOverAllOthers() throws Throwable {
		LinkFactory<LinkConfig> factory = spy(factoryNamed(serial()));
		Link dummyLink = mock(Link.class, withSettings().stubOnly());
		doReturn(dummyLink).when(factory).newLink(any(LinkConfig.class));
		withRegistered(factoryNamed(serialDash("a")), factoryNamed("a"), factory, factoryNamed("z"),
				factoryNamed(serialDash("z"))).execute(() -> {
					try (Link link = Links.getDefault()) {
						assertLinkFactoryCreatedOneLink(factory);
					}
				});
	}

	@Test
	void whenRequestingDefaultLinkStartingWithSerialDashHasPriorityOverAllOthers() throws Throwable {
		LinkFactory<LinkConfig> factory = spy(factoryNamed(serialDash("appendix-does-not-matter")));
		withRegistered(factoryNamed("a"), factory, factoryNamed("z")).execute(() -> {
			try (Link link = Links.getDefault()) {
				assertLinkFactoryCreatedOneLink(factory);
			}
		});
	}

	@Test
	void serialDashDoesHandleSerial() throws Throwable {
		LinkFactory<LinkConfig> factory = spy(factoryNamed(serialDash("appendix-does-not-matter")));
		withRegistered(factory).execute(() -> {
			try (Link link = Links.getLink("ardulink://serial")) {
				assertLinkFactoryCreatedOneLink(factory);
			}
		});
	}

	private static String serialDash(String appendix) {
		return format("%s-%s", serial(), appendix);
	}

	private static String serial() {
		return "serial";
	}

	@Test
	void isConfiguredForAllChoiceValues() throws IOException {
		String dVal1 = "dVal1";
		DummyLinkConfig.choiceValuesOfD.set(new String[] { dVal1, "dVal2" });
		try (Link link = Links.getDefault()) {
			DummyLinkConfig config = getConnection(link).getConfig();
			assertThat(config.getProto()).isInstanceOf(ArdulinkProtocol2.class);
			assertThat(config.getA()).isEqualTo("aVal1");
			assertThat(config.getD()).isEqualTo(dVal1);
			assertThat(config.getF()).isEqualTo(NANOSECONDS);
			assertThat(config.getF2()).isEqualTo(NANOSECONDS);
		}
	}

	@Test
	void streamConsume() throws IOException {
		try (Link link = Links.getDefault()) {
			DummyLinkConfig config = getConnection(link).getConfig();
			range(0, 42).forEach(__ -> assertThat(config.getF2()).isEqualTo(NANOSECONDS));
		}
	}

	@Test
	void registeredSpecialNameDefault() throws Throwable {
		LinkFactory<LinkConfig> serial = spy(factoryNamed(serial()));
		assert serial.newLinkConfig().equals(NO_ATTRIBUTES)
				: "ardulink://default would differ if the config has attributes";
		withRegistered(serial).execute(() -> {
			try (Link link1 = Links.getLink("ardulink://default"); Link link2 = Links.getDefault()) {
				assertThat(link1).isSameAs(link2);
			}
		});
	}

	@Test
	void doesCacheLinks() throws IOException {
		String uri = "ardulink://dummyLink";
		try (Link link1 = Links.getLink(uri); Link link2 = Links.getLink(uri)) {
			assertThat(asList(link1, link2)).doesNotContainNull();
			assertThat(link1).isSameAs(link2);
		}
	}

	@Test
	void doesCacheLinksWhenUsingDefaultValues() throws IOException {
		try (Link link1 = Links.getLink("ardulink://dummyLink");
				Link link2 = Links.getLink("ardulink://dummyLink?a=&b=42&c=")) {
			assertThat(asList(link1, link2)).doesNotContainNull();
			assertThat(link1).isSameAs(link2);
		}
	}

	@Test
	void canCloseConnection() throws IOException {
		try (Link link = getRandomLink()) {
			DummyConnection connection = getConnection(link);
			verify(connection, never()).close();
			close(link);
			verify(connection, times(1)).close();
		}
	}

	@Test
	void doesNotCloseConnectionIfStillInUse() throws IOException {
		String randomURI = getRandomURI();
		Link[] links = { createConnectionBasedLink(randomURI), createConnectionBasedLink(randomURI),
				createConnectionBasedLink(randomURI) };
		assertThat(links).allSatisfy(l -> assertThat(l).isSameAs(links[0]));
		// all links point to the same instance, so choose one of them
		try (Link link = links[0]) {
			range(0, links.length - 1).forEach(__ -> closeQuietly(link));
			verify(getConnection(link), never()).close();
			link.close();
			verify(getConnection(link), times(1)).close();
		}
	}

	@Test
	void afterClosingWeGetAfreshLink() throws IOException {
		String randomURI = getRandomURI();
		try (Link link1 = createConnectionBasedLink(randomURI); Link link2 = createConnectionBasedLink(randomURI)) {
			assertThat(link1).isSameAs(link2);
			close(link1, link2);
			try (Link link3 = createConnectionBasedLink(randomURI)) {
				assertThat(link3).isNotSameAs(link1).isNotSameAs(link2);
			}
		}
	}

	@Test
	void stopsListenigAfterAllCallersLikeToStopListening() throws IOException {
		String randomURI = getRandomURI();
		try (Link link1 = createConnectionBasedLink(randomURI); Link link2 = createConnectionBasedLink(randomURI)) {
			ConnectionBasedLink delegate1 = getDelegate(link1);
			ConnectionBasedLink delegate2 = getDelegate(link2);
			assertThat(delegate1).isSameAs(delegate2);
			Pin anyDigitalPin = digitalPin(3);
			link1.startListening(anyDigitalPin);
			link2.startListening(anyDigitalPin);

			link1.stopListening(anyDigitalPin);
			// stop on others (not listening-started) pins
			link1.stopListening(analogPin(anyDigitalPin.pinNum()));
			link1.stopListening(analogPin(anyDigitalPin.pinNum() + 1));
			link1.stopListening(digitalPin(anyDigitalPin.pinNum() + 1));
			verify(delegate1, never()).stopListening(anyDigitalPin);

			link2.stopListening(anyDigitalPin);
			verify(delegate1, times(1)).stopListening(anyDigitalPin);
		}
	}

	@Test
	void twoDifferentURIsWithSameParamsMustNotBeenMixed() throws Throwable {
		String name1 = new DummyLinkFactory().getName();
		String name2 = swapUpperLower(name1);

		assert name1.equalsIgnoreCase(name2);
		assert !name1.equals(name2);

		class DummyLinkFactoryExtension extends DummyLinkFactory {
			@Override
			public String getName() {
				return name2;
			}
		}

		withRegistered(new DummyLinkFactoryExtension()).execute(() -> {
			try (Link link1 = Links.getLink(makeUri(name1)); Link link2 = Links.getLink(makeUri(name2))) {
				assertThat(link1).isNotSameAs(link2);
			}
		});
	}

	private static String makeUri(String name) {
		return format("ardulink://%s?a=aVal1&b=4", name);
	}

	@Test
	void aliasLinksAreSharedToo() throws Throwable {
		withRegistered(new AliasUsingLinkFactory()).execute(() -> {
			try (Link link1 = Links.getLink(format("ardulink://%s", AliasUsingLinkFactory.NAME));
					Link link2 = Links.getLink(format("ardulink://%s", AliasUsingLinkFactory.ALIAS_FACTORY_ALIAS))) {
				assertThat(link1).isSameAs(link2);
			}
		});
	}

	private void assertLinkFactoryCreatedOneLink(LinkFactory<LinkConfig> factory) throws Exception {
		verify(factory, times(1)).newLink(any(LinkConfig.class));
	}

	private LinkFactory<LinkConfig> factoryNamed(String name) {
		return new LinkFactory<LinkConfig>() {

			@Override
			public String getName() {
				return name;
			}

			@Override
			public Link newLink(LinkConfig config) {
				return mock(Link.class);
			}

			@Override
			public LinkConfig newLinkConfig() {
				return NO_ATTRIBUTES;
			}

		};
	}

	private void close(Link... links) {
		Arrays.stream(links).forEach(LinksTest::closeQuietly);
	}

	private static void closeQuietly(Link link) {
		try {
			link.close();
		} catch (IOException e) {
			Throwables.propagate(e);
		}
	}

	private Link getRandomLink() {
		return Links.getLink(getRandomURI());
	}

	private Link createConnectionBasedLink(String uri) {
		return Links.getLink(uri);
	}

	private DummyConnection getConnection(Link link) {
		return (DummyConnection) getDelegate(link).getConnection();
	}

	private ConnectionBasedLink getDelegate(Link link) {
		return (ConnectionBasedLink) extractDelegated(link);
	}

	private String getRandomURI() {
		return format("ardulink://dummyLink?a=&b=%d&c=%d", Thread.currentThread().getId(), System.currentTimeMillis());
	}

}
