package me.contaria.seedqueue.debug;

import com.sun.management.OperatingSystemMXBean;
import me.contaria.seedqueue.SeedQueue;
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
        if (Boolean.parseBoolean(System.getProperty("seedqueue.logSeedQueueSettings", "true"))) {
            SeedQueue.LOGGER.info("SeedQueue Config Settings:");
            SeedQueue.LOGGER.info("Max Queued Seeds: {}", SeedQueue.config.maxCapacity);
            SeedQueue.LOGGER.info("Max Generating Seeds: {}", SeedQueue.config.maxConcurrently);
            SeedQueue.LOGGER.info("Max Generating Seeds (wall): {}", SeedQueue.config.maxConcurrently_onWall);
            SeedQueue.LOGGER.info("Max World Generation %: {}", SeedQueue.config.maxWorldGenerationPercentage);
            SeedQueue.LOGGER.info("Resume On Filled Queue: {}", SeedQueue.config.resumeOnFilledQueue);
            SeedQueue.LOGGER.info("SeedQueue Chunkmaps: {}", SeedQueue.config.chunkMapVisibility);
            SeedQueue.LOGGER.info("Chunkmap Scale: {}", SeedQueue.config.chunkMapScale);
            SeedQueue.LOGGER.info("Chunkmap Freezing: {}", SeedQueue.config.chunkMapFreezing);
            SeedQueue.LOGGER.info("Use Wall Screen: {}", SeedQueue.config.useWall);
            SeedQueue.LOGGER.info("Rows: {}", SeedQueue.config.rows);
            SeedQueue.LOGGER.info("Columns: {}", SeedQueue.config.columns);
            SeedQueue.LOGGER.info("Reset Cooldown: {}", SeedQueue.config.resetCooldown);
            SeedQueue.LOGGER.info("Wait for Preview Setup: {}", SeedQueue.config.waitForPreviewSetup);
            SeedQueue.LOGGER.info("Bypass Wall Screen: {}", SeedQueue.config.bypassWall);
            SeedQueue.LOGGER.info("Wall FPS: {}", SeedQueue.config.wallFPS);
            SeedQueue.LOGGER.info("Preview FPS: {}", SeedQueue.config.previewFPS);
            SeedQueue.LOGGER.info("Background Previews: {}", SeedQueue.config.preparingPreviews);
            SeedQueue.LOGGER.info("Freeze Locked Previews: {}", SeedQueue.config.freezeLockedPreviews);
            SeedQueue.LOGGER.info("Show Advanced Settings: {}", SeedQueue.config.showAdvancedSettings);
            SeedQueue.LOGGER.info("SeedQueue Thread Priority: {}", SeedQueue.config.seedQueueThreadPriority);
            SeedQueue.LOGGER.info("Server Thread Priority: {}", SeedQueue.config.serverThreadPriority);
            SeedQueue.LOGGER.info("Background Worker Threads: {}", SeedQueue.config.getBackgroundExecutorThreads());
            SeedQueue.LOGGER.info("Background Worker Priority: {}", SeedQueue.config.backgroundExecutorThreadPriority);
            SeedQueue.LOGGER.info("Wall Worker Threads: {}", SeedQueue.config.getWallExecutorThreads());
            SeedQueue.LOGGER.info("Wall Worker Priority: {}", SeedQueue.config.wallExecutorThreadPriority);
            SeedQueue.LOGGER.info("Sodium Update Threads: {}", SeedQueue.config.getChunkUpdateThreads());
            SeedQueue.LOGGER.info("Sodium Update Priority: {}", SeedQueue.config.chunkUpdateThreadPriority);
            SeedQueue.LOGGER.info("Smooth Chunk Building: {}", SeedQueue.config.reduceSchedulingBudget);
            SeedQueue.LOGGER.info("Reduce World List: {}", SeedQueue.config.reduceLevelList);
            SeedQueue.LOGGER.info("Watchdog: {}", SeedQueue.config.useWatchdog);
            SeedQueue.LOGGER.info("Show Debug Menu: {}", SeedQueue.config.showDebugMenu);
            SeedQueue.LOGGER.info("Benchmark Resets: {}", SeedQueue.config.benchmarkResets);
        }
    }
}
