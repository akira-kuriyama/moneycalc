package com.marronst.moneycalc.listener;

import com.marronst.moneycalc.activity.ManageBudgetActivity.BudgetDto;
import com.marronst.moneycalc.entity.Budget;

public interface OnListViewButtonClickListener {

	public void onclick(BudgetDto budgetDto, Budget previousMonthBudget);

}
