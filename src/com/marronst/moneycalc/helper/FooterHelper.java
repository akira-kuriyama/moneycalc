package com.marronst.moneycalc.helper;

import java.util.Calendar;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.marronst.moneycalc.R;
import com.marronst.moneycalc.activity.KakeiboTotalActivity;
import com.marronst.moneycalc.activity.RegisterRecordActivity;
import com.marronst.moneycalc.activity.ViewKakeiboListActivity;
import com.marronst.moneycalc.consts.KakeiboConsts;
import com.marronst.moneycalc.consts.KakeiboConsts.KakeiboListViewType;
import com.marronst.moneycalc.entity.Filter;
import com.marronst.moneycalc.utils.KakeiboUtils;

public class FooterHelper {

	private FooterHelper() {
		//コンストラクト禁止
	}

	/**  共通下部ボタンの設定 */
	public static void setupCommonBottomButton(final Activity activity) {

		//記入ボタン
		Button showRegisterViewButton = (Button) activity.findViewById(R.id.show_register_view);
		showRegisterViewButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				Intent i = new Intent(activity.getApplicationContext(), RegisterRecordActivity.class);
				i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				activity.startActivity(i);
			}
		});

		//今月ボタン
		Button currentMonthButton = (Button) activity.findViewById(R.id.current_month);
		currentMonthButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				Calendar cal = Calendar.getInstance();
				final KakeiboListViewType viewType = KakeiboListViewType.MONTH;
				Intent i = new Intent(activity.getApplicationContext(), KakeiboTotalActivity.class);
				i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				i.putExtra(KakeiboConsts.INTENT_KEY_START_DATE_VALUE, KakeiboUtils.getStartCal(cal, viewType,
																								activity));
				i.putExtra(KakeiboConsts.INTENT_KEY_LIST_VIEW_TYPE, viewType.name());
				Filter defaultFilter = getDefaultFilter(activity);
				if (defaultFilter != null) {
					i.putExtra(KakeiboConsts.INTENT_KEY_FILTER_ID, defaultFilter.id);
				}
				activity.startActivity(i);
			}
		});

		//今週ボタン
		Button currentWeekButton = (Button) activity.findViewById(R.id.current_week);
		currentWeekButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				Calendar cal = Calendar.getInstance();
				final KakeiboListViewType viewType = KakeiboListViewType.WEEK;
				Intent i = new Intent(activity.getApplicationContext(), KakeiboTotalActivity.class);
				i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				i.putExtra(KakeiboConsts.INTENT_KEY_START_DATE_VALUE, KakeiboUtils.getStartCal(cal, viewType,
																								activity));
				i.putExtra(KakeiboConsts.INTENT_KEY_LIST_VIEW_TYPE, viewType.name());
				Filter defaultFilter = getDefaultFilter(activity);
				if (defaultFilter != null) {
					i.putExtra(KakeiboConsts.INTENT_KEY_FILTER_ID, defaultFilter.id);
				}
				activity.startActivity(i);
			}
		});

		//今日ボタン
		Button currentDaykButton = (Button) activity.findViewById(R.id.current_day);
		currentDaykButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				Calendar cal = Calendar.getInstance();
				final KakeiboListViewType viewType = KakeiboListViewType.DAY;
				Intent i = new Intent(activity.getApplicationContext(), ViewKakeiboListActivity.class);
				i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				i.putExtra(KakeiboConsts.INTENT_KEY_START_DATE_VALUE, KakeiboUtils.getStartCal(cal, viewType,
																								activity));
				i.putExtra(KakeiboConsts.INTENT_KEY_LIST_VIEW_TYPE, viewType.name());
				Filter defaultFilter = getDefaultFilter(activity);
				if (defaultFilter != null) {
					i.putExtra(KakeiboConsts.INTENT_KEY_FILTER_ID, defaultFilter.id);
				}
				activity.startActivity(i);
			}
		});
	}

	/** デフォルトフィルターの取得 */
	private static Filter getDefaultFilter(final Activity activity) {
		FilterHelper filterHelper = new FilterHelper();
		return filterHelper.getDefaultFilter(activity.getApplicationContext());

	}
}
