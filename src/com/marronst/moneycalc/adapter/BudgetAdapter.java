package com.marronst.moneycalc.adapter;

import java.util.List;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.marronst.moneycalc.R;
import com.marronst.moneycalc.activity.ManageBudgetActivity.BudgetDto;
import com.marronst.moneycalc.entity.Budget;
import com.marronst.moneycalc.entity.Category;
import com.marronst.moneycalc.listener.OnListViewButtonClickListener;
import com.marronst.moneycalc.utils.KakeiboFormatUtils;
import com.marronst.moneycalc.utils.KakeiboUtils;

public class BudgetAdapter extends ArrayAdapter<BudgetDto> {

	@SuppressWarnings("unused")
	private static final String TAG = BudgetAdapter.class.getSimpleName();

	private final LayoutInflater inflater;
	private final int viewResourceId;

	private final Context context;

	public OnListViewButtonClickListener listener;

	public List<Budget> previousBudgetList;

	public BudgetAdapter(final Context context, final int viewResourceId2, final List<BudgetDto> items2) {
		super(context, viewResourceId2, items2);
		this.context = context;
		this.viewResourceId = viewResourceId2;
		this.inflater = LayoutInflater.from(context);
	}

	@Override
	public View getView(final int position, final View convertView, final ViewGroup parent) {
		// ビューを設定する
		View view;
		// 表示すべきデータの取得
		final BudgetDto budgetDto = getItem(position);

		if (budgetDto.isTotalRow) {
			view = this.inflater.inflate(R.layout.manage_budget_header, null);

			//カテゴリごとの合計予算
			TextView categoryTotalBudgetPriceTextView = (TextView) view
					.findViewById(R.id.kakeibo_total_budget_price);

			categoryTotalBudgetPriceTextView.setText(KakeiboFormatUtils
					.formatPrice(budgetDto.categoryTotalBudgetPrice, context));
		} else {
			view = this.inflater.inflate(viewResourceId, null);
		}

		//予算設定ボタン
		final ImageButton button = (ImageButton) view.findViewById(R.id.setting_budget);
		if (Category.INCOME_FLG_OFF.equals(budgetDto.incomeFlg) || budgetDto.isTotalRow) {
			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					listener.onclick(budgetDto, getPreviousMonthBudget(budgetDto));
				}
			});
		} else {
			button.setVisibility(View.INVISIBLE);
		}

		//カテゴリ名
		TextView categoryName = (TextView) view.findViewById(R.id.manage_budget_row_name);
		if (budgetDto.isTotalRow) {
			if (KakeiboUtils.isMonthStartDaySetting(context)) {
				categoryName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
			} else {
				categoryName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
			}
		}
		categoryName.setText(budgetDto.categoryName);
		categoryName.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(final View v) {
				button.dispatchTouchEvent(MotionEvent.obtain(1, 1, MotionEvent.ACTION_DOWN, 0, 0, 0));
				button.dispatchTouchEvent(MotionEvent.obtain(1, 1, MotionEvent.ACTION_UP, 0, 0, 0));
			}
		});

		//予算金額
		TextView budgetPriceTextView = (TextView) view.findViewById(R.id.budget_price);
		Long budgetPrice = budgetDto.budgetPrice;
		if (budgetPrice == null || budgetPrice == 0) {
			if (budgetDto.isTotalRow) {
				budgetPriceTextView.setText(R.string.NO_SETTING_BUDGET);
			} else if (Category.INCOME_FLG_OFF.equals(budgetDto.incomeFlg)) {
				budgetPriceTextView.setText(R.string.NO_SETTING_BUDGET);
			} else {
				budgetPriceTextView.setText(R.string.IMPOSSIBLE_SETTING_BUDGET);
			}
		} else {
			budgetPriceTextView.setText(KakeiboFormatUtils.formatPrice(budgetPrice, context));
		}
		budgetPriceTextView.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(final View v) {
				button.dispatchTouchEvent(MotionEvent.obtain(1, 1, MotionEvent.ACTION_DOWN, 0, 0, 0));
				button.dispatchTouchEvent(MotionEvent.obtain(1, 1, MotionEvent.ACTION_UP, 0, 0, 0));
			}
		});

		return view;
	}

	private Budget getPreviousMonthBudget(final Budget budget) {
		for (Budget previousMonthBudget : previousBudgetList) {
			if (budget.categoryId == null || previousMonthBudget.categoryId == null) {
				continue;
			}

			if (budget.categoryId.equals(previousMonthBudget.categoryId)) {
				return previousMonthBudget;
			}
		}

		return null;
	}
}