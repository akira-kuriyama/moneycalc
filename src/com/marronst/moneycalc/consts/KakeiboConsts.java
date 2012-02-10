package com.marronst.moneycalc.consts;

import com.marronst.moneycalc.R;

/**
 * 家計簿アプリ定数クラス
 * @author akira
 *
 */
public class KakeiboConsts {

	public static final String UTF_8 = "UTF-8";

	public static final String MS932 = "MS932";

	public static final int REQUEST_CODE_VIEW_LIST = 1;

	public static final int KAKEIBO_START_NOTIFICATION_ID = 1;

	/** 最大金額 */
	public static final int PRICE_LIMIT = 100000000;//一億

	/** 集計期間タイプ */
	public enum KakeiboListViewType {
		//
			YEAR(R.string.YEAR), //
			HALF_YEAR(R.string.HALF_YEAR), //
			THREE_MONTH(R.string.THREE_MONTH), //
			MONTH(R.string.MONTH), //
			WEEK(R.string.WEEK), //
			DAY(R.string.DAY), //
		;

		private KakeiboListViewType(final int nameRes) {
			this.nameRes = nameRes;
		}

		private final int nameRes;

		public int getNameRes() {
			return this.nameRes;
		}
	}

	/** グラフタイプ */
	public enum GraphType {
		//
			Pie, //
			Bar, ;
	}

	public static final String INTENT_KEY_LIST_VIEW_TYPE = "LIST_VIEW_TYPE";
	public static final String INTENT_KEY_GRAPH_TYPE = "INTENT_KEY_GRAPH_TYPE";
	public static final String INTENT_KEY_TARGET_VIEW_CATEGORY_ID = "INTENT_KEY_TARGET_VIEW_CATEGORY_ID";
	public static final String INTENT_KEY_START_DATE_VALUE = "INTENT_KEY_START_DATE_VALUE";
	public static final String INTENT_KEY_CATEGORY_ID = "INTENT_KEY_CATEGORY_ID";
	public static final String INTENT_KEY_CATEGORY_NAME = "INTENT_KEY_CATEGORY_NAME";
	public static final String INTENT_KEY_PIE_GRAPH_DATA = "INTENT_KEY_PIE_GRAPH_DATA";
	public static final String INTENT_KEY_FILTER_ID = "INTENT_KEY_FILTER_ID";
	public static final String INTENT_KEY_REGISTER_DATE = "INTENT_KEY_REGISTER_DATE";

	public static final String PREFERENCE_KEY_IS_DISP_STATUS_BAR = "is_disp_status_bar";
	public static final String PREFERENCE_KEY_IS_MOVE_DAY_LIST_AFTER_REGISTERED = "is_move_day_list_after_registered";
	public static final String PREFERENCE_KEY_CURRENCY_UNIT = "PREFERENCE_KEY_CURRENCY_UNIT";
	public static final String PREFERENCE_KEY_FIRST_DAY_OF_WEEKS = "first_day_of_weeks";
	public static final String PREFERENCE_KEY_FIRST_DAY_OF_MONTH = "first_day_of_month";
	public static final String PREFERENCE_KEY_CURRENCY_UNIT_POSITION = "PREFERENCE_KEY_CURRENCY_UNIT_POSITION";
	public static final String PREFERENCE_KEY_SEND_LOG = "send_log";
	public static final String PREFERENCE_KEY_IS_USE_CARRYOVER = "is_use_carryover";
	public static final String PREFERENCE_KEY_IS_USE_CATEGORY_CARRYOVER = "is_use_category_carryover";
	public static final String PREFERENCE_KEY_BALANCE_CALC_METHOD = "balance_calculation_method";

	public static final String carryoverConditionPlusOnly = "plus_only";
	public static final String carryoverConditionBoth = "both";

	public static final String EMPTY = "";
	public static final int DEFAULT_START_DAY = 1;

	public static final int BALANCE_CALC_METHOD_NONE = 0;
	public static final int BALANCE_CALC_METHOD_INCOME_MINUS_EXPENSE = 1;
	public static final int BALANCE_CALC_METHOD_BUDGET_MINUS_EXPENSE = 2;
	public static final int BALANCE_CALC_METHOD_BUDGET_PLUS_INCOME_MINUS_EXPENSE = 3;
	public static final int DEFAULT_BALANCE_CALC_METHOD = BALANCE_CALC_METHOD_BUDGET_PLUS_INCOME_MINUS_EXPENSE;

}
