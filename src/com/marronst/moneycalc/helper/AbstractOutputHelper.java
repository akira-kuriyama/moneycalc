package com.marronst.moneycalc.helper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.format.DateFormat;
import au.com.bytecode.opencsv.CSVWriter;

import com.marronst.moneycalc.R;
import com.marronst.moneycalc.consts.KakeiboConsts;
import com.marronst.moneycalc.entity.Budget;
import com.marronst.moneycalc.entity.Category;
import com.marronst.moneycalc.entity.Filter;
import com.marronst.moneycalc.entity.HouseKeepingBook;
import com.marronst.moneycalc.utils.KakeiboUtils;

public abstract class AbstractOutputHelper {

	public final String appDirPath = Environment.getExternalStorageDirectory().toString() + "/moneyCalc/";

	public String tmpDirPath;

	protected static final String SLASH = "/";

	protected static final String DELEMITER = "_";

	private StringBuilder csvDateFormat = null;

	private final String charSet = KakeiboUtils.getEncoding();

	/** カテゴリCSVファイル名*/
	public String categoryCsvFileName = "category.csv";

	/** 予算CSVファイル名*/
	public String budgetCsvFileName = "budget.csv";

	/** フィルターCSVファイル名*/
	public String filterCsvFileName = "filter.csv";

	/** 家計簿データCSVファイル名*/
	public String kakeiboDataCsvFileName = "kakeiboData.csv";

	/** CSV日付フォーマットファイル名 */
	public final String CSV_DATE_FORMAT_TXT_FILE_NAME = "csvDateFormat.txt";

	/** 端末情報ファイル名 */
	public final String DEVICE_INFO_TXT_FILE_NAME = "deviceInfo.txt";

	/** 設定CSVファイル名*/
	public String PREFERENCE_CSV_FILE_NAME = "preference.csv";

	/** カテゴリバックアップCSVファイルを作成する  */
	public void createCategoryCsvBackup(final Context context, final List<Category> categoryList,
			final Map<Integer, String> categoryIdNameMap, final String backupFilePath)
			throws FileNotFoundException, IOException, UnsupportedEncodingException {
		FileOutputStream output = new FileOutputStream(backupFilePath);
		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(output, charSet);
		CSVWriter csvWriter = new CSVWriter(outputStreamWriter);

		//列名の書き込み
		final Resources resources = context.getResources();
		String categoryName = resources.getString(R.string.category_column_name_category_name);//カテゴリ名
		String position = resources.getString(R.string.category_column_name_position);//表示順
		String incomeFlg = resources.getString(R.string.category_column_name_incomeFlg);//収支属性
		String[] categoryColumnNameArray = { categoryName, position, incomeFlg };
		csvWriter.writeNext(categoryColumnNameArray);

		//データの書き込み
		String[] categoryArray = new String[3];
		for (Category category : categoryList) {
			categoryArray[0] = category.categoryName;
			categoryArray[1] = Integer.toString(category.position);
			categoryArray[2] = Integer.toString(category.incomeFlg);

			csvWriter.writeNext(categoryArray);

			categoryIdNameMap.put(category.id, category.categoryName);
		}

		csvWriter.close();
	}

	/**　予算バックアップCSVファイルを作成する */
	public void createBudgetCsvBackup(final Context context, final List<Budget> budgetList,
			final Map<Integer, String> categoryIdNameMap, final String backupFilePath)
			throws FileNotFoundException, IOException, UnsupportedEncodingException {

		FileOutputStream output = new FileOutputStream(backupFilePath);
		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(output, charSet);
		CSVWriter csvWriter = new CSVWriter(outputStreamWriter);

		//列名の書き込み
		final Resources resources = context.getResources();
		String categoryName = resources.getString(R.string.budget_column_name_category_name);//カテゴリ名
		String yearMonth = resources.getString(R.string.budget_column_name_yearmonth);//年月
		String budgetPrice = resources.getString(R.string.budget_column_name_budget_price);//予算金額
		String[] budgetColumnNameArray = { categoryName, yearMonth, budgetPrice };
		csvWriter.writeNext(budgetColumnNameArray);

		//データの書き込み
		String[] budgetArray = new String[3];
		for (Budget budget : budgetList) {
			final String categoryId;
			if (budget.categoryId == null) {
				categoryId = "";
			} else {
				categoryId = categoryIdNameMap.get(budget.categoryId);
				if (TextUtils.isEmpty(categoryId)) {
					continue;
				}
			}
			budgetArray[0] = categoryId;
			budgetArray[1] = Integer.toString(budget.yearMonth);
			budgetArray[2] = Long.toString(budget.budgetPrice);

			csvWriter.writeNext(budgetArray);
		}

		csvWriter.close();
	}

