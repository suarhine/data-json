package org.dat.json;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlElement;

import org.reflect.invoke.util.Cast;
import org.reflect.invoke.util.Generic;
import org.reflect.invoke.util.Hierarchy;

/**
 * <h3>Class JSON</h3>
 * <p>
 * Class JSON เป็น Class สำหรับใช้ในการเก็บข้อมูลแบบ JSON Object
 * และใช้ในการสร้างและการอ่านค่าจาก JSON String
 * </p>
 *
 * @author เสือไฮ่
 * @version 1.1.0
 * @since JDK-1.8, invoke-2.0
 */
public class JSON implements Iterable<JSON.Entry>, Serializable {

	/**
	 * <h3>Interface JSON.Entry</h3>
	 * <p>
	 * Interface JSON.Entry เป็น Interface สำหรับสร้าง Entry ให้กับแต่ละ Element
	 * ในการวน loop ของ JSON Object
	 * </p>
	 *
	 * @author เสือไฮ่
	 */
	public interface Entry extends Map.Entry<Object, JSON> {}

	/**
	 * <h3>Interface JSON.InternalProcesser</h3>
	 * <p>
	 * Interface JSON.InternalProcesser เป็น Interface สำหรับดำเนินการภายใน JSON
	 * Object
	 * </p>
	 *
	 * @author เสือไฮ่
	 */
	public interface Internal {
		/**
		 * ดำเนินการภายใน JSON Object
		 *
		 * @param self
		 *            JSON Object ที่จะดำเนินการ
		 */
		public void proceed(JSON self);
	}

	/**
	 * <h3>Class JSON.Data</h3>
	 * <p>
	 * Class JSON.Data เป็น Class สำหรับใช้ในการเก็บข้อมูลของ JSON Object
	 * </p>
	 *
	 * @author เสือไฮ่
	 */
	protected static class Data implements Serializable, Cloneable {
		/**
		 * Field สำหรับ {@link Serializable}
		 */
		private static final long serialVersionUID = 1L;
		/**
		 * Attribute สำหรับเก็บข้อมูล JSON Object
		 */
		public Map<String, Data> object;
		/**
		 * Attribute สำหรับเก็บข้อมูล JSON Array
		 */
		public List<Data> array;
		/**
		 * Attribute สำหรับเก็บ JSON Value
		 */
		public Object value;

		@Override
		public Data clone() {
			try {
				return (Data) super.clone();
			} catch (CloneNotSupportedException e) {
				return null;
			}
		}
	}

	/**
	 * <h3>Class JSON.Props</h3>
	 * <p>
	 * Class JSON.Props เป็น Class สำหรับใช้ในการเก็บ Properties ของ
	 * {@link JSON} Object
	 * </p>
	 *
	 * @author เสือไฮ่
	 */
	public static class Props implements Serializable, Cloneable {
		/**
		 * Field สำหรับ {@link Serializable}
		 */
		private static final long serialVersionUID = 1L;
		/**
		 * Property สำหรับกำหนดการใช้ Quotation Mark บน JSON String
		 */
		public char quote = '"';
		/**
		 * Property สำหรับกำหนดว่าจะครอบ quote บน key ของ JSON Object
		 * ด้วยหรือไม่
		 */
		public boolean quoteOnKey = true;
		/**
		 * Property สำหรับกำหนดว่าจะครอบ quote บน JSON Value ที่ไม่ได้มี Data
		 * Type เป็น String ด้วยหรือไม่
		 */
		public boolean quoteOnNonString;
		/**
		 * Property สำหรับกำหนดว่าจะไม่สนใน Entry ที่มี JSON Value เป็น null
		 * หรือไม่
		 */
		public boolean ignoreNullField = true;
		/**
		 * Property สำหรับกำหนดว่าจะทำการอ่านและแปลงค่า JSON String เป็น JSON
		 * Object หรือไม่
		 */
		public boolean stringParsing = true;
		/**
		 * Property สำหรับกำหนดว่าจะทำการอ่านและแปลงค่า Java Object เป็น JSON
		 * Object หรือไม่
		 */
		public boolean objectParsing = true;
		/**
		 * Property สำหรับกำหนดว่าจะอ่านชื่อ Field JSON จาก {@link Annotation}
		 * ตัวไหน
		 */
		public Class<? extends Annotation> objectParsingKey = XmlElement.class;
		/**
		 * Property สำหรับกำหนดว่าจะอ่านชื่อ Field JSON จาก Field ไหนใน
		 * Annotation {@link #objectParsingKey}
		 */
		public String objectParsingKeyField = "name";
		/**
		 * Property สำหรับกำหนดว่าจะไม่อ่าน Field ใดๆที่ประกาศ
		 * {@link Annotation} นี้
		 */
		public Class<? extends Annotation> objectParsingKeyIgnoreField;
		/**
		 * Property สำหรับกำหนดว่า การที่จะดึงค่าจาก Java Object มาเป็น JSON
		 * Object นั้น จำเป็นต้องประกาศ Annotation {@link #objectParsingKey}
		 * ไว้ทุกField หรือไม่
		 */
		public boolean objectParsingKeyRequire;

		@Override
		public Props clone() {
			try {
				return (Props) super.clone();
			} catch (CloneNotSupportedException e) {
				return null;
			}
		}
	}

	/**
	 * <h3>Class JSON.Util</h3>
	 * <p>
	 * Class JSON.Util เป็น Class สำหรับใช้ในการทำงานภายใน Class {@link JSON}
	 * </p>
	 *
	 * @author เสือไฮ่
	 */
	protected static class Util {
		/**
		 * 
		 * <h3>Class JSON.Util.$</h3> สำหรับเก็บ Instance ของ {@link Util}
		 * 
		 * @author เสือไฮ่
		 * @version 1.0.0
		 * @since JDK-1.8, JSON-1.1.0
		 */
		private static class $ extends Util {
			/**
			 * Instance ของ {@link Util}
			 */
			private static final $ $ = new $();
		}

		/**
		 * <h3>Class JSON.Util.ObjIt</h3>
		 * <p>
		 * Class JSON.Util.ObjIt เป็น Class สำหรับสร้าง Iterator ของ JSON Object
		 * เพื่อนำไปใช้ในการวน loop for
		 * </p>
		 *
		 * @author เสือไฮ่
		 */
		protected class ObjIt implements Iterator<JSON.Entry> {
			/**
			 * <h3>Class JSON.Util.ObjIt.Entry</h3>
			 * <p>
			 * Class JSON.Util.ObjIt.Entry เป็น Class สำหรับสร้าง
			 * {@link JSON.Entry} สำหรับแต่ละ Element ใน JSON Object
			 * </p>
			 *
			 * @author เสือไฮ่
			 */
			private class Entry implements JSON.Entry {
				/**
				 * Attribute สำหรับเก็บ {@link JSON.Entry} สำหรับแต่ละ Element
				 * ใน JSON Object
				 */
				private final Map.Entry<String, Data> entry;

				/**
				 * Constructor สำหรับสร้าง {@link Entry}
				 *
				 * @param entry
				 *            {@link java.util.Map.Entry} สำหรับแต่ละ Element ใน
				 *            JSON Object
				 */
				protected Entry(Map.Entry<String, Data> entry) {
					this.entry = entry;
				}

				@Override
				public Object getKey() {
					return entry.getKey();
				}

				@Override
				public JSON getValue() {
					return entry.getValue() == null ? null
							: new JSON(entry.getValue(), props);
				}

				@Override
				public JSON setValue(JSON value) {
					try {
						return getValue();
					} finally {
						entry.setValue(value == null ? null : value.data);
					}
				}
			}

			/**
			 * Attribute สำหรับจดจำตัว Iterator ของ JSON Object
			 */
			private final Iterator<Map.Entry<String, Data>> iterator;
			/**
			 * Attribute สำหรับจดจำ Properties ของ JSON Object
			 */
			private final Props props;

			/**
			 * Constructor สำหรับสร้างตัว Iterator ของ JSON Object
			 *
			 * @param object
			 *            Map ของ JSON Object สำหรับนำไปสร้างตัว Iterator
			 * @param props
			 *            Properties ของ JSON Object
			 * @throws NullPointerException
			 *             object หรือ props เป็น null
			 */
			public ObjIt(Map<String, Data> object, Props props)
					throws NullPointerException {
				if ((this.props = props) == null)
					throw new NullPointerException();
				iterator = object.entrySet().iterator();
			}

			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public JSON.Entry next() {
				return new Entry(iterator.next());
			}

			@Override
			public void remove() {
				iterator.remove();
			}
		}

		/**
		 * <h3>Class JSON.Util.AryIt</h3>
		 * <p>
		 * Class JSON.Util.AryIt เป็น Class สำหรับสร้าง Iterator ของ JSON Array
		 * เพื่อนำไปใช้ในการวน loop for
		 * </p>
		 *
		 * @author เสือไฮ่
		 */
		protected class AryIt implements Iterator<JSON.Entry> {
			/**
			 * <h3>Class JSON.Util.AryIt.Entry</h3>
			 * <p>
			 * Class JSON.Util.AryIt.Entry เป็น Class สำหรับสร้าง
			 * {@link JSON.Entry} สำหรับแต่ละ Element ใน JSON Array
			 * </p>
			 *
			 * @author เสือไฮ่
			 */
			private class Entry implements JSON.Entry {
				/**
				 * Attribute สำหรับเก็บ Index ของ Element
				 */
				private final int index;
				/**
				 * Attribute สำหรับเก็บ Value ของ Element
				 */
				private Data value;

