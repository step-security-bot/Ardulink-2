package org.ardulink.core.protong.impl;

import static java.lang.Integer.parseInt;
import static java.lang.Math.pow;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.ardulink.core.Pin.analogPin;
import static org.ardulink.core.Pin.digitalPin;
import static org.ardulink.core.proto.api.bytestreamproccesors.ByteStreamProcessors.parse;
import static org.firmata4j.firmata.parser.FirmataToken.ANALOG_MESSAGE;
import static org.firmata4j.firmata.parser.FirmataToken.DIGITAL_MESSAGE;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.ardulink.core.Pin;
import org.ardulink.core.Pin.AnalogPin;
import org.ardulink.core.Tone;
import org.ardulink.core.messages.api.FromDeviceMessage;
import org.ardulink.core.messages.api.FromDeviceMessagePinStateChanged;
import org.ardulink.core.messages.api.FromDeviceMessageReady;
import org.ardulink.core.messages.api.ToDeviceMessageNoTone;
import org.ardulink.core.messages.api.ToDeviceMessagePinStateChange;
import org.ardulink.core.messages.api.ToDeviceMessageTone;
import org.ardulink.core.messages.impl.DefaultToDeviceMessageNoTone;
import org.ardulink.core.messages.impl.DefaultToDeviceMessagePinStateChange;
import org.ardulink.core.messages.impl.DefaultToDeviceMessageStartListening;
import org.ardulink.core.messages.impl.DefaultToDeviceMessageTone;
import org.ardulink.core.proto.api.bytestreamproccesors.ByteStreamProcessor;
import org.ardulink.core.proto.impl.FirmataProtocol;
import org.ardulink.util.Lists;
import org.ardulink.util.MapBuilder;
import org.ardulink.util.anno.LapsedWith;
import org.junit.Ignore;
import org.junit.Test;

public class FirmataProtocolTest {

	private ByteStreamProcessor sut = new FirmataProtocol().newByteStreamProcessor();

	private byte[] bytes;
	private List<FromDeviceMessage> messages;

	@Test
	public void canReadFirmwareStartupInfo() throws IOException {
		givenMessage((byte) 0xF0, (byte) 0x79, (byte) 0x01, (byte) 0x02, (byte) 0x41, (byte) 0x0, (byte) 0xF7);
		whenMessageIsProcessed();
		assertThat(messages.size(), is(1));
		assertThat(messages.get(0), instanceOf(FromDeviceMessageReady.class));
	}

	@Test
	@Ignore("not yet implemented")
	public void canReadAnalogPinViaFirmataProto() throws IOException {
		byte command = ANALOG_MESSAGE;
		byte pinNumber = 15;
		byte valueLow = 127;
		byte valueHigh = 42;

		AnalogPin pin = analogPin(pinNumber);
		givenAnalogPinReadingIsEnabledForPin(pin);
		givenMessage(command |= pinNumber, valueLow, valueHigh);
		whenMessageIsProcessed();
		thenMessageIs(pin, valueHigh << 7 | valueLow);
	}

	@Test
	public void canReadDigitalPinViaFirmataProto() throws IOException {
		byte command = DIGITAL_MESSAGE;
		byte port = 4;
		byte valueLow = binary("010" + "0101");
		byte valueHigh = binary("1");
		givenMessage(command |= port, valueLow, valueHigh);
		whenMessageIsProcessed();
		byte pin = (byte) pow(2, port + 1);

		// TODO PF onDigitalMappingReceive

		assertMessage(messages, MapBuilder.<Pin, Object>newMapBuilder() //
				.put(digitalPin(pin++), true).put(digitalPin(pin++), false) //
				.put(digitalPin(pin++), true).put(digitalPin(pin++), false) //
				.put(digitalPin(pin++), false).put(digitalPin(pin++), true) //
				.put(digitalPin(pin++), false).put(digitalPin(pin++), true) //
				.build());
	}

	// -------------------------------------------------------------------------

	@Test
	@Ignore
	public void canSetDigitalPin() {
		DefaultToDeviceMessagePinStateChange toDeviceMessage = new DefaultToDeviceMessagePinStateChange(digitalPin(42),
				false);
		assertThat(messageToSend(toDeviceMessage), is("XXX".getBytes()));
	}

