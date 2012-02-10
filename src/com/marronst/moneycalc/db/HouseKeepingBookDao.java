package com.marronst.moneycalc.db;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import android.text.format.DateFormat;

import com.marronst.moneycalc.consts.KakeiboConsts.KakeiboListViewType;
import com.marronst.moneycalc.entity.Category;
import com.marronst.moneycalc.entity.HouseKeepingBook;
import com.marronst.moneycalc.entity.Category.CnCategory;
import com.marronst.moneycalc.entity.Category.TnCategory;
import com.marronst.moneycalc.entity.HouseKeepingBook.CnHouseKeepingBook;
import com.marronst.moneycalc.entity.HouseKeepingBook.TnHouseKeepingBook;
import com.marronst.moneycalc.utils.KakeiboUtils;

public class HouseKeepingBookDao {

	protected final String TAG = this.getClass().getSimpleName();

	private SQLiteDatabase db;

	private static final SimpleDateFormat formatForDummyDateCreate = new SimpleDateFormat(
			"yyyy/MM/dd HH:mm:ss");

	@SuppressWarnings("unused")
	private HouseKeepingBookDao() {
		// 
	}

	public HouseKeepingBookDao(final SQLiteDatabase db) {
		this.db = db;
	}

	//  create table  ----------------------------------------

	/** テーブル作成  */
	public void createTable() {
		StringBuffer sb = new StringBuffer();
		sb.append(" create table " + TnHouseKeepingBook.tableName() + " (");
		sb.append(CnHouseKeepingBook.id() + " integer primary key autoincrement, ");
		sb.append(CnHouseKeepingBook.price() + " integer not null default 0, ");
		sb.append(CnHouseKeepingBook.memo() + " text not null default '', ");
		sb.append(CnHouseKeepingBook.place() + " text, ");
		sb.append(CnHouseKeepingBook.latitude() + " text, ");
		sb.append(CnHouseKeepingBook.longitude() + " text, ");
		sb.append(CnHouseKeepingBook.categoryId() + " integer not null default 0, ");
		sb.append(CnHouseKeepingBook.registerDate() + " text not null, ");
		sb.append(CnHouseKeepingBook.importVersion() + " integer ");
		sb.append(");");

		db.execSQL(sb.toString());
	}

	//  挿入  ----------------------------------------

	/** 家計簿レコードを挿入する */
	public void insert(final HouseKeepingBook houseKeepingBook) {
		ContentValues contentValues = new ContentValues();

		contentValues.put(CnHouseKeepingBook.price(), houseKeepingBook.price);
		contentValues.put(CnHouseKeepingBook.memo(), houseKeepingBook.memo);
		contentValues.put(CnHouseKeepingBook.place(), houseKeepingBook.place);
		contentValues.put(CnHouseKeepingBook.categoryId(), houseKeepingBook.categoryId);
		contentValues.put(CnHouseKeepingBook.registerDate(),
							formatRegisterDateToString(houseKeepingBook.registerDate));
		contentValues.put(CnHouseKeepingBook.latitude(), houseKeepingBook.latitude);
		contentValues.put(CnHouseKeepingBook.longitude(), houseKeepingBook.longitude);
		contentValues.put(CnHouseKeepingBook.importVersion(), houseKeepingBook.importVersion);
		db.insert(TnHouseKeepingBook.tableName(), null, contentValues);
	}

	//  検索  ----------------------------------------

	/** 全件検索 */
	public Cursor findAll() {
		Cursor c = db.query(TnHouseKeepingBook.tableName(), //
							null, null, null, null, null, CnHouseKeepingBook.id() + " asc");
		return c;
	}

	/**　IDをもとに検索　*/
	public Cursor findById(final Integer id) {
		final String[] selectionArgs = new String[] { id.toString() };
		Cursor c = db.query(TnHouseKeepingBook.tableName(),//
							null, CnHouseKeepingBook.id() + " = ?", selectionArgs, null, null, null);
		return c;

	}