				/**
				 * @param index
				 *            Index ของ Element
				 * @param value
				 *            Value ของ Element
				 */
				protected Entry(int index, Data value) {
					this.index = index;
					this.value = value;
				}

				@Override
				public Object getKey() {
					return index;
				}

				@Override
				public JSON getValue() {
					return value == null ? null : new JSON(value, props);
				}

				@Override
				public JSON setValue(JSON value) {
					try {
						return getValue();
					} finally {
						this.value = value == null ? null : value.data;
						array.set(i, this.value);
					}
				}
			}

			/**
			 * Attribute สำหรับจดจำตัว Iterator ของ JSON Array
			 */
			private final Iterator<Data> iterator;
			/**
			 * Attribute สำหรับจดจำ List ของ JSON Array
			 */
			private final List<Data> array;
			/**
			 * Attribute สำหรับจดจำ Properties ของ JSON Array
			 */
			private final Props props;

			/**
			 * Attribute สำหรับจดจำ Index ปัจจุบันของ Iterator
			 */
			private int i;

			/**
			 * Constructor สำหรับสร้างตัว Iterator ของ JSON Object
			 *
			 * @param array
			 *            List ของ JSON Array สำหรับนำไปสร้างตัว Iterator
			 * @param props
			 *            Properties ของ JSON Array
			 * @throws NullPointerException
			 *             array หรือ props เป็น null
			 */
			public AryIt(List<Data> array, Props props)
					throws NullPointerException {
				if ((this.props = props) == null)
					throw new NullPointerException();
				iterator = (this.array = array).iterator();
			}

			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public JSON.Entry next() {
				try {
					return new Entry(i++, iterator.next());
				} catch (RuntimeException e) {
					i--;
					throw e;
				}
			}

			@Override
			public void remove() {
				iterator.remove();
				i--;
			}

		}

		/**
		 * <h3>Class JSON.Util.ValIt</h3>
		 * <p>
		 * Class JSON.Util.ValIt เป็น Class สำหรับสร้าง Iterator ของ JSON Value
		 * เพื่อนำไปใช้ในการวน loop for
		 * </p>
		 *
		 * @author เสือไฮ่
		 */
		protected class ValIt implements Iterator<JSON.Entry> {
			/**
			 * <h3>Class JSON.Util.ValIt.Entry</h3>
			 * <p>
			 * Class JSON.Util.ValIt.Entry เป็น Class สำหรับสร้าง
			 * {@link JSON.Entry} สำหรับ Element ของ JSON Value
			 * </p>
			 *
			 * @author เสือไฮ่
			 */
			private class Entry implements JSON.Entry {

				@Override
				public Object getKey() {
					return null;
				}

				@Override
				public JSON getValue() {
					return new JSON(data, props);
				}

				@Override
				public JSON setValue(JSON value) {
					try {
						return getValue();
					} finally {
						set(data, props, value);
					}
				}
			}

			/**
			 * Attribute สำหรับจดจำว่ามี Element ตัวต่อไปหรือไม่
			 */
			private boolean next;
			/**
			 * Attribute สำหรับจดจำ {@link JSON.Data} ของ JSON Value
			 */
			private final Data data;
			/**
			 * Attribute สำหรับจดจำ {@link JSON.Props} ของ JSON Value
			 */
			private final Props props;

			/**
			 * Constructor สำหรับสร้างตัว Iterator ของ JSON Value
			 *
			 * @param data
			 *            {@link JSON.Data} ของ JSON Value สำหรับนำไปสร้างตัว
			 *            Iterator
			 * @param props
			 *            Properties ของ JSON Value
			 * @throws NullPointerException
			 *             array หรือ props เป็น null
			 */
			public ValIt(Data data, Props props) throws NullPointerException {
				if ((this.data = data) == null
						|| (this.props = props) == null)
					throw new NullPointerException();
				next = this.data.value != null;
			}

			@Override
			public boolean hasNext() {
				return next;
			}

			@Override
			public JSON.Entry next() {
				if (next) {
					next = false;
					return new Entry();
				}
				throw new NoSuchElementException();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		}

		/**
		 * Field สำหรับ เก็บ Regex สำหรับ Date Format
		 */
		protected String dateRegex = "(?:(\\d{4})-(\\d{1,2})-(\\d{1,2}))?(?:^|T| |$)(?:(\\d{1,2}):(\\d{1,2})(?::(\\d{1,2})([.]\\d{1,3})?)?)?(?:(Z)|([+-]\\d{1,2}(?::\\d{2})?))?";

		/**
		 * สำหรับแปลง DataType ของ Object ใดๆ เป็น DataType ที่ต้องการ
		 *
		 * @param <T>
		 *            DataType ที่ต้องการแปลง
		 * @param type
		 *            Class ของ DataType ที่ต้องการ
		 * @param value
		 *            ค่าที่ต้องการแปลง
		 * @return value ที่แปลงค่าเป็น DataType (T) แล้ว
		 * @throws ClassCastException
		 *             ไม่สามารถแปลงค่า value เป็น DataType (T) ได้
		 */
		protected <T> T cast(Class<T> type, Object value)
				throws ClassCastException {
			return Cast.$(type, value);
		}

		/**
		 * Method สำหรับตรวจสอบ DataType ว่าเป็น DataType ของ JSON Value หรือไม่
		 *
		 * @param type
		 *            DataType ที่ต้องการตรวจสอบ
		 * @return true หาก type เป็น DataType ของ JSON Value
		 */
		protected boolean isJsonValueType(Class<?> type) {
			return type == null || type.isPrimitive()
					|| Number.class.isAssignableFrom(type)
					|| Boolean.class.isAssignableFrom(type)
					|| Date.class.isAssignableFrom(type)
					|| CharSequence.class.isAssignableFrom(type);
		}

		/**
		 * Method สำหรับตรวจสอบ DataType ว่าเป็น DataType ของ JSON หรือไม่
		 *
		 * @param type
		 *            DataType ที่ต้องการตรวจสอบ
		 * @return true หาก type เป็น DataType ของ JSON
		 */
		protected boolean isJsonType(Class<?> type) {
			return isJsonValueType(type)
					|| Map.class.isAssignableFrom(type)
					|| Iterable.class.isAssignableFrom(type)
					|| type.isArray();
		}

		/**
		 * Method สำหรับตรวจสอบ Value ว่าเป็น Value ของ JSON Value หรือไม่
		 *
		 * @param value
		 *            Value ที่ต้องการตรวจสอบ
		 * @return true หาก value เป็น Value ของ JSON Value
		 */
		protected boolean isJsonValue(Object value) {
			return value == null || isJsonValueType(value.getClass());
		}

		/**
		 * ตรวจสอบค่าว่าอยู่ในรูปแบบของ {@link Number} หรือไม่
		 *
		 * @param value
		 *            ค่าที่ต้องการตัวสอบ
		 * @return true หาก value อยู่ในรูปแบบของ {@link Number}
		 */
		protected boolean isNumberFormat(String value) {
			return value != null && value.matches("(0|-?[1-9]\\d*)(\\.\\d+)?");
		}

		/**
		 * แปลงค่าให้เป็น {@link Number}
		 *
		 * @param value
		 *            ค่าที่ต้องการแปลงให้เป็น {@link Number}
		 * @return ค่าของ value ในรูปแบบของ {@link Number}
		 * @throws NullPointerException
		 *             value เป็น null
		 * @throws IllegalArgumentException
		 *             value ไม่ได้อยู่ในรูปแบบของ {@link Number}
		 */
		protected Number toNumber(String value)
				throws NullPointerException, IllegalArgumentException {
			Double number = new Double(value);
			if (number.doubleValue() == number.intValue())
				return number.intValue();
			else if (number.doubleValue() == number.longValue())
				return number.longValue();
			else return number;
		}

		/**
		 * ตรวจสอบค่าว่าอยู่ในรูปแบบของ {@link Boolean} หรือไม่
		 *
		 * @param value
		 *            ค่าที่ต้องการตรวจสอบ
		 * @return true หาก value อยู่ในรูปแบบของ {@link Boolean}
		 */
		protected boolean isBooleanFormat(String value) {
			return value != null && value.matches("(?i)true|false");
		}

		/**
		 * แปลงค่าให้เป็น {@link Boolean}
		 *
		 * @param value
		 *            ค่าที่ต้องการแปลงให้เป็น {@link Boolean}
		 * @return ค่าของ value ในรูปแบบของ {@link Boolean}
		 * @throws NullPointerException
		 *             value เป็น null
		 * @throws IllegalArgumentException
		 *             value ไม่ได้อยู่ในรูปแบบของ {@link Boolean}
		 */
		protected boolean toBoolean(String value)
				throws NullPointerException, IllegalArgumentException {
			if (value.equalsIgnoreCase("true")) return true;
			else if (value.equalsIgnoreCase("false")) return false;
			throw new IllegalArgumentException(value);
		}

