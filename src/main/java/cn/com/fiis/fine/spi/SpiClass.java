package cn.com.fiis.fine.spi;

import java.lang.reflect.Array;
import java.util.Arrays;

public class SpiClass<T> {
	private final Class<? extends T> clazz;
	private T singleton;

	public SpiClass(Class<? extends T> clazz) {
		this.clazz = clazz;
	}

	public Class<? extends T> getClazz() {
		return clazz;
	}

	public T newInstance() {
		try {
			return clazz.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			throw new RuntimeException(String.format("Error on: new %s()", clazz.getCanonicalName()), e);
		}
	}

	public T newInstance(Class<?>[] argTypes, Object[] args) {
		try {
			return clazz.getDeclaredConstructor(argTypes).newInstance(args);
		} catch (Exception e) {
			String p;
			if (args == null) {
				p = "";
			} else {
				p = Arrays.toString(args);
				p = p.substring(1, p.length() - 1);
			}
			throw new RuntimeException(String.format("Error on: new %s(%s)", clazz.getCanonicalName(), p), e);
		}
	}

	public T[] newArray(int len) {
		try {
			@SuppressWarnings("unchecked")
			T[] arr = (T[]) Array.newInstance(clazz, len);
			return arr;
		} catch (Exception e) {
			throw new RuntimeException(String.format("Error on: new %s[%s]", clazz.getCanonicalName(), len), e);
		}
	}

	public T singleton() {
		if (singleton == null) {
			synchronized (clazz) {
				if (singleton == null) {
					singleton = newInstance();
				}
			}
		}
		return singleton;
	}

	@Override
	public String toString() {
		return "SpiClass [clazz=" + clazz.getCanonicalName() + "]";
	}

}
