package util;

import java.lang.management.*;

public class RuntimeInfo {

	private static OperatingSystemMXBean osBean = ManagementFactory
			.getOperatingSystemMXBean();
	private static int availableProcessors = osBean.getAvailableProcessors();

	private static long lastSystemTime = 0;
	private static long lastProcessCpuTime = 0;

	public static int getAvailableCPU() {
		/* Total number of processors or cores available to the JVM */
		// "Available processors (cores): "
		return Runtime.getRuntime().availableProcessors();
	}

	public static long getFreeMemory() {
		/* Total amount of free memory available to the JVM */
		// "Free memory (bytes): "
		return Runtime.getRuntime().freeMemory();
	}

	public static long getMaxMemory() {
		/* This will return Long.MAX_VALUE if there is no preset limit */
		/* Maximum amount of memory the JVM will attempt to use */
		// "Maximum memory (bytes): "
		return Runtime.getRuntime().maxMemory();
	}

	public static long getTotalMemory() {
		// "Total memory (bytes): "
		/* Total memory currently in use by the JVM */
		return Runtime.getRuntime().totalMemory();
	}

	public static long getUsedMemory() {
		// "Used memory (bytes): "
		return getTotalMemory() - getFreeMemory();
	}

	public static synchronized double getCpuUsage() {
		if (lastSystemTime == 0) {
			lastSystemTime = System.nanoTime();

			if (osBean instanceof OperatingSystemMXBean) {
				lastProcessCpuTime = (long) ((OperatingSystemMXBean) osBean)
						.getSystemLoadAverage();
			}
		}

		long systemTime = System.nanoTime();
		long processCpuTime = 0;

		if (osBean instanceof OperatingSystemMXBean) {
			processCpuTime = (long) ((OperatingSystemMXBean) osBean)
					.getSystemLoadAverage();
		}

		double cpuUsage = (double) (processCpuTime - lastProcessCpuTime)
				/ (systemTime - lastSystemTime);

		lastSystemTime = systemTime;
		lastProcessCpuTime = processCpuTime;

		return cpuUsage / availableProcessors;
	}

	public static String getSummary() {
		return ("Runtime Info\n\tAvailable cores: "
				+ RuntimeInfo.getAvailableCPU() + "\n\tCPU usage: "
				+ getCpuUsage() + "\n\tMax memory (bytes): " + getMaxMemory()
				+ "\n\tFree memory (bytes): " + getFreeMemory()
				+ "\n\tTotal memory (bytes): " + getTotalMemory()
				+ "\n\tUsed memory (bytes): " + getUsedMemory());
	}

}
