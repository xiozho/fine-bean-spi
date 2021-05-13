package cn.com.fiis.fine.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

final class SpiFinder {
	private final List<Class<?>> allClassSet = Collections.synchronizedList(new ArrayList<>());
	private final ConcurrentHashMap<Class<?>, SpiChain> classChainMap = new ConcurrentHashMap<>();

	public <T> SpiClass<T> find(final Class<T> clazz) {
		if (clazz != null && allClassSet.contains(clazz)) {
			SpiChain sc = classChainMap.get(clazz);
			@SuppressWarnings("unchecked")
			Class<? extends T> cc = (Class<? extends T>) sc.getDefult();
			if (cc != null) {
				return new SpiClass<T>(cc);
			}
		}
		return new SpiClass<T>(clazz);
	}

	public <T> SpiClass<T> find(final Class<T> clazz, final String condition) {
		if (clazz != null && allClassSet.contains(clazz)) {
			if (condition != null && condition.length() > 0) {
				SpiChain sc = classChainMap.get(clazz);
				@SuppressWarnings("unchecked")
				Class<? extends T> cc = (Class<? extends T>) sc.getByCondition(condition);
				if (cc != null) {
					return new SpiClass<T>(cc);
				}
			}
		}
		return new SpiClass<T>(clazz);
	}

	public void add(final Class<?> clazz) {
		if (clazz == null) {
			return;
		}
		Stack<Class<?>> s = new Stack<>();
		Class<?> tmpClass = clazz;
		while (tmpClass != null && tmpClass != Object.class) {
			if (!tmpClass.isAnnotationPresent(SpiBean.class)) {
				break; // 不存在注解
			}
			if (allClassSet.contains(tmpClass)) {
				break;
			}
			s.push(tmpClass);
			tmpClass = tmpClass.getSuperclass();
		}
		while (!s.isEmpty()) {
			Class<?> tc = s.pop();
			Class<?> pc = tc.getSuperclass();
			SpiChain psc = classChainMap.get(pc);
			SpiChain sc;
			if (psc != null) {
				sc = psc.addChild(tc);
			} else {
				sc = new SpiChain(tc);
			}
			classChainMap.put(tc, sc);
			allClassSet.add(tc);
		}
	}

}