	/**　年と月をもとに検索　*/
	public Cursor findByMonth(final Calendar currentDate, final KakeiboListViewType type,
			final List<Integer> catgoryIdList) {

		int year = currentDate.get(Calendar.YEAR);
		int month = currentDate.get(Calendar.MONTH) + 1;
		int day = currentDate.get(Calendar.DAY_OF_MONTH);
		String searchFormat = getSearchQuery(year, month, day);

		String oneInterval = getOneInterval(type);

		StringBuffer findByIdSql = new StringBuffer();
		findByIdSql.append(" select hkb.*, c." + CnCategory.categoryName() + ", c." + CnCategory.incomeFlg());
		findByIdSql.append(" from " + TnHouseKeepingBook.tableName() + " hkb");
		findByIdSql.append(" inner join " + TnCategory.tableName() + " c");
		findByIdSql.append(" on hkb." + CnHouseKeepingBook.categoryId() + " = c." + CnCategory.id());
		findByIdSql.append(" where hkb."//
				+ CnHouseKeepingBook.registerDate() + " >= datetime('" + searchFormat + "') ");
		findByIdSql.append("  and hkb."//
				+ CnHouseKeepingBook.registerDate() + " <  datetime('" + searchFormat + "','"
				+ oneInterval
				+ "') ");
		if (KakeiboUtils.isNotEmpty(catgoryIdList)) {
			findByIdSql.append("  and hkb." + CnHouseKeepingBook.categoryId() + " in ("
					+ KakeiboUtils.join(catgoryIdList, ",") + ")");
		}
		findByIdSql.append(" order by hkb." + CnHouseKeepingBook.registerDate() + " desc ");
		findByIdSql.append(";");

		//Log.i(TAG, findByIdSql.toString());
		Cursor c = db.rawQuery(findByIdSql.toString(), null);
		return c;
	}

	/** 登録日を開始日と終了日で検索する */
	public Cursor findByStartDateAndEndDate(final Date startDate, final Date endDate) {
		Calendar startCal = Calendar.getInstance();
		startCal.setTime(startDate);
		int sYear = startCal.get(Calendar.YEAR);
		int sMonth = startCal.get(Calendar.MONTH) + 1;
		int sDay = startCal.get(Calendar.DAY_OF_MONTH);
		String startSearchFormat = getSearchQuery(sYear, sMonth, sDay);

		Calendar endCal = Calendar.getInstance();
		endCal.setTime(endDate);
		endCal.add(Calendar.DAY_OF_MONTH, 1);
		int eYear = endCal.get(Calendar.YEAR);
		int eMonth = endCal.get(Calendar.MONTH) + 1;
		int eDay = endCal.get(Calendar.DAY_OF_MONTH);
		String endSearchFormat = getSearchQuery(eYear, eMonth, eDay);

		StringBuffer findByIdSql = new StringBuffer();
		findByIdSql.append(" select hkb.*, c." + CnCategory.categoryName() + ", c." + CnCategory.incomeFlg());
		findByIdSql.append(" from " + TnHouseKeepingBook.tableName() + " hkb");
		findByIdSql.append(" inner join " + TnCategory.tableName() + " c");
		findByIdSql.append(" on hkb." + CnHouseKeepingBook.categoryId() + " = c." + CnCategory.id());
		findByIdSql.append(" where hkb."//
				+ CnHouseKeepingBook.registerDate() + " >= datetime('" + startSearchFormat + "') ");
		findByIdSql.append("  and hkb."//
				+ CnHouseKeepingBook.registerDate() + " <  datetime('" + endSearchFormat + "') ");
		findByIdSql.append(" order by hkb." + CnHouseKeepingBook.registerDate() + " asc ");
		findByIdSql.append(";");

		//Log.i(TAG, findByIdSql.toString());
		Cursor c = db.rawQuery(findByIdSql.toString(), null);
		return c;
	}

	public Integer findPreviousMonthTotalPriceByMonth(final Calendar baseCal, final Calendar previousMonth,
			final List<Integer> categoryIdList, final Integer incomeFlg) {
		int previousMonthTotalPrice = 0;
		if (incomeFlg == null || Category.INCOME_FLG_OFF == incomeFlg) {
			previousMonthTotalPrice = getPreviousMonthTotal(baseCal, previousMonth, Category.INCOME_FLG_OFF,
															categoryIdList);//支出
		}
		int previousMonthTotalIncomePrice = 0;
		if (incomeFlg == null || Category.INCOME_FLG_ON == incomeFlg) {
			previousMonthTotalIncomePrice = getPreviousMonthTotal(baseCal, previousMonth,
																	Category.INCOME_FLG_ON, categoryIdList);//収入
		}
		return previousMonthTotalPrice - previousMonthTotalIncomePrice;
	}