		/**
		 * ตรวจสอบค่าว่าอยู่ในรูปแบบของ {@link Date} หรือไม่
		 *
		 * @param value
		 *            ค่าที่ต้องการตรวจสอบ
		 * @return true หาก value อยู่ในรูปแบบของ {@link Date}
		 */
		protected boolean isDateFormat(String value) {
			return value != null && value.length() > 4
					&& value.matches(dateRegex);
		}

		/**
		 * แปลงค่าให้เป็น {@link Date}
		 *
		 * @param value
		 *            ค่าที่ต้องการแปลงให้เป็น {@link Date}
		 * @return ค่าของ value ในรูปแบบของ {@link Date}
		 * @throws NullPointerException
		 *             value เป็น null
		 * @throws IllegalArgumentException
		 *             value ไม่ได้อยู่ในรูปแบบของ {@link Date}
		 *             <code>(yyyy-mm-dd'T'hh:mm:ss.sss'Z'GMT+0)</code>
		 */
		protected Date toDate(String value)
				throws NullPointerException, IllegalArgumentException {
			// 1111-22-33 44:55:66.777(8|±99:99)
			Matcher matcher = Pattern.compile(dateRegex).matcher(value);
			if (!matcher.matches()
					|| (matcher.group(1) == null && matcher.group(4) == null))
				throw new IllegalArgumentException(value);
			TimeZone tz;
			if (matcher.group(8) == null) {
				tz = matcher.group(9) == null ? TimeZone.getDefault()
						: TimeZone.getTimeZone("GMT" + matcher.group(9));
			} else {
				tz = TimeZone.getTimeZone("GMT");
			}
			Calendar c = Calendar.getInstance(tz, Locale.US);
			c.setTimeInMillis(-tz.getOffset(0));
			try {
				c.set(Calendar.YEAR, Integer.parseInt(matcher.group(1)));
				c.set(Calendar.MONTH, Integer.parseInt(matcher.group(2)) - 1);
				c.set(Calendar.DATE, Integer.parseInt(matcher.group(3)));
			} catch (Throwable e) {}
			try {
				c.set(Calendar.HOUR_OF_DAY, Integer.parseInt(matcher.group(4)));
				c.set(Calendar.MINUTE, Integer.parseInt(matcher.group(5)));
				c.set(Calendar.SECOND, Integer.parseInt(matcher.group(6)));
				int ms = (int) (Double.parseDouble(matcher.group(7)) * 1000);
				c.set(Calendar.MILLISECOND, ms);
			} catch (Throwable e) {}
			return c.getTime();
		}

		/**
		 * ตรวจสอบค่าว่าอยู่ในรูปแบบของ null หรือไม่
		 *
		 * @param value
		 *            ค่าที่ต้องการตรวจสอบ
		 * @return true หาก value อยู่ในรูปแบบของ null
		 */
		protected boolean isNullFormat(String value) {
			return value == null || value.matches("null|undefined");
		}

		/**
		 * แปลงค่าให้เป็น Key สำหรับการระบุค่าใน JSON Object
		 *
		 * @param value
		 *            ค่าที่ต้องการแปลงให้เป็น Key
		 * @return value ที่แปลงเป็น Key
		 */
		protected String toKey(Object value) {
			return value == null ? null : value.toString();
		}

		/**
		 * แปลงค่าให้เป็น Index สำหรับการระบุตำแหน่ง JSON Array
		 *
		 * @param value
		 *            ค่าที่ต้องการแปลงค่าให้เป็น Index
		 * @return value ที่แปลงเป็น Index
		 * @throws IllegalArgumentException
		 *             ไม่สามารถแปลง value เป็น Index ได้
		 */
		protected Integer toIndex(Object value)
				throws IllegalArgumentException {
			try {
				return value instanceof Number ? ((Number) value).intValue()
						: Integer.parseInt(value.toString().trim());
			} catch (NullPointerException e) {
				return null;
			}
		}

		/**
		 * สร้างตัวเก็บข้อมูล JSON Object
		 *
		 * @return ตัวเก็บข้อมูล JSON Object
		 */
		protected Map<String, Data> newObject() {
			return new LinkedHashMap<>();
		}

		/**
		 * สร้างตัวเก็บข้อมูล JSON Array
		 *
		 * @param size
		 *            ขนาดความจุตั้งต้น, -1 = ขนาดความจุ default
		 * @return ตัวเก็บข้อมูล JSON Array
		 */
		protected List<Data> newArray(int size) {
			return size < 0 ? new ArrayList<>() : new ArrayList<>(size);
		}

		/**
		 * สร้าง Object {@link JSON.Data} สำหรับเก็บข้อมูล ของ {@link JSON}
		 * Object
		 *
		 * @return Object {@link JSON.Data} ที่สร้างขึ้น
		 */
		protected Data newData() {
			return new Data();
		}

		/**
		 * สร้าง Object {@link JSON.Data} สำหรับเก็บข้อมูล ของ {@link JSON}
		 * Object
		 *
		 * @param props
		 *            Properties ของ {@link JSON} Object
		 * @param value
		 *            ข้อมูลตั้งต้นของ {@link JSON.Data} Object
		 * @return Object {@link JSON.Data} ที่สร้างขึ้น
		 * @throws NullPointerException
		 *             props เป็น null
		 * @throws IllegalArgumentException
		 *             ไม่สามารถเก็บ value ในรูปแบบของ JSON ได้
		 */
		protected Data newData(Props props, Object value)
				throws NullPointerException, IllegalArgumentException {
			return set(newData(), props, value);
		}

		/**
		 * อ่านค่าจาก JSON String แล้วเก็บข้อมูลในรูปแบบของ JSON Object
		 *
		 * @param data
		 *            {@link Map} ของ {@link JSON.Data} สำหรับการเก็บข้อมูล JSON
		 *            Object
		 * @param props
		 *            Properties ของ {@link JSON} Object
		 * @param value
		 *            ข้อมูล JSON String
		 * @throws NullPointerException
		 *             data, props หรือ value เป็น null
		 * @throws IllegalArgumentException
		 *             ไม่สามารถเก็บ value ในรูปแบบของ JSON Object ได้
		 */
		protected void parse(Map<String, Data> data, Props props, String value)
				throws NullPointerException, IllegalArgumentException {
			if (!(value = value.trim()).isEmpty()) {
				char c0 = value.charAt(0);
				if (c0 == '\'' || c0 == '"') {
					Matcher m = Pattern.compile("[^\\\\]" + c0).matcher(value);
					if (m.find()) {
						int pocl = value.indexOf(':', m.end());
						if (value.substring(m.end(), pocl).trim().isEmpty()) {
							String key = value.substring(1, m.end() - 1)
									.replace("\\" + c0, c0 + "");
							String subval = value.substring(pocl + 1);
							data.put(key, parse(subval, props));
							return;
						}
					}
				} else {
					String[] split = value.split(":", 2);
					if (!(split[0] = split[0].trim()).isEmpty()) {
						data.put(split[0], parse(split[1], props));
						return;
					}
				}
				throw new IllegalArgumentException(value);
			}
		}

		/**
		 * อ่านค่าจาก JSON String แล้วเก็บข้อมูลในรูปแบบของ JSON Array
		 *
		 * @param data
		 *            {@link List} ของ {@link JSON.Data} สำหรับการเก็บข้อมูล
		 *            JSON Array
		 * @param props
		 *            Properties ของ {@link JSON} Object
		 * @param value
		 *            ข้อมูล JSON String
		 * @throws NullPointerException
		 *             data, props หรือ value เป็น null
		 * @throws IllegalArgumentException
		 *             ไม่สามารถเก็บ value ในรูปแบบของ JSON Array ได้
		 */
		protected void parse(List<Data> data, Props props, String value)
				throws NullPointerException, IllegalArgumentException {
			if (!(value = value.trim()).isEmpty()) {
				data.add(parse(value, props));
			}
		}

		/**
		 * อ่านค่าจาก JSON String แล้วเก็บข้อมูลในรูปแบบของ JSON
		 *
		 * @param value
		 *            ข้อมูล JSON String
		 * @param props
		 *            Properties ของ {@link JSON} Object
		 * @return Object {@link JSON.Data} ที่เก็บข้อมูล JSON
		 * @throws NullPointerException
		 *             props หรือ value เป็น null
		 * @throws IllegalArgumentException
		 *             ไม่สามารถเก็บ value ในรูปแบบของ JSON ได้
		 */
		protected Data parse(String value, Props props)
				throws NullPointerException, IllegalArgumentException {
			Data data = newData();
			char[] chars = value.trim().toCharArray();
			int lv = 0;
			int mark = 0;
			for (int i = 0, l = chars.length; i < l; i++) {
				if (chars[i] == '\'' || chars[i] == '"') {
					try {
						for (char token = chars[i]; chars[++i] != token;) {
							if (chars[i] == '\\') {
								i++;
							}
						}
					} catch (IndexOutOfBoundsException e) {
						throw new IllegalArgumentException(value);
					}
				}
				if (chars[i] == '[' || chars[i] == '{') {
					if (lv++ == 0) {
						mark = i + 1;
						if (chars[i] == '[') {
							data.array = newArray(1);
						} else {
							data.object = newObject();
						}
					}
				} else if (chars[i] == ']' || chars[i] == '}') {
					if (--lv == 0) {
						for (int j = i + 1; j < l; j++) {
							char c = chars[j];
							if (c != ' ' && c != '\r' && c != '\n')
								throw new IllegalArgumentException(value);
						}
						try {
							String subval = value.substring(mark, i);
							if (chars[i] == ']') {
								parse(data.array, props, subval);
							} else {
								parse(data.object, props, subval);
							}
						} catch (NullPointerException e) {
							throw new IllegalArgumentException(value);
						}
					}
				} else if (lv == 1 && chars[i] == ',') {
					if (data.array != null) {
						parse(data.array, props, value.substring(mark, i));
					} else {
						parse(data.object, props, value.substring(mark, i));
					}
					mark = i + 1;
				} else if (i == l - 1) {
					if (chars[i] == '\'' || chars[i] == '"') {
						if (chars[i] != chars[0])
							throw new IllegalArgumentException(value);
						String pure = value.substring(1, i)
								.replace("\\" + chars[i], chars[i] + "");
						if (isDateFormat(pure)) {
							data.value = toDate(pure);
						} else {
							data.value = pure;
						}
					} else if (isBooleanFormat(value)) {
						data.value = toBoolean(value);
					} else if (isNumberFormat(value)) {
						data.value = toNumber(value);
					} else if (isDateFormat(value)) {
						data.value = toDate(value);
					} else if (!isNullFormat(value)) {
						data.value = value;
					}
				}
			}
			if (lv != 0) throw new IllegalArgumentException(value);
			return data;
		}

