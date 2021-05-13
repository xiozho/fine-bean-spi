package cn.com.fiis.fine.spi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;

public class SpiBeanLoader {
	public static final String PATH_BEAN_SPI = "META-INF/bean.spi";

	private static SpiBeanLoader loader;

	public static SpiBeanLoader getSpiBeanLoader() {
		if (loader == null) {
			synchronized (SpiBeanLoader.class) {
				if (loader == null) {
					loader = new SpiBeanLoader();
				}
			}
		}
		return loader;
	}

	private final SpiFinder finder;
	private final SoftRefCache<String, SpiClass<?>> cache;

	protected SpiBeanLoader() {
		finder = new SpiFinder();
		cache = new SoftRefCache<>();
		init(getClass().getClassLoader());
	}

	public <T> SpiClass<T> get(final Class<T> clazz) {
		String key = clazz + "";
		SpiClass<?> csc = cache.get(key);
		if (csc == null) {
			SpiClass<T> sc = finder.find(clazz);
			cache.put(key, sc);
			return sc;
		} else {
			@SuppressWarnings("unchecked")
			SpiClass<T> sc = (SpiClass<T>) csc;
			return sc;
		}
	}

	public <T> SpiClass<T> get(final Class<T> clazz, final String condition) {
		String key = clazz + "@#" + condition;
		SpiClass<?> csc = cache.get(key);
		if (csc == null) {
			SpiClass<T> sc = finder.find(clazz, condition);
			cache.put(key, sc);
			return sc;
		} else {
			@SuppressWarnings("unchecked")
			SpiClass<T> sc = (SpiClass<T>) csc;
			return sc;
		}
	}

	protected void init(ClassLoader classLoader) {
		Enumeration<URL> paths = null;
		try {
			paths = classLoader != null ? classLoader.getResources(PATH_BEAN_SPI)
					: ClassLoader.getSystemResources(PATH_BEAN_SPI);
		} catch (IOException e) {
		}
		if (paths == null) {
			return;
		}
		ArrayList<URL> urls = new ArrayList<>();
		while (paths.hasMoreElements()) {
			urls.add(paths.nextElement());
		}
		urls.parallelStream().forEach(u -> {
			try (BufferedReader br = new BufferedReader(new InputStreamReader(u.openStream()))) {
				String buff = null;
				while ((buff = br.readLine()) != null) {
					String tmp = buff.trim();
					if (tmp.isEmpty() || tmp.startsWith("#")) {
						continue;
					}
					String className = tmp.split(" ")[0];
					try {
						Class<?> clazz = Class.forName(className);
						if (clazz.isAnnotationPresent(SpiBean.class)) {
							finder.add(clazz);
						}
					} catch (ClassNotFoundException e) {
					}
				}
			} catch (IOException e) {
			}
		});
	}
}