	private int getPreviousMonthTotal(final Calendar baseCal, final Calendar endMonth, final int incomeFlg,
			final List<Integer> categoryIdList) {

		String startSearchFormat = "";
		if (baseCal != null) {
			int year = baseCal.get(Calendar.YEAR);
			int month = baseCal.get(Calendar.MONTH) + 1;
			int day = baseCal.get(Calendar.DAY_OF_MONTH);
			startSearchFormat = getSearchQuery(year, month, day);
		}
		int endYear = endMonth.get(Calendar.YEAR);
		int endMonth2 = endMonth.get(Calendar.MONTH) + 1;
		int endDay = endMonth.get(Calendar.DAY_OF_MONTH);
		String endSearchFormat = getSearchQuery(endYear, endMonth2, endDay);

		StringBuffer findByIdSql = new StringBuffer();
		findByIdSql.append(" select sum(hkb.price)");
		findByIdSql.append(" from " + TnHouseKeepingBook.tableName() + " hkb");
		findByIdSql.append(" inner join " + TnCategory.tableName() + " c");
		findByIdSql.append(" on hkb." + CnHouseKeepingBook.categoryId() + " = c." + CnCategory.id());
		findByIdSql.append(" where ");
		if (!TextUtils.isEmpty(startSearchFormat)) {
			findByIdSql.append(" hkb."//
					+ CnHouseKeepingBook.registerDate() + " >= datetime('" + startSearchFormat + "') ");
			findByIdSql.append(" and ");
		}
		findByIdSql.append(" hkb."//
				+ CnHouseKeepingBook.registerDate() + " <  datetime('" + endSearchFormat + "') ");
		findByIdSql.append("  and c." + CnCategory.incomeFlg() + " = " + incomeFlg);
		if (KakeiboUtils.isNotEmpty(categoryIdList)) {
			findByIdSql.append("  and c." + CnCategory.id() + " in ("
					+ KakeiboUtils.join(categoryIdList, ",") + ")");
		}
		findByIdSql.append(";");

		Cursor c = db.rawQuery(findByIdSql.toString(), null);
		c.moveToNext();
		int previousMonthTotalPrice = c.getInt(0);
		c.close();
		return previousMonthTotalPrice;
	}

	private String getSearchQuery(final int year, final int month, final int day) {

		final String hyphen = "-";
		StringBuilder builder = new StringBuilder();
		builder.append(year);
		builder.append(hyphen);
		if (month < 10) {
			builder.append(0).append(month);
		} else {
			builder.append(month);
		}
		builder.append(hyphen);
		if (day < 10) {
			builder.append(0).append(day);
		} else {
			builder.append(day);
		}
		builder.append(" 00:00:00");

		return builder.toString();
	}

	/** 数ヶ月間のレコードを検索する。ただし、位置情報があるものに限る */
	//削除する？
	public Cursor findRecordOfSeveralMonths(final Calendar calendar_, final int interval) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(calendar_.getTime());
		calendar.add(Calendar.MONTH, -1);
		int currentMonth = calendar.get(Calendar.MONTH) + 1;
		int currentYear = calendar.get(Calendar.YEAR);
		int currentDay = calendar.get(Calendar.DAY_OF_MONTH);

		String searchFormat = getSearchQuery(currentYear, currentMonth, currentDay);
		StringBuffer findByIdSql = new StringBuffer();
		findByIdSql.append(" select * from " + TnHouseKeepingBook.tableName());
		findByIdSql.append(" where "//
				+ CnHouseKeepingBook.registerDate() + " >= datetime('" + searchFormat + "') ");
		findByIdSql.append(" and " + CnHouseKeepingBook.latitude() + "  is not null");
		findByIdSql.append(" and " + CnHouseKeepingBook.latitude() + "  != ''");
		findByIdSql.append(" and " + CnHouseKeepingBook.longitude() + " is not null");
		findByIdSql.append(" and " + CnHouseKeepingBook.longitude() + " != ''");
		findByIdSql.append(" order by " + CnHouseKeepingBook.id() + " desc ");
		findByIdSql.append(";");