		/**
		 * อ่านค่าจาก Java Object แล้วเก็บข้อมูลในรูปแบบของ JSON
		 *
		 * @param value
		 *            ข้อมูล Java Object
		 * @param props
		 *            Properties ของ {@link JSON} Object
		 * @return Object {@link JSON.Data} ที่เก็บข้อมูล JSON
		 * @throws NullPointerException
		 *             props เป็น null
		 * @throws IllegalArgumentException
		 *             ไม่สามารถเก็บ value ในรูปแบบของ JSON ได้
		 */
		protected Data parse(Object value, Props props) {
			Data data = newData();
			if (isJsonValue(value)) {
				data.value = value;
			} else {
				data.object = newObject();
				for (Class<?> clazz : new Hierarchy<>(value.getClass())) {
					for (Field field : clazz.getDeclaredFields()) {
						try {
							if (!field.isAccessible()) {
								field.setAccessible(true);
							}
							Object fVal = field.get(value);
							if (fVal == null && props.ignoreNullField) {
								continue;
							}
							Data dVal = newData();
							if (isJsonValueType(field.getType())) {
								dVal.value = fVal;
							} else {
								set(dVal, props, fVal);
							}
							data.object.put(getJsonKey(props, field), dVal);
						} catch (Throwable e) {}
					}
				}
			}
			return data;
		}

		/**
		 * Method สำหรับเรียก JSON Key ที่ประกาศบน {@link Field} ใน Object ใดๆ
		 *
		 * @param props
		 *            Properties ของ {@link JSON} Object
		 * @param field
		 *            Field สำหรับเรียก JSON Key
		 * @return JSON Key ของ field
		 * @throws NullPointerException
		 *             field เป็น null หรือไม่มี JSON Key บน field ที่ระบุ
		 */
		protected String getJsonKey(Props props, Field field)
				throws NullPointerException {
			if (props.objectParsingKeyIgnoreField != null
					&& field.getAnnotation(
							props.objectParsingKeyIgnoreField) != null)
				throw new NullPointerException();
			try {
				Annotation a = field.getAnnotation(props.objectParsingKey);
				Object invoked = a.getClass()
						.getMethod(props.objectParsingKeyField).invoke(a);
				return invoked.getClass().isArray()
						? Array.get(invoked, 0).toString() : invoked.toString();
			} catch (Throwable e) {
				if (props.objectParsingKeyRequire)
					throw new NullPointerException();
				return field.getName();
			}
		}

		/**
		 * ตรวจสอบค่าว่าเป็น JSON Object หรือไม่
		 *
		 * @param data
		 *            ค่าที่ต้องการตรวจสอบ
		 * @return true หาก data เป็น JSON Object
		 * @throws NullPointerException
		 *             data เป็น null
		 */
		public boolean isObject(Data data) throws NullPointerException {
			return data.object != null;
		}

		/**
		 * ตรวจสอบค่าว่าเป็น JSON Array หรือไม่
		 *
		 * @param data
		 *            ค่าที่ต้องการตรวจสอบ
		 * @return true หาก data เป็น JSON Array
		 * @throws NullPointerException
		 *             data เป็น null
		 */
		public boolean isArray(Data data) throws NullPointerException {
			return data.array != null;
		}

		/**
		 * ตรวจสอบค่าว่าเป็น JSON Value หรือไม่
		 *
		 * @param data
		 *            ค่าที่ต้องการตรวจสอบ
		 * @return true หาก data เป็น JSON Value
		 * @throws NullPointerException
		 *             data เป็น null
		 */
		public boolean isValue(Data data) throws NullPointerException {
			return data.value != null;
		}

		/**
		 * ตรวจสอบค่าว่าเป็น JSON Null หรือไม่
		 *
		 * @param data
		 *            ค่าที่ต้องการตรวจสอบ
		 * @return true หาก data เป็น JSON Null
		 * @throws NullPointerException
		 *             data เป็น null
		 */
		public boolean isNull(Data data) throws NullPointerException {
			return data.object == null
					&& data.array == null
					&& data.value == null;
		}

		/**
		 * ตรวจสอบค่าว่าเป็น JSON ที่ว่างเปล่าหรือไม่
		 *
		 * @param data
		 *            ค่าที่ต้องการตรวจสอบ
		 * @return true หาก data เป็น JSON ที่ว่างเปล่า
		 * @throws NullPointerException
		 *             data เป็น null
		 */
		public boolean isEmpty(Data data) throws NullPointerException {
			if (data.object != null) return data.object.isEmpty();
			else if (data.array != null) return data.array.isEmpty();
			else return data.value == null;
		}

		/**
		 * ตรวจสอบค่าว่ามีจำนวนข้อมูลอยู่ในข้อมูล JSON เป็นจำนวนเท่าไหร
		 *
		 * @param data
		 *            ค่าที่ต้องการตรวจสอบ
		 * @return จำนวนข้อมูลอยู่ในข้อมูล JSON
		 * @throws NullPointerException
		 *             data เป็น null
		 */
		public int size(Data data) throws NullPointerException {
			if (data.object != null) return data.object.size();
			else if (data.array != null) return data.array.size();
			else return data.value == null ? 0 : 1;
		}

		/**
		 * แปลงค่าจากข้อมูล JSON ไปเป็น Java Object DataType ที่ต้องการ
		 *
		 * @param <T>
		 *            DataType ที่ต้องการ
		 * @param data
		 *            Object {@link JSON.Data} สำหรับเก็บข้อมูล JSON
		 * @param props
		 *            Properties ของ {@link JSON} Object
		 * @param type
		 *            Type ของ DataType ที่ต้องการ
		 * @return ข้อมูล JSON Object ในรูปแบบของ Java Object ตาม DataType
		 *         ที่ต้องการ
		 * @throws NullPointerException
		 *             data หรือ props เป็น null
		 * @throws ClassCastException
		 *             ไม่สามารถแปลงค่า value เป็น DataType (T) ได้
		 */
		public <T> T get(Data data, Props props, Type type)
				throws NullPointerException, ClassCastException {
			Class<T> raw = Generic.raw(type);
			if (data.object != null) {
				if (raw == null || Map.class.isAssignableFrom(raw)) {
					Type[] act = new Generic(type).actual(Cast.$(Map.class));
					Map<Object, Object> map = new LinkedHashMap<>();
					for (Map.Entry<String, Data> entry : data.object
							.entrySet()) {
						map.put(cast(Generic.raw(act[0]), entry.getKey()),
								get(entry.getValue(), props, act[1]));
					}
					return cast(raw, map);
				} else if (isJsonValueType(raw))
					throw new ClassCastException("Is JSON Object.");
				try {
					T object = raw.newInstance();
					for (Class<? extends T> clazz : new Hierarchy<>(raw)) {
						for (Field field : clazz.getDeclaredFields()) {
							try {
								String key = getJsonKey(props, field);
								if (!field.isAccessible()) {
									field.setAccessible(true);
								}
								try {
									field.set(object, get(data.object.get(key),
											props, field.getGenericType()));
								} catch (Throwable e) {
									field.set(object, null);
								}
							} catch (Throwable e) {}
						}
					}
					return object;
				} catch (Throwable e) {
					throw new ClassCastException(e.getMessage());
				}
			} else if (data.array != null) {
				if (raw == null || Iterable.class.isAssignableFrom(raw)) {
					new Generic(type).actual(Iterable.class, "T");
					Type act = new Generic(type).actual(Iterable.class, "T");
					ArrayList<Object> list = new ArrayList<>();
					for (Data element : data.array) {
						list.add(get(element, props, act));
					}
					return cast(raw, list);
				} else if (raw.isArray()) {
					Class<?> ctype = raw.getComponentType();
					Object array = Array.newInstance(ctype, data.array.size());
					int i = 0;
					for (Data element : data.array) {
						Array.set(array, i++, get(element, props, ctype));
					}
					return Cast.$(array);
				} else throw new ClassCastException("Is JSON Array.");
			} else return cast(raw, data.value);
		}

