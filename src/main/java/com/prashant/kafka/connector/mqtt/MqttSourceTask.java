package com.prashant.kafka.connector.mqtt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Prashant
 * @version 0.0.1
 *
 */

import org.apache.kafka.connect.source.SourceRecord;
import org.apache.kafka.connect.source.SourceTask;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.prashant.kafka.connector.mqtt.ssl.SslUtils;
import com.prashant.kafka.connector.mqtt.util.Version;

/**
 * MqttSourceTask is a Kafka Connect SourceTask implementation that reads from
 * MQTT and generates Kafka Connect records.
 */
public class MqttSourceTask extends SourceTask implements MqttCallback {
	private static final Logger log = LoggerFactory.getLogger(MqttSourceConnector.class);

	MqttClient mClient;
	String mKafkaTopic;
	String mMqttClientId;
	BlockingQueue<MqttMessageProcessor> mQueue = new LinkedBlockingQueue<>();
	MqttSourceConnectorConfig mConfig;

	/**
	 * Get the version of this task. Usually this should be the same as the
	 * corresponding {@link MqttSourceConnector} class's version.
	 *
	 * @return the version, formatted as a String
	 */
	@Override
	public String version() {
		return Version.getVersion();
	}

	/**
	 * Start the task.
	 *
	 * @param props initial configuration
	 */
	@Override
	public void start(Map<String, String> props) {
		log.info("Start a MqttSourceTask");

		mConfig = new MqttSourceConnectorConfig(props);

		mMqttClientId = mConfig.getString(MqttSourceConstant.MQTT_CLIENT_ID) != null
				? mConfig.getString(MqttSourceConstant.MQTT_CLIENT_ID)
				: MqttClient.generateClientId();

		// Setup Kafka
		mKafkaTopic = mConfig.getString(MqttSourceConstant.KAFKA_TOPIC);

		// Setup MQTT Connect Options
		MqttConnectOptions connectOptions = new MqttConnectOptions();

		String sslCa = mConfig.getString(MqttSourceConstant.MQTT_SSL_CA_CERT);
		String sslCert = mConfig.getString(MqttSourceConstant.MQTT_SSL_CERT);
		String sslPrivateKey = mConfig.getString(MqttSourceConstant.MQTT_SSL_PRIV_KEY);

		if (sslCa != null && sslCert != null && sslPrivateKey != null) {
			try {
				connectOptions.setSocketFactory(SslUtils.getSslSocketFactory(sslCa, sslCert, sslPrivateKey, ""));
			} catch (Exception e) {
				log.info("[{}] error creating socketFactory", mMqttClientId);
				e.printStackTrace();
				return;
			}
		}

		if (mConfig.getBoolean(MqttSourceConstant.MQTT_CLEAN_SESSION)) {
			connectOptions.setCleanSession(mConfig.getBoolean(MqttSourceConstant.MQTT_CLEAN_SESSION));
		}
		connectOptions.setConnectionTimeout(mConfig.getInt(MqttSourceConstant.MQTT_CONNECTION_TIMEOUT));
		connectOptions.setKeepAliveInterval(mConfig.getInt(MqttSourceConstant.MQTT_KEEP_ALIVE_INTERVAL));
		connectOptions.setServerURIs(mConfig.getString(MqttSourceConstant.MQTT_SERVER_URIS).split(","));

		if (mConfig.getString(MqttSourceConstant.MQTT_USERNAME) != null) {
			connectOptions.setUserName(mConfig.getString(MqttSourceConstant.MQTT_USERNAME));
		}

		if (mConfig.getString(MqttSourceConstant.MQTT_PASSWORD) != null) {
			connectOptions.setPassword(mConfig.getString(MqttSourceConstant.MQTT_PASSWORD).toCharArray());
		}

		// Connect to Broker
		try {
			// Address of the server to connect to, specified as a URI, is overridden using
			// MqttConnectOptions#setServerURIs(String[]) bellow.
			mClient = new MqttClient("tcp://127.0.0.1:1883", mMqttClientId, new MemoryPersistence());
			mClient.setCallback(this);
			mClient.connect(connectOptions);

			log.info("[{}] Connected to Broker", mMqttClientId);
		} catch (MqttException e) {
			log.error("[{}] Connection to Broker failed!", mMqttClientId, e);
		}

		// Setup topic
		try {
			String topic = mConfig.getString(MqttSourceConstant.MQTT_TOPIC);
			Integer qos = mConfig.getInt(MqttSourceConstant.MQTT_QUALITY_OF_SERVICE);

			mClient.subscribe(topic, qos);

			log.info("[{}] Subscribe to '{}' with QoS '{}'", mMqttClientId, topic, qos.toString());
		} catch (MqttException e) {
			log.error("[{}] Subscribe failed! ", mMqttClientId, e);
		}
	}

	/**
	 * Stop this task.
	 */
	@Override
	public void stop() {
		log.info("Stoping the MqttSourceTask");

		try {
			mClient.disconnect();

			log.info("[{}] Disconnected from Broker.", mMqttClientId);
		} catch (MqttException e) {
			log.error("[{}] Disconnecting from Broker failed!", mMqttClientId, e);
		}
	}

	/**
	 * Poll this SourceTask for new records. This method should block if no data is
	 * currently available.
	 *
	 * @return a list of source records
	 *
	 * @throws InterruptedException thread is waiting, sleeping, or otherwise
	 *                              occupied, and the thread is interrupted, either
	 *                              before or during the activity
	 */
	@Override
	public List<SourceRecord> poll() throws InterruptedException {
		List<SourceRecord> records = new ArrayList<>();
		MqttMessageProcessor message = mQueue.take();
		log.debug("[{}] Polling new data from queue for '{}' topic.", mMqttClientId, mKafkaTopic);

		Collections.addAll(records, message.getRecords(mKafkaTopic));

		return records;
	}

	/**
	 * This method is called when the connection to the server is lost.
	 *
	 * @param cause the reason behind the loss of connection.
	 */
	@Override
	public void connectionLost(Throwable cause) {
		log.error("MQTT connection lost!", cause);
	}

	/**
	 * Called when delivery for a message has been completed, and all
	 * acknowledgments have been received.
	 *
	 * @param token the delivery token associated with the message.
	 */
	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		// Nothing to implement.
	}

	/**
	 * This method is called when a message arrives from the server.
	 *
	 * @param topic   name of the topic on the message was published to
	 * @param message the actual message.
	 *
	 * @throws Exception if a terminal error has occurred, and the client should be
	 *                   shut down.
	 */
	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		log.debug("[{}] New message on '{}' arrived.", mMqttClientId, topic);

		this.mQueue.add(mConfig.getConfiguredInstance(MqttSourceConstant.MESSAGE_PROCESSOR, MqttMessageProcessor.class)
				.process(topic, message));
	}
}
