package com.marronst.moneycalc.db;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.marronst.moneycalc.dxo.FilterDxo;
import com.marronst.moneycalc.entity.Filter;
import com.marronst.moneycalc.entity.Filter.CnFilter;
import com.marronst.moneycalc.entity.Filter.TnFilter;

/** フィルターDao  */
public class FilterDao {

	private SQLiteDatabase db;

	private final FilterDxo filterDxo = new FilterDxo();

	@SuppressWarnings("unused")
	private FilterDao() {
		//
	}

	public FilterDao(final SQLiteDatabase db) {
		this.db = db;
	}

	//  create table  ----------------------------------------

	/** テーブル作成  */
	public void createTable() {
		StringBuffer sb = new StringBuffer();
		sb.append(" create table " + TnFilter.tableName() + " (");
		sb.append(CnFilter.id() + " integer primary key autoincrement, ");
		sb.append(CnFilter.filterName() + " text not null, ");
		sb.append(CnFilter.categoryIdList() + " text , ");
		sb.append(CnFilter.isDefault() + " integer not null default 0 ");
		sb.append(");");

		db.execSQL(sb.toString());
	}

	//  挿入  ----------------------------------------

	/** 予算レコードを挿入する */
	public void insert(final Filter filter) {
		ContentValues values = new ContentValues();
		values = new ContentValues();
		values.put(CnFilter.categoryIdList(), filter.categoryIdList);
		values.put(CnFilter.filterName(), filter.filterName);
		values.put(CnFilter.isDefault(), filter.isDefault);
		db.insert(TnFilter.tableName(), null, values);
	}

	//  更新  ----------------------------------------

	/** 予算レコードを更新する */
	public void update(final Filter filter) {

		ContentValues contentValues = new ContentValues();
		contentValues.put(CnFilter.filterName(), filter.filterName);
		contentValues.put(CnFilter.categoryIdList(), filter.categoryIdList);
		contentValues.put(CnFilter.isDefault(), filter.isDefault);

		db.update(TnFilter.tableName(),//
					contentValues,//
					CnFilter.id() + " = ?",//
					new String[] { filter.id.toString() });
	}

	/** デフォルトフラグを全てOFFに更新する */
	public void updateAllDefaultFlgOff() {

		ContentValues contentValues = new ContentValues();
		contentValues.put(CnFilter.isDefault(), Filter.DEFAULT_FLG_OFF.toString());

		db.update(TnFilter.tableName(),//
					contentValues,//
					CnFilter.isDefault() + " = ?",//
					new String[] { Filter.DEFAULT_FLG_ON.toString() });
	}

	//  検索  ----------------------------------------

	public List<Filter> findAll() {
		ArrayList<Filter> filterList = new ArrayList<Filter>();

		String orderBy = CnFilter.id() + " asc";
		Cursor c = db.query(TnFilter.tableName(), null, null, null, null, null, orderBy);
		while (c.moveToNext()) {
			filterList.add(filterDxo.createFromCursol(c));
		}
		c.close();
		return filterList;
	}

	/**　フィルターIDで検索*/
	public Filter findById(final Integer id) {

		String whereQuery = CnFilter.id() + " = ? ";
		Cursor c = db.query(TnFilter.tableName(), null, whereQuery, new String[] { id.toString() }, null,
							null, null);
		Filter filter = null;
		while (c.moveToNext()) {
			filter = filterDxo.createFromCursol(c);
		}
		c.close();
		return filter;
	}

	/**　デフォルトのフィルターを検索*/
	public Filter findDefaultFilter() {

		String whereQuery = CnFilter.isDefault() + " = ? ";
		Cursor c = db.query(TnFilter.tableName(), null, whereQuery, new String[] { Filter.DEFAULT_FLG_ON
				.toString() }, null, null, null);
		Filter filter = null;
		while (c.moveToNext()) {
			filter = filterDxo.createFromCursol(c);
		}
		c.close();
		return filter;
	}

	//  削除  ----------------------------------------

	/**　IDをもとに削除　*/
	public void deleteById(final Integer id) {

		db.delete(TnFilter.tableName(), CnFilter.id() + " = ?", new String[] { id.toString() });

	}

	/**
	 * 全削除
	 */
	public void deleteAll() {
		db.delete(TnFilter.tableName(), null, null);
	}

}
