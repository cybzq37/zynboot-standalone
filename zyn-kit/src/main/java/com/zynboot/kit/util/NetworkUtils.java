package com.zynboot.kit.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 网络工具类。
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NetworkUtils {

    private static final Pattern IPV4_PATTERN =
            Pattern.compile("^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$");

    private static final Pattern IPV6_PATTERN =
            Pattern.compile("^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$");

    private static final Pattern MAC_PATTERN =
            Pattern.compile("^([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}$");

    // ==================== IP ====================

    public static boolean isIpv4(String ip) {
        return !StringUtils.isBlank(ip) && IPV4_PATTERN.matcher(ip.trim()).matches();
    }

    public static boolean isIpv6(String ip) {
        return !StringUtils.isBlank(ip) && IPV6_PATTERN.matcher(ip.trim()).matches();
    }

    public static boolean isIp(String ip) {
        return isIpv4(ip) || isIpv6(ip);
    }

    /**
     * 判断是否为内网 IP。
     */
    public static boolean isInternalIp(String ip) {
        if (!isIpv4(ip)) return false;
        String[] parts = ip.split("\\.");
        int first = Integer.parseInt(parts[0]);
        int second = Integer.parseInt(parts[1]);
        // 10.x.x.x
        if (first == 10) return true;
        // 172.16.x.x ~ 172.31.x.x
        if (first == 172 && second >= 16 && second <= 31) return true;
        // 192.168.x.x
        if (first == 192 && second == 168) return true;
        // 127.x.x.x
        if (first == 127) return true;
        return false;
    }

    public static boolean isLocalhost(String ip) {
        return "127.0.0.1".equals(ip) || "0.0.0.0".equals(ip) || "::1".equals(ip);
    }

    /**
     * 获取本机 IP（优先非 loopback 的 IPv4）。
     */
    public static String getLocalIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp() || ni.isVirtual()) continue;
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException ignored) {
        }
        return "127.0.0.1";
    }

    /**
     * 获取主机名。
     */
    public static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }

    /**
     * 获取所有本机 IP 地址。
     */
    public static List<String> getAllLocalIps() {
        List<String> ips = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (!addr.isLoopbackAddress()) {
                        ips.add(addr.getHostAddress());
                    }
                }
            }
        } catch (SocketException ignored) {
        }
        return ips;
    }

    // ==================== MAC ====================

    public static boolean isMac(String mac) {
        return !StringUtils.isBlank(mac) && MAC_PATTERN.matcher(mac.trim()).matches();
    }

    /**
     * 获取本机 MAC 地址。
     */
    public static String getLocalMac() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface ni = NetworkInterface.getByInetAddress(ip);
            if (ni == null) return null;
            byte[] mac = ni.getHardwareAddress();
            if (mac == null) return null;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                if (i > 0) sb.append(":");
                sb.append(String.format("%02X", mac[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== 端口 ====================

    /**
     * 检查端口是否可用（未被占用）。
     */
    public static boolean isPortAvailable(int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", port), 200);
            return false; // 连接成功，端口被占用
        } catch (Exception e) {
            return true; // 连接失败，端口可用
        }
    }

    /**
     * 检查远程主机端口是否可达。
     */
    public static boolean isPortReachable(String host, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
