package com.marronst.moneycalc.entity;

public class Budget {

	// DBのカラム ---------------------------------
	public Integer id;
	public Integer categoryId;
	public Integer yearMonth;
	public Long budgetPrice;

	// カラム付随情報
	/**　１か月全体予算 */
	public static final Integer TOTAL_BUDGET = null;

	//関連テーブルのカラム
	public String categoryName;
	public Integer incomeFlg;
	public Integer dispFlg;

	// DBのテーブル名  -------------------------------------------------
	public static class TnBudget {
		public static String tableName() {
			return "budget";
		}
	}

	// DBのカラム名  -------------------------------------------------
	public static class CnBudget {

		public static String id() {
			return "_id";
		}

		public static String categoryId() {
			return "category_id";
		}

		public static String yearMonth() {
			return "year_month";
		}

		public static String budgetPrice() {
			return "budget_price";
		}
	}

	// DBのカラムIndex  -------------------------------------------------
	public static class IdxBudget {

		public static int id() {
			return 0;
		}

		public static int categoryId() {
			return 1;
		}

		public static int yearMonth() {
			return 2;
		}

		public static int budgetPrice() {
			return 3;
		}

		public static int lastIndex() {
			return 3;
		}
	}

	@Override
	public String toString() {
		return "Budget [budgetPrice=" + budgetPrice + ", categoryId=" + categoryId + ", id=" + id
				+ ", yearMonth=" + yearMonth + "]";
	}

}