		/**
		 * แปลงค่าจากข้อมูล JSON ไปเป็น Java Object DataType ที่ต้องการ
		 *
		 * @param <T>
		 *            DataType ที่ต้องการ
		 * @param data
		 *            Object {@link JSON.Data} สำหรับเก็บข้อมูล JSON
		 * @param props
		 *            Properties ของ {@link JSON} Object
		 * @param type
		 *            Class ของ DataType ที่ต้องการ
		 * @return ข้อมูล JSON Object ในรูปแบบของ Java Object ตาม DataType
		 *         ที่ต้องการ
		 * @throws NullPointerException
		 *             data หรือ props เป็น null
		 * @throws ClassCastException
		 *             ไม่สามารถแปลงค่า value เป็น DataType (T) ได้
		 * @see #get(Data, Props, Type)
		 */
		public <T> T get(Data data, Props props, Class<T> type) {
			return get(data, props, (Type) type);
		}

		/**
		 * ดึงข้อมูลจาก {@link JSON} Object ณ key / index ที่ระบุ
		 *
		 * @param data
		 *            Object {@link JSON.Data} สำหรับเก็บข้อมูล JSON
		 * @param props
		 *            Properties ของ {@link JSON} Object
		 * @param key
		 *            key / index สำหรับการระบุตำแหน่งของข้อมูลที่ต้องการดึงค่า
		 * @return ข้อมูลใน data ณ key / index ที่ระบุ
		 * @throws NullPointerException
		 *             data หรือ props เป็น null หรือ data ไม่ใช่ JSON Array
		 *             หรือ data ไม่ใช่ JSON Object
		 * @throws IllegalArgumentException
		 *             Index ไม่ใช่ Integer (สำหรับJSON Array)
		 */
		public Data get(Data data, Props props, Object key)
				throws NullPointerException, IllegalArgumentException {
			if (data.object != null) return data.object.get(toKey(key));
			else if (data.array != null) {
				try {
					return data.array.get(toIndex(key));
				} catch (IndexOutOfBoundsException e) {
					return null;
				} catch (Exception e) {
					throw new IllegalArgumentException(
							"Index " + key + " is incorrect.");
				}
			}
			throw new NullPointerException();
		}

		/**
		 * แปลงค่าจากข้อมูล JSON ไปเป็น Java Object ณ key / index ที่ระบุ
		 *
		 * @param <T>
		 *            DataType ที่ต้องการ
		 * @param data
		 *            Object {@link JSON.Data} สำหรับเก็บข้อมูล
		 * @param props
		 *            Properties ของ {@link JSON} Object
		 * @param key
		 *            key / index สำหรับการระบุตำแหน่งของข้อมูลที่ต้องการดึงค่า
		 * @param type
		 *            Class ของ DataType ที่ต้องการ
		 * @return ข้อมูล JSON Object ในรูปแบบของ Java Object ณ key ที่ระบุ ตาม
		 *         DataType ที่ต้องการ
		 * @throws NullPointerException
		 *             data ไม่ใช่ JSON Array หรือ JSON Object
		 * @throws IllegalArgumentException
		 *             Index ไม่ใช่ Integer (JSON Array)
		 * @throws ClassCastException
		 *             t ไม่ใช่ DataType ที่ถูกต้องสำหรับข้อมูลใน Data
		 */
		public <T> T get(Data data, Props props, Object key, Class<T> type)
				throws NullPointerException,
				IllegalArgumentException,
				ClassCastException {
			Data getted = get(data, props, key);
			return getted == null ? null : get(getted, props, type);
		}

		/**
		 * สร้างข้อมูล JSON ตัวใหม่ต่อจาก JSON Array ของข้อมูลที่มีอยู่
		 *
		 * @param data
		 *            Object {@link JSON.Data} สำหรับเก็บข้อมูล JSON
		 * @param props
		 *            Properties ของ {@link JSON} Object
		 * @return Data ของ JSON Object ตัวใหม่
		 * @throws NullPointerException
		 *             data หรือ props เป็น null
		 */
		public Data create(Data data, Props props)
				throws NullPointerException {
			return add(data, props, null);
		}

		/**
		 * สร้างข้อมูล JSON ตัวใหมต่อจากมูลที่มีอยู่ ณ key / index ที่ระบุ
		 *
		 * @param data
		 *            Object {@link JSON.Data} สำหรับเก็บข้อมูล JSON
		 * @param props
		 *            Properties ของ {@link JSON} Object
		 * @param key
		 *            key / index สำหรับการระบุตำแหน่งของข้อมูลที่ต้องการสร้าง
		 *            JSON Object ตัวใหม
		 * @return Data ของ JSON Object ตัวใหม่
		 */
		public Data create(Data data, Props props, Object key) {
			return set(data, props, key, null);
		}

		/**
		 * กำหนดค่าให้กับ {@link JSON} Value
		 *
		 * @param data
		 *            Object {@link JSON.Data} สำหรับเก็บข้อมูล JSON
		 * @param props
		 *            Properties ของ {@link JSON} Object
		 * @param value
		 *            ค่าที่ต้องการกำหนดให้กับ {@link JSON} Value
		 * @return Data ที่ถูกกำหนดค่าเรียบร้อยแล้ว
		 * @throws NullPointerException
		 *             data หรือ props เป็น null
		 */
		public Data setval(Data data, Props props, Object value) {
			data.object = null;
			data.array = null;
			data.value = isJsonValue(value) ? value : value.toString();
			return data;
		}

		/**
		 * กำหนดค่าให้กับข้อมูล {@link JSON} Object
		 *
		 * @param data
		 *            Object {@link JSON.Data} สำหรับเก็บข้อมูล JSON
		 * @param props
		 *            Properties ของ {@link JSON} Object
		 * @param value
		 *            ข้อมูลที่ต้องการกำหนดให้กับ {@link JSON} Object
		 * @return Data ที่ถูกกำหนดค่าเรียบร้อยแล้ว
		 * @throws NullPointerException
		 *             data หรือ props เป็น null
		 * @throws IllegalArgumentException
		 *             ไม่สามารถเก็บ value ในรูปแบบของ JSON ได้
		 */
		public Data set(Data data, Props props, Object value)
				throws NullPointerException, IllegalArgumentException {
			if (value instanceof Data) {
				data.object = ((Data) value).object;
				data.array = ((Data) value).array;
				data.value = ((Data) value).value;
			} else if (value instanceof JSON)
				return set(data, props, ((JSON) value).data);
			else if (isJsonValue(value)) {
				if (value instanceof CharSequence && props.stringParsing)
					return set(data, props, parse(value.toString(), props));
				data.object = null;
				data.array = null;
				data.value = value;
			} else if (value instanceof Object[]) {
				List<Data> array = newArray(((Object[]) value).length);
				for (Object element : (Object[]) value) {
					array.add(newData(props, element));
				}
				data.object = null;
				data.array = array;
				data.value = null;
			} else if (value.getClass().isArray()) {
				int length = Array.getLength(value);
				List<Data> array = newArray(length);
				for (int i = 0; i < length; i++) {
					array.add(newData(props, Array.get(value, i)));
				}
				data.object = null;
				data.array = array;
				data.value = null;
			} else if (value instanceof Iterable)
				return set(data, props, ((Iterable<?>) value).iterator());
			else if (value instanceof Iterator) {
				List<Data> array = newArray(0);
				for (Iterator<?> i = (Iterator<?>) value; i.hasNext();) {
					array.add(newData(props, i.next()));
				}
				data.object = null;
				data.array = array;
				data.value = null;
			} else if (value instanceof Map) {
				Map<String, Data> object = newObject();
				for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
					object.put(toKey(entry.getKey()),
							newData(props, entry.getValue()));
				}
				data.object = object;
				data.array = null;
				data.value = null;
			} else if (value == Object.class
					|| value.getClass() == Object.class) {
				data.object = newObject();
				data.array = null;
				data.value = null;
			} else if (value == Object[].class || value == Array.class) {
				data.object = null;
				data.array = newArray(0);
				data.value = null;
			} else if (value instanceof Enum) {
				data.object = null;
				data.array = null;
				data.value = ((Enum<?>) value).name();
			} else if (props.objectParsing) {
				set(data, props, parse(value, props));
			} else {
				data.object = null;
				data.array = null;
				data.value = value.toString();
			}
			return data;
		}

