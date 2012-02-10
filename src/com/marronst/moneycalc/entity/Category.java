package com.marronst.moneycalc.entity;

import java.util.Date;

public class Category {

	// DBのカラム ---------------------------------
	public Integer id;
	public String categoryName;
	public Integer position;
	public Date registerDate;
	public Integer dispFlg;//★削除予定★
	public Integer incomeFlg;
	public String iconName;

	// カラム付随情報
	/**　カテゴリ表示オン */
	public static final Integer DISP_FLG_ON = 1;
	/**　カテゴリ表示オフ */
	public static final Integer DISP_FLG_OFF = 0;

	/**　収入フラグオン */
	public static final Integer INCOME_FLG_ON = 1;
	/**　収入フラグオフ */
	public static final Integer INCOME_FLG_OFF = 0;

	// DBのテーブル名  -------------------------------------------------
	public static class TnCategory {
		public static String tableName() {
			return "category";
		}
	}

	// DBのカラム名  -------------------------------------------------
	public static class CnCategory {
		public static String id() {
			return "_id";
		}

		public static String categoryName() {
			return "category_name";
		}

		public static String position() {
			return "position";
		}

		public static String registerDate() {
			return "register_date";
		}

		public static String dispFlg() {
			return "disp_flg";
		}

		public static String incomeFlg() {
			return "income_flg";
		}

		public static String iconName() {
			return "icon_name";
		}
	}

	// DBのカラムIndex  -------------------------------------------------
	public static class IdxCategory {
		public static int id() {
			return 0;
		}

		public static int categoryName() {
			return 1;
		}

		public static int position() {
			return 2;
		}

		public static int registerDate() {
			return 3;
		}

		public static int dispFlg() {
			return 4;
		}

		public static int incomeFlg() {
			return 5;
		}

		public static int iconName() {
			return 6;
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("id = " + id).append(", ");
		builder.append("categoryName = " + categoryName).append(", ");
		builder.append("position = " + position).append(", ");
		builder.append("incomeFlg = " + incomeFlg).append(", ");
		return builder.toString();
	}
}
