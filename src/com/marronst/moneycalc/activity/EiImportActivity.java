package com.marronst.moneycalc.activity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mozilla.universalchardet.UniversalDetector;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
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
import com.marronst.moneycalc.db.BudgetDao;
import com.marronst.moneycalc.db.CategoryDao;
import com.marronst.moneycalc.db.HouseKeepingBookDao;
import com.marronst.moneycalc.db.HouseKeepingBookOpenHelper;
import com.marronst.moneycalc.entity.Budget;
import com.marronst.moneycalc.entity.Category;
import com.marronst.moneycalc.entity.HouseKeepingBook;
import com.marronst.moneycalc.helper.BackupRestoreHelper;
import com.marronst.moneycalc.helper.MenuHelper;
import com.marronst.moneycalc.helper.MenuHelper.MenuType;
import com.marronst.moneycalc.task.QuickToastTask;
import com.marronst.moneycalc.utils.KakeiboUtils;

public class EiImportActivity extends Activity {
	protected final String TAG = this.getClass().getSimpleName();

	protected SQLiteDatabase db;

	private HouseKeepingBookOpenHelper houseKeepingBookOpenHelper;
	private HouseKeepingBookDao houseKeepingBookDao;
	private CategoryDao categoryDao;
	private BudgetDao budgetDao;

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

		private final Map<String, Integer> categoryNameIdMap = new HashMap<String, Integer>();

		public RestoreTask(final Activity activity, final String backupFileName) {
			this.progressDialog = new ProgressDialog(activity);
			this.alertDialogBuilder = new AlertDialog.Builder(activity);
			this.errorCommonMsgTextView = new TextView(activity);
			this.backupFileName = backupFileName;
			this.deviceInfo = KakeiboUtils.getDeviceInfo(activity);
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
