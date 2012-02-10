package com.marronst.moneycalc.dxo;

import java.util.Date;

import android.database.Cursor;

import com.marronst.moneycalc.entity.Category;
import com.marronst.moneycalc.entity.Category.IdxCategory;

public class CategoryDxo {

	public Category createFromCursol(final Cursor c) {

		Integer id = c.getInt(IdxCategory.id());
		String categoryName = c.getString(IdxCategory.categoryName());
		Integer position = c.getInt(IdxCategory.position());
		//String registerDate = c.getString(IdxCategory.registerDate());
		Integer dispFlg = c.getInt(IdxCategory.dispFlg());
		Integer incomeFlg = c.getInt(IdxCategory.incomeFlg());
		String iconName = c.getString(IdxCategory.iconName());

		Category category = new Category();
		category.id = id;
		category.categoryName = categoryName;
		category.position = position;
		category.registerDate = new Date();//使われていないので。KakeiboFormatUtils.formatStringToDateForDB(registerDate);
		category.dispFlg = dispFlg;
		category.incomeFlg = incomeFlg;
		category.iconName = iconName;

		return category;
	}
}
