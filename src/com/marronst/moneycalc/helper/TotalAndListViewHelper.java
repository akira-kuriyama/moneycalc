package com.marronst.moneycalc.helper;

import java.util.Calendar;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;

import com.marronst.moneycalc.R;
import com.marronst.moneycalc.consts.KakeiboConsts;
import com.marronst.moneycalc.consts.KakeiboConsts.KakeiboListViewType;
import com.marronst.moneycalc.utils.KakeiboUtils;

public class TotalAndListViewHelper {

	public static String nextButtonName(final KakeiboListViewType kakeiboListViewType, final Activity activity) {
		String nextButtonName = "";
		boolean isJapan = KakeiboUtils.isJapan();
		switch (kakeiboListViewType) {
			case YEAR:
				if (isJapan) {
					nextButtonName = "次年";
				} else {
					nextButtonName = "Next year";
				}
				break;
			case HALF_YEAR:
				if (isJapan) {
					nextButtonName = "次の半年";
				} else {
					nextButtonName = "Next half months";
				}
				break;
			case THREE_MONTH:
				if (isJapan) {
					nextButtonName = "次の３ヶ月";
				} else {
					nextButtonName = "Next 3 months";
				}
				break;
			case MONTH:
				if (isJapan) {
					nextButtonName = "次月";
				} else {
					nextButtonName = "Next month";
				}
				break;
			case WEEK:
				if (isJapan) {
					nextButtonName = "次週";
				} else {
					nextButtonName = "Next week";
				}
				break;
			case DAY:
				if (isJapan) {
					nextButtonName = "次日";
				} else {
					nextButtonName = "Next day";
				}
				break;
			default:
				break;
		}
		if (KakeiboUtils.isLandscape(activity)) {
			if (!isJapan) {
				nextButtonName = nextButtonName.split(" ")[0];
			}
			StringBuilder b = new StringBuilder();
			final String breaklLne = "\n";
			for (char c : nextButtonName.toCharArray()) {
				b.append(c).append(breaklLne);
			}
			return b.substring(0, b.length() - 1);
		} else {
			return nextButtonName;
		}

	}

	public static String previousButtonName(final KakeiboListViewType kakeiboListViewType,
			final Activity activity) {
		String previousButtonName = "";
		boolean isJapan = KakeiboUtils.isJapan();
		switch (kakeiboListViewType) {
			case YEAR:
				if (isJapan) {
					previousButtonName = "前年";
				} else {
					previousButtonName = "Prev year";
				}
				break;
			case HALF_YEAR:
				if (isJapan) {
					previousButtonName = "前の半年";
				} else {
					previousButtonName = "Prev half months";
				}
				break;
			case THREE_MONTH:
				if (isJapan) {
					previousButtonName = "前の３ヶ月";
				} else {
					previousButtonName = "Prev 3 months";
				}
				break;
			case MONTH:
				if (isJapan) {
					previousButtonName = "前月";
				} else {
					previousButtonName = "Prev month";
				}
				break;
			case WEEK:
				if (isJapan) {
					previousButtonName = "前週";
				} else {
					previousButtonName = "Prev week";
				}
				break;
			case DAY:
				if (isJapan) {
					previousButtonName = "前日";
				} else {
					previousButtonName = "Prev day";
				}
				break;
			default:
				break;
		}
		if (KakeiboUtils.isLandscape(activity)) {
			if (!isJapan) {
				previousButtonName = previousButtonName.split(" ")[0];
			}

			StringBuilder b = new StringBuilder();
			final String breaklLne = "\n";
			for (char c : previousButtonName.toCharArray()) {
				b.append(c).append(breaklLne);
			}
			return b.substring(0, b.length() - 1);
		} else {
			return previousButtonName;
		}
	}

	public static int getFieldValue(final KakeiboListViewType kakeiboListViewType) {
		switch (kakeiboListViewType) {
			case YEAR:
				return 1;
			case HALF_YEAR:
				return 6;
			case THREE_MONTH:
				return 3;
			case MONTH:
				return 1;
			case WEEK:
				return 1;
			case DAY:
				return 1;
			default:
				break;
		}
		return 0;
	}

	public static int getField(final KakeiboListViewType kakeiboListViewType) {
		switch (kakeiboListViewType) {
			case YEAR:
				return Calendar.YEAR;
			case HALF_YEAR:
				return Calendar.MONTH;
			case THREE_MONTH:
				return Calendar.MONTH;
			case MONTH:
				return Calendar.MONTH;
			case WEEK:
				return Calendar.WEEK_OF_MONTH;
			case DAY:
				return Calendar.DAY_OF_MONTH;
			default:
				break;
		}
		return 0;
	}

