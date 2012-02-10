package com.marronst.moneycalc.preference;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.widget.ArrayAdapter;

import com.marronst.moneycalc.R;
import com.marronst.moneycalc.consts.KakeiboConsts;
import com.marronst.moneycalc.utils.KakeiboUtils;

public class BalancePreference extends DialogPreference {
	private String[] mEntries;
	private final int[] mEntryValues;
	private int mValue;

	private int mClickedDialogEntryIndex;

	public BalancePreference(final Context context, final AttributeSet attrs) {
		super(context, attrs);

		mEntryValues = new int[] {//
				KakeiboConsts.BALANCE_CALC_METHOD_NONE,//
				KakeiboConsts.BALANCE_CALC_METHOD_INCOME_MINUS_EXPENSE,
				KakeiboConsts.BALANCE_CALC_METHOD_BUDGET_MINUS_EXPENSE,
				KakeiboConsts.BALANCE_CALC_METHOD_BUDGET_PLUS_INCOME_MINUS_EXPENSE };

	}

	private void setEntries(final Context context) {
		final Resources resources = getContext().getResources();
		mEntries = new String[4];
		mEntries[0] = (String) resources.getText(R.string.balance_calculation_method_none);

		boolean isUseCarryover = KakeiboUtils.isUseCarryover(context);
		boolean isUseCategoryCarryover = KakeiboUtils.isUseCategoryCarryover(context);
		if (isUseCarryover || isUseCategoryCarryover) {
			mEntries[1] = (String) resources
					.getText(R.string.balance_calculation_method_income_plus_carryover_minus_expense);
			mEntries[2] = (String) resources
					.getText(R.string.balance_calculation_method_budget_plus_carryover_minus_expense);
			mEntries[3] = (String) resources
					.getText(R.string.balance_calculation_method_budget_plus_income_plus_carryover_minus_expense);
		} else {
			mEntries[1] = (String) resources
					.getText(R.string.balance_calculation_method_income_minus_expense);
			mEntries[2] = (String) resources
					.getText(R.string.balance_calculation_method_budget_minus_expense);
			mEntries[3] = (String) resources
					.getText(R.string.balance_calculation_method_budget_plus_income_minus_expense);

		}
	}

	public BalancePreference(final Context context) {
		this(context, null);
	}

	@Override
	protected void onPrepareDialogBuilder(final Builder builder) {

		super.onPrepareDialogBuilder(builder);

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
		mValue = preferences.getInt(KakeiboConsts.PREFERENCE_KEY_BALANCE_CALC_METHOD,
									KakeiboConsts.DEFAULT_BALANCE_CALC_METHOD);

		setEntries(getContext());

		if (mEntries == null || mEntryValues == null) {
			throw new IllegalStateException(
					"ListPreference requires an entries array and an entryValues array.");
		}

		mClickedDialogEntryIndex = getValueIndex();
		final ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(),
				R.layout.simple_list_item_single_choice_small, mEntries);
		adapter.setDropDownViewResource(R.layout.simple_list_item_single_choice_small);
		builder.setTitle(R.string.pref_dialog_title_balance_calculation_method);
		builder.setSingleChoiceItems(adapter, mClickedDialogEntryIndex,
										new DialogInterface.OnClickListener() {
											public void onClick(final DialogInterface dialog, final int which) {
												mClickedDialogEntryIndex = which;

												/*
												 * Clicking on an item simulates the positive button
												 * click, and dismisses the dialog.
												 */
												BalancePreference.this
														.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
												dialog.dismiss();
											}
										});

		builder.setPositiveButton(null, null);
	}

	@Override
	protected void onDialogClosed(final boolean positiveResult) {
		super.onDialogClosed(positiveResult);

		if (positiveResult && mClickedDialogEntryIndex >= 0 && mEntryValues != null) {
			int value = mEntryValues[mClickedDialogEntryIndex];
			if (callChangeListener(value)) {
				setValue(value);
			}
		}
	}

	/**
	 * Returns the index of the given value (in the entry values array).
	 * 
	 * @param value The value whose index should be returned.
	 * @return The index of the value, or -1 if not found.
	 */
	public int findIndexOfValue(final int value) {
		if (mEntryValues != null) {
			for (int i = mEntryValues.length - 1; i >= 0; i--) {
				if (mEntryValues[i] == value) {
					return i;
				}
			}
		}
		return -1;
	}

	private int getValueIndex() {
		return findIndexOfValue(mValue);
	}

	/**
	* Sets the value of the key. This should be one of the entries in
	* {@link #getEntryValues()}.
	* 
	* @param value The value to set for the key.
	*/
	public void setValue(final int value) {
		mValue = value;

		persistInt(value);
	}

}
