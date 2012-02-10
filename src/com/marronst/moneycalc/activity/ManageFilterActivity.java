package com.marronst.moneycalc.activity;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

import com.marronst.moneycalc.R;
import com.marronst.moneycalc.adapter.FilterAdapter;
import com.marronst.moneycalc.consts.KakeiboConsts;
import com.marronst.moneycalc.db.FilterDao;
import com.marronst.moneycalc.db.HouseKeepingBookOpenHelper;
import com.marronst.moneycalc.dxo.FilterDxo;
import com.marronst.moneycalc.entity.Filter;
import com.marronst.moneycalc.helper.MenuHelper;
import com.marronst.moneycalc.helper.MenuHelper.MenuType;
import com.marronst.moneycalc.utils.KakeiboUtils;

public class ManageFilterActivity extends Activity {

	protected final String TAG = this.getClass().getSimpleName();

	private SQLiteDatabase db;

	private HouseKeepingBookOpenHelper houseKeepingBookOpenHelper;

	private FilterDao filterDao;

	private final FilterDxo filterDxo = new FilterDxo();

	private List<Filter> filterList;

	//リストクリック時のフィルター
	private Filter selectTargetFilter;

	//リストクリック時用
	private final int SELECT_FILTER_NAME = 0;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		Log.i(TAG, "onCreate start");
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.setContentView(R.layout.manage_filter);

		houseKeepingBookOpenHelper = new HouseKeepingBookOpenHelper(getApplicationContext());

		try {
			db = houseKeepingBookOpenHelper.getWritableDatabase();
		} catch (SQLiteException e) {
			Log.e(TAG, "getWritableDatabase時にエラー", e);
			throw e;
		}
		filterDao = new FilterDao(db);

		Log.i(TAG, "onCreate end");
	}

	@Override
	protected void onResume() {
		Log.i(TAG, "onResume start");
		super.onResume();

		//フィルターリストを更新
		updateFilterList();

		//追加ボタンの設定
		setupAddButton();

		Log.i(TAG, "onResume end");
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

	/** ダイアログの設定 */
	@Override
	protected Dialog onCreateDialog(final int id) {
		switch (id) {
			case SELECT_FILTER_NAME:
				String[] dialogList = new String[] { getResources().getString(R.string.DIALOG_EDIT),
						getResources().getString(R.string.DIALOG_DELETE) };
				final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {

					@Override
					public void onClick(final DialogInterface dialog, final int which) {
						switch (which) {
							case 0://フィルターの編集

								Intent intent = new Intent(getApplicationContext(),
										AddEditFilterActivity.class);
								intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
								intent.putExtra(KakeiboConsts.INTENT_KEY_FILTER_ID, selectTargetFilter.id
										.intValue());
								startActivity(intent);
								dialog.dismiss();

								break;
							case 1://削除
								// 削除確認ダイアログ
								new AlertDialog.Builder(ManageFilterActivity.this)
										//.setTitle(R.string.DIALOG_TITLE_DELETE_CONFIRM)
										.setMessage(R.string.DIALOG_TITLE_DELETE_CONFIRM)
										.setIcon(R.drawable.alert_dialog_icon)
										.setPositiveButton(R.string.BUTTON_YES,
															new DialogInterface.OnClickListener() {

																@Override
																public void onClick(
																		final DialogInterface dialog,
																		final int which) {
																	// 削除実行

																	deleteFilter(selectTargetFilter.id);

																	KakeiboUtils
																			.toastShow(
																						getApplicationContext(),
																						R.string.DELETE_COMPLETE_MESSAGE);
																	// 削除後、カテゴリリストを更新
																	updateFilterList();
																}

															})
										.setNegativeButton(R.string.BUTTON_NO,
															new DialogInterface.OnClickListener() {
																@Override
																public void onClick(
																		final DialogInterface dialog,
																		final int which) {
																	//
																}
															}).show();
								break;
							default:
								break;
						}

					}

				};
				Dialog dialog = new AlertDialog.Builder(this).setTitle(R.string.DIALOG_TITLE_CHOICE)
						.setItems(dialogList, listener)
						.setNegativeButton(R.string.DIALOG_CANCEL, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(final DialogInterface dialog, final int which) {
								//なにもしない
							}
						}).create();
				return dialog;
		}
		return null;
	}

	/**追加ボタンの設定 */
	private void setupAddButton() {
		Button button = (Button) findViewById(R.id.add_filter);
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(final View v) {

				Intent intent = new Intent(getApplicationContext(), AddEditFilterActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			}

		});
	}

	/** フィルターの削除 */
	private void deleteFilter(final Integer filterId) {
		filterDao.deleteById(filterId);
	}

	/** カテゴリリストを更新 */
	private void updateFilterList() {
		ListView listView = (ListView) findViewById(R.id.filter_name_list);
		filterList = filterDao.findAll();

		FilterAdapter adapter = new FilterAdapter(this, R.layout.manage_filter_row, filterList);
		listView.setAdapter(adapter);

		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(final AdapterView<?> parent, final View view, final int position,
					final long id) {
				ListView listView = (ListView) parent;
				selectTargetFilter = (Filter) listView.getItemAtPosition(position);

				showDialog(SELECT_FILTER_NAME);

			}
		});

		View filterNameListBottomDivider = findViewById(R.id.filter_name_list_bottom_divider);
		if (KakeiboUtils.isEmpty(filterList)) {
			filterNameListBottomDivider.setVisibility(View.GONE);
		} else {
			filterNameListBottomDivider.setVisibility(View.VISIBLE);
		}

	}
}
