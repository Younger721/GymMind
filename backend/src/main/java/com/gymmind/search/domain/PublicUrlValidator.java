package com.gymmind.search.domain;

import java.net.InetAddress;
import java.net.URI;
import org.springframework.stereotype.Component;

/** Validates outbound import targets before any HTTP client follows them. */
@Component
public final class PublicUrlValidator {
    public boolean isAllowed(String rawUrl) {
        try {
            URI uri = URI.create(rawUrl);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) return false;
            if (uri.getHost() == null || uri.getUserInfo() != null || uri.getPort() == 0) return false;
            String host = uri.getHost();
            if ("localhost".equalsIgnoreCase(host) || host.endsWith(".localhost")) return false;
            for (InetAddress address : InetAddress.getAllByName(host)) {
                if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress() || address.isMulticastAddress()) return false;
            }
            return true;
        } catch (IllegalArgumentException | java.net.UnknownHostException ex) {
            return false;
        }
    }
}
