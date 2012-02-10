package com.marronst.moneycalc.dxo;

import android.database.Cursor;
import android.text.TextUtils;

import com.marronst.moneycalc.entity.Budget;
import com.marronst.moneycalc.entity.Budget.IdxBudget;

public class BudgetDxo {

	private Integer id;
	private String categoryIdStr;
	private Integer yearMonth;
	private Long budgetPrice;

	private Budget budget;
	private Integer categoryId;

	public Budget createFromCursol(final Cursor c) {

		id = c.getInt(IdxBudget.id());
		categoryIdStr = c.getString(IdxBudget.categoryId());
		yearMonth = c.getInt(IdxBudget.yearMonth());
		budgetPrice = c.getLong(IdxBudget.budgetPrice());

		budget = new Budget();
		budget.id = id;
		categoryId = null;
		if (!TextUtils.isEmpty(categoryIdStr)) {
			categoryId = Integer.parseInt(categoryIdStr);
		}
		budget.categoryId = categoryId;
		budget.yearMonth = yearMonth;
		budget.budgetPrice = budgetPrice;

		return budget;
	}
}
