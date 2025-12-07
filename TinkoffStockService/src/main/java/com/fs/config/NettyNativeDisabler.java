package com.fs.config;

/**
 * Disables Netty native libraries to prevent SIGSEGV errors in Docker containers.
 * This class must be loaded before any Netty/gRPC classes are initialized.
 */
public class NettyNativeDisabler {
    static {
        // Disable Netty native libraries BEFORE any Netty classes are loaded
        System.setProperty("io.netty.noUnsafe", "true");
        System.setProperty("io.netty.noPreferDirect", "true");
        System.setProperty("io.netty.transport.noNative", "true");
        System.setProperty("io.netty.allocator.type", "unpooled");
        System.setProperty("io.netty.leakDetection.level", "DISABLED");
        System.setProperty("io.netty.transport.native.epoll.enabled", "false");
        System.setProperty("io.netty.transport.native.kqueue.enabled", "false");
        
        // Additional properties to prevent native library loading
        System.setProperty("io.grpc.netty.shaded.io.netty.transport.native.epoll.enabled", "false");
        System.setProperty("io.grpc.netty.shaded.io.netty.transport.native.kqueue.enabled", "false");
    }
    
    public static void disable() {
        // Method to ensure class is loaded
    }
}

