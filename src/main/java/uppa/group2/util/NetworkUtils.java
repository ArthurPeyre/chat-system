package uppa.group2.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

public class NetworkUtils {

    private NetworkUtils() {}

    /**
     * Retourne l'adresse IP locale la plus appropriée (non-loopback).
     */
    public static String getLocalHost() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) continue;

                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    // Prendre la première IPv4 non-loopback
                    if (!addr.isLoopbackAddress() && addr.getHostAddress().indexOf(':') < 0) {
                        return addr.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            Logger.warn("Impossible de détecter l'IP locale: " + e.getMessage());
        }
        return "127.0.0.1";
    }

    /**
     * Formate une taille en octets en chaîne lisible (Ko, Mo, Go...).
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " o";
        if (bytes < 1024 * 1024) return String.format("%.1f Ko", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f Mo", bytes / (1024.0 * 1024));
        return String.format("%.1f Go", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * Vérifie si un port est dans la plage valide.
     */
    public static boolean isValidPort(int port) {
        return port > 1024 && port < 65535;
    }
}