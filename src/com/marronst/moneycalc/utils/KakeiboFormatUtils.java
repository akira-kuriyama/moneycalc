package com.marronst.moneycalc.utils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.text.format.DateFormat;

import com.marronst.moneycalc.consts.KakeiboConsts;

public class KakeiboFormatUtils {
	private static final DecimalFormat priceFormat_Ja = new DecimalFormat("#,###");
	private static final DecimalFormat priceFormat = new DecimalFormat("#,##0.00");
	private static final DecimalFormat priceFormatForRegisterView = new DecimalFormat("#,##0.##");
	private static final DecimalFormat priceFormatWithDefimalOneForRegisterView = new DecimalFormat(
			"#,##0.0#");
	private static final DecimalFormat priceFormatWithDefimalTwoForRegisterView = new DecimalFormat(
			"#,##0.00");
	private static final SimpleDateFormat formatStringToDateForDBFormat = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss");
	private static final String formatDate_ja = "yyyy年MM月dd日";
	private static final String formatDate_youbi_ja = "yyyy年MM月dd日(E)";
	private static final String formatTime_ja = "kk時mm分";
	private static final String formatTime = "kk:mm";
	public static final String unit_ja = "円";
	public static final String unit_us = "$";
	private static final String dateTimeFormat_ja = formatDate_ja + " " + formatTime_ja;
	private static final String dateTimeFormatLineBreak_ja = formatDate_ja + "\n" + formatTime_ja;
	private static final SimpleDateFormat formatStringToDateTimeFormat_ja = new SimpleDateFormat(
			dateTimeFormat_ja);

	public static String formatPrice(final Long value, final Context context) {
		String price = formatPriceNoUnit(value);
		if (KakeiboUtils.isCurrencyUnitPositionFront(context)) {
			price = KakeiboUtils.getCurrencyUnit(context) + price;
		} else {
			price = price + KakeiboUtils.getCurrencyUnit(context);
		}
		return price;
	}

	public static String formatPriceNoUnit(final Long value) {
		Long tmpValue = value;
		if (tmpValue == null) {
			tmpValue = 0L;
		}
		if (KakeiboUtils.isJapan()) {
			return priceFormat_Ja.format(value);
		} else {
			BigDecimal valueBigDecimal = new BigDecimal(value);
			valueBigDecimal = valueBigDecimal.divide(new BigDecimal(100), 2, BigDecimal.ROUND_HALF_UP);
			return priceFormat.format(valueBigDecimal.doubleValue());
		}
	}

	public static String formatPriceForRegisterView(final Double value, final Context context) {
		String price = priceFormatForRegisterView.format(value);
		if (KakeiboUtils.isCurrencyUnitPositionFront(context)) {
			price = KakeiboUtils.getCurrencyUnit(context) + price;
		} else {
			price = price + KakeiboUtils.getCurrencyUnit(context);
		}
		return price;
	}

	public static String formatPriceWithDecimalOneForRegisterView(final Double value, final Context context) {
		String price = priceFormatWithDefimalOneForRegisterView.format(value);
		if (KakeiboUtils.isCurrencyUnitPositionFront(context)) {
			price = KakeiboUtils.getCurrencyUnit(context) + price;
		} else {
			price = price + KakeiboUtils.getCurrencyUnit(context);
		}
		return price;

	}

	public static String formatPriceWithDecimalTwoForRegisterView(final Double value, final Context context) {
		String price = priceFormatWithDefimalTwoForRegisterView.format(value);
		if (KakeiboUtils.isCurrencyUnitPositionFront(context)) {
			price = KakeiboUtils.getCurrencyUnit(context) + price;
		} else {
			price = price + KakeiboUtils.getCurrencyUnit(context);
		}
		return price;
	}

	public static Date formatStringToDateForDB(final String dateStr) {

		Date date;
		try {
			date = formatStringToDateForDBFormat.parse(dateStr);

		} catch (ParseException e) {
			date = null;
		}
		return date;
	}

