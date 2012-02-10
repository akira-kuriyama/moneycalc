package com.marronst.moneycalc.activity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.marronst.moneycalc.R;
import com.marronst.moneycalc.db.BudgetDao;
import com.marronst.moneycalc.db.CategoryDao;
import com.marronst.moneycalc.db.FilterDao;
import com.marronst.moneycalc.db.HouseKeepingBookDao;
import com.marronst.moneycalc.db.HouseKeepingBookOpenHelper;
import com.marronst.moneycalc.dxo.CategoryDxo;
import com.marronst.moneycalc.dxo.HouseKeepingBookDxo;
import com.marronst.moneycalc.entity.Budget;
import com.marronst.moneycalc.entity.Category;
import com.marronst.moneycalc.entity.Filter;
import com.marronst.moneycalc.entity.HouseKeepingBook;
import com.marronst.moneycalc.helper.SendLogHelper;
import com.marronst.moneycalc.utils.KakeiboUtils;

public class SendLogActivity extends Activity {

	private final String TAG = this.getClass().getSimpleName();

	protected SQLiteDatabase db;

	private HouseKeepingBookOpenHelper houseKeepingBookOpenHelper;
	private HouseKeepingBookDao houseKeepingBookDao;
	private CategoryDao categoryDao;
	private BudgetDao budgetDao;
	private FilterDao filterDao;

	private final CategoryDxo categoryDxo = new CategoryDxo();
	private final HouseKeepingBookDxo houseKeepingBookDxo = new HouseKeepingBookDxo();

	private final Map<Integer, String> categoryIdNameMap = new HashMap<Integer, String>();

	private SendLogTask sendLogTask;

	private SendLogHelper sendLogHelper;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		Log.i(TAG, "onCreate start");

		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.setContentView(R.layout.send_log);

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

