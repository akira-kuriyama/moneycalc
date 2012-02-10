package com.marronst.moneycalc.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.widget.Toast;

import com.marronst.moneycalc.R;
import com.marronst.moneycalc.activity.RegisterRecordActivity;
import com.marronst.moneycalc.consts.KakeiboConsts;
import com.marronst.moneycalc.consts.KakeiboConsts.KakeiboListViewType;

public class KakeiboUtils {

	private static String unit;
	private static String unitPosition;
	private static Integer startDay;

	/** トーストを表示する */
	public static void toastShow(final Context context, final int messageResId) {
		Toast toast = Toast.makeText(context, messageResId, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();

	}

	/** トーストを長めに表示する */
	public static void toastShowLong(final Context context, final int messageResId) {
		Toast toast = Toast.makeText(context, messageResId, Toast.LENGTH_LONG);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();

	}

	/** ノーティファケーション開始する */
	public static void startNotification(final Context content, NotificationManager notificationManager) {
		Notification notification = new Notification();
		notification.icon = R.drawable.super_mario_coin;
		Resources resources = content.getResources();
		final CharSequence appName = resources.getText(R.string.app_name);
		notification.tickerText = appName;
		notification.number = 0;
		String contentText = resources.getString(R.string.notification_message);
		notification.setLatestEventInfo(content, appName, contentText, //
										getPendingIntent(content));
		notification.flags = Notification.FLAG_NO_CLEAR;
		notificationManager = (NotificationManager) content.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(KakeiboConsts.KAKEIBO_START_NOTIFICATION_ID, notification);
	}

	/** ノーティファケーションからアクティビティを起動するためのIntentを返す */
	private static PendingIntent getPendingIntent(final Context content) {
		Intent intent = new Intent(content, RegisterRecordActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(content, 0, intent,
																PendingIntent.FLAG_UPDATE_CURRENT);

		return pendingIntent;
	}

	/** ノーティファケーションをキャンセルする　 */
	public static void cancelNotification(final NotificationManager notificationManager) {
		notificationManager.cancel(KakeiboConsts.KAKEIBO_START_NOTIFICATION_ID);
	}

	/**
	 * 一番近い一つ後の締め日に戻る
	 */
	public static void changeToPreviousStartCalender(final Context context, final Calendar nowDispCalendar) {

		int monthStartDay = KakeiboUtils.getMonthStartDay(context);
		while (monthStartDay != nowDispCalendar.get(Calendar.DAY_OF_MONTH)) {
			nowDispCalendar.add(Calendar.DAY_OF_MONTH, -1);
		}
	}

	/**
	 * 一番近い一つ前の締め日へ進む
	 */
	public static void changeToNextStartCalender(final Context context, final Calendar nowDispCalendar) {

		int monthStartDay = KakeiboUtils.getMonthStartDay(context);
		while (monthStartDay != nowDispCalendar.get(Calendar.DAY_OF_MONTH)) {
			nowDispCalendar.add(Calendar.DAY_OF_MONTH, 1);
		}
	}

	/** 月の開始日を返す  */
	public static String getStartCal(final Calendar startCal, final KakeiboListViewType type,
			final Context context) {
		Calendar startCal2 = getStartCalInner(startCal, type, context);
		StringBuilder builder = new StringBuilder();

		final String slash = "/";
		builder.append(startCal2.get(Calendar.YEAR));
		builder.append(slash);
		builder.append(startCal2.get(Calendar.MONTH));
		builder.append(slash);
		builder.append(startCal2.get(Calendar.DAY_OF_MONTH));

		return builder.toString();
	}

	/** 月の開始日を返すためのインナーメソッド */
	private static Calendar getStartCalInner(final Calendar startCal, final KakeiboListViewType type,
			final Context context) {
		startCal.set(Calendar.HOUR_OF_DAY, 0);
		startCal.set(Calendar.MINUTE, 0);
		startCal.set(Calendar.SECOND, 0);
		startCal.set(Calendar.MILLISECOND, 0);

		switch (type) {
			case YEAR:
				if (KakeiboUtils.isMonthStartDaySetting(context)) {

					Calendar jCal = Calendar.getInstance();
					jCal.setTime(startCal.getTime());
					jCal.set(Calendar.YEAR, startCal.get(Calendar.YEAR));
					jCal.set(Calendar.MONTH, Calendar.DECEMBER);
					jCal.set(Calendar.DAY_OF_MONTH, KakeiboUtils.getMonthStartDay(context));
					if (jCal.after(startCal)) {
						jCal.add(Calendar.YEAR, -1);
					}
					startCal.set(jCal.get(Calendar.YEAR), jCal.get(Calendar.MONTH), jCal
							.get(Calendar.DAY_OF_MONTH));
				} else {
					startCal.set(Calendar.MONTH, Calendar.JANUARY);
					startCal.set(Calendar.DAY_OF_MONTH, 1);
				}
				break;
			case HALF_YEAR:
				if (KakeiboUtils.isMonthStartDaySetting(context)) {
					while (startCal.get(Calendar.MONTH) != Calendar.DECEMBER && //
							startCal.get(Calendar.MONTH) != Calendar.JUNE) {
						startCal.add(Calendar.MONTH, -1);
					}
				} else {
					while (startCal.get(Calendar.MONTH) != Calendar.JANUARY && //
							startCal.get(Calendar.MONTH) != Calendar.JULY) {
						startCal.add(Calendar.MONTH, -1);
					}
					startCal.set(Calendar.DAY_OF_MONTH, 1);
				}
				break;
			case THREE_MONTH:
				if (KakeiboUtils.isMonthStartDaySetting(context)) {
					while (startCal.get(Calendar.MONTH) != Calendar.MARCH
							&& startCal.get(Calendar.MONTH) != Calendar.JUNE
							&& startCal.get(Calendar.MONTH) != Calendar.SEPTEMBER
							&& startCal.get(Calendar.MONTH) != Calendar.DECEMBER) {
						startCal.add(Calendar.MONTH, -1);
					}
				} else {
					while (startCal.get(Calendar.MONTH) != Calendar.JANUARY
							&& startCal.get(Calendar.MONTH) != Calendar.APRIL
							&& startCal.get(Calendar.MONTH) != Calendar.JULY
							&& startCal.get(Calendar.MONTH) != Calendar.OCTOBER) {
						startCal.add(Calendar.MONTH, -1);
					}
					startCal.set(Calendar.DAY_OF_MONTH, 1);
				}
				break;
			case MONTH:

				if (KakeiboUtils.isMonthStartDaySetting(context)) {
					int monthStartDay = KakeiboUtils.getMonthStartDay(context);
					while (monthStartDay != startCal.get(Calendar.DAY_OF_MONTH)) {
						startCal.add(Calendar.DAY_OF_MONTH, -1);
					}
				} else {
					startCal.set(Calendar.DAY_OF_MONTH, 1);
				}

				break;
			case WEEK:

				SharedPreferences defaultSharedPreferences = PreferenceManager
						.getDefaultSharedPreferences(context);
				String firstDayOfWeeks = defaultSharedPreferences
						.getString(KakeiboConsts.PREFERENCE_KEY_FIRST_DAY_OF_WEEKS, null);
				int firstDayOfWeekValue = Calendar.MONDAY;

				if ("monday".equals(firstDayOfWeeks)) {
					firstDayOfWeekValue = Calendar.MONDAY;
				} else if ("sunday".equals(firstDayOfWeeks)) {
					firstDayOfWeekValue = Calendar.SUNDAY;
				}
				while (startCal.get(Calendar.DAY_OF_WEEK) != firstDayOfWeekValue) {
					startCal.add(Calendar.DAY_OF_YEAR, -1);
				}
				break;
			case DAY:

				break;

			default:
				break;
		}

		return startCal;
	}

	/**
	 * 縦向きかどうか
	 * @param activity Activity
	 * @return 縦向きならtrue
	 */
	public static boolean isPortrait(final Context context) {
		return Configuration.ORIENTATION_PORTRAIT == context.getResources().getConfiguration().orientation;
	}

	/**
	 * 画面が横向きならtrueを返す
	 * @param activity Activity
	 * @return 画面が横向きならtrue
	 */
	public static boolean isLandscape(final Context context) {
		return !isPortrait(context);

	}

	/** ロケールが日本ならtrueを返す */
	public static boolean isJapan() {
		return Locale.JAPAN.equals(Locale.getDefault());
	}

	/** エンコードを返す */
	public static String getEncoding() {
		String encoding;
		if (KakeiboUtils.isJapan()) {
			encoding = KakeiboConsts.MS932;
		} else {
			encoding = KakeiboConsts.UTF_8;
		}
		return encoding;
	}

	/** 月の開始日を設定しているかどうか */
	public static boolean isMonthStartDaySetting(final Context context) {
		return getMonthStartDay(context) != KakeiboConsts.DEFAULT_START_DAY;
	}

	/**
	 * 月の開始日を返す
	 * @return　月の開始日
	 */
	public static int getMonthStartDay(final Context context) {
		if (startDay == null) {
			SharedPreferences defaultSharedPreferences = PreferenceManager
					.getDefaultSharedPreferences(context);
			String startDayStr = defaultSharedPreferences
					.getString(KakeiboConsts.PREFERENCE_KEY_FIRST_DAY_OF_MONTH, Integer
							.toString(KakeiboConsts.DEFAULT_START_DAY));
			startDay = Integer.valueOf(startDayStr);
		}

		return startDay;
	}

	/** 通貨単位を返す */
	public static String getCurrencyUnit(final Context context) {
		if (TextUtils.isEmpty(unit)) {
			String unitTmp = KakeiboConsts.EMPTY;
			SharedPreferences defaultSharedPreferences = PreferenceManager
					.getDefaultSharedPreferences(context);
			unitTmp = defaultSharedPreferences.getString(KakeiboConsts.PREFERENCE_KEY_CURRENCY_UNIT, null);
			if (unitTmp == null) {
				if (isJapan()) {
					unitTmp = KakeiboFormatUtils.unit_ja;
				} else {
					unitTmp = KakeiboFormatUtils.unit_us;
				}
			}
			if (!KakeiboFormatUtils.unit_ja.equals(unitTmp)) {
				if (isCurrencyUnitPositionFront(context)) {
					unitTmp = unitTmp + " ";
				} else {
					unitTmp = " " + unitTmp;
				}
			}
			unit = unitTmp;
		}
		return unit;
	}

	/** 通貨単位をセットする */
	public static void setCurrencyUnit(final String unit, final Context context) {
		String unitTmp = unit;
		if (!KakeiboFormatUtils.unit_ja.equals(unit)) {
			if (isCurrencyUnitPositionFront(context)) {
				unitTmp = unitTmp + " ";
			} else {
				unitTmp = " " + unitTmp;
			}
		}
		KakeiboUtils.unit = unitTmp;
	}

	/**
	 * SDカードがあれば、trueを返す。
	 */
	public static boolean isMediaMounted() {
		return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
	}

	/**　通貨単位の位置がフロントかどうか返す */
	public static boolean isCurrencyUnitPositionFront(final Context context) {
		if (unitPosition == null) {
			SharedPreferences defaultSharedPreferences = PreferenceManager
					.getDefaultSharedPreferences(context);
			String unitPositionTmp = defaultSharedPreferences
					.getString(KakeiboConsts.PREFERENCE_KEY_CURRENCY_UNIT_POSITION, null);
			if (unitPositionTmp == null) {
				if (isJapan()) {
					unitPositionTmp = "back";
				} else {
					unitPositionTmp = "front";

				}
			}
			unitPosition = unitPositionTmp;
		}

		return unitPosition.equals("front");
	}

	/**　通貨単位の位置をセットする */
	public static void setUnitPosition(final String unitPosition) {
		KakeiboUtils.unitPosition = unitPosition;
	}

	/** 月の開始日をセットする */
	public static void setStartDay(final int startDay) {
		KakeiboUtils.startDay = startDay;
	}

	/** ZIPファイルを作成する */
	public static void makeZipFile(final List<String> filePathList, final String zipFilePath)
			throws FileNotFoundException, IOException {
		ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(new File(zipFilePath)));
		byte[] buf = new byte[1024];

		FileInputStream fileInputStream = null;
		for (String path : filePathList) {
			fileInputStream = new FileInputStream(path);
			ZipEntry zipEntry = new ZipEntry(path);
			zipOutputStream.putNextEntry(zipEntry);

			int len = 0;
			while ((len = fileInputStream.read(buf)) != -1) {
				zipOutputStream.write(buf, 0, len);
			}
			fileInputStream.close();
			zipOutputStream.closeEntry();
		}
		zipOutputStream.close();
	}

	/** 端末情報を取得する */
	public static String getDeviceInfo(final Activity activity) {

		String device = Build.DEVICE;
		String brand = Build.BRAND;
		String sdk = Build.VERSION.SDK;
		String model = Build.MODEL;
		String versionName = getApplicationVersionName(activity.getApplicationContext());
		DisplayMetrics metrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		int widthPixels = metrics.widthPixels;
		int heightPixels = metrics.heightPixels;

		StringBuilder deviceInfo = new StringBuilder();
		deviceInfo.append("[");
		deviceInfo.append("brand=" + brand);
		deviceInfo.append(", device=" + device);
		deviceInfo.append(", sdk=" + sdk);
		deviceInfo.append(", model=" + model);
		deviceInfo.append(", versionName=" + versionName);
		deviceInfo.append(", widthPixels=" + widthPixels);
		deviceInfo.append(", heightPixels=" + heightPixels);
		deviceInfo.append("]");
		return deviceInfo.toString();
	}

	/** 残高繰越機能を使用するかどうか  */
	public static boolean isUseCarryover(final Context context) {
		SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		int balanceCalcMethod = defaultSharedPreferences
				.getInt(KakeiboConsts.PREFERENCE_KEY_BALANCE_CALC_METHOD,
						KakeiboConsts.DEFAULT_BALANCE_CALC_METHOD);

		if (balanceCalcMethod == KakeiboConsts.BALANCE_CALC_METHOD_NONE) {
			return false;
		} else {
			boolean isUseCarryover = defaultSharedPreferences
					.getBoolean(KakeiboConsts.PREFERENCE_KEY_IS_USE_CARRYOVER, false);
			return isUseCarryover;
		}
	}

	/** カテゴリごとの残高繰越機能を使用するかどうか  */
	public static boolean isUseCategoryCarryover(final Context context) {
		SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		int balanceCalcMethod = defaultSharedPreferences
				.getInt(KakeiboConsts.PREFERENCE_KEY_BALANCE_CALC_METHOD,
						KakeiboConsts.DEFAULT_BALANCE_CALC_METHOD);

		if (balanceCalcMethod == KakeiboConsts.BALANCE_CALC_METHOD_NONE
				|| balanceCalcMethod == KakeiboConsts.BALANCE_CALC_METHOD_INCOME_MINUS_EXPENSE) {
			return false;
		} else {
			boolean isUseCategoryCarryover = defaultSharedPreferences
					.getBoolean(KakeiboConsts.PREFERENCE_KEY_IS_USE_CATEGORY_CARRYOVER, false);
			return isUseCategoryCarryover;
		}
	}

	/** 残高算出方法を返す  */
	public static int getBalanceCalcMethod(final Context context) {
		SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		int balanceCalcMethod = defaultSharedPreferences
				.getInt(KakeiboConsts.PREFERENCE_KEY_BALANCE_CALC_METHOD,
						KakeiboConsts.DEFAULT_BALANCE_CALC_METHOD);
		return balanceCalcMethod;
	}

	private static String getApplicationVersionName(final Context context) {
		String versionName = "versionName can not get..";
		try {

			if (context != null) {
				PackageInfo sPackInfo = context.getPackageManager().getPackageInfo(context.getPackageName(),
																					0);
				versionName = sPackInfo.versionName;
			}
		} catch (NameNotFoundException e) {
			//握りつぶす。
		}
		return versionName;
	}

	/** リストが空だったらtrue */
	public static <T> boolean isEmpty(final List<T> list) {
		return list == null || list.isEmpty();
	}

	/** リストが空じゃなかったらtrue */
	public static <T> boolean isNotEmpty(final List<T> list) {
		return !isEmpty(list);
	}

	/** リストを結合文字列で結合する */
	public static <T> String join(final List<T> list, final String separator) {
		StringBuilder str = new StringBuilder(KakeiboConsts.EMPTY);
		if (isEmpty(list)) {
			return str.toString();
		}
		for (T t : list) {
			if (t != null) {
				str.append(t.toString()).append(separator);
			}
		}
		String result = KakeiboConsts.EMPTY;
		if (str.length() > 0) {
			result = str.substring(0, str.length() - 1);
		}
		return result;

	}
}
