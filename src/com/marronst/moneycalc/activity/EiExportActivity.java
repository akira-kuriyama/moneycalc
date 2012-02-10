package com.marronst.moneycalc.activity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import au.com.bytecode.opencsv.CSVWriter;

import com.marronst.moneycalc.R;
import com.marronst.moneycalc.consts.KakeiboConsts;
import com.marronst.moneycalc.db.HouseKeepingBookDao;
import com.marronst.moneycalc.db.HouseKeepingBookOpenHelper;
import com.marronst.moneycalc.dxo.HouseKeepingBookDxo;
import com.marronst.moneycalc.entity.HouseKeepingBook;
import com.marronst.moneycalc.entity.HouseKeepingBook.IdxHouseKeepingBook;
import com.marronst.moneycalc.helper.ExportImportHelper;
import com.marronst.moneycalc.helper.MenuHelper;
import com.marronst.moneycalc.helper.MenuHelper.MenuType;
import com.marronst.moneycalc.utils.KakeiboFormatUtils;
import com.marronst.moneycalc.utils.KakeiboUtils;

public class EiExportActivity extends Activity {
	protected final String TAG = this.getClass().getSimpleName();

	protected SQLiteDatabase db;

	private HouseKeepingBookOpenHelper houseKeepingBookOpenHelper;
	private HouseKeepingBookDao houseKeepingBookDao;

	private final HouseKeepingBookDxo houseKeepingBookDxo = new HouseKeepingBookDxo();

	private ExportImportHelper exportImportHelper;

	private ExportTask exportTask;

	/** 選択したエクスポート方法 */
	private ExportMethod selectExportMethod;
	/** 選択したエクスポート期間 */
	private ExportPeriod selectExportPeriod;

	/** 開始日 */
	private Calendar exportPeriodStartButtonCal;
	/** 終了日 */
	private Calendar exportPeriodEndButtonCal;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		Log.i(TAG, "onCreate start");

		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.setContentView(R.layout.ei_export);

		houseKeepingBookOpenHelper = new HouseKeepingBookOpenHelper(getApplicationContext());

		try {
			db = houseKeepingBookOpenHelper.getWritableDatabase();
		} catch (SQLiteException e) {
			Log.e(TAG, "getWritableDatabase時にエラー", e);
			throw e;
		}

		houseKeepingBookDao = new HouseKeepingBookDao(db);

