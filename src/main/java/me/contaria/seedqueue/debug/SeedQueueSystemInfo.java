package me.contaria.seedqueue.debug;

import com.google.gson.JsonObject;
import com.sun.management.OperatingSystemMXBean;
import me.contaria.seedqueue.SeedQueue;
import me.contaria.seedqueue.SeedQueueConfig;
import org.lwjgl.opengl.GL11;

import java.lang.management.ManagementFactory;

public class SeedQueueSystemInfo {
    public static void logSystemInformation() {
        SeedQueue.LOGGER.info("System Information (Logged by SeedQueue):");
        SeedQueue.LOGGER.info("Operating System: {}", System.getProperty("os.name"));
        SeedQueue.LOGGER.info("OS Version: {}", System.getProperty("os.version"));
        SeedQueue.LOGGER.info("CPU: {}", getCpuInfo());
        SeedQueue.LOGGER.info("GPU: {}", getGpuInfo());
        SeedQueue.LOGGER.info("Java Version: {}", System.getProperty("java.version"));
        SeedQueue.LOGGER.info("JVM Arguments: {}", getJavaArguments());
        SeedQueue.LOGGER.info("Total Physical Memory (MB): {}", getTotalPhysicalMemory());
        SeedQueue.LOGGER.info("Max Memory (MB): {}", getMaxAllocatedMemory());
        SeedQueue.LOGGER.info("Total Processors: {}", getTotalPhysicalProcessors());
        SeedQueue.LOGGER.info("Available Processors: {}", getAvailableProcessors());
    }

    private static String getCpuInfo() {
        // see GLX#_init
        oshi.hardware.Processor[] processors = new oshi.SystemInfo().getHardware().getProcessors();
        return String.format("%dx %s", processors.length, processors[0]).replaceAll("\\s+", " ");
    }

    private static String getGpuInfo() {
        // see GlDebugInfo#getRenderer
        return GL11.glGetString(GL11.GL_RENDERER);
    }

    private static String getJavaArguments() {
        // Logs the java arguments being used by the JVM
        return String.join(" ", ManagementFactory.getRuntimeMXBean().getInputArguments());
    }

    private static long getTotalPhysicalMemory() {
        // Logs the total RAM on the system
        return ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class).getTotalPhysicalMemorySize() / (1024 * 1024);
    }

    private static long getMaxAllocatedMemory() {
        // Logs the total RAM on the system
        return Runtime.getRuntime().maxMemory() / (1024 * 1024);
    }

    private static int getTotalPhysicalProcessors() {
        // Logs the total number of processors on the system
        // also includes the ones which are affected by affinity
        return ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors();
    }

    private static int getAvailableProcessors() {
        // Logs the available number of processors
        // excludes the ones which are affected by affinity
        return Runtime.getRuntime().availableProcessors();
    }

    public static void logConfigSettings() {
        if (Boolean.parseBoolean(System.getProperty("seedqueue.logConfigSettings", "true"))) {
            JsonObject json = SeedQueueConfig.container.toJson();
            String configSettings = json.toString();

            SeedQueue.LOGGER.info("SeedQueue Config settings: {}", configSettings);
        }
    }
}