	@Test
	public void canSetAnalogPin() {
		int value = 42;
		DefaultToDeviceMessagePinStateChange toDeviceMessage = new DefaultToDeviceMessagePinStateChange(analogPin(10),
				value);
		assertThat(messageToSend(toDeviceMessage), is(new byte[] { (byte) 0xEA, (byte) value, (byte) 0x00 }));
		// TODO Verify the EXTENDED_ANALOG (for higher pin numbers/values)
	}

	// -------------------------------------------------------------------------

	/**
	 * <a href=
	 * "https://github.com/firmata/protocol/blob/master/proposals/tone-proposal.md">Not
	 * part of Firmata! This is a proposal</a>
	 */
	@Test
	public void canSendTone() {
		byte pinNumber = 1;
		ToDeviceMessageTone toDeviceMessage = new DefaultToDeviceMessageTone(
				Tone.forPin(analogPin(pinNumber)).withHertz(234).withDuration(5, SECONDS));
		assertThat(messageToSend(toDeviceMessage), is(new byte[] { (byte) 0xF0, (byte) 0x5F, (byte) 0x00, pinNumber,
				(byte) 0x6A, (byte) 0x01, (byte) 0x08, (byte) 0x27, (byte) 0xF7 }));
	}

	/**
	 * <a href=
	 * "https://github.com/firmata/protocol/blob/master/proposals/tone-proposal.md">Not
	 * part of Firmata! This is a proposal</a>
	 */
	@Test
	public void canSendNoTone() {
		byte pinNumber = 1;
		ToDeviceMessageNoTone toDeviceMessage = new DefaultToDeviceMessageNoTone(analogPin(pinNumber));
		assertThat(messageToSend(toDeviceMessage),
				is(new byte[] { (byte) 0xF0, (byte) 0x5F, (byte) 0x01, pinNumber, (byte) 0xF7 }));
	}

	// -------------------------------------------------------------------------

	private void givenAnalogPinReadingIsEnabledForPin(Pin pin) {
		byte[] a = sut.toDevice(new DefaultToDeviceMessageStartListening(pin));
	}

	private void givenMessage(byte... bytes) {
		this.bytes = bytes;
	}

	private void thenMessageIs(Pin pin, Object value) {
		assertThat(messages.size(), is(1));
		FromDeviceMessagePinStateChanged pinStateChanged = (FromDeviceMessagePinStateChanged) messages.get(0);
		assertThat(pinStateChanged.getPin(), is(pin));
		assertThat(pinStateChanged.getValue(), is(value));
	}

	private void assertMessage(List<FromDeviceMessage> messages, Map<Pin, Object> expectedStates) {
		assertThat(messages.size(), is(expectedStates.size()));
		for (FromDeviceMessage message : messages) {
			FromDeviceMessagePinStateChanged pinStateChanged = (FromDeviceMessagePinStateChanged) message;
			Object object = expectedStates.get(pinStateChanged.getPin());
			assertThat("No expected state for pin " + pinStateChanged.getPin(), object, notNullValue());
			assertThat("Pin " + pinStateChanged.getPin(), pinStateChanged.getValue(), is(object));
		}
	}

	private byte[] messageToSend(ToDeviceMessagePinStateChange toDeviceMessage) {
		return sut.toDevice(toDeviceMessage);
	}

	private byte[] messageToSend(ToDeviceMessageTone toDeviceMessage) {
		return sut.toDevice(toDeviceMessage);
	}

	private byte[] messageToSend(ToDeviceMessageNoTone toDeviceMessage) {
		return sut.toDevice(toDeviceMessage);
	}

	@LapsedWith(module = "JDK7", value = "binary literals")
	private static byte binary(String string) {
		return (byte) parseInt(string, 2);
	}

	private void whenMessageIsProcessed() throws IOException {
		process(bytes);
	}

	private void process(byte[] bytes) throws IOException {
		// read in "random" (two) junks
		messages = Lists.newArrayList();
		InputStream stream = new ByteArrayInputStream(bytes);
		messages.addAll(parse(sut, read(stream, 2)));
		messages.addAll(parse(sut, read(stream, stream.available())));
	}

	private static byte[] read(InputStream stream, int length) throws IOException {
		byte[] bytes = new byte[length];
		stream.read(bytes);
		return bytes;
	}

}
