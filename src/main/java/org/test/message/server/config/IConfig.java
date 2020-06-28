package org.test.message.server.config;

public interface IConfig {
    String DEFAULT_CONFIG = "/config/conf.properties";

    String getProperty(String name);

    String getProperty(String name, String defaultValue);

    void setProperty(String name, String value);

    default int getProperty(String name, int defaultValue) {
        String propertyValue = getProperty(name);
        return propertyValue == null ? defaultValue : Integer.parseInt(propertyValue);
    }

    default boolean getProperty(String name, boolean defaultValue) {
        String propertyValue = getProperty(name);
        if (propertyValue == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(propertyValue);
    }
}
