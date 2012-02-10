package com.marronst.moneycalc.activity;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.text.TextUtils;

import com.marronst.moneycalc.R;
import com.marronst.moneycalc.consts.KakeiboConsts;
import com.marronst.moneycalc.preference.BalancePreference;
import com.marronst.moneycalc.utils.KakeiboUtils;

public class KakeiboPreferenceActivity extends PreferenceActivity {

	private NotificationManager notificationManager;

	private boolean isChangeStartDay;

	@Override
	public void onCreate(final Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setTitle(R.string.pref_settings_title);
		addPreferencesFromResource(R.xml.pref);

		//残高算出方法の設定
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		int balanceCalcMethod = preferences.getInt(KakeiboConsts.PREFERENCE_KEY_BALANCE_CALC_METHOD,
													KakeiboConsts.DEFAULT_BALANCE_CALC_METHOD);
		//初期表示設定
		changeStateOfCategoryPreference(balanceCalcMethod);

		BalancePreference balanceCalculationMethod = (BalancePreference) findPreference("balance_calculation_method");
		balanceCalculationMethod.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
			@Override
			public boolean onPreferenceChange(final Preference preference, final Object newValue) {
				if (newValue == null || !TextUtils.isDigitsOnly(String.valueOf(newValue))) {
					return false;
				}
				int balanceCalcMethod = Integer.valueOf(newValue.toString()).intValue();
				changeStateOfCategoryPreference(balanceCalcMethod);
				return true;
			}
		});

		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		//通知バーに表示するかどうかが変更された場合
		CheckBoxPreference checkBoxPreference = (CheckBoxPreference) findPreference(KakeiboConsts.PREFERENCE_KEY_IS_DISP_STATUS_BAR);
		checkBoxPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(final Preference preference, final Object newValue) {
				if (Boolean.TRUE.equals(newValue)) {
					KakeiboUtils.startNotification(getApplicationContext(), notificationManager);
				} else {
					KakeiboUtils.cancelNotification(notificationManager);
				}
				return true;
			}
		});

		//月の開始日が変更された場合
		ListPreference listPreference = (ListPreference) findPreference(KakeiboConsts.PREFERENCE_KEY_FIRST_DAY_OF_MONTH);
		listPreference.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

			@Override
			public boolean onPreferenceChange(final Preference preference, final Object newValue) {
				if (newValue != null) {

					try {
						int startDay = Integer.parseInt((newValue.toString()));
						KakeiboUtils.setStartDay(startDay);
						isChangeStartDay = true;
					} catch (NumberFormatException e) {
						//なにもしない
					}
				}
				return true;
			}
		});

		Preference sendLogPreference = findPreference(KakeiboConsts.PREFERENCE_KEY_SEND_LOG);
		sendLogPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(final Preference preference) {
				Intent intent = new Intent(getApplicationContext(), SendLogActivity.class);
				startActivity(intent);
				return true;
			}
		});
	}

	/** 残高繰越設定の有効無効を切り替える(残高表示をしないなら非表示に) */
	private void changeStateOfCategoryPreference(final int value) {
		boolean isNoDisplayBalance = KakeiboConsts.BALANCE_CALC_METHOD_NONE == value;
		boolean isImisusEBalanceCalcMethod = KakeiboConsts.BALANCE_CALC_METHOD_INCOME_MINUS_EXPENSE == value;
		findPreference("is_use_carryover").setEnabled(!isNoDisplayBalance);
		findPreference("is_use_category_carryover").setEnabled(
																!isNoDisplayBalance
																		&& !isImisusEBalanceCalcMethod);
	}

	@Override
	protected void onResume() {
		super.onResume();
		isChangeStartDay = false;
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (isChangeStartDay) {
			Intent intent = new Intent(getApplicationContext(), RegisterRecordActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
		}
	}
}
