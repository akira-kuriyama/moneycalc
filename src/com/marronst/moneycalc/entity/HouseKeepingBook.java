package com.marronst.moneycalc.entity;

import java.util.Date;

import android.os.Parcel;
import android.os.Parcelable;

public class HouseKeepingBook implements Parcelable {

	private static final long serialVersionUID = -9161223180645125243L;

	public HouseKeepingBook() {
		//
	}

	// DBのカラム ---------------------------------
	public Integer id;
	public Integer price;
	public String memo;
	public String place;
	public Integer categoryId;
	public Date registerDate;
	/**　緯度 */
	public String latitude;
	/** 経度 */
	public String longitude;
	/** インポートバージョン */
	public Integer importVersion;

	// 関連テーブルの情報 ---------------------------------
	public String title;
	public int incomeFlg;

	// DBのテーブル名  -------------------------------------------------
	public static class TnHouseKeepingBook {
		public static String tableName() {
			return "housekeeping_book";
		}
	}

	// DBのカラム名  -------------------------------------------------
	public static class CnHouseKeepingBook {
		public static String id() {
			return "_id";
		}

		public static String price() {
			return "price";
		}

		public static String memo() {
			return "memo";
		}

		public static String place() {
			return "place";
		}

		public static String categoryId() {
			return "category_id";
		}

		public static String registerDate() {
			return "register_date";
		}

		public static String latitude() {
			return "latitude";
		}

		public static String longitude() {
			return "longitude";
		}

		public static String importVersion() {
			return "importVersion";
		}
	}

	@Override
	public int describeContents() {
		return 0;
	}

	// DBのカラムIndex  -------------------------------------------------
	public static class IdxHouseKeepingBook {
		public static int id() {
			return 0;
		}

		public static int price() {
			return 1;
		}

		public static int memo() {
			return 2;
		}

		public static int place() {
			return 3;
		}

		public static int latitude() {
			return 4;
		}

		public static int longitude() {
			return 5;
		}

		public static int categoryId() {
			return 6;
		}

		public static int registerDate() {
			return 7;
		}

		public static int importVersion() {
			return 8;
		}

		public static int lastIndex() {
			return importVersion();
		}
	}

	@Override
	public void writeToParcel(final Parcel dest, final int flags) {
		dest.writeInt(id);
		dest.writeInt(price);
		dest.writeString(memo);
		dest.writeString(place);
		dest.writeInt(categoryId);
		dest.writeString(title);
		dest.writeSerializable(registerDate);
		dest.writeString(latitude);
		dest.writeString(longitude);
		dest.writeString(importVersion == null ? null : Integer.toString(importVersion));

	}

	public static final Parcelable.Creator<HouseKeepingBook> CREATOR = new Parcelable.Creator<HouseKeepingBook>() {
		public HouseKeepingBook createFromParcel(final Parcel in) {
			return new HouseKeepingBook(in);
		}

		public HouseKeepingBook[] newArray(final int size) {
			return new HouseKeepingBook[size];
		}
	};

	private HouseKeepingBook(final Parcel in) {
		id = in.readInt();
		price = in.readInt();
		memo = in.readString();
		place = in.readString();
		categoryId = in.readInt();
		title = in.readString();
		registerDate = (Date) in.readSerializable();
		latitude = in.readString();
		longitude = in.readString();
		String importVersionStr = in.readString();
		importVersion = importVersionStr == null ? null : Integer.valueOf(importVersionStr);
	}
}
