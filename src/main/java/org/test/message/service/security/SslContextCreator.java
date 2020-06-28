package org.test.message.service.security;

import javax.net.ssl.SSLContext;

public interface SslContextCreator {
    SSLContext initSSLContext();
}
