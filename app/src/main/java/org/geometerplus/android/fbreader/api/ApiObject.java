/*
 * This code is in the public domain.
 */

package org.geometerplus.android.fbreader.api;

import java.util.List;
import java.util.ArrayList;

import android.os.Parcel;
import android.os.Parcelable;

public abstract class ApiObject implements Parcelable {
	protected static interface Type {
		int ERROR = -1;
		int VOID = 0;
		int INT = 1;
		int STRING = 2;
		int BOOLEAN = 3;
		int DATE = 4;
		int LONG = 5;
		int TEXT_POSITION = 10;
	}

	static class Void extends ApiObject {
		static Void Instance = new Void();

		private Void() {
		}

		@Override
		protected int type() {
			return Type.VOID;
		}
	}

	static class Integer extends ApiObject {
		final int Value;

		Integer(int value) {
			Value = value;
		}

		@Override
		protected int type() {
			return Type.INT;
		}

		@Override
		public void writeToParcel(Parcel parcel, int flags) {
			super.writeToParcel(parcel, flags);
			parcel.writeInt(Value);
		}
	}

	static class Long extends ApiObject {
		final long Value;

		Long(long value) {
			Value = value;
		}

		@Override
		protected int type() {
			return Type.LONG;
		}

		@Override
		public void writeToParcel(Parcel parcel, int flags) {
			super.writeToParcel(parcel, flags);
			parcel.writeLong(Value);
		}
	}

	static class Boolean extends ApiObject {
		final boolean Value;

		Boolean(boolean value) {
			Value = value;
		}

		@Override
		protected int type() {
			return Type.BOOLEAN;
		}

		@Override
		public void writeToParcel(Parcel parcel, int flags) {
			super.writeToParcel(parcel, flags);
			parcel.writeByte((byte)(Value ? 1 : 0));
		}
	}

	static class Date extends ApiObject {
		final java.util.Date Value;

		Date(java.util.Date value) {
			Value = value;
		}

		@Override
		protected int type() {
			return Type.DATE;
		}

		@Override
		public void writeToParcel(Parcel parcel, int flags) {
			super.writeToParcel(parcel, flags);
			parcel.writeLong(Value.getTime());
		}
	}

	static class String extends ApiObject {
		final java.lang.String Value;

		String(java.lang.String value) {
			Value = value;
		}

		@Override
		protected int type() {
			return Type.STRING;
		}

		@Override
		public void writeToParcel(Parcel parcel, int flags) {
			super.writeToParcel(parcel, flags);
			parcel.writeString(Value);
		}
	}

	static class Error extends ApiObject {
		final java.lang.String Message;

		Error(java.lang.String message) {
			Message = message;
		}

		@Override
		protected int type() {
			return Type.ERROR;
		}

		@Override
		public void writeToParcel(Parcel parcel, int flags) {
			super.writeToParcel(parcel, flags);
			parcel.writeString(Message);
		}
	}

	static ApiObject envelope(int value) {
		return new Integer(value);
	}

	static ApiObject envelope(boolean value) {
		return new Boolean(value);
	}

	static ApiObject envelope(java.lang.String value) {
		return new String(value);
	}

	static ApiObject envelope(java.util.Date value) {
		return new Date(value);
	}

	static List<ApiObject> envelope(List<java.lang.String> values) {
		final ArrayList<ApiObject> objects = new ArrayList<ApiObject>(values.size());
		for (java.lang.String v : values) {
			objects.add(new String(v));
		}
		return objects;
	}

	abstract protected int type();

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel parcel, int flags) {
		parcel.writeInt(type());
	}

	public static final Parcelable.Creator<ApiObject> CREATOR =
		new Parcelable.Creator<ApiObject>() {
			public ApiObject createFromParcel(Parcel parcel) {
				final int code = parcel.readInt();
				switch (code) {
					default:
						return new Error("Unknown object code: " + code);
					case Type.ERROR:
						return new Error(parcel.readString());
					case Type.VOID:
						return Void.Instance;
					case Type.INT:
						return new Integer(parcel.readInt());
					case Type.LONG:
						return new Long(parcel.readLong());
					case Type.BOOLEAN:
						return new Boolean(parcel.readByte() == 1);
					case Type.DATE:
						return new Date(new java.util.Date(parcel.readLong()));
					case Type.STRING:
						return new String(parcel.readString());
					case Type.TEXT_POSITION:
						return new TextPosition(parcel.readInt(), parcel.readInt(), parcel.readInt());
				}
			}

			public ApiObject[] newArray(int size) {
				return new ApiObject[size];
			}
		};
}
