package dev.batuhanyetgin.gatewayserver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Reverse proxies (e.g. the Traefik ingress terminating TLS) whose
 * {@code X-Forwarded-For} header is trusted. Configured as a CIDR list via
 * {@code gateway.trusted-proxies} (env {@code GATEWAY_TRUSTEDPROXIES}); empty by
 * default, which keeps the "no proxy in front" behaviour: forwarded headers are
 * stripped and the TCP peer is the client.
 *
 * <p>Only safe once the gateway is reachable exclusively through that proxy
 * (Service type ClusterIP, no NodePort) — otherwise a NodePort hit SNAT'd into
 * the pod CIDR would also look trusted and could spoof XFF.
 */
@Component
public class TrustedProxies {

    private final List<String> cidrs;

    public TrustedProxies(@Value("${gateway.trusted-proxies:}") List<String> cidrs) {
        this.cidrs = cidrs.stream().map(String::trim).filter(s -> !s.isEmpty()).toList();
        this.cidrs.forEach(c -> inCidr("127.0.0.1", c)); // fail fast on a malformed entry
    }

    public boolean isTrusted(ServerWebExchange exchange) {
        InetSocketAddress address = exchange.getRequest().getRemoteAddress();
        return address != null && address.getAddress() != null
                && cidrs.stream().anyMatch(c -> inCidr(address.getAddress(), c));
    }

    static boolean inCidr(String ip, String cidr) {
        try {
            return inCidr(InetAddress.getByName(ip), cidr);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(ip, e);
        }
    }

    private static boolean inCidr(InetAddress ip, String cidr) {
        String[] parts = cidr.split("/");
        byte[] net;
        try {
            net = InetAddress.getByName(parts[0]).getAddress();
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Invalid trusted proxy CIDR: " + cidr, e);
        }
        byte[] addr = ip.getAddress();
        if (addr.length != net.length) {
            return false;
        }
        int bits = parts.length > 1 ? Integer.parseInt(parts[1]) : net.length * 8;
        for (int i = 0; i < net.length && bits > 0; i++, bits -= 8) {
            int mask = bits >= 8 ? 0xFF : (0xFF << (8 - bits)) & 0xFF;
            if ((addr[i] & mask) != (net[i] & mask)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Client IP: the rightmost X-Forwarded-For entry when the TCP peer is a
     * trusted proxy (that entry is what the proxy itself observed; anything to
     * its left is client-controlled), otherwise the TCP peer.
     */
    public String clientIp(ServerWebExchange exchange) {
        if (isTrusted(exchange)) {
            List<String> xff = exchange.getRequest().getHeaders().get("X-Forwarded-For");
            if (xff != null && !xff.isEmpty()) {
                String[] hops = xff.get(xff.size() - 1).split(",");
                String last = hops[hops.length - 1].trim();
                if (!last.isEmpty()) {
                    return last;
                }
            }
        }
        String peer = peerIp(exchange);
        return peer != null ? peer : "unknown";
    }

    private static String peerIp(ServerWebExchange exchange) {
        InetSocketAddress address = exchange.getRequest().getRemoteAddress();
        return address != null && address.getAddress() != null
                ? address.getAddress().getHostAddress()
                : null;
    }
}
