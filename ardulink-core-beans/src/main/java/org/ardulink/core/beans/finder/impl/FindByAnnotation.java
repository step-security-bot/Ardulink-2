/**
Copyright 2013 project Ardulink http://www.ardulink.org/
 
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
 
    http://www.apache.org/licenses/LICENSE-2.0
 
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package org.ardulink.core.beans.finder.impl;

import static java.lang.reflect.Modifier.isPublic;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.ardulink.core.beans.finder.impl.FindByIntrospection.beanAttributes;
import static org.ardulink.core.beans.finder.impl.ReadMethod.isReadMethod;
import static org.ardulink.core.beans.finder.impl.WriteMethod.isWriteMethod;
import static org.ardulink.util.Preconditions.checkArgument;
import static org.ardulink.util.Streams.stream;
import static org.ardulink.util.Throwables.propagate;
import static org.ardulink.util.anno.LapsedWith.JDK8;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.ardulink.core.beans.Attribute.AttributeReader;
import org.ardulink.core.beans.Attribute.AttributeWriter;
import org.ardulink.core.beans.Attribute.TypedAttributeProvider;
import org.ardulink.core.beans.finder.api.AttributeFinder;
import org.ardulink.util.anno.LapsedWith;

/**
 * [ardulinktitle] [ardulinkversion]
 * 
 * project Ardulink http://www.ardulink.org/
 * 
 * [adsense]
 *
 */
public class FindByAnnotation implements AttributeFinder {

	private static class AttributeReaderDelegate implements AttributeReader {

		private final AttributeReader delegate;
		private final String name;
		private final Field annoFoundOn;

		public AttributeReaderDelegate(AttributeReader delegate, String name, Field annoFoundOn) {
			this.delegate = delegate;
			this.name = name;
			this.annoFoundOn = annoFoundOn;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public Class<?> getType() {
			return delegate.getType();
		}

		@Override
		public Object getValue() throws Exception {
			return delegate.getValue();
		}

		@Override
		public void addAnnotations(Collection<Annotation> annotations) {
			Collections.addAll(annotations, this.annoFoundOn.getAnnotations());
		}

	}

	public static class AttributeWriterDelegate implements AttributeWriter {

		private final AttributeWriter delegate;
		private final String name;
		private final Field annoFoundOn;

		public AttributeWriterDelegate(AttributeWriter delegate, String name, Field annoFoundOn) {
			this.delegate = delegate;
			this.name = name;
			this.annoFoundOn = annoFoundOn;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public Class<?> getType() {
			return delegate.getType();
		}

		@Override
		public void setValue(Object value) throws Exception {
			delegate.setValue(value);
		}

		@Override
		public void addAnnotations(Collection<Annotation> annotations) {
			Collections.addAll(annotations, this.annoFoundOn.getAnnotations());
		}

	}

	private final Class<? extends Annotation> annotationClass;
	private final Method getAnnotationsAttributeReadMethod;

	private FindByAnnotation(Class<? extends Annotation> annotationClass, String annotationAttribute) {
		this.annotationClass = annotationClass;
		this.getAnnotationsAttributeReadMethod = getAttribMethod(annotationClass, annotationAttribute);
		Class<?> returnType = this.getAnnotationsAttributeReadMethod.getReturnType();
		checkArgument(returnType.equals(String.class), "The returntype of %s's %s has to be %s but was %s",
				annotationClass.getName(), annotationAttribute, String.class, returnType);
	}

	private Method getAttribMethod(Class<? extends Annotation> annotationClass, String annotationAttribute) {
		try {
			return annotationClass.getMethod(annotationAttribute);
		} catch (SecurityException e) {
			throw propagate(e);
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException(
					annotationClass.getName() + " has no attribute named " + annotationAttribute);
		}
	}

	public static AttributeFinder propertyAnnotated(Class<? extends Annotation> annotationClass) {
		return propertyAnnotated(annotationClass, "value");
	}

	public static AttributeFinder propertyAnnotated(Class<? extends Annotation> annotationClass,
			String annotationAttribute) {
		return new FindByAnnotation(annotationClass, annotationAttribute);
	}

	@Override
	@LapsedWith(module = JDK8, value = "Streams")
	public Iterable<? extends AttributeReader> listReaders(Object bean) {
		try {
			List<AttributeReader> readers = stream(bean.getClass().getDeclaredMethods())
					.filter(m -> m.isAnnotationPresent(annotationClass) && isReadMethod(m))
					.map(m -> readMethod(bean, m)).collect(toList());

			for (Field field : bean.getClass().getDeclaredFields()) {
				if (field.isAnnotationPresent(annotationClass)) {
					Optional<? extends AttributeReader> readMethodForAttribute = readMethodForAttribute(bean,
							field.getName());
					if (readMethodForAttribute.isPresent()) {
						readers.add(new AttributeReaderDelegate(readMethodForAttribute.get(),
								annoValue(field.getAnnotation(annotationClass)), field));
					} else if (isPublic(field.getModifiers())) {
						readers.add(fieldAccess(bean, field));
					}
				}
			}
			return readers;
		} catch (Exception e) {
			throw propagate(e);
		}
	}

	@Override
	@LapsedWith(module = JDK8, value = "Streams")
	public Iterable<AttributeWriter> listWriters(Object bean) {
		try {
			List<AttributeWriter> writers = stream(bean.getClass().getDeclaredMethods())
					.filter(m -> m.isAnnotationPresent(annotationClass) && isWriteMethod(m))
					.map(m -> writeMethod(bean, m)).collect(toList());

			for (Field field : bean.getClass().getDeclaredFields()) {
				if (field.isAnnotationPresent(annotationClass)) {
					Optional<? extends AttributeWriter> writeMethodForAttribute = writeMethodForAttribute(bean,
							field.getName());
					if (writeMethodForAttribute.isPresent()) {
						writers.add(new AttributeWriterDelegate(writeMethodForAttribute.get(),
								annoValue(field.getAnnotation(annotationClass)), field));
					} else if (isPublic(field.getModifiers())) {
						writers.add(fieldAccess(bean, field));
					}
				}
			}
			return writers;
		} catch (Exception e) {
			throw propagate(e);
		}
	}

	private FieldAccess fieldAccess(Object bean, Field field) {
		return new FieldAccess(bean, annoValue(field.getAnnotation(annotationClass)), field);
	}

	private ReadMethod readMethod(Object bean, Method method) {
		return new ReadMethod(bean, annoValue(method.getAnnotation(annotationClass)), method);
	}

	private WriteMethod writeMethod(Object bean, Method method) {
		return new WriteMethod(bean, annoValue(method.getAnnotation(annotationClass)), method);
	}

	private Optional<? extends AttributeReader> readMethodForAttribute(Object bean, String name) {
		return findWithName(name, stream(beanAttributes().listReaders(bean)));
	}

	private Optional<? extends AttributeWriter> writeMethodForAttribute(Object bean, String name) {
		return findWithName(name, stream(beanAttributes().listWriters(bean)));
	}

	private <T extends TypedAttributeProvider> Optional<T> findWithName(String name, Stream<T> stream) {
		return stream.filter(hasName(name)).findFirst();
	}

	private Predicate<TypedAttributeProvider> hasName(String name) {
		return p -> p.getName().equals(name);
	}

	private String annoValue(Annotation annotation) {
		try {
			return (String) getAnnotationsAttributeReadMethod.invoke(annotation);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw propagate(e);
		}
	}

}
