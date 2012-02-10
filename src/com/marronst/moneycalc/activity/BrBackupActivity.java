package com.marronst.moneycalc.activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

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
import com.marronst.moneycalc.helper.BackupRestoreHelper;
import com.marronst.moneycalc.helper.MenuHelper;
import com.marronst.moneycalc.helper.MenuHelper.MenuType;
import com.marronst.moneycalc.task.QuickToastTask;
import com.marronst.moneycalc.utils.KakeiboUtils;

public class BrBackupActivity extends Activity {
	protected final String TAG = this.getClass().getSimpleName();

	protected SQLiteDatabase db;

	private HouseKeepingBookOpenHelper houseKeepingBookOpenHelper;
	private HouseKeepingBookDao houseKeepingBookDao;
	private CategoryDao categoryDao;
	private BudgetDao budgetDao;
	private FilterDao filterDao;

	private final CategoryDxo categoryDxo = new CategoryDxo();
	private final HouseKeepingBookDxo houseKeepingBookDxo = new HouseKeepingBookDxo();

	private BackupRestoreHelper backupRestoreHelper;

	private final Map<Integer, String> categoryIdNameMap = new HashMap<Integer, String>();

	private BackupTask backupTask;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		Log.i(TAG, "onCreate start");

		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.setContentView(R.layout.br_backup);

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

		//データをバックアップする場合
		Button doBackupButton = (Button) findViewById(R.id.do_backup_button);
		doBackupButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(final View v) {

				//SDカードがあるかチェック
				if (!KakeiboUtils.isMediaMounted()) {
					AlertDialog.Builder builder = new AlertDialog.Builder(BrBackupActivity.this);
					builder.setMessage(R.string.backup_no_sdcard_error_msg);
					builder.setPositiveButton(android.R.string.ok, null);
					builder.create().show();
					return;
				}

				backupRestoreHelper = new BackupRestoreHelper();
				backupTask = new BackupTask(BrBackupActivity.this);
				backupTask.execute();
			}
		});

		Log.i(TAG, "onResume end");
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (backupTask != null) {
			backupTask.cancel(false);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		houseKeepingBookOpenHelper.close();
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
	 * バックアップタスク
	 * @author akira
	 *
	 */
	private class BackupTask extends AsyncTask<String, Integer, Boolean> {

		private final ProgressDialog progressDialog;
		private final Builder alertDialogBuilder;

		private final TextView errorCommonMsgTextView;

		private final String deviceInfo;

		public BackupTask(final Activity activity) {
			progressDialog = new ProgressDialog(activity);
			alertDialogBuilder = new AlertDialog.Builder(activity);
			errorCommonMsgTextView = new TextView(activity);
			this.deviceInfo = KakeiboUtils.getDeviceInfo(activity);
		}

		@Override
		protected void onPreExecute() {
			progressDialog.setMessage(getResources().getString(R.string.during_backup_msg));
			progressDialog.setIndeterminate(false);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setMax(100);
			progressDialog.show();
		}

		@Override
		protected Boolean doInBackground(final String... params) {

			//バックアップの実行
			boolean isSuccess = doBackup();

			return isSuccess;
		}

		/** バックアップの実行  */
		private boolean doBackup() {
			Log.i(TAG, "doBackup start");

			backupRestoreHelper.makeTempDir(backupRestoreHelper.backupDirPath);

			boolean isSuccess = false;

			try {
				if (!isCancelled()) {
					//カテゴリのバックアップ
					doCategoryBackup();
				}

				if (!isCancelled()) {
					//予算のバックアップ
					doBuggetBackup();
				}

				if (!isCancelled()) {
					//フィルダーのバックアップ
					doFilterBackup();
				}

				if (!isCancelled()) {
					//家計簿データのバックアップ
					doKakeiboBackup();
				}

				//Dateフォーマット形式を保存
				backupRestoreHelper.createCsvDateFormat(getApplicationContext(), backupRestoreHelper
						.getFormatStringToDateTimeForCsvFilePath());

				//設定データを保存
				backupRestoreHelper.createPreferenceText(getApplicationContext(), backupRestoreHelper
						.getPreferenceCsvFilePath());

				if (isCancelled()) {
					backupRestoreHelper.deleteFiles(backupRestoreHelper.getTempDir());
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
					backupRestoreHelper.deleteFiles(backupRestoreHelper.getTempDir());
				}
			}

			Log.i(TAG, "doBackup end");

			return isSuccess;
		}

		/** カテゴリのバックアップ  */
		private void doCategoryBackup() throws IOException, SecurityException {
			Log.i(TAG, "doCategoryBackup start");

			List<Category> categoryList = new ArrayList<Category>();
			Cursor c = categoryDao.findAll();
			startManagingCursor(c);
			while (c.moveToNext()) {
				categoryList.add(categoryDxo.createFromCursol(c));
			}
			c.close();

			backupRestoreHelper.createCategoryCsvBackup(getApplicationContext(), categoryList,
														categoryIdNameMap, backupRestoreHelper
																.getCategoryCsvFilePath());

			publishProgress(33);
			Log.i(TAG, "doCategoryBackup end");
		}

		/** 予算のバックアップ */
		private void doBuggetBackup() throws IOException, SecurityException {
			Log.i(TAG, "doBuggetBackup start");

			List<Budget> budgetList = budgetDao.findAll();

			backupRestoreHelper.createBudgetCsvBackup(getApplicationContext(), budgetList, categoryIdNameMap,
														backupRestoreHelper.getBudgetCsvFilePath());
			publishProgress(55);
			Log.i(TAG, "doBuggetBackup end");
		}

		/** フィルターのバックアップ */
		private void doFilterBackup() throws IOException, SecurityException {
			Log.i(TAG, "doFilterBackup start");

			List<Filter> filterList = filterDao.findAll();

			backupRestoreHelper.createFilterCsvBackup(getApplicationContext(), filterList, categoryIdNameMap,
														backupRestoreHelper.getFilterCsvFilePath());
			publishProgress(60);
			Log.i(TAG, "doFilterBackup end");
		}

		/** 家計簿データのバックアップ  */
		private void doKakeiboBackup() throws IOException, SecurityException {
			Log.i(TAG, "doKakeiboBackup start");
			List<HouseKeepingBook> houseKeepingBookList = new ArrayList<HouseKeepingBook>();
			Cursor c = houseKeepingBookDao.findAll();
			startManagingCursor(c);
			while (c.moveToNext()) {
				houseKeepingBookList.add(houseKeepingBookDxo.createFromCursor(c, true));
			}
			c.close();
			publishProgress(72);

			backupRestoreHelper.createKakeiboCsvBackup(getApplicationContext(), houseKeepingBookList,
														categoryIdNameMap, backupRestoreHelper
																.getKakeiboDataCsvFilePath());

			publishProgress(100);
			Log.i(TAG, "doKakeiboBackup end");
		}

		private void showErrorHappenedDialog() {
			String msg = "";
			msg += getResources().getString(R.string.backup_error_msg);
			msg += "\n";
			msg += getResources().getString(R.string.error_common_msg);
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

		@Override
		protected void onPostExecute(final Boolean isSuccess) {
			progressDialog.dismiss();
			if (!isSuccess) {
				showErrorHappenedDialog();
				backupRestoreHelper.deleteFiles(backupRestoreHelper.getTempDir());
			} else {
				new QuickToastTask(getApplicationContext(), R.string.backup_complete_msg).setLong().execute();
			}
		}

		@Override
		protected void onProgressUpdate(final Integer... values) {
			progressDialog.setProgress(values[0]);
		}

	}
}
