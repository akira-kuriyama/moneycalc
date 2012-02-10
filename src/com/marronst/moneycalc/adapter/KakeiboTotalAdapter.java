package com.marronst.moneycalc.adapter;

import java.util.List;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.marronst.moneycalc.R;
import com.marronst.moneycalc.consts.KakeiboConsts;
import com.marronst.moneycalc.dto.KakeiboTotalDto;
import com.marronst.moneycalc.entity.Category;
import com.marronst.moneycalc.utils.KakeiboFormatUtils;
import com.marronst.moneycalc.utils.KakeiboUtils;

public class KakeiboTotalAdapter extends ArrayAdapter<KakeiboTotalDto> {

	@SuppressWarnings("unused")
	private static final String TAG = KakeiboTotalAdapter.class.getSimpleName();

	private final LayoutInflater inflater;
	private final int expenseColor;
	private final int incomeColor;
	private final boolean isUseCarryover;
	private final boolean isUseCategoryCarryover;
	private final int balanceCalcMethod;
	private final Context context;

	public KakeiboTotalAdapter(final Context context, final int textViewResourceId,
			final List<KakeiboTotalDto> items2) {
		super(context, textViewResourceId, items2);
		this.context = context;
		this.inflater = LayoutInflater.from(context);
		expenseColor = context.getResources().getColor(R.color.expense_color);
		incomeColor = context.getResources().getColor(R.color.income_color);

		isUseCarryover = KakeiboUtils.isUseCarryover(context);
		isUseCategoryCarryover = KakeiboUtils.isUseCategoryCarryover(context);

		balanceCalcMethod = KakeiboUtils.getBalanceCalcMethod(context);
	}