	/** フィルターバックアップCSVファイルを作成する  */
	public void createFilterCsvBackup(final Context context, final List<Filter> filterList,
			final Map<Integer, String> categoryIdNameMap, final String backupFilePath)
			throws FileNotFoundException, IOException, UnsupportedEncodingException {
		FileOutputStream output = new FileOutputStream(backupFilePath);
		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(output, charSet);
		CSVWriter csvWriter = new CSVWriter(outputStreamWriter);

		//列名の書き込み
		final Resources resources = context.getResources();
		String filterName = resources.getString(R.string.filter_column_name_filter_name);//カテゴリ名
		String categoryIdList = resources.getString(R.string.filter_column_name_category_id_list);//表示順
		String isDefault = resources.getString(R.string.filter_column_name_is_default);//収支属性
		String[] categoryColumnNameArray = { filterName, categoryIdList, isDefault };
		csvWriter.writeNext(categoryColumnNameArray);

		//データの書き込み
		String[] categoryArray = new String[3];
		for (Filter filter : filterList) {
			categoryArray[0] = filter.filterName;
			List<String> categoryNameList = new ArrayList<String>();
			if (!TextUtils.isEmpty(filter.categoryIdList)) {
				String[] split = filter.categoryIdList.split(",");
				for (String categoryIdStr : split) {
					String categoryName = categoryIdNameMap.get(Integer.valueOf(categoryIdStr));
					if (!TextUtils.isEmpty(categoryName)) {
						categoryNameList.add(URLEncoder.encode(categoryName));
					}
				}
			}
			categoryArray[1] = KakeiboUtils.join(categoryNameList, ",");
			categoryArray[2] = Integer.toString(filter.isDefault);
			csvWriter.writeNext(categoryArray);
		}

		csvWriter.close();
	}

	/**　家計簿バックアップCSVファイルを作成する */
	public void createKakeiboCsvBackup(final Context context,
			final List<HouseKeepingBook> houseKeepingBookList, final Map<Integer, String> categoryIdNameMap,
			final String backupFilePath) throws FileNotFoundException, IOException,
			UnsupportedEncodingException {

		FileOutputStream output = new FileOutputStream(backupFilePath);
		OutputStreamWriter outputStreamWriter = new OutputStreamWriter(output, charSet);
		CSVWriter csvWriter = new CSVWriter(outputStreamWriter);

		//列名の書き込み
		final Resources resources = context.getResources();
		String price = resources.getString(R.string.kakeibo_data_column_name_price);//金額
		String categoryName = resources.getString(R.string.kakeibo_data_column_name_category_name);//カテゴリ名
		String registerDate = resources.getString(R.string.kakeibo_data_column_name_registerDate);//購入日時
		String memo = resources.getString(R.string.kakeibo_data_column_name_memo);//メモ
		String importVersion = resources.getString(R.string.kakeibo_data_column_name_importVersion);//インポートバージョン
		String place = resources.getString(R.string.kakeibo_data_column_name_place);//購入場所
		String latitude = resources.getString(R.string.kakeibo_data_column_name_latitude);//購入緯度
		String longitude = resources.getString(R.string.kakeibo_data_column_name_longitude);//購入経度
		String[] categoryColumnNameArray = { price, categoryName, registerDate, memo, importVersion, place,
				latitude, longitude };
		csvWriter.writeNext(categoryColumnNameArray);

		String[] houseKeepingBookArray = new String[8];
		int size = houseKeepingBookList.size();
		for (int i = 0; i < size; i++) {
			HouseKeepingBook houseKeepingBook = houseKeepingBookList.get(i);
			houseKeepingBookArray[0] = Integer.toString(houseKeepingBook.price);
			houseKeepingBookArray[1] = categoryIdNameMap.get(houseKeepingBook.categoryId);
			houseKeepingBookArray[2] = formatDateTimeToStringForCsv(context, houseKeepingBook.registerDate);
			houseKeepingBookArray[3] = houseKeepingBook.memo;
			houseKeepingBookArray[4] = houseKeepingBook.importVersion == null ? KakeiboConsts.EMPTY : Integer
					.toString(houseKeepingBook.importVersion);
			houseKeepingBookArray[5] = houseKeepingBook.place;
			houseKeepingBookArray[6] = houseKeepingBook.latitude;
			houseKeepingBookArray[7] = houseKeepingBook.longitude;

			csvWriter.writeNext(houseKeepingBookArray);

		}

		csvWriter.close();
	}

