package com.prashant.kafka.connector.mqtt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.source.SourceConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.prashant.kafka.connector.mqtt.util.Version;

/**
 * @author Prashant
 * @version 0.0.1
 *
 */

/**
 * MqttSourceConnector is a Kafka Connect SourceConnector implementation that
 * generates tasks to ingest mqtt messages.
 **/

public class MqttSourceConnector extends SourceConnector {
	private static final Logger log = LoggerFactory.getLogger(MqttSourceConnector.class);

	MqttSourceConnectorConfig mConfig;
	private Map<String, String> mConfigProperties;

	/**
	 * Get the version of this connector.
	 *
	 * @return the version, formatted as a String
	 */
	@Override
	public String version() {
		return Version.getVersion();
	}

	/**
	 * Start this connector. This method will only be called on a clean Connector,
	 * i.e. it has either just been instantiated and initialized or {@link #stop()}
	 * has been invoked.
	 *
	 * @param props configuration settings
	 */
	@Override
	public void start(Map<String, String> props) {
		log.info("Start a MqttSourceConnector");
		mConfigProperties = props;
		mConfig = new MqttSourceConnectorConfig(props);
	}

	/**
	 * SourceTask implementation for this connector.
	 *
	 * @return SourceTask class instance
	 */
	@Override
	public Class<? extends Task> taskClass() {
		return MqttSourceTask.class;
	}

	/**
	 * Returns a set of configurations for Tasks based on the current configuration,
	 * producing at most count configurations.
	 *
	 * @param maxTasks maximum number of configurations to generate
	 *
	 * @return configurations for Tasks
	 */
	@Override
	public List<Map<String, String>> taskConfigs(int maxTasks) {
		List<Map<String, String>> taskConfigs = new ArrayList<>(1);
		Map<String, String> taskProps = new HashMap<>(mConfigProperties);
		taskConfigs.add(taskProps);
		return taskConfigs;
	}

	/**
	 * Stop this connector.
	 */
	@Override
	public void stop() {
		log.info("Stop the MqttSourceConnector");
	}

	@Override
	public ConfigDef config() {
		return MqttSourceConnectorConfig.config;
	}
}
