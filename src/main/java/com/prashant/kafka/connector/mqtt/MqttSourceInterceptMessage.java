package com.prashant.kafka.connector.mqtt;

import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * @author Prashant
 * @version 0.0.1
 *
 */

/**
 * MqttSourceInterceptMessage is a container for mqtt message.
 */
public class MqttSourceInterceptMessage {
	private String mTopic;
	private MqttMessage mMessage;

	public MqttSourceInterceptMessage(String topic, MqttMessage message) {
		this.mTopic = topic;
		this.mMessage = message;
	}

	public String getTopic() {
		return mTopic;
	}

	public byte[] getMessage() {
		return mMessage.getPayload();
	}
}
