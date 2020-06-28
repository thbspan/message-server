package org.test.message.server.config;

import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertiesResourceLoader implements IConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesResourceLoader.class);

    private final Properties properties;

    public PropertiesResourceLoader() {
        this.properties = new Properties();
        try {
            properties.load( this.getClass().getResourceAsStream(IConfig.DEFAULT_CONFIG));
        } catch (IOException e) {
            LOGGER.error("load properties file exception", e);
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String getProperty(String name) {
        return properties.getProperty(name);
    }

    @Override
    public String getProperty(String name, String defaultValue) {
        return properties.getProperty(name, defaultValue);
    }

    @Override
    public void setProperty(String name, String value) {
        properties.setProperty(name, value);
    }
}