	/** Dateフォーマット形式を保存 */
	public void createCsvDateFormat(final Context context, final String filePath) throws IOException,
			SecurityException {
		String csvDateFormat = getCsvDateFormat(context);
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(new File(filePath));
			fileOutputStream.write(csvDateFormat.getBytes());
			fileOutputStream.close();

		} catch (IOException e) {
			throw e;
		} catch (SecurityException e) {
			throw e;
		}

	}

	/**　端末情報を保存 */
	public void createDeviceInfoFile(final Activity activity, final String filePath) throws IOException,
			SecurityException {
		String deviceInfo = KakeiboUtils.getDeviceInfo(activity);
		try {
			FileOutputStream fileOutputStream = new FileOutputStream(new File(filePath));
			fileOutputStream.write(deviceInfo.getBytes());
			fileOutputStream.close();

		} catch (IOException e) {
			throw e;
		} catch (SecurityException e) {
			throw e;
		}

	}

	/** 設定データを保存する */
	public void createPreferenceText(final Context context, final String filePath) throws IOException,
			SecurityException {

		try {
			SharedPreferences defaultSharedPreferences = PreferenceManager
					.getDefaultSharedPreferences(context);
			FileOutputStream output = new FileOutputStream(filePath);
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(output, KakeiboConsts.UTF_8);
			CSVWriter csvWriter = new CSVWriter(outputStreamWriter);
			@SuppressWarnings("unchecked")//
			Map<String, Object> all = (Map<String, Object>) defaultSharedPreferences.getAll();
			if (all != null && all.size() > 0) {
				for (Entry<String, Object> entry : all.entrySet()) {
					String key = entry.getKey();
					Object value = entry.getValue();
					if (value == null) {
						value = KakeiboConsts.EMPTY;
					}
					String[] prefArray = { key, value.toString() };
					csvWriter.writeNext(prefArray);
				}
			}
			csvWriter.close();

		} catch (FileNotFoundException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		}
	}

	/** フォルダがなければ作成する */
	protected void makeDirIfNotExists(final String dirPath) throws SecurityException {
		try {

			File file0 = new File(appDirPath);
			if (!file0.exists()) {
				file0.mkdir();
			}
			File file = new File(dirPath);
			if (!file.exists()) {
				file.mkdir();
			}
		} catch (SecurityException e) {
			throw e;
		}
	}

	/** DateオブジェクトをCSV日付フォーマットして文字列にして返す */
	public String formatDateTimeToStringForCsv(final Context context, final Date date) {
		String formatter = getCsvDateFormat(context);
		String dateStr = (String) DateFormat.format(formatter, date);
		return dateStr;
	}

	/** CSV日付フォーマットを返す */
	public String getCsvDateFormat(final Context context) {
		if (csvDateFormat != null) {
			return csvDateFormat.toString();
		}

		csvDateFormat = new StringBuilder();
		char[] dateFormatOrder = DateFormat.getDateFormatOrder(context);
		for (char c : dateFormatOrder) {
			switch (c) {
				case DateFormat.DATE:
					csvDateFormat.append("dd");
					break;
				case DateFormat.MONTH:
					csvDateFormat.append("MM");

					break;
				case DateFormat.YEAR:
					csvDateFormat.append("yyyy");
					break;
				default:
					break;
			}
			csvDateFormat.append("/");
		}
		csvDateFormat = new StringBuilder(csvDateFormat.substring(0, csvDateFormat.length() - 1));
		csvDateFormat.append(" kk:mm");
		return csvDateFormat.toString();
	}

	/** 一時ディレクトの作成 */
	public void makeTempDir(final String baseDirPath) throws SecurityException {
		String path = null;

		Calendar cal = Calendar.getInstance();

		StringBuilder builder = new StringBuilder();
		builder.append(cal.get(Calendar.YEAR));
		builder.append(DELEMITER);
		builder.append(addZero(cal.get(Calendar.MONTH) + 1));
		builder.append(DELEMITER);
		builder.append(addZero(cal.get(Calendar.DAY_OF_MONTH)));
		builder.append(DELEMITER);
		builder.append(addZero(cal.get(Calendar.HOUR_OF_DAY)));
		builder.append(DELEMITER);
		builder.append(addZero(cal.get(Calendar.MINUTE)));
		builder.append(DELEMITER);
		builder.append(addZero(cal.get(Calendar.SECOND)));
		tmpDirPath = builder.toString();

		try {
			path = baseDirPath + tmpDirPath;
			File file = new File(path);
			if (!file.exists()) {
				file.mkdir();
			}
		} catch (SecurityException e) {
			throw e;
		}
	}

	/** ゼロパディングする */
	private StringBuilder addZero(final int i) {
		StringBuilder b = new StringBuilder();
		b.append(i);
		if (i < 10) {
			b.insert(0, "0");
		}
		return b;
	}

	/** 指定されたファイルまたはディレクトを再帰的に削除する */
	public void deleteFiles(final String path) {
		File file = new File(path);
		if (file.isFile()) {
			file.delete();
		}
		if (file.isDirectory()) {
			File[] listFiles = file.listFiles();
			if (listFiles == null || listFiles.length == 0) {
				file.delete();
			} else {
				for (File file2 : listFiles) {
					deleteFiles(file2.getPath());
				}
				file.delete();
			}
		}
	}

	/** ファイルパスに対するファイルがなければ作成する */
	protected void createFileIfNotExists(final String filePath) throws SecurityException, IOException {
		try {
			File file = new File(filePath);
			if (!file.exists()) {
				file.createNewFile();
			}
		} catch (SecurityException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		}
	}
}