	//	public static Date formatStringToDateTime(final String dateTimeStr) {
	//
	//		Date date;
	//		try {
	//			if (KakeiboUtils.isJapan()) {
	//				date = formatStringToDateTimeFormat_ja.parse(dateTimeStr);
	//			} else {
	//				java.text.DateFormat dateTimeInstance = java.text.DateFormat
	//						.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT, Locale
	//								.getDefault());
	//				date = dateTimeInstance.parse(dateTimeStr);
	//			}
	//		} catch (ParseException e) {
	//			date = null;
	//		}
	//		return date;
	//	}

	public static CharSequence formatDateToStringPlusYoubi(final Context context, final Date date) {
		if (date == null) {
			return KakeiboConsts.EMPTY;
		}
		String dateStr;
		if (KakeiboUtils.isJapan()) {
			dateStr = (String) DateFormat.format(formatDate_youbi_ja, date);
		} else {
			//外国の曜日のつけ方がいまいち分からないので、そのままにしておく
			dateStr = DateFormat.getDateFormat(context).format(date);
		}
		return dateStr;
	}

	public static String formatDateToString(final Context context, final Date date) {
		if (date == null) {
			return KakeiboConsts.EMPTY;
		}
		String dateStr;
		if (KakeiboUtils.isJapan()) {
			dateStr = (String) DateFormat.format(formatDate_ja, date);
		} else {
			dateStr = DateFormat.getDateFormat(context).format(date);
			//			String formatter = "";
			//			char[] dateFormatOrder = DateFormat.getDateFormatOrder(context);
			//			for (char c : dateFormatOrder) {
			//				switch (c) {
			//					case DateFormat.DATE:
			//						formatter += "dd";
			//						break;
			//					case DateFormat.MONTH:
			//						formatter += "MM";
			//
			//						break;
			//					case DateFormat.YEAR:
			//						formatter += "yyyy";
			//						break;
			//					default:
			//						break;
			//				}
			//				formatter += "/";
			//			}
			//			formatter = formatter.substring(0, formatter.length() - 1);
			//			dateStr = (String) DateFormat.format(formatter, date);
		}
		return dateStr;
	}

	public static String formatTimeToString(final Context context, final Date date) {
		if (date == null) {
			return KakeiboConsts.EMPTY;
		}

		String timeStr;
		if (KakeiboUtils.isJapan()) {
			timeStr = (String) DateFormat.format(formatTime_ja, date);
		} else {
			timeStr = (String) DateFormat.format(formatTime, date);
		}
		return timeStr;
	}

	public static String formatDateTimeToString(final Context context, final Date date) {
		return formatDateTimeToString(context, date, false);
	}

	public static String formatDateTimeToStringLineBreak(final Context context, final Date date) {
		return formatDateTimeToString(context, date, true);
	}

	private static String formatDateTimeToString(final Context context, final Date date,
			final boolean isLineBreak) {
		if (date == null) {
			return KakeiboConsts.EMPTY;
		}
		String format;
		if (KakeiboUtils.isJapan()) {
			if (isLineBreak) {
				format = dateTimeFormatLineBreak_ja;
			} else {
				format = dateTimeFormat_ja;
			}
		} else {
			String dateStr = formatDateToString(context, date);
			String timeStr = formatTimeToString(context, date);
			if (isLineBreak) {
				format = dateStr + "\n" + timeStr;
			} else {
				format = dateStr + " " + timeStr;
			}
		}

		return (String) DateFormat.format(format, date);
	}

	public enum DataFormatType {
		//	
			YYYYMMDD("yyyy-MM-dd"), //
			YYYYMMDDHHMMSS("yyyy-MM-dd HH:mm:ss"); //

		private final String pattern;

		private DataFormatType(final String pattern) {
			this.pattern = pattern;
		}

		public String getPattern() {
			return this.pattern;
		}
	}

	public static final String formatDate(final Date date, final DataFormatType type) {
		if (date == null || type == null) {
			return "";
		}

		SimpleDateFormat format = new SimpleDateFormat(type.getPattern());
		return format.format(date);
	}
}
