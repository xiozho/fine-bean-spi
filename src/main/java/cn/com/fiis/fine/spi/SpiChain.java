package cn.com.fiis.fine.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** SPI链 */
class SpiChain {
	private final Class<?> clazz;
	private final String[] conditions;
	private final ArrayList<SpiChain> children = new ArrayList<>();

	public SpiChain(Class<?> clazz) {
		this.clazz = clazz;
		SpiBean sb = clazz.getAnnotation(SpiBean.class);
		this.conditions = sb.conditions();
	}

	public Class<?> getDefult() {
		if (children.size() > 0) {
			List<Class<?>> clazzs = children.parallelStream().map(x -> x.getDefult()).filter(x -> x != null)
					.collect(Collectors.toList());
			if (clazzs == null || clazzs.isEmpty()) {
			} else if (clazzs.size() == 1) {
				return clazzs.get(0);
			} else {
				throw new RuntimeException(
						String.format("<DEFAULT::%s>Multiple Subclasses%s", clazz.getSimpleName(), clazzs));
			}
		}
		if (conditions == null || conditions.length == 0) {
			return clazz;
		} else {
			for (String cond : conditions) {
				if (cond == null || cond.length() == 0) {
					return clazz;
				}
			}
		}
		return null;
	}

	public Class<?> getByCondition(final String condition) {
		if (children.size() > 0) {
			List<Class<?>> clazzs = children.parallelStream().map(x -> x.getByCondition(condition))
					.filter(x -> x != null).collect(Collectors.toList());
			if (clazzs == null || clazzs.isEmpty()) {
			} else if (clazzs.size() == 1) {
				return clazzs.get(0);
			} else {
				throw new RuntimeException(String.format("<CONDITION:%s@%s>Multiple Subclasses%s",
						clazz.getSimpleName(), condition, clazzs));
			}
		}
		if (conditions != null && conditions.length > 0) {
			for (String cond : conditions) {
				if (cond.equals(condition)) {
					return clazz;
				}
			}
		}
		return getDefult();
	}

	public Class<?> getClazz() {
		return clazz;
	}

	public String[] getConditions() {
		return conditions;
	}

	public List<SpiChain> getChildren() {
		return children;
	}

	public SpiChain addChild(Class<?> clazz) {
		if (clazz == null) {
			return null;
		} else if (clazz == this.clazz) {
			return this;// 自身
		} else if (this.clazz.isAssignableFrom(clazz)) {
			for (SpiChain spiChain : children) {
				SpiChain sc = null;
				if ((sc = spiChain.addChild(clazz)) != null) {
					return sc;
				}
			}
			SpiChain sc = new SpiChain(clazz);
			this.children.add(sc);
			return sc;
		} else {
			return null;
		}
	}

	@Override
	public String toString() {
		return "SpiChain [clazz=" + clazz + ", conditions=" + conditions + ", children=" + children + "]";
	}

}