	public static String getKakeiboTopRowTitle(final Calendar nowDispCalendar,
			final KakeiboListViewType kakeiboListViewType, final Context context) {
		return getKakeiboTopRowTitle(nowDispCalendar, kakeiboListViewType, context, false);
	}

	/**
	 * KakeiboListViewTypeをもとに、一つ前の締め日に戻る
	 */
	public static void changeToPreviousStartCalenderByKakeiboListViewType(final Context context,
			final Calendar nowDispCalendar, final KakeiboListViewType kakeiboListViewType) {

		switch (kakeiboListViewType) {
			case YEAR:
				for (int i = 0; i < 12; i++) {
					nowDispCalendar.add(Calendar.DAY_OF_MONTH, -1);
					KakeiboUtils.changeToPreviousStartCalender(context, nowDispCalendar);
				}
				break;
			case HALF_YEAR:
				for (int i = 0; i < 6; i++) {
					nowDispCalendar.add(Calendar.DAY_OF_MONTH, -1);
					KakeiboUtils.changeToPreviousStartCalender(context, nowDispCalendar);
				}
				break;
			case THREE_MONTH:
				for (int i = 0; i < 3; i++) {
					nowDispCalendar.add(Calendar.DAY_OF_MONTH, -1);
					KakeiboUtils.changeToPreviousStartCalender(context, nowDispCalendar);
				}
				break;
			case MONTH:
				nowDispCalendar.add(Calendar.DAY_OF_MONTH, -1);
				KakeiboUtils.changeToPreviousStartCalender(context, nowDispCalendar);
				break;
			case WEEK:
				nowDispCalendar.add(Calendar.WEEK_OF_MONTH, -1);
				break;
			case DAY:
				nowDispCalendar.add(Calendar.DAY_OF_MONTH, -1 * 1);
				break;
			default:
				break;
		}

	}

	/**
	 * KakeiboListViewTypeをもとに、一つ先の締め日へ進む
	 */
	public static void changeToNextStartCalenderByKakeiboListViewType(final Context context,
			final Calendar nowDispCalendar, final KakeiboListViewType kakeiboListViewType) {
		switch (kakeiboListViewType) {
			case YEAR:
				for (int i = 0; i < 12; i++) {
					nowDispCalendar.add(Calendar.DAY_OF_MONTH, 1);
					KakeiboUtils.changeToNextStartCalender(context, nowDispCalendar);
				}
				break;
			case HALF_YEAR:
				for (int i = 0; i < 6; i++) {
					nowDispCalendar.add(Calendar.DAY_OF_MONTH, 1);
					KakeiboUtils.changeToNextStartCalender(context, nowDispCalendar);
				}
				break;
			case THREE_MONTH:
				for (int i = 0; i < 3; i++) {
					nowDispCalendar.add(Calendar.DAY_OF_MONTH, 1);
					KakeiboUtils.changeToNextStartCalender(context, nowDispCalendar);
				}
				break;
			case MONTH:
				nowDispCalendar.add(Calendar.DAY_OF_MONTH, 1);
				KakeiboUtils.changeToNextStartCalender(context, nowDispCalendar);
				break;
			case WEEK:
				nowDispCalendar.add(Calendar.WEEK_OF_MONTH, 1);
				break;
			case DAY:
				nowDispCalendar.add(Calendar.DAY_OF_MONTH, 1);
				break;
			default:
				break;
		}
	}