		/**
		 * กำหนดค่าให้กับข้อมูล {@link JSON} Object ณ key / index ที่ระบุ
		 *
		 * @param data
		 *            Object {@link JSON.Data} สำหรับเก็บข้อมูล JSON
		 * @param props
		 *            Properties ของ {@link JSON} Object
		 * @param key
		 *            key / index
		 *            สำหรับการระบุตำแหน่งของข้อมูลที่ต้องการกำหนดค่า
		 * @param value
		 *            ข้อมูลที่ต้องการกำหนดให้กับ {@link JSON} Object ณ key /
		 *            index ที่ระบุ
		 * @return Data ที่ถูกกำหนดค่าใน {@link JSON} Object ณ key / index
		 *         ที่ระบุ
		 * @throws NullPointerException
		 *             data หรือ props เป็น null หรือ data ไม่ใช่ JSON Array
		 *             หรือ data ไม่ใช่ JSON Object
		 * @throws IllegalArgumentException
		 *             ไม่สามารถเก็บ value ในรูปแบบของ JSON ได้ หรือ index
		 *             ไม่ถูกต้อง (สำหรับ JSON Array)
		 */
		public Data set(Data data, Props props, Object key, Object value)
				throws NullPointerException, IllegalArgumentException {
			if (data.object != null) {
				Data newData = newData(props, value);
				data.object.put(toKey(key), newData);
				return newData;
			} else if (data.array != null) {
				Data newData = newData(props, value);
				try {
					int index = toIndex(key);
					while (index >= data.array.size()) {
						data.array.add(null);
					}
					data.array.set(index, newData);
					return newData;
				} catch (Exception e) {
					throw new IllegalArgumentException(
							"Index " + key + " incorrect.");
				}
			} else if (data.value != null) throw new NullPointerException();
			else {
				if (key instanceof Number) {
					data.array = newArray(1);
				} else {
					data.object = newObject();
				}
				return set(data, props, key, value);
			}
		}

		/**
		 * เพิ่มข้อมูลให้กับ {@link JSON} Object
		 *
		 * @param data
		 *            Object {@link JSON.Data} สำหรับเก็บข้อมูล JSON
		 * @param props
		 *            Properties ของ {@link JSON} Object
		 * @param value
		 *            ข้อมูลที่ต้องการเพิ่มให้กับ {@link JSON} Object
		 * @return Data ที่ถูกเพิ่มข้อมูลให้กับ {@link JSON} Object
		 * @throws NullPointerException
		 *             data หรือ props เป็น null หรือ data ไม่ใช่ JSON Array
		 * @throws IllegalArgumentException
		 *             ไม่สามารถเก็บ value ในรูปแบบของ JSON ได้
		 */
		public Data add(Data data, Props props, Object value)
				throws NullPointerException, IllegalArgumentException {
			Data newData = newData(props, value);
			if (data.array == null) {
				if (data.object == null && data.value == null) {
					data.array = newArray(1);
				} else throw new NullPointerException();
			}
			data.array.add(newData);
			return newData;
		}

		/**
		 * เพิ่มข้อมูลให้กับ {@link JSON} ณ key / index ที่ระบุ
		 *
		 * @param data
		 *            Object {@link JSON.Data} สำหรับเก็บข้อมูล JSON
		 * @param props
		 *            Properties ของ {@link JSON} Object
		 * @param key
		 *            key / index
		 *            สำหรับการระบุตำแหน่งของข้อมูลที่ต้องการเพิ่มค่า
		 * @param value
		 *            ข้อมูลที่ต้องการเพิ่มให้กับ {@link JSON} Object ณ key /
		 *            index ที่ระบุ
		 * @return Data ที่ถูกเพิ่มข้อมูลให้กับ {@link JSON} Object ณ key /
		 *         index ที่ระบุ
		 * @throws NullPointerException
		 *             data หรือ props เป็น null หรือ data ณ key / index ที่ระบุ
		 *             ไม่ใช่ JSON Array
		 * @throws IllegalArgumentException
		 *             ไม่สามารถเก็บ value ในรูปแบบของ JSON ได้
		 */
		public Data add(Data data, Props props, Object key, Object value)
				throws NullPointerException, IllegalArgumentException {
			Data get = get(data, props, key);
			if (get == null)
				return set(data, props, key, new Object[] { value });
			return add(get, props, value);
		}

		/**
		 * ผนวกข้อมูลลงใน {@link JSON} Object
		 *
		 * @param data
		 *            Object {@link JSON.Data} สำหรับเก็บข้อมูล JSON
		 * @param props
		 *            Properties ของ {@link JSON} Object
		 * @param value
		 *            ข้อมูลที่ต้องการผนวกลงใน {@link JSON} Object
		 * @throws NullPointerException
		 *             data หรือ props เป็น null หรือ data ไม่ใช่ JSON Array
		 *             หรือ JSON Object และ value ไม่ใช่ JSON null
		 * @throws IllegalArgumentException
		 *             ไม่สามารถเก็บ value ในรูปแบบของ JSON ได้
		 */
		public void append(Data data, Props props, Object value)
				throws NullPointerException, IllegalArgumentException {
			Data newData = newData(props, value);
			if (data.object != null) {
				if (newData.object != null) {
					data.object.putAll(newData.object);
				} else if (newData.array != null) {
					int i = 0;
					for (Data element : newData.array) {
						data.object.put(i++ + "", element);
					}
				} else {
					data.object.put(null, newData);
				}
			} else if (data.array != null) {
				if (newData.object != null) {
					for (Map.Entry<String, Data> entry : newData.object
							.entrySet()) {
						data.array.add(entry.getValue());
					}
				} else if (newData.array != null) {
					data.array.addAll(newData.array);
				} else {
					data.array.add(newData);
				}
			} else if (data.value != null) {
				if (newData.object != null || newData.array != null)
					throw new NullPointerException();
				else if (data.value instanceof Integer
						&& newData.value instanceof Integer) {
					data.value = (Integer) data.value + (Integer) newData.value;
				} else if (data.value instanceof Number
						&& newData.value instanceof Number) {
					data.value = ((Number) data.value).doubleValue()
							+ ((Number) newData.value).doubleValue();
				} else if (data.value instanceof Date
						&& newData.value instanceof Date) {
					long time = ((Date) data.value).getTime()
							+ ((Date) data.value).getTime();
					data.value = new Date(time);
				} else if (newData.value != null) {
					data.value = data.value.toString()
							+ newData.value.toString();
				}
			} else {
				set(data, props, newData);
			}
		}

		/**
		 * ผนวกข้อมูลลงใน {@link JSON} Object ณ key / index ที่ระบุ
		 *
		 * @param data
		 *            Object {@link JSON.Data} สำหรับเก็บข้อมูล JSON
		 * @param props
		 *            Properties ของ {@link JSON} Object
		 * @param key
		 *            key / index สำหรับการระบุตำแหน่งของข้อมูลที่ต้องการผนวกค่า
		 * @param value
		 *            ข้อมูลที่ต้องการผนวกลงใน {@link JSON} Object ณ key / index
		 *            ที่ระบุ
		 * @throws NullPointerException
		 *             data หรือ props เป็น null หรือ data ไม่ใช่ JSON Array
		 *             หรือ JSON Object และ value ไม่ใช่ JSON null
		 * @throws IllegalArgumentException
		 *             ไม่สามารถเก็บ value ในรูปแบบของ JSON ได้
		 */
		public void append(Data data, Props props, Object key, Object value)
				throws NullPointerException, IllegalArgumentException {
			Data get = get(data, props, key);
			if (get == null) {
				set(data, props, key, value);
			} else {
				append(get, props, value);
			}
		}

		/**
		 * สร้าง JSON String จากข้อมูลของ {@link JSON} Object
		 *
		 * @param data
		 *            Object {@link JSON.Data} สำหรับเก็บข้อมูล JSON
		 * @param props
		 *            Properties ของ {@link JSON} Object
		 * @return JSON String
		 * @throws NullPointerException
		 *             data หรือ props เป็น null
		 */
		public CharSequence buildString(Data data, Props props)
				throws NullPointerException {
			if (data.object != null) {
				StringBuilder builder = new StringBuilder();
				Pattern pattern = Pattern.compile("^\\d|\\W");
				for (Map.Entry<String, Data> entry : data.object.entrySet()) {
					if (isNull(entry.getValue())) {
						continue;
					} else if (props.quoteOnKey
							|| pattern.matcher(entry.getKey()).find()) {
						builder.append(',').append(props.quote)
								.append(entry.getKey()).append(props.quote);
					} else {
						builder.append(',').append(entry.getKey());
					}
					builder.append(':').append(
							buildString(entry.getValue(), props));
				}
				return builder.delete(0, 1).insert(0, '{').append('}');
			} else if (data.array != null) {
				StringBuilder builder = new StringBuilder();
				for (Data element : data.array) {
					builder.append(',').append(buildString(element, props));
				}
				return builder.delete(0, 1).insert(0, '[').append(']');
			} else if (data.value == null) return "null";
			else if (data.value instanceof CharSequence
					|| props.quoteOnNonString) {
				String value = data.value.toString()
						.replace("" + props.quote, "\\" + props.quote);
				return new StringBuilder().append(props.quote)
						.append(value).append(props.quote);
			} else if (data.value instanceof Date) {
				String pattern = "\"yyyy-MM-dd'T'HH:mm:ss.SSSZ\"";
				String value = new SimpleDateFormat(pattern, Locale.US)
						.format(data.value);
				return new StringBuilder(value).insert(27, ':');
			} else return data.value.toString();
		}

		/**
		 * Method สำหรับตรวจสอบว่า {@link JSON} Object
		 * ที่ระบุมีค่าเท่ากันหรือไม่
		 *
		 * @param value1
		 *            ที่ระบุ 1
		 * @param value2
		 *            ที่ระบุ 2
		 * @param props
		 *            Properties ของ {@link JSON} Object
		 * @return true หาก <code>value1</code> และ <code>value1</code>
		 *         มีค่าเท่ากัน
		 * @throws NullPointerException
		 *             <code>props</code> เป็น null
		 */
		public boolean equals(Data value1, Data value2, Props props)
				throws NullPointerException {
			if (value1 == value2) return true;
			else if (value1 == null || value2 == null) return false;
			else if (value1.object != null)
				return value1.object.equals(value2.object);
			else if (value1.array != null)
				return value1.array.equals(value2.array);
			else if (value1.value != null)
				return value1.value.equals(value2.value);
			else return value2.object == null
					&& value2.array == null
					&& value2.value == null;

		}

