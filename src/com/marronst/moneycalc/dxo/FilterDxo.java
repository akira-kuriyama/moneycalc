package com.marronst.moneycalc.dxo;

import android.database.Cursor;

import com.marronst.moneycalc.entity.Filter;
import com.marronst.moneycalc.entity.Filter.IdxFilter;

public class FilterDxo {

	public Filter createFromCursol(final Cursor c) {
		Filter filter = new Filter();
		filter.id = c.getInt(IdxFilter.id());
		filter.filterName = c.getString(IdxFilter.filterName());
		filter.categoryIdList = c.getString(IdxFilter.categoryIdList());
		filter.isDefault = c.getInt(IdxFilter.isDefault());

		return filter;
	}
}
