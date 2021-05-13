package cn.com.fiis.fine.spi;

import java.lang.ref.SoftReference;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/** 软引用缓存 */
public final class SoftRefCache<K, V> {
	/** 允许最大容量 */
	private static final int MAXIMUM_CAPACITY = 1 << 30;

	private final ConcurrentHashMap<K, SoftReference<CacheObject<V>>> $$; // 内容
	private final ConcurrentLinkedQueue<K> $k; // 键队列，用于LRU
	private final int $capacity; // 容量

	/** 默认最大长度1024 */
	public SoftRefCache() {
		this(1024); // 默认最大容量
	}

	public SoftRefCache(int capacity) {
		$$ = new ConcurrentHashMap<>(capacity);
		$k = new ConcurrentLinkedQueue<>();
		$capacity = capacity > MAXIMUM_CAPACITY ? MAXIMUM_CAPACITY : capacity;
	}

	/** 超容量移除最近最少使用 */
	private void fixLRU(K key) {
		$k.remove(key);
		$k.offer(key); // 加到队尾
		if ($k.size() > $capacity) {
			// 容量超了，取出队头键移除
			K oldkey = $k.poll();
			$$.remove(oldkey);
		}
	}

	/**
	 * 缓存(不过期)
	 * 
	 * @param key   键
	 * @param value 值
	 */
	public void put(K key, V value) {
		this.put(key, value, 0);// 默认不过期
	}

	/**
	 * 缓存
	 * 
	 * @param key            键
	 * @param value          值
	 * @param periodInMillis 过期时间(小于0:移除键对应值,0:不过期,大于0:过期时间)
	 */
	public void put(K key, V value, long periodInMillis) {
		if (key == null) {
			return;
		}
		if (value != null && periodInMillis >= 0) {
			fixLRU(key);
			long expiryTime = Long.MAX_VALUE;
			if (periodInMillis > 0) {
				expiryTime = System.currentTimeMillis() + periodInMillis;
				if (expiryTime < 0) {
					// 相加超出 Long最大值，则取最大值
					expiryTime = Long.MAX_VALUE;
				}
			}
			SoftReference<CacheObject<V>> sr = new SoftReference<>(new CacheObject<V>(value, expiryTime));
			$$.put(key, sr);
		} else {
			// 值为空或过期时间小于0,移除
			remove(key);
		}
	}

	/** 获取缓存 */
	public V get(K key) {
		V value = (V) Optional.ofNullable($$.get(key)).map(SoftReference::get)
				.filter(cacheObject -> !cacheObject.isExpired()).map(CacheObject::getValue).orElse(null);
		if (value != null) {
			fixLRU(key);
		}
		return value;
	}

	/** 移除缓存 */
	public V remove(K key) {
		V value = get(key);
		$$.remove(key);
		$k.remove(key);
		return value;
	}

	/** 清除缓存 */
	public void clear() {
		$$.clear();
		$k.clear();
	}

	/** 缓存对象 */
	private static class CacheObject<T> {
		private final T value;
		private final long expiryTime;

		private CacheObject(T value, long expiryTime) {
			this.value = value;
			this.expiryTime = expiryTime;
		}

		boolean isExpired() {
			return System.currentTimeMillis() > expiryTime;
		}

		public T getValue() {
			return value;
		}
	}
}