	/** 先頭行のタイトル(日付部分)を返す */
	public static String getKakeiboTopRowTitle(final Calendar nowDispCalendar,
			final KakeiboListViewType kakeiboListViewType, final Context context, final boolean isSingleLine) {

		String title = "";
		Calendar cal = Calendar.getInstance();
		cal.setTime(nowDispCalendar.getTime());
		Calendar endCal = Calendar.getInstance();
		Resources resources = context.getResources();
		String yearFormat = resources.getString(R.string.year_format);
		String monthFormat = resources.getString(R.string.month_format);
		String dayFormat = resources.getString(R.string.day_format);
		// 参考： Jan Feb Mar Apr May June July Aug Sep Oct Nov Dec
		switch (kakeiboListViewType) {
			case YEAR:

				if (KakeiboUtils.isMonthStartDaySetting(context)) {
					//now 2010年12月19日 startDay 25日
					//2009年12月25日～2010年12月24日  | Dec 25 ,2009 - Dec 24 ,2010

					//now 2010年12月19日 startDay 15日
					//2010年12月15日～2011年12月14日  | Dec 15 ,2010 - Dec 14 ,2011

					//now 2010年11月19日 startDay 25日
					//2009年12月25日～2010年12月24日  | Dec 25 ,2009 - Dec 24 ,2010

					//now 2010年11月19日 startDay 15日
					//2009年12月25日～2010年12月24日  | Dec 25 ,2009 - Dec 24 ,2010

					endCal.setTime(cal.getTime());
					endCal.add(Calendar.YEAR, +1);
					endCal.set(Calendar.DAY_OF_MONTH, KakeiboUtils.getMonthStartDay(context) - 1);

					if (KakeiboUtils.isJapan()) {
						title += (String) DateFormat.format(yearFormat + monthFormat + dayFormat, cal);
						title += "～\n";
						title += (String) DateFormat.format(yearFormat + monthFormat + dayFormat, endCal);
					} else {
						title += DateFormat.getMediumDateFormat(context).format(cal.getTime());
						title += " -\n";
						title += DateFormat.getMediumDateFormat(context).format(endCal.getTime());
					}

				} else {
					title = (String) DateFormat.format(yearFormat, cal);
				}
				break;
			case HALF_YEAR:

				//2010年1月～6月 | Jan - June ,2010

				endCal.setTime(cal.getTime());
				if (KakeiboUtils.isMonthStartDaySetting(context)) {
					endCal.add(Calendar.MONTH, 6);
					endCal.set(Calendar.DAY_OF_MONTH, KakeiboUtils.getMonthStartDay(context) - 1);
				} else {
					endCal.add(Calendar.MONTH, 5);
				}
				if (KakeiboUtils.isMonthStartDaySetting(context)) {

					if (KakeiboUtils.isJapan()) {
						title += (String) DateFormat.format(yearFormat + monthFormat + dayFormat, cal);
						title += "～\n";
						title += (String) DateFormat.format(yearFormat + monthFormat + dayFormat, endCal);
					} else {
						title += DateFormat.getMediumDateFormat(context).format(cal.getTime());
						title += " -\n";
						title += DateFormat.getMediumDateFormat(context).format(endCal.getTime());
					}
				} else {

					if (Locale.JAPAN.equals(Locale.getDefault())) {
						title = (String) DateFormat.format(yearFormat + monthFormat, cal);
						title += "～";
						title += (String) DateFormat.format(monthFormat, endCal);
					} else {
						title = (String) DateFormat.format(monthFormat, cal);
						title += " - ";
						title += (String) DateFormat.format(monthFormat, endCal);
						title += " ,";
						title += (String) DateFormat.format(yearFormat, cal);
					}
				}

				break;
			case THREE_MONTH:
				//2010年1月～3月 | Jan - Mar ,2010

				endCal.setTime(cal.getTime());
				if (KakeiboUtils.isMonthStartDaySetting(context)) {
					endCal.add(Calendar.MONTH, 3);
					endCal.set(Calendar.DAY_OF_MONTH, KakeiboUtils.getMonthStartDay(context) - 1);
				} else {
					endCal.add(Calendar.MONTH, 2);
				}

				if (KakeiboUtils.isMonthStartDaySetting(context)) {

					if (KakeiboUtils.isJapan()) {
						title += (String) DateFormat.format(yearFormat + monthFormat + dayFormat, cal);
						title += "～\n";
						title += (String) DateFormat.format(yearFormat + monthFormat + dayFormat, endCal);
					} else {
						title += DateFormat.getMediumDateFormat(context).format(cal.getTime());
						title += " -\n";
						title += DateFormat.getMediumDateFormat(context).format(endCal.getTime());
					}
				} else {
					if (Locale.JAPAN.equals(Locale.getDefault())) {
						title = (String) DateFormat.format(yearFormat + monthFormat, cal);
						title += "～";
						title += (String) DateFormat.format(monthFormat, endCal);
					} else {
						title = (String) DateFormat.format(monthFormat, cal);
						title += " - ";
						title += (String) DateFormat.format(monthFormat, endCal);
						title += " ,";
						title += (String) DateFormat.format(yearFormat, cal);
					}
				}
				break;
			case MONTH:
				if (KakeiboUtils.isMonthStartDaySetting(context)) {
					endCal.setTime(cal.getTime());
					endCal.add(Calendar.MONTH, 1);
					endCal.set(Calendar.DAY_OF_MONTH, KakeiboUtils.getMonthStartDay(context) - 1);
					if (KakeiboUtils.isJapan()) {
						title += (String) DateFormat.format(yearFormat + monthFormat + dayFormat, cal);
						title += "～\n";
						title += (String) DateFormat.format(yearFormat + monthFormat + dayFormat, endCal);
					} else {
						title += DateFormat.getMediumDateFormat(context).format(cal.getTime());
						title += " -\n";
						title += DateFormat.getMediumDateFormat(context).format(endCal.getTime());
					}
				} else {
					if (Locale.JAPAN.equals(Locale.getDefault())) {
						title = (String) DateFormat.format(yearFormat + monthFormat, cal);
					} else {
						title = (String) DateFormat.format(monthFormat + " ," + yearFormat, cal);
					}
				}
				break;
			case WEEK:
				//2010年8月9日～8月15日 | Aug 9 - Aug 15 ,2010
				SharedPreferences defaultSharedPreferences = PreferenceManager
						.getDefaultSharedPreferences(context);
				String firstDayOfWeeks = defaultSharedPreferences.getString(KakeiboConsts.PREFERENCE_KEY_FIRST_DAY_OF_WEEKS,
																			null);
				int firstDayOfWeekValue = Calendar.MONDAY;

				if ("monday".equals(firstDayOfWeeks)) {
					firstDayOfWeekValue = Calendar.MONDAY;
				} else if ("sunday".equals(firstDayOfWeeks)) {
					firstDayOfWeekValue = Calendar.SUNDAY;
				}
				while (cal.get(Calendar.DAY_OF_WEEK) != firstDayOfWeekValue) {
					cal.add(Calendar.DAY_OF_MONTH, -1);
				}
				if (Locale.JAPAN.equals(Locale.getDefault())) {
					title = (String) DateFormat.format(yearFormat + monthFormat + dayFormat, cal);
					if (!isSingleLine) {
						title += "\n 　　";
					}
					title += "～";
					cal.add(Calendar.DAY_OF_WEEK, 6);
					title += (String) DateFormat.format(monthFormat + dayFormat, cal);
				} else {
					char[] dateFormatOrder = DateFormat.getDateFormatOrder(context);
					if (DateFormat.DATE == dateFormatOrder[0]) {
						title = (String) DateFormat.format(dayFormat + " " + monthFormat, cal);
					} else {
						title = (String) DateFormat.format(monthFormat + " " + dayFormat, cal);
					}
					title += " - ";
					if (!isSingleLine) {
						title += "\n";
					}
					cal.add(Calendar.DAY_OF_WEEK, 6);
					if (DateFormat.DATE == dateFormatOrder[0]) {
						title += (String) DateFormat.format(dayFormat + " " + monthFormat, cal);
					} else {
						title += (String) DateFormat.format(monthFormat + " " + dayFormat, cal);
					}
					title += "  ,";
					title += (String) DateFormat.format(yearFormat, cal);
				}

				break;
			case DAY:
				//2010年8月9日  | Aug 9 ,2010
				if (Locale.JAPAN.equals(Locale.getDefault())) {
					title = (String) DateFormat.format(yearFormat + monthFormat + dayFormat + "(E)", cal);
				} else {
					title = DateFormat.getMediumDateFormat(context).format(cal.getTime());
				}
				break;
			default:
				break;
		}

		return title;
	}

	/** 画面タイトルを返す */
	public static String getTitle(final KakeiboListViewType kakeiboListViewType) {

		String title = "";
		boolean isJapan = KakeiboUtils.isJapan();
		switch (kakeiboListViewType) {
			case YEAR:
				if (isJapan) {
					title = "１年";
				} else {
					title = "One year";
				}
				break;

			case HALF_YEAR:
				if (isJapan) {
					title = "半年";
				} else {
					title = "half year";
				}

				break;

			case THREE_MONTH:
				if (isJapan) {
					title = "３ヶ月";
				} else {
					title = "Three months";
				}

				break;

			case MONTH:
				if (isJapan) {
					title = "１ヶ月";
				} else {
					title = "One month";
				}

				break;

			case WEEK:
				if (isJapan) {
					title = "１週間";
				} else {
					title = "One week";
				}

				break;

			case DAY:
				if (isJapan) {
					title = "１日";
				} else {
					title = "One day";
				}

				break;

			default:
				break;
		}
		return title;
	}

}
