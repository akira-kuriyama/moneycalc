package com.marronst.moneycalc.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.marronst.moneycalc.entity.HouseKeepingBook.CnHouseKeepingBook;
import com.marronst.moneycalc.entity.HouseKeepingBook.TnHouseKeepingBook;

public class HouseKeepingBookOpenHelper extends SQLiteOpenHelper {

	private static final int DB_VERSION = 3;

	public static final String DB_NAME = "housekeeping_book.db";

	public HouseKeepingBookOpenHelper(final Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(final SQLiteDatabase db) {

		HouseKeepingBookDao houseKeepingBookDao = new HouseKeepingBookDao(db);
		houseKeepingBookDao.createTable();

		CategoryDao categoryDao = new CategoryDao(db);
		categoryDao.createTable();
		categoryDao.initCategoryTable();

		BudgetDao budgetDao = new BudgetDao(db);
		budgetDao.createTable();

		FilterDao filterDao = new FilterDao(db);
		filterDao.createTable();

		//ダミーデータを作成するかどうか
		boolean isCreateDummyData = false;

		if (isCreateDummyData) {
			categoryDao.createDummyData();
			houseKeepingBookDao.createDummyData();
		}
	}

	@Override
	public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {

		if (oldVersion < 2) {
			db.execSQL("alter table " + TnHouseKeepingBook.tableName() + " add column "
					+ CnHouseKeepingBook.importVersion() + " integer");
		}
		if (oldVersion < 3) {
			FilterDao filterDao = new FilterDao(db);
			filterDao.createTable();
		}
	}
}