		//ログ送信ボタン
		Button sendlogButton = (Button) findViewById(R.id.send_log_button);
		sendlogButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(final View v) {
				//ログ送信ボタン押下時
				sendLogHelper = new SendLogHelper();
				sendLogTask = new SendLogTask(SendLogActivity.this);
				sendLogTask.execute();
			}
		});

		Log.i(TAG, "onResume end");
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (sendLogTask != null) {
			sendLogTask.cancel(false);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		houseKeepingBookOpenHelper.close();
	}

	/**
	 * バックアップタスク
	 * @author akira
	 *
	 */
	private class SendLogTask extends AsyncTask<String, Integer, Boolean> {

		private final ProgressDialog progressDialog;
		private final String deviceInfo;

		public SendLogTask(final Activity activity) {
			progressDialog = new ProgressDialog(activity);
			deviceInfo = KakeiboUtils.getDeviceInfo(activity);
		}

		@Override
		protected void onPreExecute() {
			progressDialog.setMessage(getResources().getString(R.string.during_create_log_msg));
			progressDialog.setIndeterminate(true);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progressDialog.show();
		}

		@Override
		protected Boolean doInBackground(final String... params) {

			//バックアップの実行
			boolean isSuccess = createLog();

			return isSuccess;
		}

		/** バックアップの実行  */
		private boolean createLog() {
			Log.i(TAG, "createLog start");

			sendLogHelper.makeTempDir(sendLogHelper.sendLogDirPath);

			boolean isSuccess = false;
			try {
				if (!isCancelled()) {
					//カテゴリのログ作成
					createCategoryLog();
				}

				if (!isCancelled()) {
					//予算のログ作成
					createBuggetLog();
				}

				if (!isCancelled()) {
					//フィルターのログ作成
					createFilterLog();
				}

				if (!isCancelled()) {
					//家計簿データのログ作成
					createKakeiboLog();
				}

				//Dateフォーマット形式を保存
				sendLogHelper.createCsvDateFormat(getApplicationContext(), sendLogHelper
						.getFormatStringToDateTimeForCsvFilePath());

				//端末情報を保存
				sendLogHelper.createDeviceInfoFile(SendLogActivity.this, sendLogHelper
						.getDeviceInfoFilePath());

				//設定データを保存
				sendLogHelper.createPreferenceText(getApplicationContext(), sendLogHelper
						.getPreferenceCsvFilePath());

				//ZIPファイルを作成する
				List<String> csvFilePathList = new ArrayList<String>();
				csvFilePathList.add(sendLogHelper.getCategoryCsvFilePath());
				csvFilePathList.add(sendLogHelper.getBudgetCsvFilePath());
				csvFilePathList.add(sendLogHelper.getFilterCsvFilePath());
				csvFilePathList.add(sendLogHelper.getKakeiboDataCsvFilePath());
				csvFilePathList.add(sendLogHelper.getPreferenceCsvFilePath());
				csvFilePathList.add(sendLogHelper.getFormatStringToDateTimeForCsvFilePath());
				csvFilePathList.add(sendLogHelper.getDeviceInfoFilePath());

				String zipFilePath = sendLogHelper.getAppLogFilePath();
				KakeiboUtils.makeZipFile(csvFilePathList, zipFilePath);

				//とりあえずいらないログを削除
				for (String path : csvFilePathList) {
					new File(path).delete();
				}

				if (isCancelled()) {
					sendLogHelper.deleteFiles(sendLogHelper.getTempDir());
				}
				isSuccess = true;

			} catch (SecurityException e) {
				throw new IllegalStateException("SecurityException###deviceInfo=" + deviceInfo, e);
			} catch (IOException e) {
				throw new IllegalStateException("IOException###deviceInfo=" + deviceInfo, e);
			} catch (Exception e) {
				throw new IllegalStateException("Exception###deviceInfo=" + deviceInfo, e);
			} finally {
				if (!isSuccess) {
					sendLogHelper.deleteFiles(sendLogHelper.getTempDir());
				}
			}

			Log.i(TAG, "createLog end");

			return isSuccess;
		}

		/** カテゴリのログ作成  */
		private void createCategoryLog() throws IOException, SecurityException {
			Log.i(TAG, "createCategoryLog start");

			List<Category> categoryList = new ArrayList<Category>();
			Cursor c = categoryDao.findAll();
			startManagingCursor(c);
			while (c.moveToNext()) {
				categoryList.add(categoryDxo.createFromCursol(c));
			}
			c.close();

			sendLogHelper.createCategoryCsvBackup(getApplicationContext(), categoryList, categoryIdNameMap,
													sendLogHelper.getCategoryCsvFilePath());

			Log.i(TAG, "createCategoryLog end");
		}

		/** 予算のログ作成 */
		private void createBuggetLog() throws IOException, SecurityException {
			Log.i(TAG, "createBuggetLog start");

			List<Budget> budgetList = budgetDao.findAll();

			sendLogHelper.createBudgetCsvBackup(getApplicationContext(), budgetList, categoryIdNameMap,
												sendLogHelper.getBudgetCsvFilePath());

			Log.i(TAG, "createBuggetLog end");
		}

		/** フィルターのログ作成*/
		private void createFilterLog() throws IOException, SecurityException {
			Log.i(TAG, "createFilterLog start");

			List<Filter> filterList = filterDao.findAll();

			sendLogHelper.createFilterCsvBackup(getApplicationContext(), filterList, categoryIdNameMap,
												sendLogHelper.getFilterCsvFilePath());
			Log.i(TAG, "createFilterLog end");
		}

		/** 家計簿データのログ作成  */
		private void createKakeiboLog() throws IOException, SecurityException {
			Log.i(TAG, "createKakeiboLog start");

			List<HouseKeepingBook> houseKeepingBookList = new ArrayList<HouseKeepingBook>();
			Cursor c = houseKeepingBookDao.findAll();
			startManagingCursor(c);
			while (c.moveToNext()) {
				houseKeepingBookList.add(houseKeepingBookDxo.createFromCursor(c, true));
			}
			c.close();

			sendLogHelper.createKakeiboCsvBackup(getApplicationContext(), houseKeepingBookList,
													categoryIdNameMap, sendLogHelper
															.getKakeiboDataCsvFilePath());

			Log.i(TAG, "createKakeiboLog end");
		}

		@Override
		protected void onPostExecute(final Boolean isSuccess) {
			progressDialog.dismiss();
			if (!isSuccess) {
				sendLogHelper.deleteFiles(sendLogHelper.getTempDir());
			} else {
				Intent intent = new Intent(Intent.ACTION_SEND);
				final Resources resources = getResources();
				intent.putExtra(Intent.EXTRA_SUBJECT, resources.getString(R.string.send_log_subject));
				intent.putExtra(Intent.EXTRA_TEXT, resources.getString(R.string.send_log_content));
				intent.putExtra(Intent.EXTRA_EMAIL,
								new String[] { resources.getString(R.string.mail_address) });
				intent.setType("text/plain");
				intent
						.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"
								+ sendLogHelper.getAppLogFilePath()));
				startActivity(Intent.createChooser(intent, resources.getString(R.string.mailer_chooser)));
				//	sendLogHelper.deleteFiles(sendLogHelper.sendLogDirPath);
			}
		}

		@Override
		protected void onProgressUpdate(final Integer... values) {
			progressDialog.setProgress(values[0]);
		}
	}
}
