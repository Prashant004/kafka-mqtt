package com.prashant.kafka.connector.mqtt.sample;

/**
 * @author Prashant
 * @version 0.0.1
 *
 */

import org.apache.kafka.connect.data.Schema;
import org.apache.kafka.connect.source.SourceRecord;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.prashant.kafka.connector.mqtt.MqttMessageProcessor;

public class SimpleProcessor implements MqttMessageProcessor {
	private static final Logger log = LoggerFactory.getLogger(SimpleProcessor.class);
	private MqttMessage mMessage;
	private Object mTopic;

	@Override
	public MqttMessageProcessor process(String topic, MqttMessage message) {
		log.debug("processing data for topic: {}; with message {}", topic, message);
		this.mTopic = topic;
		this.mMessage = message;
		return this;
	}

	@Override
	public SourceRecord[] getRecords(String kafkaTopic) {
		return new SourceRecord[] { new SourceRecord(null, null, kafkaTopic, null, Schema.STRING_SCHEMA, mTopic,
				Schema.BYTES_SCHEMA, mMessage.getPayload()) };
	}
}