		Cursor c = db.rawQuery(findByIdSql.toString(), null);
		return c;
	}

	//  更新  ----------------------------------------

	/**　家計簿レコードの更新 */
	public void update(final HouseKeepingBook houseKeepingBook) {

		ContentValues contentValues = new ContentValues();
		contentValues.put(CnHouseKeepingBook.price(), houseKeepingBook.price);
		contentValues.put(CnHouseKeepingBook.memo(), houseKeepingBook.memo);
		contentValues.put(CnHouseKeepingBook.place(), houseKeepingBook.place);
		contentValues.put(CnHouseKeepingBook.categoryId(), houseKeepingBook.categoryId);
		contentValues.put(CnHouseKeepingBook.registerDate(),
							formatRegisterDateToString(houseKeepingBook.registerDate));
		db.update(TnHouseKeepingBook.tableName(), contentValues, CnHouseKeepingBook.id() + " = ?",
					new String[] { houseKeepingBook.id.toString() });
	}

	//  削除  ----------------------------------------

	/**　IDをもとにレコードの削除 */
	public void deleteById(final Integer id) {

		StringBuffer deleteByIdSql = new StringBuffer();
		deleteByIdSql.append(" delete from " + TnHouseKeepingBook.tableName());
		deleteByIdSql.append(" where " + CnHouseKeepingBook.id() + " = " + id);
		deleteByIdSql.append(";");

		db.execSQL(deleteByIdSql.toString());
	}

	/**　カテゴリIDをもとにレコードの削除 */
	public void deleteByCategoryId(final Integer categoryId) {

		StringBuffer deleteByIdSql = new StringBuffer();
		deleteByIdSql.append(" delete from " + TnHouseKeepingBook.tableName());
		deleteByIdSql.append(" where " + CnHouseKeepingBook.categoryId() + " = " + categoryId);
		deleteByIdSql.append(";");

		db.execSQL(deleteByIdSql.toString());
	}

	/**
	 * 全削除
	 */
	public void deleteAll() {
		db.delete(TnHouseKeepingBook.tableName(), null, null);
	}

	//　内部メソッド ----------------------------------------

	/**　Date型を日付検索用文字列に変換する */
	private String formatRegisterDateToString(final Date date) {
		return (String) DateFormat.format("yyyy-MM-dd kk:mm:ss", date);
	}

	private String getOneInterval(final KakeiboListViewType type) {
		String unit;
		switch (type) {
			case YEAR:
				unit = "+1 year";
				break;
			case HALF_YEAR:
				unit = "+6 month";
				break;
			case THREE_MONTH:
				unit = "+3 month";
				break;
			case MONTH:
				unit = "+1 month";
				break;
			case WEEK:
				unit = "+7 day";
				break;
			case DAY:
				unit = "+1 day";
				break;
			default:
				unit = "+1 month";
				break;
		}

		return unit;
	}

	/** レコードを挿入する(簡易版) */
	private void insertHouseKeepingBook(final int categoryId, final int price, final Date registerDate,
			final String memo) {
		ContentValues values;
		values = new ContentValues();
		values.put(CnHouseKeepingBook.categoryId(), categoryId);
		values.put(CnHouseKeepingBook.price(), price);
		values.put(CnHouseKeepingBook.registerDate(), formatRegisterDateToString(registerDate));
		values.put(CnHouseKeepingBook.memo(), memo);
		db.insert(TnHouseKeepingBook.tableName(), null, values);
	}

	public void createDummyData() {

		insertHouseKeepingBook(1, 1000, toDate(new Date(), 0, 0, 0, 0), "");
		insertHouseKeepingBook(1, 10000, toDate(new Date(), 0, 0, 0, -1), "");
		insertHouseKeepingBook(1, 2000, toDate(new Date(), 0, 0, 0, -2), "");
		insertHouseKeepingBook(1, 1200, toDate(new Date(), 0, 0, 0, -3), "");
		insertHouseKeepingBook(2, 1500, toDate(new Date(), 0, 0, 0, -4), "うむむ");
		insertHouseKeepingBook(3, 1300, toDate(new Date(), 0, 0, 0, -6), "うわわ");
		insertHouseKeepingBook(4, 41000, toDate(new Date(), 0, 0, 0, -7), "");
		insertHouseKeepingBook(5, 500, toDate(new Date(), 0, 0, 0, -8), "");
		insertHouseKeepingBook(6, 600, toDate(new Date(), 0, 0, 0, -9), "あわわわ…");
		insertHouseKeepingBook(7, 70, toDate(new Date(), 0, 0, 0, -10), "");
		insertHouseKeepingBook(8, 80, toDate(new Date(), 0, 0, 0, -11), "特別に購入したの");
		insertHouseKeepingBook(9, 900, toDate(new Date(), 0, 0, 0, -12), "");
		insertHouseKeepingBook(10, 1000, toDate(new Date(), 0, 0, 0, -13), "安かったから");
		insertHouseKeepingBook(11, 1100, toDate(new Date(), 0, 0, -1, 0), "");
		insertHouseKeepingBook(12, 120, toDate(new Date(), 0, 0, -2, 0), "");
		insertHouseKeepingBook(1, 1200, toDate(new Date(), 0, 0, -3, 0), "");
		insertHouseKeepingBook(2, 1500, toDate(new Date(), 0, 0, -4, 0), "うむむ");
		insertHouseKeepingBook(3, 1300, toDate(new Date(), 0, 0, -5, 0), "うわわ");
		insertHouseKeepingBook(4, 41000, toDate(new Date(), 0, 0, -6, 0), "");
		insertHouseKeepingBook(5, 500, toDate(new Date(), 0, 0, -7, 0), "");
		insertHouseKeepingBook(6, 600, toDate(new Date(), 0, 0, -8, 0), "あわわわ…");
		insertHouseKeepingBook(7, 70, toDate(new Date(), 0, 0, -9, 0), "");
		insertHouseKeepingBook(8, 80, toDate(new Date(), 0, 0, -10, 0), "特別に購入したの");
		insertHouseKeepingBook(9, 900, toDate(new Date(), 0, 0, -11, 0), "");
		insertHouseKeepingBook(10, 1000, toDate(new Date(), 0, 0, -12, 0), "安かったから");
		insertHouseKeepingBook(11, 1100, toDate(new Date(), 0, 0, -13, 0), "");
		insertHouseKeepingBook(12, 120, toDate(new Date(), 0, 0, -14, 0), "");

		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM");
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MONTH, -1);
		createDataOneMonth(simpleDateFormat.format(calendar.getTime()));
		calendar.add(Calendar.MONTH, -1);
		createDataOneMonth(simpleDateFormat.format(calendar.getTime()));
		calendar.add(Calendar.MONTH, -1);
		createDataOneMonth(simpleDateFormat.format(calendar.getTime()));

		//
		insertHouseKeepingBook(1, 1000, toDate("2010/07/01 10:00:00"), "");
		insertHouseKeepingBook(1, 10000, toDate("2010/07/02 11:00:00"), "");
		insertHouseKeepingBook(1, 2000, toDate("2010/06/02 12:00:00"), "");
		insertHouseKeepingBook(1, 1200, toDate("2010/06/01 13:00:00"), "");
		insertHouseKeepingBook(2, 1500, toDate("2010/06/04 11:00:00"), "うむむ");
		insertHouseKeepingBook(3, 1300, toDate("2010/07/04 12:00:00"), "うわわ");
		insertHouseKeepingBook(4, 41000, toDate("2010/06/12 13:00:00"), "");
		insertHouseKeepingBook(5, 500, toDate("2010/06/01 14:00:00"), "");
		insertHouseKeepingBook(6, 600, toDate("2010/06/04 15:00:00"), "あわわわ…");
		insertHouseKeepingBook(7, 70, toDate("2010/07/04 16:00:00"), "");
		insertHouseKeepingBook(8, 80, toDate("2010/06/02 17:00:00"), "特別に購入したの");
		insertHouseKeepingBook(9, 900, toDate("2010/06/08 18:00:00"), "");
		insertHouseKeepingBook(10, 1000, toDate("2010/06/11 19:00:00"), "安かったから");
		insertHouseKeepingBook(11, 1100, toDate("2010/07/01 20:00:00"), "");
		insertHouseKeepingBook(12, 120, toDate("2010/07/08 22:00:00"), "");
		insertHouseKeepingBook(1, 1200, toDate("2010/06/12 13:00:00"), "");
		insertHouseKeepingBook(2, 1500, toDate("2010/07/01 11:00:00"), "うむむ");
		insertHouseKeepingBook(3, 1300, toDate("2010/06/08 12:00:00"), "うわわ");
		insertHouseKeepingBook(4, 41000, toDate("2010/06/11 13:00:00"), "");
		insertHouseKeepingBook(5, 500, toDate("2010/07/08 14:00:00"), "");
		insertHouseKeepingBook(6, 600, toDate("2010/06/02 15:00:00"), "あわわわ…");
		insertHouseKeepingBook(7, 70, toDate("2010/06/11 16:00:00"), "");
		insertHouseKeepingBook(8, 80, toDate("2010/07/11 17:00:00"), "特別に購入したの");
		insertHouseKeepingBook(9, 900, toDate("2010/06/02 18:00:00"), "");
		insertHouseKeepingBook(10, 1000, toDate("2010/07/04 19:00:00"), "安かったから");
		insertHouseKeepingBook(11, 1100, toDate("2010/06/04 20:00:00"), "");
		insertHouseKeepingBook(12, 120, toDate("2010/07/01 22:00:00"), "");

		insertHouseKeepingBook(1, 1000, toDate("2010/07/15 10:00:00"), "");
		insertHouseKeepingBook(1, 10000, toDate("2010/06/18 11:00:00"), "");
		insertHouseKeepingBook(1, 2000, toDate("2010/06/26 12:00:00"), "");
		insertHouseKeepingBook(1, 1200, toDate("2010/06/26 13:00:00"), "");
		insertHouseKeepingBook(2, 1500, toDate("2010/06/26 11:00:00"), "うむむ");
		insertHouseKeepingBook(3, 1300, toDate("2010/06/21 12:00:00"), "うわわ");
		insertHouseKeepingBook(4, 41000, toDate("2010/06/12 13:00:00"), "");
		insertHouseKeepingBook(5, 500, toDate("2010/07/27 14:00:00"), "");
		insertHouseKeepingBook(6, 600, toDate("2010/07/18 15:00:00"), "あわわわ…");
		insertHouseKeepingBook(7, 70, toDate("2010/08/21 16:00:00"), "");
		insertHouseKeepingBook(8, 80, toDate("2010/07/26 17:00:00"), "特別に購入したの");
		insertHouseKeepingBook(9, 900, toDate("2010/06/27 18:00:00"), "");
		insertHouseKeepingBook(10, 1000, toDate("2010/06/11 19:00:00"), "安かったから");
		insertHouseKeepingBook(11, 1100, toDate("2010/06/18 20:00:00"), "");
		insertHouseKeepingBook(12, 120, toDate("2010/06/18 22:00:00"), "");
		insertHouseKeepingBook(1, 1200, toDate("2010/06/15 13:00:00"), "");
		insertHouseKeepingBook(2, 1500, toDate("2010/06/27 11:00:00"), "うむむ");
		insertHouseKeepingBook(3, 1300, toDate("2010/06/27 12:00:00"), "うわわ");
		insertHouseKeepingBook(4, 41000, toDate("2010/06/27 13:00:00"), "");
		insertHouseKeepingBook(5, 500, toDate("2010/07/27 14:00:00"), "");
		insertHouseKeepingBook(6, 600, toDate("2010/06/07 15:00:00"), "あわわわ…");
		insertHouseKeepingBook(7, 70, toDate("2010/07/07 16:00:00"), "");
		insertHouseKeepingBook(8, 80, toDate("2010/07/07 17:00:00"), "特別に購入したの");
		insertHouseKeepingBook(9, 900, toDate("2010/07/21 18:00:00"), "");
		insertHouseKeepingBook(10, 1000, toDate("2010/07/21 19:00:00"), "安かったから");
		insertHouseKeepingBook(11, 1100, toDate("2010/07/15 20:00:00"), "");
		insertHouseKeepingBook(12, 120, toDate("2010/07/15 22:00:00"), "");

		//
		insertHouseKeepingBook(1, 1000, toDate("2010/05/01 10:00:00"), "");
		insertHouseKeepingBook(1, 10000, toDate("2010/05/02 11:00:00"), "");
		insertHouseKeepingBook(1, 2000, toDate("2010/04/02 12:00:00"), "");
		insertHouseKeepingBook(1, 1200, toDate("2010/04/01 13:00:00"), "");
		insertHouseKeepingBook(2, 1500, toDate("2010/03/04 11:00:00"), "うむむ");
		insertHouseKeepingBook(3, 1300, toDate("2010/03/04 12:00:00"), "うわわ");
		insertHouseKeepingBook(4, 41000, toDate("2010/02/12 13:00:00"), "");
		insertHouseKeepingBook(5, 500, toDate("2010/02/01 14:00:00"), "");
		insertHouseKeepingBook(6, 600, toDate("2010/01/04 15:00:00"), "あわわわ…");
		insertHouseKeepingBook(7, 70, toDate("2010/01/04 16:00:00"), "");
		insertHouseKeepingBook(8, 80, toDate("2010/02/02 17:00:00"), "特別に購入したの");
		insertHouseKeepingBook(9, 900, toDate("2010/04/08 18:00:00"), "");
		insertHouseKeepingBook(10, 1000, toDate("2010/02/11 19:00:00"), "安かったから");

	}

	private void createDataOneMonth(final String createYYYYMM) {
		insertHouseKeepingBook(1, 1000, toDate(createYYYYMM + "/31 10:00:00"), "");
		insertHouseKeepingBook(1, 10000, toDate(createYYYYMM + "/31 11:00:00"), "");
		insertHouseKeepingBook(1, 2000, toDate(createYYYYMM + "/31 12:00:00"), "");
		insertHouseKeepingBook(1, 1200, toDate(createYYYYMM + "/31 13:00:00"), "");
		insertHouseKeepingBook(2, 1500, toDate(createYYYYMM + "/31 11:00:00"), "うむむ");
		insertHouseKeepingBook(3, 1300, toDate(createYYYYMM + "/31 12:00:00"), "うわわ");
		insertHouseKeepingBook(4, 41000, toDate(createYYYYMM + "/31 13:00:00"), "");
		insertHouseKeepingBook(5, 500, toDate(createYYYYMM + "/31 14:00:00"), "");
		insertHouseKeepingBook(6, 600, toDate(createYYYYMM + "/31 15:00:00"), "あわわわ…");
		insertHouseKeepingBook(7, 70, toDate(createYYYYMM + "/31 16:00:00"), "");
		insertHouseKeepingBook(8, 80, toDate(createYYYYMM + "/31 17:00:00"), "特別に購入したの");
		insertHouseKeepingBook(9, 900, toDate(createYYYYMM + "/31 18:00:00"), "");
		insertHouseKeepingBook(10, 1000, toDate(createYYYYMM + "/31 19:00:00"), "安かったから");
		insertHouseKeepingBook(11, 1100, toDate(createYYYYMM + "/31 20:00:00"), "");
		insertHouseKeepingBook(12, 120, toDate(createYYYYMM + "/31 22:00:00"), "");
		insertHouseKeepingBook(1, 1200, toDate(createYYYYMM + "/31 13:00:00"), "");
		insertHouseKeepingBook(2, 1500, toDate(createYYYYMM + "/31 11:00:00"), "うむむ");
		insertHouseKeepingBook(3, 1300, toDate(createYYYYMM + "/31 12:00:00"), "うわわ");
		insertHouseKeepingBook(4, 41000, toDate(createYYYYMM + "/31 13:00:00"), "");
		insertHouseKeepingBook(5, 500, toDate(createYYYYMM + "/31 14:00:00"), "");
		insertHouseKeepingBook(6, 600, toDate(createYYYYMM + "/31 15:00:00"), "あわわわ…");
		insertHouseKeepingBook(7, 70, toDate(createYYYYMM + "/31 16:00:00"), "");
		insertHouseKeepingBook(8, 80, toDate(createYYYYMM + "/31 17:00:00"), "特別に購入したの");
		insertHouseKeepingBook(9, 900, toDate(createYYYYMM + "/31 18:00:00"), "");
		insertHouseKeepingBook(10, 1000, toDate(createYYYYMM + "/31 19:00:00"), "安かったから");
		insertHouseKeepingBook(11, 1100, toDate(createYYYYMM + "/31 20:00:00"), "");
		insertHouseKeepingBook(12, 120, toDate(createYYYYMM + "/31 22:00:00"), "");

		insertHouseKeepingBook(1, 1000, toDate(createYYYYMM + "/01 10:00:00"), "");
		insertHouseKeepingBook(1, 10000, toDate(createYYYYMM + "/02 11:00:00"), "");
		insertHouseKeepingBook(1, 2000, toDate(createYYYYMM + "/02 12:00:00"), "");
		insertHouseKeepingBook(1, 1200, toDate(createYYYYMM + "/01 13:00:00"), "");
		insertHouseKeepingBook(2, 1500, toDate(createYYYYMM + "/04 11:00:00"), "うむむ");
		insertHouseKeepingBook(3, 1300, toDate(createYYYYMM + "/04 12:00:00"), "うわわ");
		insertHouseKeepingBook(4, 41000, toDate(createYYYYMM + "/12 13:00:00"), "");
		insertHouseKeepingBook(5, 500, toDate(createYYYYMM + "/01 14:00:00"), "");
		insertHouseKeepingBook(6, 600, toDate(createYYYYMM + "/04 15:00:00"), "あわわわ…");
		insertHouseKeepingBook(7, 70, toDate(createYYYYMM + "/04 16:00:00"), "");
		insertHouseKeepingBook(8, 80, toDate(createYYYYMM + "/02 17:00:00"), "特別に購入したの");
		insertHouseKeepingBook(9, 900, toDate(createYYYYMM + "/08 18:00:00"), "");
		insertHouseKeepingBook(10, 1000, toDate(createYYYYMM + "/11 19:00:00"), "安かったから");
		insertHouseKeepingBook(11, 1100, toDate(createYYYYMM + "/01 20:00:00"), "");
		insertHouseKeepingBook(12, 120, toDate(createYYYYMM + "/08 22:00:00"), "");
		insertHouseKeepingBook(1, 1200, toDate(createYYYYMM + "/12 13:00:00"), "");
		insertHouseKeepingBook(2, 1500, toDate(createYYYYMM + "/01 11:00:00"), "うむむ");
		insertHouseKeepingBook(3, 1300, toDate(createYYYYMM + "/08 12:00:00"), "うわわ");
		insertHouseKeepingBook(4, 41000, toDate(createYYYYMM + "/11 13:00:00"), "");
		insertHouseKeepingBook(5, 500, toDate(createYYYYMM + "/08 14:00:00"), "");
		insertHouseKeepingBook(6, 600, toDate(createYYYYMM + "/02 15:00:00"), "あわわわ…");
		insertHouseKeepingBook(7, 70, toDate(createYYYYMM + "/11 16:00:00"), "");
		insertHouseKeepingBook(8, 80, toDate(createYYYYMM + "/11 17:00:00"), "特別に購入したの");
		insertHouseKeepingBook(9, 900, toDate(createYYYYMM + "/02 18:00:00"), "");
		insertHouseKeepingBook(10, 1000, toDate(createYYYYMM + "/04 19:00:00"), "安かったから");
		insertHouseKeepingBook(11, 1100, toDate(createYYYYMM + "/04 20:00:00"), "");
		insertHouseKeepingBook(12, 120, toDate(createYYYYMM + "/01 22:00:00"), "");

		insertHouseKeepingBook(1, 1000, toDate(createYYYYMM + "/15 10:00:00"), "");
		insertHouseKeepingBook(1, 10000, toDate(createYYYYMM + "/18 11:00:00"), "");
		insertHouseKeepingBook(1, 2000, toDate(createYYYYMM + "/26 12:00:00"), "");
		insertHouseKeepingBook(1, 1200, toDate(createYYYYMM + "/26 13:00:00"), "");
		insertHouseKeepingBook(2, 1500, toDate(createYYYYMM + "/26 11:00:00"), "うむむ");
		insertHouseKeepingBook(3, 1300, toDate(createYYYYMM + "/21 12:00:00"), "うわわ");
		insertHouseKeepingBook(4, 41000, toDate(createYYYYMM + "/12 13:00:00"), "");
		insertHouseKeepingBook(5, 500, toDate(createYYYYMM + "/27 14:00:00"), "");
		insertHouseKeepingBook(6, 600, toDate(createYYYYMM + "/18 15:00:00"), "あわわわ…");
		insertHouseKeepingBook(7, 70, toDate(createYYYYMM + "/21 16:00:00"), "");
		insertHouseKeepingBook(8, 80, toDate(createYYYYMM + "/26 17:00:00"), "特別に購入したの");
		insertHouseKeepingBook(9, 900, toDate(createYYYYMM + "/27 18:00:00"), "");
		insertHouseKeepingBook(10, 1000, toDate(createYYYYMM + "/11 19:00:00"), "安かったから");
		insertHouseKeepingBook(11, 1100, toDate(createYYYYMM + "/18 20:00:00"), "");
		insertHouseKeepingBook(12, 120, toDate(createYYYYMM + "/18 22:00:00"), "");
		insertHouseKeepingBook(1, 1200, toDate(createYYYYMM + "/15 13:00:00"), "");
		insertHouseKeepingBook(2, 1500, toDate(createYYYYMM + "/27 11:00:00"), "うむむ");
		insertHouseKeepingBook(3, 1300, toDate(createYYYYMM + "/27 12:00:00"), "うわわ");
		insertHouseKeepingBook(4, 41000, toDate(createYYYYMM + "/27 13:00:00"), "");
		insertHouseKeepingBook(5, 500, toDate(createYYYYMM + "/27 14:00:00"), "");
		insertHouseKeepingBook(6, 600, toDate(createYYYYMM + "/07 15:00:00"), "あわわわ…");
		insertHouseKeepingBook(7, 70, toDate(createYYYYMM + "/07 16:00:00"), "");
		insertHouseKeepingBook(8, 80, toDate(createYYYYMM + "/07 17:00:00"), "特別に購入したの");
		insertHouseKeepingBook(9, 900, toDate(createYYYYMM + "/21 18:00:00"), "");
		insertHouseKeepingBook(10, 1000, toDate(createYYYYMM + "/21 19:00:00"), "安かったから");
		insertHouseKeepingBook(11, 1100, toDate(createYYYYMM + "/15 20:00:00"), "");
		insertHouseKeepingBook(12, 120, toDate(createYYYYMM + "/15 22:00:00"), "");
	}

	private Date toDate(final String str) {
		try {
			return formatForDummyDateCreate.parse(str);
		} catch (ParseException e) {
			e.printStackTrace();
			throw new AssertionError();
		}
	}

	private Date toDate(final Date date, final int year, final int month, final int day, final int hour) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);

		cal.add(Calendar.YEAR, year);
		cal.add(Calendar.MONTH, month);
		cal.add(Calendar.DAY_OF_MONTH, day);
		cal.add(Calendar.HOUR_OF_DAY, hour);

		return cal.getTime();

	}

}
