package org.ardulink.camel.test;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.ardulink.core.Pin.analogPin;
import static org.ardulink.core.Pin.digitalPin;
import static org.ardulink.testsupport.mock.TestSupport.getMock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.ardulink.core.Link;
import org.ardulink.core.convenience.Links;
import org.ardulink.testsupport.mock.junit5.MockUri;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(value = 5, unit = SECONDS)
class ArdulinkComponentListenerTest {

	@Test
	void startListeningOnPassedPins(@MockUri String mockUri) throws Exception {
		try (Link link = Links.getLink(mockUri)) {
			haltCamel(startCamel(mockUri + "&listenTo=d1,d2,a1"));
			Link mock = getMock(link);
			verify(mock).startListening(digitalPin(1));
			verify(mock).startListening(digitalPin(2));
			verify(mock).startListening(analogPin(1));
			verify(mock).close();
			verifyNoMoreInteractions(mock);
		}
	}

	@Test
	void listeningIsCaseInsensitive(@MockUri String mockUri) throws Exception {
		try (Link link = Links.getLink(mockUri)) {
			haltCamel(startCamel(mockUri + "&listenTo=d1,D2,a3,A4"));
			Link mock = getMock(link);
			verify(mock).startListening(digitalPin(1));
			verify(mock).startListening(digitalPin(2));
			verify(mock).startListening(analogPin(3));
			verify(mock).startListening(analogPin(4));
			verify(mock).close();
			verifyNoMoreInteractions(mock);
		}
	}

	@Test
	void ignoresMultipleOccurencesOfSamePin(@MockUri String mockUri) throws Exception {
		try (Link link = Links.getLink(mockUri)) {
			haltCamel(startCamel(mockUri + "&listenTo=d1,D1,a2,A2"));
			Link mock = getMock(link);
			verify(mock).startListening(digitalPin(1));
			verify(mock).startListening(analogPin(2));
			verify(mock).close();
			verifyNoMoreInteractions(mock);
		}
	}

	private CamelContext haltCamel(CamelContext context) throws Exception {
		context.stop();
		return context;
	}

	private CamelContext startCamel(String uri) throws Exception {
		CamelContext context = new DefaultCamelContext();
		context.addRoutes(new RouteBuilder() {
			@Override
			public void configure() {
				from(noMatterWhat()).to(uri);
			}
		});
		context.start();
		return context;
	}

	private String noMatterWhat() {
		return "direct:bean";
	}

}