	@Override
	public View getView(final int position, final View convertView, final ViewGroup parent) {
		// ビューを受け取る
		View view = convertView;
		KakeiboTotalDto kakeiboTotalDto = getItem(position);
		if (view == null) {
			view = this.inflater.inflate(R.layout.kakeibo_total_row, null);
		}

		TextView title = (TextView) view.findViewById(R.id.row_title);
		title.setText(kakeiboTotalDto.title);
		title.setTextSize(TypedValue.COMPLEX_UNIT_SP, kakeiboTotalDto.titleTextSize);

		final boolean isDisplayRemainingValue = isDisplayRemainingValue(kakeiboTotalDto);

		if (kakeiboTotalDto.isTotalRow && !kakeiboTotalDto.isSetttingFilter) {
			//トータル行---------------------
			TextView expensePrice = (TextView) view.findViewById(R.id.expense_price);
			expensePrice.setText(KakeiboFormatUtils.formatPrice(kakeiboTotalDto.expensePrice, context));
			expensePrice.setTextColor(expenseColor);
			if (kakeiboTotalDto.isDispIncomePrice) {
				TextView incomePrice = (TextView) view.findViewById(R.id.income_price);
				incomePrice.setText(KakeiboFormatUtils.formatPrice(kakeiboTotalDto.incomePrice, context));
				incomePrice.setTextColor(incomeColor);

				view.findViewById(R.id.income_price).setVisibility(View.VISIBLE);
				view.findViewById(R.id.income_label).setVisibility(View.VISIBLE);
			} else {
				if (KakeiboUtils.isMonthStartDaySetting(context)) {
					//タイトルが2行になるように、収入の要素を表示させる。。
					TextView incomePrice = (TextView) view.findViewById(R.id.income_price);
					TextView incomeLabel = (TextView) view.findViewById(R.id.income_label);
					incomePrice.setText("");
					incomeLabel.setText("");
					view.findViewById(R.id.income_price).setVisibility(View.VISIBLE);
					view.findViewById(R.id.income_label).setVisibility(View.VISIBLE);
				} else {
					view.findViewById(R.id.income_price).setVisibility(View.GONE);
					view.findViewById(R.id.income_label).setVisibility(View.GONE);
				}
			}

			if (isUseCarryover) {
				TextView carryOverPrice = (TextView) view.findViewById(R.id.carry_over_price);
				carryOverPrice.setText(KakeiboFormatUtils
						.formatPrice(kakeiboTotalDto.carryOverPrice, context));

				carryOverPrice.setVisibility(View.VISIBLE);
				view.findViewById(R.id.carry_over_price_label).setVisibility(View.VISIBLE);
			} else {
				view.findViewById(R.id.carry_over_price).setVisibility(View.GONE);
				view.findViewById(R.id.carry_over_price_label).setVisibility(View.GONE);
			}

			view.findViewById(R.id.expense_price).setVisibility(View.VISIBLE);
			view.findViewById(R.id.expense_label).setVisibility(View.VISIBLE);
		} else if (kakeiboTotalDto.isTotalRow && kakeiboTotalDto.isSetttingFilter) {
			//トータル行(フィルターが設定されていた場合)---------------------
			TextView expensePrice = (TextView) view.findViewById(R.id.expense_price);
			expensePrice.setText(KakeiboFormatUtils.formatPrice(kakeiboTotalDto.expensePrice, context));
			expensePrice.setTextColor(expenseColor);
			if (kakeiboTotalDto.isDispIncomePrice) {
				TextView incomePrice = (TextView) view.findViewById(R.id.income_price);
				incomePrice.setText(KakeiboFormatUtils.formatPrice(kakeiboTotalDto.incomePrice, context));
				incomePrice.setTextColor(incomeColor);

				view.findViewById(R.id.income_price).setVisibility(View.VISIBLE);
				view.findViewById(R.id.income_label).setVisibility(View.VISIBLE);
			} else {
				if (KakeiboUtils.isMonthStartDaySetting(context)) {
					//タイトルが2行になるように、収入の要素を表示させる。。
					TextView incomePrice = (TextView) view.findViewById(R.id.income_price);
					TextView incomeLabel = (TextView) view.findViewById(R.id.income_label);
					incomePrice.setText("");
					incomeLabel.setText("");
					view.findViewById(R.id.income_price).setVisibility(View.VISIBLE);
					view.findViewById(R.id.income_label).setVisibility(View.VISIBLE);
				} else {
					view.findViewById(R.id.income_price).setVisibility(View.GONE);
					view.findViewById(R.id.income_label).setVisibility(View.GONE);
				}
			}
			view.findViewById(R.id.expense_price).setVisibility(View.VISIBLE);
			view.findViewById(R.id.expense_label).setVisibility(View.VISIBLE);

			//			boolean isUseCarryoverComplex = isUseCarryover;
			//			if (balanceCalcMethod == KakeiboConsts.BALANCE_CALC_METHOD_BUDGET_MINUS_EXPENSE
			//					|| balanceCalcMethod == KakeiboConsts.BALANCE_CALC_METHOD_BUDGET_PLUS_INCOME_MINUS_EXPENSE) {
			//				if (!isUseCategoryCarryover) {
			//					isUseCarryoverComplex = false;
			//				}
			//			}
			//
			//			if (isUseCarryoverComplex && isDisplayRemainingValue) {
			//				TextView carryOverPrice = (TextView) view.findViewById(R.id.carry_over_price);
			//				carryOverPrice.setText(KakeiboFormatUtils
			//						.formatPrice(kakeiboTotalDto.carryOverPrice, context));
			//
			//				carryOverPrice.setVisibility(View.VISIBLE);
			//				view.findViewById(R.id.carry_over_price_label).setVisibility(View.VISIBLE);
			//			} else {
			view.findViewById(R.id.carry_over_price).setVisibility(View.GONE);
			view.findViewById(R.id.carry_over_price_label).setVisibility(View.GONE);
			//			}

		} else {

			//カテゴリ行---------------------
			if (Category.INCOME_FLG_OFF.equals(kakeiboTotalDto.incomeFlg)) {
				//支出カテゴリの場合---
				TextView expensePrice = (TextView) view.findViewById(R.id.expense_price);
				expensePrice.setText(KakeiboFormatUtils.formatPrice(kakeiboTotalDto.expensePrice, context));
				expensePrice.setTextColor(expenseColor);

				expensePrice.setVisibility(View.VISIBLE);
				view.findViewById(R.id.expense_label).setVisibility(View.VISIBLE);
				view.findViewById(R.id.income_price).setVisibility(View.GONE);
				view.findViewById(R.id.income_label).setVisibility(View.GONE);

				if (isUseCategoryCarryover && isDisplayRemainingValue && !kakeiboTotalDto.isSetttingFilter) {
					TextView carryOverPrice = (TextView) view.findViewById(R.id.carry_over_price);
					carryOverPrice.setText(KakeiboFormatUtils.formatPrice(kakeiboTotalDto.carryOverPrice,
																			context));

					carryOverPrice.setVisibility(View.VISIBLE);
					view.findViewById(R.id.carry_over_price_label).setVisibility(View.VISIBLE);
				} else {
					view.findViewById(R.id.carry_over_price).setVisibility(View.GONE);
					view.findViewById(R.id.carry_over_price_label).setVisibility(View.GONE);
				}

			} else {
				//収入カテゴリの場合---
				TextView incomePrice = (TextView) view.findViewById(R.id.income_price);
				incomePrice.setText(KakeiboFormatUtils.formatPrice(kakeiboTotalDto.incomePrice, context));
				incomePrice.setTextColor(incomeColor);

				incomePrice.setVisibility(View.VISIBLE);
				view.findViewById(R.id.income_label).setVisibility(View.VISIBLE);
				view.findViewById(R.id.expense_price).setVisibility(View.GONE);
				view.findViewById(R.id.expense_label).setVisibility(View.GONE);

				view.findViewById(R.id.carry_over_price).setVisibility(View.GONE);
				view.findViewById(R.id.carry_over_price_label).setVisibility(View.GONE);
			}

		}

		if (isDisplayRemainingArea(kakeiboTotalDto)) {
			//予算、残高繰越、残高、残高メーターの設定

			//予算
			TextView budgetLabel = (TextView) view.findViewById(R.id.budget_label);
			TextView budgetPrice = (TextView) view.findViewById(R.id.budget_price);
			if (kakeiboTotalDto.budgetPrice != 0L) {
				budgetPrice.setText(KakeiboFormatUtils.formatPrice(kakeiboTotalDto.budgetPrice, context));
				budgetLabel.setVisibility(View.VISIBLE);
				budgetPrice.setVisibility(View.VISIBLE);
			} else {
				budgetPrice.setVisibility(View.GONE);
				budgetLabel.setVisibility(View.GONE);
			}

			//残高
			TextView remainingValueLabel = (TextView) view.findViewById(R.id.remaining_value_label);
			TextView remainingValuePrice = (TextView) view.findViewById(R.id.remaining_value_price);
			remainingValuePrice.setText(KakeiboFormatUtils.formatPrice(kakeiboTotalDto.remainingValuePrice,
																		context));

			//残高メーターoff
			ImageView remainingValueMeterOff = (ImageView) view.findViewById(R.id.remaining_value_meter_off);
			LayoutParams layoutParams = remainingValueMeterOff.getLayoutParams();
			layoutParams.width = kakeiboTotalDto.remainingValueMeterOffPixel;
			remainingValueMeterOff.setLayoutParams(layoutParams);
			if (kakeiboTotalDto.remainingValueMeterOnPixel <= 0) {
				remainingValueMeterOff.setImageResource(R.drawable.remaining_value_meter_off_empty);
			} else {
				remainingValueMeterOff.setImageResource(R.drawable.remaining_value_meter_off);
			}

			//残高メーターon
			ImageView remainingValueMeterOn = (ImageView) view.findViewById(R.id.remaining_value_meter_on);
			LayoutParams layoutParams2 = remainingValueMeterOn.getLayoutParams();
			layoutParams2.width = kakeiboTotalDto.remainingValueMeterOnPixel;
			remainingValueMeterOn.setLayoutParams(layoutParams2);

			if (isDisplayRemainingValue) {
				//残高を表示するなら、残高と残高メーターを表示設定
				remainingValueLabel.setVisibility(View.VISIBLE);
				remainingValuePrice.setVisibility(View.VISIBLE);
				remainingValueMeterOn.setVisibility(View.VISIBLE);
				remainingValueMeterOff.setVisibility(View.VISIBLE);
			} else {
				//残高を表示しないなら、残高と残高メーターを非表示設定
				remainingValueLabel.setVisibility(View.GONE);
				remainingValuePrice.setVisibility(View.GONE);
				remainingValueMeterOn.setVisibility(View.GONE);
				remainingValueMeterOff.setVisibility(View.GONE);
			}

			view.findViewById(R.id.budget_and_remaining_area).setVisibility(View.VISIBLE);
		} else {
			view.findViewById(R.id.budget_and_remaining_area).setVisibility(View.GONE);
		}
		return view;
	}

