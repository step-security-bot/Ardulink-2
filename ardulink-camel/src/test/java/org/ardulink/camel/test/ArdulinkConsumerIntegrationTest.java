package org.ardulink.camel.test;

import static org.ardulink.core.Pin.analogPin;
import static org.ardulink.core.Pin.digitalPin;
import static org.ardulink.core.events.DefaultAnalogPinValueChangedEvent.analogPinValueChanged;
import static org.ardulink.core.events.DefaultDigitalPinValueChangedEvent.digitalPinValueChanged;
import static org.ardulink.core.proto.impl.ALProtoBuilder.alpProtocolMessage;
import static org.ardulink.core.proto.impl.ALProtoBuilder.ALPProtocolKey.ANALOG_PIN_READ;
import static org.ardulink.core.proto.impl.ALProtoBuilder.ALPProtocolKey.DIGITAL_PIN_READ;
import static org.ardulink.testsupport.mock.StaticRegisterLinkFactory.register;
import static org.ardulink.testsupport.mock.TestSupport.eventFireringLink;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.ardulink.core.Link;
import org.ardulink.testsupport.mock.StaticRegisterLinkFactory.Registration;
import org.junit.jupiter.api.Test;

class ArdulinkConsumerIntegrationTest {

	private static final String OUT = "mock:result";

	@Test
	void messageIsSentOnAnalogPinChange() throws Exception {
		int pin = 2;
		int value = 42;
		try (Link link = eventFireringLink(analogPinValueChanged(analogPin(pin), value));
				Registration registration = register(link);
				CamelContext context = camelContext(registration.ardulinkUri())) {
			MockEndpoint out = getMockEndpoint(context);
			out.expectedBodiesReceived(alpProtocolMessage(ANALOG_PIN_READ).forPin(pin).withValue(value));
			out.assertIsSatisfied();
		}
	}

	@Test
	void messageIsSentOnDigitalPinChange() throws Exception {
		int pin = 3;
		boolean state = true;
		try (Link link = eventFireringLink(digitalPinValueChanged(digitalPin(pin), state));
				Registration registration = register(link);
				CamelContext context = camelContext(registration.ardulinkUri())) {
			MockEndpoint out = getMockEndpoint(context);
			out.expectedBodiesReceived(alpProtocolMessage(DIGITAL_PIN_READ).forPin(pin).withState(state));
			out.assertIsSatisfied();
		}
	}

	private CamelContext camelContext(String from) throws Exception {
		CamelContext context = new DefaultCamelContext();
		context.addRoutes(new RouteBuilder() {
			@Override
			public void configure() throws Exception {
				from(from).to(OUT);
			}
		});
		context.start();
		return context;
	}

	private MockEndpoint getMockEndpoint(CamelContext context) {
		return context.getEndpoint(OUT, MockEndpoint.class);
	}

}
