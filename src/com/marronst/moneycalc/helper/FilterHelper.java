package com.marronst.moneycalc.helper;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.text.TextUtils;
import android.util.Log;

import com.marronst.moneycalc.R;
import com.marronst.moneycalc.db.FilterDao;
import com.marronst.moneycalc.db.HouseKeepingBookOpenHelper;
import com.marronst.moneycalc.entity.Filter;
import com.marronst.moneycalc.utils.KakeiboUtils;

public class FilterHelper {

	protected final String TAG = this.getClass().getSimpleName();

	/** デフォルトフィルターを取得する */
	public Filter getDefaultFilter(final Context context) {

		HouseKeepingBookOpenHelper houseKeepingBookOpenHelper = new HouseKeepingBookOpenHelper(context);
		Filter defaultFilter = null;
		try {
			SQLiteDatabase db = houseKeepingBookOpenHelper.getReadableDatabase();
			FilterDao filterDao = new FilterDao(db);
			defaultFilter = filterDao.findDefaultFilter();
			db.close();
		} catch (SQLiteException e) {
			Log.e(TAG, "getWritableDatabase時にエラー", e);
			throw e;
		}

		houseKeepingBookOpenHelper.close();

		return defaultFilter;
	}

	/** 全フィルターを取得する */
	public List<Filter> getAllFilterList(final Context context) {

		List<Filter> filterList = new ArrayList<Filter>();
		HouseKeepingBookOpenHelper houseKeepingBookOpenHelper = new HouseKeepingBookOpenHelper(context);

		try {
			SQLiteDatabase db = houseKeepingBookOpenHelper.getReadableDatabase();
			FilterDao filterDao = new FilterDao(db);
			List<Filter> result = filterDao.findAll();
			filterList = result;
			db.close();
		} catch (SQLiteException e) {
			Log.e(TAG, "getWritableDatabase時にエラー", e);
			throw e;
		}

		houseKeepingBookOpenHelper.close();

		return filterList;
	}

	/** フィルター指定なしを生成し返す */
	public Filter getFilterNone(final Context context) {
		Filter filter = new Filter();
		filter.id = Filter.FILTER_NONE;
		filter.filterName = context.getResources().getString(R.string.filter_none);
		return filter;
	}

	public void updateFilterForCategoryDeleted(final Context context, final Integer deleteCategoryId) {
		HouseKeepingBookOpenHelper houseKeepingBookOpenHelper = new HouseKeepingBookOpenHelper(context);
		try {
			SQLiteDatabase db = houseKeepingBookOpenHelper.getReadableDatabase();
			FilterDao filterDao = new FilterDao(db);
			List<Filter> filterList = filterDao.findAll();

			for (Filter filter : filterList) {
				String categoryIdStrList = filter.categoryIdList;
				if (TextUtils.isEmpty(categoryIdStrList)) {
					continue;
				}
				List<Integer> categoryIdList = new ArrayList<Integer>();
				String[] split = categoryIdStrList.split(",");
				for (String categoryIdStr : split) {

					if (!categoryIdStr.equals(deleteCategoryId.toString())) {
						categoryIdList.add(Integer.parseInt(categoryIdStr));
					}
				}
				filter.categoryIdList = KakeiboUtils.join(categoryIdList, ",");
				if (TextUtils.isEmpty(filter.categoryIdList)) {
					filterDao.deleteById(filter.id);
				} else {
					filterDao.update(filter);
				}
			}

			db.close();
		} catch (SQLiteException e) {
			Log.e(TAG, "getWritableDatabase時にエラー", e);
			throw e;
		}

		houseKeepingBookOpenHelper.close();

	}

	/** タイトルにフィルター名を付加して返す */
	public static String getAddFilterTitle(final Filter filter, final String title) {
		String result = title;
		if (filter != null) {
			result += " - " + filter.filterName;
		}
		return result;
	}
}
