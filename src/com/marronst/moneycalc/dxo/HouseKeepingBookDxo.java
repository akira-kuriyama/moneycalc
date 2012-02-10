package com.marronst.moneycalc.dxo;

import android.database.Cursor;

import com.marronst.moneycalc.entity.HouseKeepingBook;
import com.marronst.moneycalc.entity.HouseKeepingBook.IdxHouseKeepingBook;
import com.marronst.moneycalc.utils.KakeiboFormatUtils;

public class HouseKeepingBookDxo {

	public HouseKeepingBook createFromCursor(final Cursor c, final boolean isGetRegisterDate) {
		int id = c.getInt(IdxHouseKeepingBook.id());
		int price = c.getInt(IdxHouseKeepingBook.price());
		int categoryId = c.getInt(IdxHouseKeepingBook.categoryId());
		String memo = c.getString(IdxHouseKeepingBook.memo());
		String place = "";//c.getString(IdxHouseKeepingBook.place());
		String latitude = "";// c.getString(IdxHouseKeepingBook.latitude());
		String longitude = "";// c.getString(IdxHouseKeepingBook.longitude());
		String registerDate = null;
		if (isGetRegisterDate) {
			registerDate = c.getString(IdxHouseKeepingBook.registerDate());
		}
		String importVersionStr = c.getString(IdxHouseKeepingBook.importVersion());
		Integer importVersion = importVersionStr == null ? null : Integer.valueOf(importVersionStr);
		HouseKeepingBook houseKeepingBook = new HouseKeepingBook();
		houseKeepingBook.id = id;
		houseKeepingBook.price = price;
		houseKeepingBook.memo = memo;
		houseKeepingBook.place = place;
		houseKeepingBook.categoryId = categoryId;
		if (isGetRegisterDate) {
			houseKeepingBook.registerDate = KakeiboFormatUtils.formatStringToDateForDB(registerDate);
		}
		houseKeepingBook.latitude = latitude;
		houseKeepingBook.longitude = longitude;
		houseKeepingBook.importVersion = importVersion;
		return houseKeepingBook;
	}
}
