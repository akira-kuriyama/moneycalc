package com.marronst.moneycalc.entity;

public class Filter {

	// DBのカラム ---------------------------------
	public Integer id;
	public String filterName;
	public String categoryIdList;
	//1ならデフォルト
	public Integer isDefault = DEFAULT_FLG_OFF;

	// カラム付随情報
	/**　デフォルトフラグOFF */
	public static final Integer DEFAULT_FLG_OFF = 0;
	/**　デフォルトフラグON */
	public static final Integer DEFAULT_FLG_ON = 1;

	/**　フィルターなし*/
	public static final Integer FILTER_NONE = -1;

	// DBのテーブル名  -------------------------------------------------
	public static class TnFilter {
		public static String tableName() {
			return "filter";
		}
	}

	// DBのカラム名  -------------------------------------------------
	public static class CnFilter {

		public static String id() {
			return "_id";
		}

		public static String filterName() {
			return "filter_name";
		}

		public static String categoryIdList() {
			return "category_id_list";
		}

		public static String isDefault() {
			return "is_default";
		}
	}

	// DBのカラムIndex  -------------------------------------------------
	public static class IdxFilter {

		public static int id() {
			return 0;
		}

		public static int filterName() {
			return 1;
		}

		public static int categoryIdList() {
			return 2;
		}

		public static int isDefault() {
			return 3;
		}

		public static int lastIndex() {
			return 3;
		}
	}

	@Override
	public String toString() {
		return "Filter [categoryIdList=" + categoryIdList + ", filterName=" + filterName + ", id=" + id
				+ ", isDefault=" + isDefault + "]";
	}

}
