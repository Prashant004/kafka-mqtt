package com.prashant.kafka.connector.mqtt;

import org.apache.kafka.connect.source.SourceRecord;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * @author Prashant
 * @version 0.0.1
 *
 */

public interface MqttMessageProcessor {

	MqttMessageProcessor process(String topic, MqttMessage message);

	SourceRecord[] getRecords(String kafkaTopic);

}