		Log.i(TAG, "onCreate end");
	}

	@Override
	protected void onResume() {
		Log.i(TAG, "onResume start");
		super.onResume();

		//メール添付時注意文
		final View exportToMailAttachedNotice = findViewById(R.id.export_to_mail_attached_notice);
		exportToMailAttachedNotice.setVisibility(View.GONE);

		//メール本文添付時注意文
		final View exportToMailMessageBodyNotice = findViewById(R.id.export_to_mail_message_body_notice);
		exportToMailMessageBodyNotice.setVisibility(View.GONE);

		//SDカード保存時注意文
		final View exportToSdCardNotice = findViewById(R.id.export_to_sd_card_notice);
		exportToSdCardNotice.setVisibility(View.GONE);

		//エクスポート方法を指定するスピナー
		final Spinner exportMethodSpinner = (Spinner) findViewById(R.id.export_method_spinner);

		ArrayList<String> exportMethodList = new ArrayList<String>();
		Resources resources = getResources();
		for (ExportMethod exportMethod : ExportMethod.values()) {
			int identifier = resources.getIdentifier(exportMethod.getStringsId(), "string", getPackageName());
			String name = resources.getString(identifier);
			exportMethodList.add(name);
		}
		final ArrayAdapter<String> exportMethodAdapter = new ArrayAdapter<String>(getApplicationContext(),
				R.layout.multiline_simple_spinner_item, exportMethodList);
		exportMethodAdapter.setDropDownViewResource(R.layout.multiline_spinner_item);
		exportMethodSpinner.setAdapter(exportMethodAdapter);
		exportMethodSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(final AdapterView<?> parent, final View view, final int position,
					final long id) {
				ExportMethod selectExportMethod = getSelectExportMethod(position);
				switch (selectExportMethod) {
					case SEND_MAIL_BY_ATTACHED:
						exportToMailAttachedNotice.setVisibility(View.VISIBLE);
						exportToMailMessageBodyNotice.setVisibility(View.GONE);
						exportToSdCardNotice.setVisibility(View.GONE);
						break;

					case SEND_MAIL_BY_MESSAGE_BODY:
						exportToMailAttachedNotice.setVisibility(View.GONE);
						exportToMailMessageBodyNotice.setVisibility(View.VISIBLE);
						exportToSdCardNotice.setVisibility(View.GONE);
						break;
					case SAVE_SD_CARD:
						exportToMailAttachedNotice.setVisibility(View.GONE);
						exportToMailMessageBodyNotice.setVisibility(View.GONE);
						exportToSdCardNotice.setVisibility(View.VISIBLE);
						break;
					default:
						break;
				}
			}

			@Override
			public void onNothingSelected(final AdapterView<?> parent) {
				// なにもしない
			}

		});

		//エクスポート方法が選択済みだったら、選択肢の選択内容を復元する
		if (selectExportMethod != null) {
			ExportMethod[] exportMethods = ExportMethod.values();
			for (int i = 0; i < exportMethods.length; i++) {
				if (selectExportMethod == exportMethods[i]) {
					exportMethodSpinner.setSelection(i);
				}
			}
		}

		//エクスポート開始日指定エリア
		final View exportPeriodStartArea = findViewById(R.id.export_period_start_area);
		exportPeriodStartArea.setVisibility(View.GONE);
		//エクスポート終了日指定エリア
		final View exportPeriodEndArea = findViewById(R.id.export_period_end_area);
		exportPeriodEndArea.setVisibility(View.GONE);

		//エクスポート開始日がnullなら初期化する
		if (exportPeriodStartButtonCal == null) {
			exportPeriodStartButtonCal = Calendar.getInstance();
			exportPeriodStartButtonCal.set(Calendar.DAY_OF_MONTH, 1);
			exportPeriodStartButtonCal.add(Calendar.MONTH, -1);
		}

		//エクスポート開始日指定ボタン
		final Button exportPeriodStartButton = (Button) findViewById(R.id.export_period_start_button);
		exportPeriodStartButton.setText(formatDate(exportPeriodStartButtonCal.getTime()));
		exportPeriodStartButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(final View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(EiExportActivity.this);
				final DatePicker datePicker = new DatePicker(EiExportActivity.this);
				datePicker.updateDate(exportPeriodStartButtonCal.get(Calendar.YEAR),
										exportPeriodStartButtonCal.get(Calendar.MONTH),
										exportPeriodStartButtonCal.get(Calendar.DAY_OF_MONTH));
				builder.setView(datePicker);
				builder.setTitle(R.string.export_period_start_text);
				builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(final DialogInterface dialog, final int which) {
						int year = datePicker.getYear();
						int month = datePicker.getMonth();
						int dayOfMonth = datePicker.getDayOfMonth();
						exportPeriodStartButtonCal.set(year, month, dayOfMonth);
						exportPeriodStartButton.setText(formatDate(exportPeriodStartButtonCal.getTime()));
					}
				});
				builder.setNegativeButton(android.R.string.no, null);
				AlertDialog alertDialog = builder.create();
				alertDialog.show();

			}
		});

		//エクスポート終了日がnullなら初期化する
		if (exportPeriodEndButtonCal == null) {
			exportPeriodEndButtonCal = Calendar.getInstance();
			exportPeriodEndButtonCal.set(Calendar.DAY_OF_MONTH, 1);
		}

		//エクスポート終了日指定ボタン
		final Button exportPeriodEndButton = (Button) findViewById(R.id.export_period_end_button);
		exportPeriodEndButton.setText(formatDate(exportPeriodEndButtonCal.getTime()));
		exportPeriodEndButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(final View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(EiExportActivity.this);
				final DatePicker datePicker = new DatePicker(EiExportActivity.this);
				datePicker.updateDate(exportPeriodEndButtonCal.get(Calendar.YEAR),//
										exportPeriodEndButtonCal.get(Calendar.MONTH),//
										exportPeriodEndButtonCal.get(Calendar.DAY_OF_MONTH));
				builder.setView(datePicker);
				builder.setTitle(R.string.export_period_end_text);
				builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(final DialogInterface dialog, final int which) {
						int year = datePicker.getYear();
						int month = datePicker.getMonth();
						int dayOfMonth = datePicker.getDayOfMonth();
						exportPeriodEndButtonCal.set(year, month, dayOfMonth);
						exportPeriodEndButton.setText(formatDate(exportPeriodEndButtonCal.getTime()));
					}
				});
				builder.setNegativeButton(android.R.string.no, null);
				AlertDialog alertDialog = builder.create();
				alertDialog.show();
			}
		});

		//エクスポート期間を指定するスピナー
		final Spinner exportPeriodSpinner = (Spinner) findViewById(R.id.export_period_spinner);

		ArrayList<String> exportPeriodList = new ArrayList<String>();
		for (ExportPeriod exportPeriod : ExportPeriod.values()) {
			exportPeriodList.add(getExportPeriodName(exportPeriod));
		}
		final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(),
				android.R.layout.simple_spinner_item, exportPeriodList);
		arrayAdapter.setDropDownViewResource(R.layout.multiline_spinner_item);
		exportPeriodSpinner.setAdapter(arrayAdapter);
		exportPeriodSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
			@Override
			public void onItemSelected(final AdapterView<?> parent, final View view, final int position,
					final long id) {
				ExportPeriod selectExportPeriod = getSelectExportPeriod(position);
				if (ExportPeriod.SPECIFIED_PERIOD == selectExportPeriod) {
					exportPeriodStartArea.setVisibility(View.VISIBLE);
					exportPeriodEndArea.setVisibility(View.VISIBLE);
				} else {
					exportPeriodStartArea.setVisibility(View.GONE);
					exportPeriodEndArea.setVisibility(View.GONE);
				}
			}

			@Override
			public void onNothingSelected(final AdapterView<?> parent) {
				// なにもしない 
			}
		});
		//エクスポート期間が選択済みだったら、選択肢の選択内容を復元する
		if (selectExportPeriod != null) {
			ExportPeriod[] exportPeriods = ExportPeriod.values();
			for (int i = 0; i < exportPeriods.length; i++) {
				if (selectExportPeriod == exportPeriods[i]) {
					exportPeriodSpinner.setSelection(i);
				}
			}
		}

		//データエクスポートボタン
		Button doExportButton = (Button) findViewById(R.id.do_export_button);
		doExportButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(final View v) {

				selectExportMethod = getSelectExportMethod(exportMethodSpinner.getSelectedItemPosition());

				selectExportPeriod = getSelectExportPeriod(exportPeriodSpinner.getSelectedItemPosition());

				exportImportHelper = new ExportImportHelper();

				//SDカードがあるかチェック
				if (!KakeiboUtils.isMediaMounted()) {
					AlertDialog.Builder builder = new AlertDialog.Builder(EiExportActivity.this);
					if (ExportMethod.SAVE_SD_CARD == selectExportMethod) {

						builder.setMessage(R.string.export_to_sd_card_no_sdcard_error_msg);
					} else {

						builder.setMessage(R.string.export_to_mail_no_sdcard_error_msg);
					}
					builder.setPositiveButton(android.R.string.ok, null);
					builder.create().show();
					return;
				}

				//エクスポート用フォルダがなければ作成する 
				exportImportHelper.makeExportDireIfNotExists();

				//エクスポートタスクの実行
				exportTask = new ExportTask(EiExportActivity.this, selectExportMethod, selectExportPeriod);
				exportTask.execute();
			}
		});

		Log.i(TAG, "onResume end");
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		//Log.w(TAG, "### onSaveInstanceState");

		if (selectExportMethod != null) {
			outState.putString("selectExportMethod", selectExportMethod.name());
		}

		if (selectExportPeriod != null) {
			outState.putString("selectExportPeriod", selectExportPeriod.name());
		}

		if (exportPeriodStartButtonCal != null) {
			int sYear = exportPeriodStartButtonCal.get(Calendar.YEAR);
			int sMonth = exportPeriodStartButtonCal.get(Calendar.MONTH);
			int sDayOfMonth = exportPeriodStartButtonCal.get(Calendar.DAY_OF_MONTH);
			outState.putStringArray("exportPeriodStartButtonCal", new String[] { String.valueOf(sYear),
					String.valueOf(sMonth), String.valueOf(sDayOfMonth) });
		}

		if (exportPeriodEndButtonCal != null) {
			int eYear = exportPeriodEndButtonCal.get(Calendar.YEAR);
			int eMonth = exportPeriodEndButtonCal.get(Calendar.MONTH);
			int eDayOfMonth = exportPeriodEndButtonCal.get(Calendar.DAY_OF_MONTH);
			outState.putStringArray("exportPeriodEndButtonCal", new String[] { String.valueOf(eYear),
					String.valueOf(eMonth), String.valueOf(eDayOfMonth) });
		}
	}

	@Override
	protected void onRestoreInstanceState(final Bundle savedInstanceState) {
		//Log.w(TAG, "### onRestoreInstanceState");

		String selectExportMethodStr = savedInstanceState.getString("selectExportMethod");
		if (selectExportMethodStr != null) {
			selectExportMethod = ExportMethod.valueOf(selectExportMethodStr);
		}
		String selectExportPeriodStr = savedInstanceState.getString("selectExportPeriod");
		if (selectExportPeriodStr != null) {
			selectExportPeriod = ExportPeriod.valueOf(selectExportPeriodStr);
		}
		String[] exportPeriodStartButtonCalArray = savedInstanceState
				.getStringArray("exportPeriodStartButtonCal");
		if (exportPeriodStartButtonCalArray != null) {
			exportPeriodStartButtonCal = Calendar.getInstance();
			exportPeriodStartButtonCal
					.set(Calendar.YEAR, Integer.valueOf(exportPeriodStartButtonCalArray[0]));
			exportPeriodStartButtonCal.set(Calendar.MONTH, Integer
					.valueOf(exportPeriodStartButtonCalArray[1]));
			exportPeriodStartButtonCal.set(Calendar.DAY_OF_MONTH, Integer
					.valueOf(exportPeriodStartButtonCalArray[2]));
		}

		String[] exportPeriodEndButtonCalArray = savedInstanceState
				.getStringArray("exportPeriodEndButtonCal");
		if (exportPeriodEndButtonCalArray != null) {
			exportPeriodEndButtonCal = Calendar.getInstance();
			exportPeriodEndButtonCal.set(Calendar.YEAR, Integer.valueOf(exportPeriodEndButtonCalArray[0]));
			exportPeriodEndButtonCal.set(Calendar.MONTH, Integer.valueOf(exportPeriodEndButtonCalArray[1]));
			exportPeriodEndButtonCal.set(Calendar.DAY_OF_MONTH, Integer
					.valueOf(exportPeriodEndButtonCalArray[2]));
		}

	}

	/** 選択したエクスポート方法enumを返す */
	private ExportMethod getSelectExportMethod(final int position) {
		ExportMethod[] values = ExportMethod.values();
		ExportMethod selectExportMethod = null;
		for (int i = 0; i < values.length; i++) {
			if (i == position) {
				selectExportMethod = values[i];
			}
		}
		return selectExportMethod;
	}

	/** 選択したエクスポート期間enumを返す */
	private ExportPeriod getSelectExportPeriod(final int position) {
		ExportPeriod[] values = ExportPeriod.values();
		ExportPeriod selectExportPeriod = null;
		for (int i = 0; i < values.length; i++) {
			if (i == position) {
				selectExportPeriod = values[i];
			}
		}
		return selectExportPeriod;
	}

	/** エクスポート開始日を返す  */
	private Date getExportStartDate(final ExportPeriod exportPeriod) {
		Calendar fromCal = Calendar.getInstance();
		switch (exportPeriod) {
			case THIS_MONTH:
				fromCal.set(Calendar.DAY_OF_MONTH, 1);
				break;
			case LAST_MONTH:
				fromCal.set(Calendar.DAY_OF_MONTH, 1);
				fromCal.add(Calendar.MONTH, -1);
				break;
			case PAST_THREE_MONTH:
				fromCal.set(Calendar.DAY_OF_MONTH, 1);
				fromCal.add(Calendar.MONTH, -3);
				break;
			case PAST_HALF_YEAR:
				fromCal.set(Calendar.DAY_OF_MONTH, 1);
				fromCal.add(Calendar.MONTH, -6);
				break;
			case THIS_YEAR:
				fromCal.set(Calendar.DAY_OF_MONTH, 1);
				fromCal.set(Calendar.MONTH, 0);
				break;
			case LAST_YEAR:
				fromCal.set(Calendar.DAY_OF_MONTH, 1);
				fromCal.set(Calendar.MONTH, 0);
				fromCal.add(Calendar.YEAR, -1);
				break;
			case SPECIFIED_PERIOD:
				fromCal = exportPeriodStartButtonCal;
				break;

			default:
				break;
		}

		return fromCal.getTime();
	}

	/** エクスポート終了日を返す  */
	private Date getExportEndDate(final ExportPeriod exportPeriod) {
		Calendar toCal = Calendar.getInstance();
		switch (exportPeriod) {
			case THIS_MONTH:
				break;
			case LAST_MONTH:
				toCal.set(Calendar.DAY_OF_MONTH, 1);
				toCal.add(Calendar.DAY_OF_MONTH, -1);
				break;
			case PAST_THREE_MONTH:
				break;
			case PAST_HALF_YEAR:
				break;
			case THIS_YEAR:
				break;
			case LAST_YEAR:
				toCal.set(Calendar.DAY_OF_MONTH, 1);
				toCal.set(Calendar.MONTH, 0);
				toCal.add(Calendar.DAY_OF_MONTH, -1);
				break;
			case SPECIFIED_PERIOD:
				toCal = exportPeriodEndButtonCal;
				break;
			default:
				break;
		}

		return toCal.getTime();
	}

	/** ExportPeriod enum に対する表示文字列を返す */
	private String getExportPeriodName(final ExportPeriod exportPeriod) {
		Resources resources = getResources();
		int identifier = resources.getIdentifier(exportPeriod.getStringsId(), "string", getPackageName());
		String name = resources.getString(identifier);
		Calendar toCal = Calendar.getInstance();
		Calendar fromCal = Calendar.getInstance();
		String from = KakeiboConsts.EMPTY;
		String to = KakeiboConsts.EMPTY;
		switch (exportPeriod) {
			case THIS_MONTH:
				fromCal.set(Calendar.DAY_OF_MONTH, 1);
				from = formatDate(fromCal.getTime());
				to = formatDate(toCal.getTime());
				break;
			case LAST_MONTH:
				fromCal.set(Calendar.DAY_OF_MONTH, 1);
				fromCal.add(Calendar.MONTH, -1);
				from = formatDate(fromCal.getTime());
				toCal.set(Calendar.DAY_OF_MONTH, 1);
				toCal.add(Calendar.DAY_OF_MONTH, -1);
				to = formatDate(toCal.getTime());
				break;
			case PAST_THREE_MONTH:
				fromCal.set(Calendar.DAY_OF_MONTH, 1);
				fromCal.add(Calendar.MONTH, -3);
				from = formatDate(fromCal.getTime());
				to = formatDate(toCal.getTime());
				break;
			case PAST_HALF_YEAR:
				fromCal.set(Calendar.DAY_OF_MONTH, 1);
				fromCal.add(Calendar.MONTH, -6);
				from = formatDate(fromCal.getTime());
				to = formatDate(toCal.getTime());
				break;
			case THIS_YEAR:
				fromCal.set(Calendar.DAY_OF_MONTH, 1);
				fromCal.set(Calendar.MONTH, 0);
				from = formatDate(fromCal.getTime());
				to = formatDate(toCal.getTime());

				break;
			case LAST_YEAR:
				fromCal.set(Calendar.DAY_OF_MONTH, 1);
				fromCal.set(Calendar.MONTH, 0);
				fromCal.add(Calendar.YEAR, -1);
				from = formatDate(fromCal.getTime());
				toCal.set(Calendar.DAY_OF_MONTH, 1);
				toCal.set(Calendar.MONTH, 0);
				toCal.add(Calendar.DAY_OF_MONTH, -1);
				to = formatDate(toCal.getTime());
				break;
			case SPECIFIED_PERIOD:
				return name;

			default:
				break;
		}
		name += "\n(#from# - #to#)";
		name = name.replace("#from#", from);
		name = name.replace("#to#", to);
		return name;
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (exportTask != null) {
			exportTask.cancel(false);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		houseKeepingBookOpenHelper.close();
	}

	/**
	 * エクスポートタスク
	 */
	private class ExportTask extends AsyncTask<String, Integer, Boolean> {

		private final ProgressDialog progressDialog;
		private final Builder alertDialogBuilder;

		private final TextView errorCommonMsgTextView;

		private final String charSet;

		private final ExportMethod exportMethod;
		private final ExportPeriod exportPeriod;

		private File exportFile;

		List<String> kakeiboDataStrList = new ArrayList<String>();

		private boolean isNoData = false;

		private final String deviceInfo;

		public ExportTask(final Activity activity, final ExportMethod exportMethod,
				final ExportPeriod exportPeriod) {
			this.progressDialog = new ProgressDialog(activity);
			this.alertDialogBuilder = new AlertDialog.Builder(activity);
			this.errorCommonMsgTextView = new TextView(activity);
			this.charSet = KakeiboUtils.getEncoding();
			this.exportMethod = exportMethod;
			this.exportPeriod = exportPeriod;
			this.deviceInfo = KakeiboUtils.getDeviceInfo(activity);
		}

		//前準備
		@Override
		protected void onPreExecute() {
			progressDialog.setMessage(getResources().getString(R.string.during_export_msg));
			progressDialog.setIndeterminate(false);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setMax(100);
			progressDialog.show();
		}

		//実際の処理
		@Override
		protected Boolean doInBackground(final String... params) {

			//エクスポートの実行
			boolean isSuccess = doExport();

			return isSuccess;
		}

		//プログレスバーの更新
		@Override
		protected void onProgressUpdate(final Integer... values) {
			progressDialog.setProgress(values[0]);
		}

		/** エクスポートの実行  */
		private boolean doExport() {
			Log.i(TAG, "doExport start");

			//エクスポートファイルを生成＆取得

			boolean isSuccess = false;

			try {
				exportFile = exportImportHelper.makeExportFile(getApplicationContext());

				if (!isCancelled()) {

					//家計簿データのエクスポート
					doKakeiboExport();

					isSuccess = true;
				}

			} catch (SecurityException e) {
				throw new IllegalStateException("SecurityException###deviceInfo=" + deviceInfo, e);
			} catch (IOException e) {
				throw new IllegalStateException("IOException###deviceInfo=" + deviceInfo, e);
			} catch (Exception e) {
				throw new IllegalStateException("Exception###deviceInfo=" + deviceInfo, e);
			} finally {
				if (!isSuccess) {
					//エクスポートファイルを削除
					exportImportHelper.deleteExportFile();
				}
			}

			Log.i(TAG, "doExport end");

			return isSuccess;
		}

		/** 家計簿データのエクスポート  */
		private void doKakeiboExport() throws IOException, SecurityException {
			Log.i(TAG, "doKakeiboExport start");

			//データの抽出
			List<HouseKeepingBook> houseKeepingBookList = new ArrayList<HouseKeepingBook>();
			Date startDate = getExportStartDate(exportPeriod);
			Date endDate = getExportEndDate(exportPeriod);
			Cursor c = houseKeepingBookDao.findByStartDateAndEndDate(startDate, endDate);
			startManagingCursor(c);
			while (c.moveToNext()) {
				HouseKeepingBook houseKeepingBook = houseKeepingBookDxo.createFromCursor(c, true);
				String categoryName = c.getString(IdxHouseKeepingBook.lastIndex() + 1);
				houseKeepingBook.title = categoryName;
				houseKeepingBookList.add(houseKeepingBook);
			}
			c.close();

			int size = houseKeepingBookList.size();
			if (size == 0) {
				isNoData = true;
				return;
			}

			publishProgress(50);

			FileOutputStream output = new FileOutputStream(exportFile);
			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(output, charSet);
			CSVWriter csvWriter = new CSVWriter(outputStreamWriter);

			//列名の書き込み
			String price = getResources().getString(R.string.kakeibo_data_column_name_price);//金額
			String categoryName = getResources().getString(R.string.kakeibo_data_column_name_category_name);//カテゴリ名
			String registerDate = getResources().getString(R.string.kakeibo_data_column_name_registerDate);//購入日時
			String memo = getResources().getString(R.string.kakeibo_data_column_name_memo);//メモ
			String[] categoryColumnNameArray = { price, categoryName, registerDate, memo };
			csvWriter.writeNext(categoryColumnNameArray);

			String[] houseKeepingBookArray = new String[4];
			for (int i = 0; i < size; i++) {
				HouseKeepingBook houseKeepingBook = houseKeepingBookList.get(i);
				houseKeepingBookArray[0] = KakeiboFormatUtils.formatPriceNoUnit(houseKeepingBook.price
						.longValue());
				houseKeepingBookArray[1] = houseKeepingBook.title;
				houseKeepingBookArray[2] = exportImportHelper
						.formatDateTimeToStringForCsv(getApplicationContext(), houseKeepingBook.registerDate);
				houseKeepingBookArray[3] = houseKeepingBook.memo;

				csvWriter.writeNext(houseKeepingBookArray);

				int prg = 50 + (int) (((float) i / size) / 2 * 100);
				if (prg % 5 == 0) {
					publishProgress(prg);
				}
			}
			csvWriter.close();
			if (ExportMethod.SEND_MAIL_BY_ATTACHED == exportMethod
					|| ExportMethod.SEND_MAIL_BY_MESSAGE_BODY == exportMethod) {

				FileInputStream fis = null;
				fis = new FileInputStream(exportFile);
				InputStreamReader inputStreamReader = new InputStreamReader(fis, charSet);
				BufferedReader br = new BufferedReader(inputStreamReader);
				String line;
				while ((line = br.readLine()) != null) {
					kakeiboDataStrList.add(line);
				}
			}

			publishProgress(100);
			Log.i(TAG, "doKakeiboExport end");
		}

		//後仕事
		@Override
		protected void onPostExecute(final Boolean isSuccess) {
			progressDialog.dismiss();
			Log.w("ExportTask", "onPostExecute -----");

			if (isNoData) {
				//家計簿データが１件もない場合

				exportImportHelper.deleteExportFile();
				KakeiboUtils.toastShowLong(getApplicationContext(), R.string.export_data_nothing);
			} else if (!isSuccess) {
				//失敗時
				exportImportHelper.deleteExportFile();
				showErrorHappenedDialog();

			} else {
				//成功時
				Resources resources = getResources();
				if (ExportMethod.SAVE_SD_CARD == exportMethod) {
					AlertDialog.Builder builder = new AlertDialog.Builder(EiExportActivity.this);
					String msg = resources.getString(R.string.export_complete_msg);
					msg += "\n\n";
					msg += resources.getString(R.string.export_destination_msg);
					msg += "\n";
					msg += exportImportHelper.getExportFileNameForUser();
					builder.setMessage(msg);
					builder.setPositiveButton(android.R.string.ok, null);
					builder.create().show();
				} else {

					if (ExportMethod.SEND_MAIL_BY_MESSAGE_BODY == exportMethod) {
						exportImportHelper.deleteExportFile();

						Uri uri = Uri.parse("mailto:");
						Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
						intent.putExtra(Intent.EXTRA_SUBJECT, resources
								.getString(R.string.export_mail_subject));
						String msgContent = "";
						for (String line : kakeiboDataStrList) {
							msgContent += line + "\n";
						}
						intent.putExtra(Intent.EXTRA_TEXT, msgContent);
						startActivity(intent);
					} else if (ExportMethod.SEND_MAIL_BY_ATTACHED == exportMethod) {

						Intent intent = new Intent(Intent.ACTION_SEND);
						intent.putExtra(Intent.EXTRA_SUBJECT, resources
								.getString(R.string.export_mail_subject));
						intent.setType("text/plain");
						intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + exportFile.getPath()));
						startActivity(intent);
					}
				}
			}
		}

		/** エラー発生したよダイアログを表示 */
		private void showErrorHappenedDialog() {
			String msg = "";
			msg += getResources().getString(R.string.export_error_msg);
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

	/** date -> string */
	private String formatDate(final Date date) {
		return DateFormat.getDateFormat(this).format(date);
	}

	/** エクスポート方法enum */
	private enum ExportMethod {
		//
			SEND_MAIL_BY_ATTACHED("export_to_mail_attached"), //
			SEND_MAIL_BY_MESSAGE_BODY("export_to_mail_message_body"), //
			SAVE_SD_CARD("export_to_sd_card"), ;

		private final String stringsId;

		private ExportMethod(final String stringsId) {
			this.stringsId = stringsId;
		}

		public String getStringsId() {
			return stringsId;
		}

	}

	/** エクスポート期間enum */
	private enum ExportPeriod {
		/** 今月(2010/11/01~2010/11/14)*/
		THIS_MONTH("export_period_this_month"),
		/** 先月(2010/10/01~2010/10/31) */
		LAST_MONTH("export_period_last_month"),
		/** 過去三ヶ月(2010/08/01~2010/11/14) */
		PAST_THREE_MONTH("export_period_past_three_month"),
		/** 過去半年(2010/05/01~2010/11/14) */
		PAST_HALF_YEAR("export_period_past_half_year"),
		/** 今年(2010/01/01~2010/11/14) */
		THIS_YEAR("export_period_this_year"),
		/** 去年(2009/01/01~2010/12/31) */
		LAST_YEAR("export_period_last_year"),
		/** 期間指定 */
		SPECIFIED_PERIOD("export_period_specified");
		private final String stringsId;

		private ExportPeriod(final String stringsId) {
			this.stringsId = stringsId;
		}

		public String getStringsId() {
			return stringsId;
		}
	}
}