	/**  残高を表示するかどうか  */
	private boolean isDisplayRemainingValue(final KakeiboTotalDto kakeiboTotalDto) {
		boolean isDisplayRemainingValue = false;

		if (kakeiboTotalDto.isTotalRow) {
			switch (balanceCalcMethod) {
				case KakeiboConsts.BALANCE_CALC_METHOD_NONE:
					//ignore
					break;
				case KakeiboConsts.BALANCE_CALC_METHOD_INCOME_MINUS_EXPENSE:
					isDisplayRemainingValue = true;
					break;
				case KakeiboConsts.BALANCE_CALC_METHOD_BUDGET_MINUS_EXPENSE:
				case KakeiboConsts.BALANCE_CALC_METHOD_BUDGET_PLUS_INCOME_MINUS_EXPENSE:
					if (kakeiboTotalDto.budgetPrice != 0L) {
						isDisplayRemainingValue = true;
					}
					break;
				default:
					//ignore
					break;
			}
		} else {
			switch (balanceCalcMethod) {
				case KakeiboConsts.BALANCE_CALC_METHOD_NONE:
					//ignore
					break;
				case KakeiboConsts.BALANCE_CALC_METHOD_INCOME_MINUS_EXPENSE:
					//ignore
					break;
				case KakeiboConsts.BALANCE_CALC_METHOD_BUDGET_MINUS_EXPENSE:
				case KakeiboConsts.BALANCE_CALC_METHOD_BUDGET_PLUS_INCOME_MINUS_EXPENSE:
					if (kakeiboTotalDto.budgetPrice != 0L) {
						isDisplayRemainingValue = true;
					}
					break;
				default:
					//ignore
					break;
			}
		}

		return isDisplayRemainingValue;
	}

	/** 残高エリアを表示するかどうか */
	private boolean isDisplayRemainingArea(final KakeiboTotalDto kakeiboTotalDto) {
		boolean isDisplayRemainingArea = false;

		if (kakeiboTotalDto.isTotalRow) {
			if (kakeiboTotalDto.budgetPrice != 0
					|| KakeiboConsts.BALANCE_CALC_METHOD_INCOME_MINUS_EXPENSE == balanceCalcMethod) {
				isDisplayRemainingArea = true;
			}
		} else {
			if (kakeiboTotalDto.incomeFlg == Category.INCOME_FLG_ON) {
				return false;
			}
			if (kakeiboTotalDto.budgetPrice != 0) {
				isDisplayRemainingArea = true;
			}
		}

		return isDisplayRemainingArea;
	}
}