		/**
		 * Method สำหรับ Generate Hash Code ให้กับ {@link JSON} Object ที่ระบ
		 *
		 * @param value
		 *            ค่าที่ต้องการ Generate Hash Code
		 * @param props
		 *            Properties ของ {@link JSON} Object
		 * @return Hash Code
		 * @throws NullPointerException
		 *             <code>value</code> หรือ <code>props</code> เป็น null
		 */
		public int hashCode(Data value, Props props)
				throws NullPointerException {
			if (value.object != null) return value.hashCode();
			else if (value.array != null) return value.array.hashCode();
			else if (value.value != null) return value.value.hashCode();
			else return 0;
		}

		/**
		 * สร้าง {@link Iterator} สำหรับ {@link JSON} Object
		 *
		 * @param data
		 *            Object {@link JSON.Data} สำหรับเก็บข้อมูล JSON
		 * @param props
		 *            Properties ของ {@link JSON} Object
		 * @return Iterator ของข้อมูล JSON
		 * @throws NullPointerException
		 *             data หรือ props เป็น null
		 */
		public Iterator<JSON.Entry> iterator(Data data, Props props)
				throws NullPointerException {
			if (data.object != null) return new ObjIt(data.object, props);
			else if (data.array != null) return new AryIt(data.array, props);
			else return new ValIt(data, props);
		}
	}

	/**
	 * Field สำหรับ {@link Serializable}
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Function สำหรับเรียกค่า {@link JSON} จาก String ที่ระบุ
	 *
	 * @param value
	 *            JSON String
	 * @return {@link JSON} จาก <code>value</code> ที่ระบุ
	 * @throws IllegalArgumentException
	 *             <code>value</code> ไม่ใช่ JSON String ที่ถูกต้อง
	 */
	public static JSON valueOf(String value) throws IllegalArgumentException {
		return new JSON(value);
	}

	/**
	 * Attribute สำหรับเก็บข้อมูลของ JSON Object
	 */
	protected final Data data;
	/**
	 * Attribute สำหรับใช้ในการเก็บ Properties ของ {@link JSON} Object
	 */
	public final Props props;

	/**
	 * Constructor สำหรับสร้าง JSON Object โดยนำ {@link JSON.Data} และ
	 * {@link JSON.Props} จาก {@link JSON} ตัวอื่นมาใช้
	 *
	 * @param data
	 *            {@link JSON.Data} จาก {@link JSON} ตัวอื่น
	 * @param props
	 *            {@link JSON.Props} จาก {@link JSON} ตัวอื่น
	 * @throws NullPointerException
	 *             data หรือ props เป็น null
	 */
	protected JSON(Data data, Props props) throws NullPointerException {
		if ((this.data = data) == null) throw new NullPointerException();
		this.props = props.clone();
	}

	/**
	 * Constructor สำหรับสร้าง JSON Object
	 *
	 * @param value
	 *            ข้อมูลตั้งต้นของ JSON Object
	 * @throws IllegalArgumentException
	 *             ไม่สามารถเก็บ value ในรูปแบบของ JSON ได้
	 */
	public JSON(Object value) throws IllegalArgumentException {
		data = getUtil().newData(props = new Props(), value);
	}

