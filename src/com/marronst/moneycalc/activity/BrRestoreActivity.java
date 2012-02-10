package com.marronst.moneycalc.activity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mozilla.universalchardet.UniversalDetector;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import au.com.bytecode.opencsv.CSVReader;

import com.marronst.moneycalc.R;
import com.marronst.moneycalc.consts.KakeiboConsts;
import com.marronst.moneycalc.db.BudgetDao;
import com.marronst.moneycalc.db.CategoryDao;
import com.marronst.moneycalc.db.FilterDao;
import com.marronst.moneycalc.db.HouseKeepingBookDao;
import com.marronst.moneycalc.db.HouseKeepingBookOpenHelper;
import com.marronst.moneycalc.entity.Budget;
import com.marronst.moneycalc.entity.Category;
import com.marronst.moneycalc.entity.Filter;
import com.marronst.moneycalc.entity.HouseKeepingBook;
import com.marronst.moneycalc.helper.BackupRestoreHelper;
import com.marronst.moneycalc.helper.MenuHelper;
import com.marronst.moneycalc.helper.MenuHelper.MenuType;
import com.marronst.moneycalc.task.QuickToastTask;
import com.marronst.moneycalc.utils.KakeiboUtils;

public class BrRestoreActivity extends Activity {
	protected final String TAG = this.getClass().getSimpleName();

	protected SQLiteDatabase db;

	private HouseKeepingBookOpenHelper houseKeepingBookOpenHelper;
	private HouseKeepingBookDao houseKeepingBookDao;
	private CategoryDao categoryDao;
	private BudgetDao budgetDao;
	private FilterDao filterDao;

	private BackupRestoreHelper backupRestoreHelper;

	private RestoreTask restoreTask;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		Log.i(TAG, "onCreate start");

		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.setContentView(R.layout.br_restore);

		houseKeepingBookOpenHelper = new HouseKeepingBookOpenHelper(getApplicationContext());

		try {
			db = houseKeepingBookOpenHelper.getWritableDatabase();
		} catch (SQLiteException e) {
			Log.e(TAG, "getWritableDatabase時にエラー", e);
			throw e;
		}

		houseKeepingBookDao = new HouseKeepingBookDao(db);
		categoryDao = new CategoryDao(db);
		budgetDao = new BudgetDao(db);
		filterDao = new FilterDao(db);

