package com.marronst.moneycalc.preference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;

import com.marronst.moneycalc.R;
import com.marronst.moneycalc.consts.KakeiboConsts;
import com.marronst.moneycalc.utils.KakeiboFormatUtils;
import com.marronst.moneycalc.utils.KakeiboUtils;

public class CurrencyUnitPreference extends DialogPreference {

	private EditText currencyUnitEditText;
	private Spinner currencyUnitSpinner;
	private Spinner currencyUnitPositionSpinner;

	private final List<String> defaultCurrencyTypeList = Arrays.asList(new String[] { "$", "€", "£", "₨",
			"￥", "円", "other" });

	public CurrencyUnitPreference(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		setDialogLayoutResource(R.layout.currency_unit_preference);
	}

	@Override
	protected void showDialog(final Bundle state) {
		Context context = getContext();

		Builder mBuilder = new AlertDialog.Builder(context)//
				.setTitle(getDialogTitle())//
				.setNegativeButton(getNegativeButtonText(), null)//
				.setPositiveButton(getPositiveButtonText(), new OkClickListener())//
		;

		View contentView = onCreateDialogView();
		onBindDialogView(contentView);
		mBuilder.setView(contentView);

		final View currencyUnitEditTextTitle = contentView.findViewById(R.id.currency_unit_edit_text_title);
		currencyUnitEditText = (EditText) contentView.findViewById(R.id.currency_unit_edit_text);

		//通貨タイプを指定するスピナー
		currencyUnitSpinner = (Spinner) contentView.findViewById(R.id.currency_unit_spinner);

		final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getContext(),
				android.R.layout.simple_spinner_item, defaultCurrencyTypeList);
		arrayAdapter.setDropDownViewResource(R.layout.multiline_spinner_item);
		currencyUnitSpinner.setAdapter(arrayAdapter);
		currencyUnitSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(final AdapterView<?> parent, final View view, final int position,
					final long id) {
				String type = defaultCurrencyTypeList.get(position);
				if ("other".equals(type)) {
					currencyUnitEditTextTitle.setVisibility(View.VISIBLE);
					currencyUnitEditText.setVisibility(View.VISIBLE);
				} else {
					currencyUnitEditTextTitle.setVisibility(View.GONE);
					currencyUnitEditText.setVisibility(View.GONE);
				}
			}

			@Override
			public void onNothingSelected(final AdapterView<?> parent) {
				// なにもしない 
			}
		});

		//通貨位置を指定するスピナー
		currencyUnitPositionSpinner = (Spinner) contentView.findViewById(R.id.currency_unit_position_spinner);
		List<String> currencyUnitPositionList = new ArrayList<String>();
		currencyUnitPositionList.add(getContext().getResources()
				.getString(R.string.pref_currency_position_front));
		currencyUnitPositionList.add(getContext().getResources()
				.getString(R.string.pref_currency_position_back));
		final ArrayAdapter<String> currencyUnitPositionArrayAdapter = new ArrayAdapter<String>(getContext(),
				android.R.layout.simple_spinner_item, currencyUnitPositionList);
		currencyUnitPositionArrayAdapter.setDropDownViewResource(R.layout.multiline_spinner_item);
		currencyUnitPositionSpinner.setAdapter(currencyUnitPositionArrayAdapter);

		//初期値の設定
		SharedPreferences defaultSharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(getContext());
		String currencyType = defaultSharedPreferences.getString(KakeiboConsts.PREFERENCE_KEY_CURRENCY_UNIT,
																	null);
		if (currencyType == null) {
			if (KakeiboUtils.isJapan()) {
				currencyType = KakeiboFormatUtils.unit_ja;
			} else {
				currencyType = KakeiboFormatUtils.unit_us;
			}
		}

		int selectedPosition = defaultCurrencyTypeList.size() - 1;
		for (int i = 0; i < defaultCurrencyTypeList.size(); i++) {
			if (defaultCurrencyTypeList.get(i).equals(currencyType)) {
				selectedPosition = i;
			}
		}
		currencyUnitSpinner.setSelection(selectedPosition);
		int currencyUnitPosition;
		String currencyUnitPositionStr = defaultSharedPreferences
				.getString(KakeiboConsts.PREFERENCE_KEY_CURRENCY_UNIT_POSITION, null);
		if (currencyUnitPositionStr == null) {
			if (KakeiboUtils.isJapan()) {
				currencyUnitPosition = 1;//back
			} else {
				currencyUnitPosition = 0;//front
			}
		} else {
			if ("front".equals(currencyUnitPositionStr)) {
				currencyUnitPosition = 0;//front
			} else {
				currencyUnitPosition = 1;//back
			}
		}
		currencyUnitPositionSpinner.setSelection(currencyUnitPosition);

		if (selectedPosition == defaultCurrencyTypeList.size() - 1) {
			currencyUnitEditText.setText(currencyType);
		}

		onPrepareDialogBuilder(mBuilder);

		// Create the dialog
		final Dialog dialog = mBuilder.create();
		if (state != null) {
			dialog.onRestoreInstanceState(state);
		}
		dialog.setOnDismissListener(this);
		dialog.show();
	}

	private class OkClickListener implements OnClickListener {

		@Override
		public void onClick(final DialogInterface dialog, final int which) {

			String currencyType = (String) currencyUnitSpinner.getSelectedItem();
			if (defaultCurrencyTypeList.get(defaultCurrencyTypeList.size() - 1).equals(currencyType)) {
				currencyType = currencyUnitEditText.getText().toString();
				if (TextUtils.isEmpty(currencyType)) {
					return;
				}
				currencyType = currencyType.replaceAll("\n", "");
			}
			String currencyUnitPosition;
			int currencyUnitPositionPosition = currencyUnitPositionSpinner.getSelectedItemPosition();
			if (currencyUnitPositionPosition == 0) {
				currencyUnitPosition = "front";
			} else {
				currencyUnitPosition = "back";
			}

			SharedPreferences defaultSharedPreferences = PreferenceManager
					.getDefaultSharedPreferences(getContext());
			Editor edit = defaultSharedPreferences.edit();
			edit.putString(KakeiboConsts.PREFERENCE_KEY_CURRENCY_UNIT, currencyType);
			edit.putString(KakeiboConsts.PREFERENCE_KEY_CURRENCY_UNIT_POSITION, currencyUnitPosition);
			edit.commit();
			KakeiboUtils.setUnitPosition(currencyUnitPosition);
			KakeiboUtils.setCurrencyUnit(currencyType, getContext());
		}
	}
}