	/**
	 * Constructor สำหรับสร้าง JSON Object
	 *
	 * @param value
	 *            ข้อมูลตั้งต้นของ JSON Object
	 * @param charset
	 *            Charset ที่เข้ารหัส value
	 * @throws IllegalArgumentException
	 *             ไม่สามารถเก็บ value ในรูปแบบของ JSON ได้
	 * @throws IOException
	 *             ไม่สามารถอ่านข้อมูลจาก value ได้
	 */
	public JSON(InputStream value, Charset charset)
			throws IllegalArgumentException, IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] b = new byte[1024 * 1024];
		for (int r; (r = value.read(b)) > 0;) {
			baos.write(b, 0, r);
		}
		String text = new String(baos.toByteArray(), charset);
		data = getUtil().newData(props = new Props(), text);
	}

	/**
	 * Constructor สำหรับสร้าง JSON Object
	 *
	 * @param value
	 *            ข้อมูลตั้งต้นของ JSON Object
	 * @throws IllegalArgumentException
	 *             ไม่สามารถเก็บ value ในรูปแบบของ JSON ได้
	 * @throws IOException
	 *             ไม่สามารถอ่านข้อมูลจาก value ได้
	 */
	public JSON(InputStream value)
			throws IllegalArgumentException, IOException {
		this(value, Charset.defaultCharset());
	}

	/**
	 * Constructor สำหรับสร้าง JSON Object
	 *
	 * @param processer
	 *            ตัวดำเนินการภายใน JSON Object
	 */
	public JSON(Internal processer) {
		this();
		processer.proceed(this);
	}

	/**
	 * Constructor สำหรับสร้าง JSON Object
	 */
	public JSON() {
		data = getUtil().newData();
		props = new Props();
	}

	/**
	 * เรียกตัวช่วยในการทำงานภายใน Class JSON
	 *
	 * @return {@link JSON.Util}
	 */
	protected Util getUtil() {
		return Util.$.$;
	}

	/**
	 * ตรวจสอบว่าข้อมูล JSON เป็น JSON Object หรือไม่
	 *
	 * @return true หากเป็นข้อมูล JSON Object
	 */
	public boolean isObject() {
		return getUtil().isObject(data);
	}

	/**
	 * ตรวจสอบว่าข้อมูล JSON เป็น JSON Array หรือไม่
	 *
	 * @return true หากเป็นข้อมูล JSON Array
	 */
	public boolean isArray() {
		return getUtil().isArray(data);
	}

	/**
	 * ตรวจสอบว่าข้อมูล JSON เป็น JSON Value หรือไม่
	 *
	 * @return true หากเป็นข้อมูล JSON Value
	 */
	public boolean isValue() {
		return getUtil().isValue(data);
	}

	/**
	 * ตรวจสอบว่าข้อมูล JSON เป็น JSON Null หรือไม่
	 *
	 * @return true หากเป็นข้อมูล JSON Null
	 */
	public boolean isNull() {
		return getUtil().isNull(data);
	}

	/**
	 * ตรวจสอบว่าข้อมูล JSON เป็นข้อมูลที่ว่างเปล่าหรือไม่
	 *
	 * @return true หากเป็นข้อมูล JSON ที่ว่างเปล่า
	 */
	public boolean isEmpty() {
		return getUtil().isEmpty(data);
	}

	/**
	 * ตรวจสอบว่ามีจำนวนข้อมูลอยู่ในข้อมูล JSON เป็นจำนวนเท่าไหร
	 *
	 * @return จำนวนข้อมูลอยู่ในข้อมูล JSON
	 */
	public int size() {
		return getUtil().size(data);
	}

	/**
	 * แปลงค่าจากข้อมูล JSON ไปเป็น Java Object
	 *
	 * @param <R>
	 *            Data Type ของข้อมูลที่ต้องการในการ Return
	 * @return ข้อมูล JSON ในรูปแบบของ Java Object
	 * @throws ClassCastException
	 *             R ไม่ใช่ Data Type ที่ถูกต้องสำหรับข้อมูล JSON Object
	 */
	public <R> R get() throws ClassCastException {
		return getUtil().get(data, props, null);
	}

	/**
	 * แปลงค่าจากข้อมูล JSON ไปเป็น Java Object DataType ที่ต้องการ
	 *
	 * @param <T>
	 *            DataType ที่ต้องการ
	 * @param type
	 *            Class ของ DataType ที่ต้องการ
	 * @return ข้อมูล JSON Object ในรูปแบบของ Java Object ตาม DataType
	 *         ที่ต้องการ
	 * @throws ClassCastException
	 *             ไม่สามารถแปลงข้อมูล JSON เป็น DataType (T) ได้
	 */
	public <T> T get(Class<T> type) throws ClassCastException {
		return getUtil().get(data, props, type);
	}

	/**
	 * เรียกข้อมูล JSON Object จาก key ที่ระบุ
	 *
	 * @param key
	 *            key สำหรับการระบุ JSON Object ที่ต้องการข้อมูล
	 * @return ข้อมูล JSON Object ณ key ที่ระบุ
	 * @throws NullPointerException
	 *             ไม่ใช่ JSON Array หรือ JSON Object
	 * @throws IllegalArgumentException
	 *             Index ไม่ใช่ Integer (JSON Array)
	 */
	public JSON get(Object key)
			throws NullPointerException, IllegalArgumentException {
		Data getted = getUtil().get(data, props, key);
		return getted == null ? null : new JSON(getted, props);
	}

	/**
	 * เรียกค่าจากข้อมูล JSON ไปเป็น Java Object ณ key ที่ระบุ
	 *
	 * @param <T>
	 *            Data Type ของข้อมูลที่ต้องการ
	 * @param key
	 *            key สำหรับการระบุ JSON Object ที่ต้องการข้อมูล
	 * @param type
	 *            {@link Class} ของ Data Type ของข้อมูลที่ต้องการ
	 * @return ข้อมูล JSON Object ในรูปแบบของ Java Object ณ key ที่ระบุ
	 * @throws NullPointerException
	 *             ไม่ใช่ JSON Array หรือ JSON Object
	 * @throws IllegalArgumentException
	 *             Index ไม่ใช่ Integer (JSON Array)
	 * @throws ClassCastException
	 *             t ไม่ใช่ Data Type ที่ถูกต้องสำหรับข้อมูล ณ key ที่ระบุณ key
	 *             ที่ระบุ
	 */
	public <T> T get(Object key, Class<T> type)
			throws NullPointerException,
			IllegalArgumentException,
			ClassCastException {
		return getUtil().get(data, props, key, type);
	}

	/**
	 * กำหนดค่าให้กับ JSON Value
	 *
	 * @param value
	 *            ค่าที่จะกำหนดให้กับ JSON Value
	 * @return Object ตัวเอง
	 */
	public JSON setval(Object value) {
		getUtil().setval(data, props, value);
		return this;
	}

	/**
	 * กำหนดค่าให้กับ JSON Object
	 *
	 * @param value
	 *            ค่าที่จะกำหนดให้กับ JSON Object
	 * @return Object ตัวเอง
	 * @throws IllegalArgumentException
	 *             ไม่สามารถเก็บ value ในรูปแบบของ JSON ได้
	 */
	public JSON set(Object value) throws IllegalArgumentException {
		getUtil().set(data, props, value);
		return this;
	}

	/**
	 * กำหนดค่าให้กับ JSON Object ณ key ที่ระบุ
	 *
	 * @param key
	 *            key สำหรับการระบุ JSON Object ที่ต้องการกำหนดค่า
	 * @param value
	 *            ค่าที่ต้องการกำหนดให้กับ JSON Object ณ key ที่ระบุ
	 * @return Object ตัวเอง
	 * @throws IllegalArgumentException
	 *             ไม่สามารถเก็บ value ในรูปแบบของ JSON ได้ หรือ Index ไม่ใช่
	 *             Integer (JSON Array)
	 */
	public JSON set(Object key, Object value) throws IllegalArgumentException {
		getUtil().set(data, props, key, value);
		return this;
	}

	/**
	 * เพิ่มข้อมูลลงใน JSON Array
	 *
	 * @param value
	 *            ข้อมูลที่ต้องการเพิ่มลงใน JSON Object
	 * @return Object ตัวเอง
	 */
	public JSON add(Object value) {
		getUtil().add(data, props, value);
		return this;
	}

	/**
	 * เพิ่มข้อมูลลงใน JSON Array ณ key ที่ระบุ
	 *
	 * @param key
	 *            key สำหรับการระบุ JSON Object ที่ต้องการเพิ่มข้อมูล
	 * @param value
	 *            ข้อมูลที่ต้องการเพิ่มลงใน JSON Object ณ key ที่ระบุ
	 * @return Object ตัวเอง
	 */
	public JSON add(Object key, Object value) {
		getUtil().add(data, props, key, value);
		return this;
	}

	/**
	 * ผนวกข้อมูลลงใน JSON Object เดิม
	 *
	 * @param value
	 *            ข้อมูลที่ต้องการนำมาผนวก
	 * @return Object ตัวเอง
	 */
	public JSON append(Object value) {
		getUtil().append(data, props, value);
		return this;
	}

	/**
	 * ผนวกข้อมูลลงใน JSON Object ณ key ที่ระบุ
	 *
	 * @param key
	 *            key สำหรับการระบุ JSON Object ที่ต้องการผนวกข้อมูล
	 * @param value
	 *            ข้อมูลที่ต้องการนำมาผนวก
	 * @return Object ตัวเอง
	 */
	public JSON append(Object key, Object value) {
		getUtil().append(data, props, key, value);
		return this;
	}

	/**
	 * สร้าง JSON ตัวใหม่ ต่อจาก JSON Array ที่มีอยู่
	 *
	 * @return JSON Object ตัวใหม่
	 */
	public JSON create() {
		return new JSON(getUtil().create(data, props), props);
	}

	/**
	 * สร้าง JSON ตัวใหมต่อจากมูลที่มีอยู่ ณ key ที่ระบุ
	 *
	 * @param key
	 *            key สำหรับระบุการสร้าง JSON Object
	 * @return JSON Object ตัวใหม่
	 */
	public JSON create(Object key) {
		return new JSON(getUtil().create(data, props, key), props);
	}

	/**
	 * ดำเนินการภายใน JSON Object
	 *
	 * @param processer
	 *            ตัวดำเนินการภายใน JSON Object
	 * @return Object ตัวเอง
	 */
	public JSON internal(Internal processer) {
		processer.proceed(this);
		return this;
	}

	/**
	 * กำหนดการใช้ Quotation Mark บน JSON String
	 *
	 * @param value
	 *            เครื่องหมาย Quotation Mark
	 * @return Object ตัวเอง
	 */
	public JSON setQuote(char value) {
		props.quote = value;
		return this;
	}

	/**
	 * กำหนดว่าจะครอบ quote บน key ของ JSON Object ด้วยหรือไม่
	 *
	 * @param enable
	 *            true: ครอบ quote บน key ของ JSON Object
	 * @return Object ตัวเอง
	 */
	public JSON enableQuoteOnKey(boolean enable) {
		props.quoteOnKey = enable;
		return this;
	}

	/**
	 * กำหนดว่าจะครอบ quote บน JSON Value ที่ไม่ได้มี Data Type เป็น String
	 * ด้วยหรือไม่
	 *
	 * @param enable
	 *            true: ครอบ quote บน JSON Value
	 * @return Object ตัวเอง
	 */
	public JSON enableQuoteOnNonString(boolean enable) {
		props.quoteOnNonString = enable;
		return this;
	}

	/**
	 * กำหนดว่าจะครอบ quote บน JSON Value ที่ไม่ได้มี Data Type เป็น String
	 * ด้วยหรือไม่
	 *
	 * @param enable
	 *            true: ครอบ quote บน JSON Value
	 * @return Object ตัวเอง
	 */
	public JSON enableIgnoreNullField(boolean enable) {
		props.ignoreNullField = enable;
		return this;
	}

	/**
	 * กำหนดว่าจะทำการอ่านและแปลงค่า JSON String เป็น JSON Object หรือไม่
	 *
	 * @param enable
	 *            อ่านและแปลงค่า JSON String เป็น JSON Object
	 * @return Object ตัวเอง
	 */
	public JSON enableStringParsing(boolean enable) {
		props.stringParsing = enable;
		return this;
	}

	/**
	 * กำหนดว่าจะทำการอ่านและแปลงค่า Java Object เป็น JSON Object หรือไม่
	 *
	 * @param enable
	 *            อ่านและแปลงค่า Java Object เป็น JSON Object
	 * @return Object ตัวเอง
	 */
	public JSON enableObjectParsing(boolean enable) {
		props.objectParsing = enable;
		return this;
	}

	/**
	 * กำหนดว่าจะอ่านชื่อ Field JSON จาก {@link Annotation} ตัวไหน
	 *
	 * @param value
	 *            {@link Annotation} ที่จะอ่านชื่อ Field JSON
	 * @return Object ตัวเอง
	 */
	public JSON setObjectParsingKey(Class<? extends Annotation> value) {
		props.objectParsingKey = value;
		return this;
	}

	/**
	 * กำหนดว่าจะอ่านชื่อ Field JSON จาก Field ไหนใน Annotation
	 * {@link #setObjectParsingKey(Class)}
	 *
	 * @param value
	 *            ชื่อ Field Annotation ที่จะอ่านชื่อ Field JSON
	 * @return Object ตัวเอง
	 */
	public JSON setObjectParsingKeyField(String value) {
		props.objectParsingKeyField = value;
		return this;
	}

	/**
	 * กำหนดว่าจะไม่อ่าน Field ใดๆที่ประกาศ {@link Annotation} นี้
	 *
	 * @param value
	 *            {@link Annotation} ที่ประกาศ
	 * @return Object ตัวเอง
	 */
	public JSON setObjectParsingKeyIgnoreField(
			Class<? extends Annotation> value) {
		props.objectParsingKeyIgnoreField = value;
		return this;
	}

	/**
	 * กำหนดว่า การที่จะดึงค่าจาก Java Object มาเป็น JSON Object นั้น
	 * จำเป็นต้องประกาศ Annotation objectParsingKey ไว้ทุกField หรือไม่
	 *
	 * @param enable
	 *            enable
	 * @return Object ตัวเอง
	 */
	public JSON enableObjectParsingKeyRequire(boolean enable) {
		props.objectParsingKeyRequire = true;
		return this;
	}

	@Override
	public String toString() {
		return getUtil().buildString(data, props).toString();
	}

	@Override
	public boolean equals(Object object) {
		if (object == this) return true;
		else if (object instanceof JSON)
			return getUtil().equals(data, ((JSON) object).data, props);
		else return false;
	}

	@Override
	public int hashCode() {
		return getUtil().hashCode(data, props);
	}

	@Override
	public Iterator<JSON.Entry> iterator() {
		return getUtil().iterator(data, props);
	}
}