		Log.i(TAG, "onCreate end");
	}

	@Override
	protected void onResume() {
		Log.i(TAG, "onResume start");
		super.onResume();

		backupRestoreHelper = new BackupRestoreHelper();

		//レストアボタン
		final Button doRestoreButton = (Button) findViewById(R.id.do_restore_button);
		//バックアップデータスピナー
		final Spinner backupDataSpinner = (Spinner) findViewById(R.id.backup_data_spinner);

		final File[] backupDirectories = backupRestoreHelper.getBackupDirectories();

		doRestoreButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(final View v) {
				//レストア
				File file = backupDirectories[backupDataSpinner.getSelectedItemPosition()];
				restore(file.getPath());
			}

		});

		final String backupDataIsNothingMsg = getResources().getString(R.string.backup_data_is_nothing_msg);
		List<String> backupFileNameList = backupRestoreHelper.getBackupFileNameList();
		if (backupFileNameList.size() == 0) {
			backupFileNameList.add(backupDataIsNothingMsg);
			doRestoreButton.setEnabled(false);
		}
		final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(),
				android.R.layout.simple_spinner_item, backupFileNameList);
		arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		backupDataSpinner.setAdapter(arrayAdapter);

		Log.i(TAG, "onResume end");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		houseKeepingBookOpenHelper.close();
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (restoreTask != null) {
			restoreTask.cancel(false);
		}
	}

	/**
	 * データのレストア
	 */
	private void restore(final String filePath) {

		restoreTask = new RestoreTask(this, filePath);

		Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.restore_confirm_title_msg);
		builder.setMessage(R.string.restore_confirm_msg);
		builder.setIcon(android.R.drawable.ic_dialog_alert);
		builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				restoreTask.execute();
			}
		});

		builder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				// なにもしない
			}
		});
		builder.create().show();
	}

	/** メニューの設定 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		MenuHelper.createOptionsMenu(this, menu, MenuType.PREFERENCE);
		return result;
	}

	/** メニュー項目の選択時 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {

		if (MenuHelper.optionsItemSelected(this, item)) {
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * レストアタスク
	 */
	private class RestoreTask extends AsyncTask<String, Integer, Boolean> {

		private final String TAG = this.getClass().getSimpleName();

		private final ProgressDialog progressDialog;
		private final Builder alertDialogBuilder;
		private final TextView errorCommonMsgTextView;
		private final String backupFileName;
		private final String deviceInfo;
		private final SharedPreferences defaultSharedPreferences;

		private final Map<String, Integer> categoryNameIdMap = new HashMap<String, Integer>();

		public RestoreTask(final Activity activity, final String backupFileName) {
			this.progressDialog = new ProgressDialog(activity);
			this.alertDialogBuilder = new AlertDialog.Builder(activity);
			this.errorCommonMsgTextView = new TextView(activity);
			this.backupFileName = backupFileName;
			this.deviceInfo = KakeiboUtils.getDeviceInfo(activity);

			defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity
					.getApplicationContext());

		}

		@Override
		protected void onPreExecute() {
			progressDialog.setMessage(getResources().getString(R.string.during_restore_msg));
			progressDialog.setIndeterminate(false);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setMax(100);
			progressDialog.show();
		}

		@Override
		protected Boolean doInBackground(final String... params) {

			//レストアの実行
			boolean isSuccess = doRestore();

			return isSuccess;
		}

		@Override
		protected void onPostExecute(final Boolean isSuccess) {
			progressDialog.dismiss();
			if (!isSuccess) {
				showErrorHappenedDialog();
			} else {
				new QuickToastTask(getApplicationContext(), R.string.restore_complete_msg).setLong()
						.execute();
			}
		}

		@Override
		protected void onProgressUpdate(final Integer... values) {
			progressDialog.setProgress(values[0]);
		}

		/** レストアの実行 */
		private boolean doRestore() {

			boolean isSuccess = false;

			try {
				db.beginTransaction();

				if (!isCancelled()) {
					deleteAllData();//データ全消し
				}

				if (!isCancelled()) {
					restoreCategory();
				}

				if (!isCancelled()) {
					restoreBudget();
				}

				if (!isCancelled()) {
					restoreFilter();
				}

				if (!isCancelled()) {
					restorePreference();
				}

				if (!isCancelled()) {
					restoreKakeiboData();
				}

				isSuccess = true;

				if (!isCancelled() && isSuccess) {
					db.setTransactionSuccessful();
				}
			} catch (FileNotFoundException e) {
				throw new IllegalStateException("FileNotFoundException###deviceInfo=" + deviceInfo, e);
			} catch (IOException e) {
				throw new IllegalStateException("IOException###deviceInfo=" + deviceInfo, e);
			} finally {
				db.endTransaction();
			}

			return isSuccess;
		}

		/** データ全削除 */
		private void deleteAllData() {

			houseKeepingBookDao.deleteAll();

			budgetDao.deleteAll();

			categoryDao.deleteAll();

			filterDao.deleteAll();

			defaultSharedPreferences.edit().clear().commit();

			publishProgress(10);

		}

		/** カテゴリのレストア  */
		private void restoreCategory() throws IOException {
			String categoryCsvFilePath = this.backupFileName + "/" + backupRestoreHelper.categoryCsvFileName;
			File categoryCsvFile = new File(categoryCsvFilePath);
			if (!categoryCsvFile.exists()) {
				throw new FileNotFoundException();
			}

			String[] nextLine = null;
			FileInputStream fis = new FileInputStream(categoryCsvFile);
			String encoding = getEncoding(new FileInputStream(categoryCsvFile));
			InputStreamReader inputStreamReader = new InputStreamReader(fis, encoding);
			CSVReader csvReader = new CSVReader(inputStreamReader);
			nextLine = csvReader.readNext();//最初の一行目は無視
			while ((nextLine = csvReader.readNext()) != null) {
				//Log.w(TAG, Arrays.asList(nextLine).toString());
				String categoryName = nextLine[0];//カテゴリ名
				//String position = nextLine[1];//表示順
				String incomeFlgStr = nextLine[2];//収支属性
				int incomeFlg = Category.INCOME_FLG_OFF;
				if (Integer.toString(Category.INCOME_FLG_OFF).equals(incomeFlgStr)
						|| Integer.toString(Category.INCOME_FLG_ON).equals(incomeFlgStr)) {
					incomeFlg = Integer.valueOf(incomeFlgStr);
				}
				long categoryId = categoryDao.insert(categoryName, incomeFlg);
				categoryNameIdMap.put(categoryName, ((Long) categoryId).intValue());
			}

			publishProgress(25);
		}

		/** 予算のレストア */
		private void restoreBudget() throws IOException {
			String budgetCsvFilePath = this.backupFileName + "/" + backupRestoreHelper.budgetCsvFileName;

			removeJunkData(budgetCsvFilePath);

			File budgetCsvFile = new File(budgetCsvFilePath);
			if (!budgetCsvFile.exists()) {
				throw new FileNotFoundException();
			}

			String[] nextLine = null;

			FileInputStream fis = new FileInputStream(budgetCsvFile);
			String encoding = getEncoding(new FileInputStream(budgetCsvFile));
			InputStreamReader inputStreamReader = new InputStreamReader(fis, encoding);

			CSVReader csvReader = new CSVReader(inputStreamReader);
			nextLine = csvReader.readNext();//最初の一行目は無視
			while ((nextLine = csvReader.readNext()) != null) {
				//Log.w(TAG, "Budget=" + Arrays.asList(nextLine).toString());
				Budget budget = new Budget();
				String categoryName = nextLine[0];//カテゴリ名
				String yearMonth = nextLine[1];//年月
				String budgetPrice = nextLine[2];//予算金額
				budget.categoryId = categoryNameIdMap.get(categoryName);
				budget.yearMonth = Integer.parseInt(yearMonth);
				budget.budgetPrice = Long.parseLong(budgetPrice);
				budgetDao.insert(budget);
			}

			publishProgress(40);
		}

		/** 不正な予算データを取り除く */
		private void removeJunkData(final String budgetCsvFilePath) throws IOException {

			String encoding = getEncoding(new FileInputStream(budgetCsvFilePath));

			BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(
					budgetCsvFilePath), encoding));

			String resultData = "";
			String temp = "";
			while ((temp = br.readLine()) != null) {
				if (!TextUtils.isEmpty(temp) && !temp.startsWith(",")) {
					resultData += temp + "\n";
				}
			}
			br.close();

			OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(budgetCsvFilePath), encoding);
			out.write(resultData);
			out.close();

		}

		/** フィルターのレストア */
		private void restoreFilter() throws IOException {
			String filterCsvFileName = this.backupFileName + "/" + backupRestoreHelper.filterCsvFileName;
			File filterCsvFile = new File(filterCsvFileName);
			if (!filterCsvFile.exists()) {
				return;
			}

			String[] nextLine = null;
			FileInputStream fis = new FileInputStream(filterCsvFile);
			String encoding = getEncoding(new FileInputStream(filterCsvFile));
			InputStreamReader inputStreamReader = new InputStreamReader(fis, encoding);
			CSVReader csvReader = new CSVReader(inputStreamReader);
			nextLine = csvReader.readNext();//最初の一行目は無視
			while ((nextLine = csvReader.readNext()) != null) {
				//Log.w(TAG, "Filter=" + Arrays.asList(nextLine).toString());
				Filter filter = new Filter();
				String filterName = nextLine[0];//フィルター名
				String categoryNameList = nextLine[1];//カテゴリIdリスト
				String isDefault = nextLine[2];//デフォルトフラグ

				filter.filterName = filterName;

				List<Integer> categoryIdList = new ArrayList<Integer>();
				if (!TextUtils.isEmpty(categoryNameList)) {
					String[] split = categoryNameList.split(",");
					for (String name : split) {
						Integer categoryId = categoryNameIdMap.get(URLDecoder.decode(name));
						if (categoryId != null) {
							categoryIdList.add(categoryId);
						}
					}
				}

				filter.categoryIdList = KakeiboUtils.join(categoryIdList, ",");
				filter.isDefault = isDefault == null ? Filter.DEFAULT_FLG_OFF : Integer.valueOf(isDefault);
				if (!TextUtils.isEmpty(filter.categoryIdList)) {
					filterDao.insert(filter);
				}
			}

			publishProgress(45);
		}

		/** 設定情報のレストア  */
		private void restorePreference() throws IOException {
			String preferenceCsvFileName = this.backupFileName + "/"
					+ backupRestoreHelper.PREFERENCE_CSV_FILE_NAME;
			File preferenceCsvFile = new File(preferenceCsvFileName);
			if (!preferenceCsvFile.exists()) {
				return;
			}

			String[] nextLine = null;
			FileInputStream fis = new FileInputStream(preferenceCsvFile);
			String encoding = getEncoding(new FileInputStream(preferenceCsvFile));
			InputStreamReader inputStreamReader = new InputStreamReader(fis, encoding);
			CSVReader csvReader = new CSVReader(inputStreamReader);
			Editor edit = defaultSharedPreferences.edit();
			while ((nextLine = csvReader.readNext()) != null) {
				//Log.w(TAG, "Filter=" + Arrays.asList(nextLine).toString());
				String key = nextLine[0];//プレファレンスキー
				String value = nextLine[1];//プレファレンス値

				if (KakeiboConsts.PREFERENCE_KEY_IS_DISP_STATUS_BAR.equals(key)) {
					//ステータスバー表示するかどうか
					edit.putBoolean(key, Boolean.parseBoolean(value));

				} else if (KakeiboConsts.PREFERENCE_KEY_IS_MOVE_DAY_LIST_AFTER_REGISTERED.equals(key)) {
					//家計簿記入後の動作
					edit.putBoolean(key, Boolean.parseBoolean(value));

				} else if (KakeiboConsts.PREFERENCE_KEY_BALANCE_CALC_METHOD.equals(key)) {
					//残高算出方法
					if (!TextUtils.isEmpty(value)) {
						edit.putInt(key, Integer.parseInt(value));
					}

				} else if (KakeiboConsts.PREFERENCE_KEY_IS_USE_CARRYOVER.equals(key)) {
					//月の残高繰越をするか
					edit.putBoolean(key, Boolean.parseBoolean(value));

				} else if (KakeiboConsts.PREFERENCE_KEY_IS_USE_CATEGORY_CARRYOVER.equals(key)) {
					//カテゴリごとの残高繰越をするか
					edit.putBoolean(key, Boolean.parseBoolean(value));

				} else if (KakeiboConsts.PREFERENCE_KEY_FIRST_DAY_OF_WEEKS.equals(key)) {
					//週の初めの曜日
					if (!TextUtils.isEmpty(value)) {
						edit.putString(key, value);
					}

				} else if (KakeiboConsts.PREFERENCE_KEY_FIRST_DAY_OF_MONTH.equals(key)) {
					//月の開始日
					if (!TextUtils.isEmpty(value)) {
						edit.putString(key, value);
					}

				} else if (KakeiboConsts.PREFERENCE_KEY_CURRENCY_UNIT.equals(key)) {
					//通貨単位
					if (!TextUtils.isEmpty(value)) {
						edit.putString(key, value);
					}

				} else if (KakeiboConsts.PREFERENCE_KEY_CURRENCY_UNIT_POSITION.equals(key)) {
					//通貨単位の位置
					if (!TextUtils.isEmpty(value)) {
						edit.putString(key, value);
					}
				}

			}
			edit.commit();

			publishProgress(50);

		}

		/** 家計簿データのレストア */
		private void restoreKakeiboData() throws IOException {
			String kakeiboDataCsvFilePath = this.backupFileName + "/"
					+ backupRestoreHelper.kakeiboDataCsvFileName;
			File kakeiboDataCsvFile = new File(kakeiboDataCsvFilePath);
			if (!kakeiboDataCsvFile.exists()) {
				throw new FileNotFoundException();
			}
			String csvDateFormatFilePath = this.backupFileName + "/"
					+ backupRestoreHelper.CSV_DATE_FORMAT_TXT_FILE_NAME;
			File csvDateFormatFile = new File(csvDateFormatFilePath);
			FileReader fileReader = new FileReader(csvDateFormatFile);
			BufferedReader br = new BufferedReader(fileReader);
			String dateFormatter = br.readLine();

			String[] nextLine = null;
			FileInputStream fis = new FileInputStream(kakeiboDataCsvFile);
			String encoding = getEncoding(new FileInputStream(kakeiboDataCsvFile));
			InputStreamReader inputStreamReader = new InputStreamReader(fis, encoding);
			CSVReader csvReader = new CSVReader(inputStreamReader);
			nextLine = csvReader.readNext();//最初の一行目は無視

			List<String[]> readAll = csvReader.readAll();
			int size = readAll.size();
			for (int i = 0; i < size; i++) {
				nextLine = readAll.get(i);
				//Log.w(TAG, "HouseKeepingBook=" + Arrays.asList(nextLine).toString());
				HouseKeepingBook houseKeepingBook = new HouseKeepingBook();
				String price = nextLine[0];//金額
				String categoryName = nextLine[1];//カテゴリ名
				String registerDateStr = nextLine[2];//購入日時
				String memo = nextLine[3];//メモ
				String importVersionStr = nextLine[4];//インポートバージョン
				String place = nextLine[5];//購入場所
				String latitude = nextLine[6];//購入緯度
				String longitude = nextLine[7];//購入経度

				houseKeepingBook.price = TextUtils.isEmpty(price) ? 0 : Integer.valueOf(price);
				houseKeepingBook.categoryId = categoryNameIdMap.get(categoryName);
				houseKeepingBook.registerDate = backupRestoreHelper
						.formatStringToDateTimeForCsv(registerDateStr, dateFormatter);
				houseKeepingBook.memo = memo;
				houseKeepingBook.importVersion = TextUtils.isEmpty(importVersionStr) ? null : Integer
						.valueOf(importVersionStr);
				houseKeepingBook.place = place;
				houseKeepingBook.latitude = latitude;
				houseKeepingBook.longitude = longitude;
				houseKeepingBookDao.insert(houseKeepingBook);

				int prg = 50 + (int) (((float) i / size) / 2 * 100);
				if (prg % 5 == 0) {
					publishProgress(prg);
				}
			}

			publishProgress(100);
		}

		private void showErrorHappenedDialog() {
			String msg = "";
			msg += getResources().getString(R.string.restore_error_msg);
			msg += "\n";
			msg += getResources().getString(R.string.restore_error_common_msg);
			msg += "\n";
			errorCommonMsgTextView.setAutoLinkMask(Linkify.EMAIL_ADDRESSES);
			errorCommonMsgTextView.setText(msg);
			errorCommonMsgTextView.setTextColor(getResources().getColor(R.color.WHITE));

			alertDialogBuilder.setView(errorCommonMsgTextView);
			alertDialogBuilder.setIcon(R.drawable.show_suprise);
			alertDialogBuilder.setTitle("Oh...");
			alertDialogBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(final DialogInterface dialog, final int which) {
					// なにもしない
				}
			});
			alertDialogBuilder.create().show();
		}

	}

	private String getEncoding(final FileInputStream fis) {
		String encode = KakeiboUtils.getEncoding();
		byte[] buf = new byte[4096];

		UniversalDetector detector = new UniversalDetector(null);
		int nread;
		try {
			while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
				detector.handleData(buf, 0, nread);
			}
			detector.dataEnd();
			String detectedCharset = detector.getDetectedCharset();
			if (detectedCharset != null) {
				encode = detectedCharset;
			}
		} catch (IOException e) {
			//Log.e(TAG, Arrays.asList(e.getStackTrace()).toString());
		}
		return encode;

	}
}
