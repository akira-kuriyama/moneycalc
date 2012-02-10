package com.marronst.moneycalc.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemClickListener;

import com.admob.android.ads.AdView;
import com.marronst.moneycalc.R;
import com.marronst.moneycalc.adapter.CategoryAdapter;
import com.marronst.moneycalc.db.BudgetDao;
import com.marronst.moneycalc.db.CategoryDao;
import com.marronst.moneycalc.db.HouseKeepingBookOpenHelper;
import com.marronst.moneycalc.dxo.CategoryDxo;
import com.marronst.moneycalc.entity.Category;
import com.marronst.moneycalc.helper.FilterHelper;
import com.marronst.moneycalc.helper.MenuHelper;
import com.marronst.moneycalc.helper.MenuHelper.MenuType;
import com.marronst.moneycalc.utils.KakeiboUtils;

public class EditCategoryActivity extends Activity {

	protected final String TAG = this.getClass().getSimpleName();

	private SQLiteDatabase db;

	private HouseKeepingBookOpenHelper houseKeepingBookOpenHelper;

	private CategoryDao categoryDao;

	private BudgetDao budgetDao;

	private FilterHelper filterHelper;

	private final CategoryDxo categoryDxo = new CategoryDxo();

	//リストクリック時用
	private final int SELECT_CATEGORY_NAME = 0;

	//内部プロパティ
	private Category editTargetCategory;

	private List<Category> categoryList;

	private static final String POSITION_NO_MOVE = "--";

	private AdView adView;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		Log.i(TAG, "onCreate start");
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.setContentView(R.layout.edit_category);

		houseKeepingBookOpenHelper = new HouseKeepingBookOpenHelper(getApplicationContext());

		try {
			db = houseKeepingBookOpenHelper.getWritableDatabase();
		} catch (SQLiteException e) {
			Log.e(TAG, "getWritableDatabase時にエラー", e);
			throw e;
		}

		categoryDao = new CategoryDao(db);
		budgetDao = new BudgetDao(db);
		filterHelper = new FilterHelper();

		Log.i(TAG, "onCreate end");
	}

	@Override
	protected void onResume() {
		Log.i(TAG, "onResume start");
		super.onResume();

		//Admobの設定
		//AdManager.setTestDevices(new String[] { AdManager.TEST_EMULATOR, });//test
		adView = new AdView(this);

		//カテゴリリストを更新
		updateCategoryList();

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
			case SELECT_CATEGORY_NAME:
				String[] dialogList = new String[] { getResources().getString(R.string.DIALOG_EDIT),
						getResources().getString(R.string.DIALOG_DELETE) };
				final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {

					@Override
					public void onClick(final DialogInterface dialog, final int which) {
						switch (which) {
							case 0://カテゴリの編集

								AlertDialog alertDialog = getEditCategoryDialog();
								alertDialog.show();

								break;
							case 1://削除
								// 削除確認ダイアログ
								new AlertDialog.Builder(EditCategoryActivity.this)
										.setTitle(R.string.DELETE_CATEGORY_TITLE)
										.setMessage(R.string.DELETE_CATEGORY_MESSAGE)
										.setIcon(R.drawable.alert_dialog_icon)
										.setPositiveButton(R.string.BUTTON_YES,
															new DialogInterface.OnClickListener() {

																@Override
																public void onClick(
																		final DialogInterface dialog,
																		final int which) {
																	// 削除実行

																	if (categoryList.size() <= 1) {
																		KakeiboUtils
																				.toastShowLong(
																								getApplicationContext(),
																								R.string.ERRORS_CANNOT_CATEGORY_DELETE);
																		return;
																	}

																	deleteCategory(editTargetCategory.id);

																	KakeiboUtils
																			.toastShow(
																						getApplicationContext(),
																						R.string.DELETE_COMPLETE_MESSAGE);
																	// 削除後、カテゴリリストを更新
																	updateCategoryList();
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
		Button button = (Button) findViewById(R.id.add_category);
		button.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(final View v) {
				AlertDialog addCategoryDialog = getAddCategoryDialog();
				addCategoryDialog.show();
			}

		});
	}

	/** カテゴリの追加処理 */
	private void addCategory(final View addCategoryRecordView) {
		EditText categoryNameInput = (EditText) addCategoryRecordView.findViewById(R.id.input_value);
		String categoryName = categoryNameInput.getText().toString();
		if (TextUtils.isEmpty(categoryName)) {
			KakeiboUtils.toastShowLong(getApplicationContext(), R.string.ERRORS_EMPTY_CATEGORY_NAME);
			return;
		}
		for (Category category : categoryList) {
			if (category.categoryName.equals(categoryName)) {
				KakeiboUtils.toastShowLong(getApplicationContext(), R.string.ERRORS_DUPLICATE_CATEGORY_NAME);
				return;
			}
		}

		RadioButton incomeFlgRadioButtonOn = (RadioButton) addCategoryRecordView
				.findViewById(R.id.income_flg_on);
		int incomeFlg = Category.INCOME_FLG_OFF;
		if (incomeFlgRadioButtonOn.isChecked()) {
			incomeFlg = Category.INCOME_FLG_ON;
		}

		categoryDao.insert(categoryName, incomeFlg);

		categoryNameInput.setText("");// 入力値のクリアー

		InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		inputMethodManager.hideSoftInputFromWindow(addCategoryRecordView.getWindowToken(), 0);

		KakeiboUtils.toastShow(getApplicationContext(), R.string.REGISTER_COMPLETE_MESSAGE);

		updateCategoryList();

	}

	/**　カテゴリ登録用ダイアログの生成と取得　 */
	private AlertDialog getAddCategoryDialog() {
		AlertDialog.Builder addCategoryDialogBuilder = new AlertDialog.Builder(this);

		LayoutInflater inflater = LayoutInflater.from(this);

		final View addCategoryRecordView = inflater.inflate(R.layout.add_category_record, null);

		//収支ラジオボタン
		RadioButton bPRadioButton = (RadioButton) addCategoryRecordView.findViewById(R.id.income_flg_off);
		bPRadioButton.setChecked(true);

		//ダイアログの生成
		addCategoryDialogBuilder.setView(addCategoryRecordView);

		addCategoryDialogBuilder.setPositiveButton(R.string.DIALOG_DECIDE,
													new DialogInterface.OnClickListener() {
														@Override
														public void onClick(final DialogInterface dialog,
																final int which) {

															addCategory(addCategoryRecordView);
															dialog.cancel();

														}
													});
		addCategoryDialogBuilder.setNegativeButton(R.string.DIALOG_CANCEL,
													new DialogInterface.OnClickListener() {
														@Override
														public void onClick(final DialogInterface dialog,
																final int which) {
															// 何もしない
														}
													});

		AlertDialog addCategoryDialog = addCategoryDialogBuilder.create();
		//ダイアログのタイトル設定
		addCategoryDialog.setTitle(R.string.DIALOG_TITLE_CATEGRY_ADD);
		return addCategoryDialog;
	}

	/**　カテゴリ編集用ダイアログの生成と取得　 */
	private AlertDialog getEditCategoryDialog() {
		AlertDialog.Builder editCategoryDialogBuilder = new AlertDialog.Builder(this);

		LayoutInflater inflater = LayoutInflater.from(this);

		final View editCategoryRecordView = inflater.inflate(R.layout.edit_category_record, null);

		//カテゴリ名テキストボックス
		EditText editText = (EditText) editCategoryRecordView.findViewById(R.id.input_value);
		editText.setText(editTargetCategory.categoryName);

		//表示、非表示ラジオボタン ⇒削除予定
		//		final int checkTargetDFRadioButtonId;
		//		if (Category.DISP_FLG_ON.equals(editTargetCategory.dispFlg)) {
		//			checkTargetDFRadioButtonId = R.id.disp_flg_on;
		//		} else {
		//			checkTargetDFRadioButtonId = R.id.disp_flg_off;
		//		}
		//		RadioButton radioButton = (RadioButton) editCategoryRecordView
		//				.findViewById(checkTargetDFRadioButtonId);
		//		radioButton.setChecked(true);

		//収支ラジオボタン
		final int checkTargetBPFRadioButtonId;
		if (Category.INCOME_FLG_ON.equals(editTargetCategory.incomeFlg)) {
			checkTargetBPFRadioButtonId = R.id.income_flg_on;
		} else {
			checkTargetBPFRadioButtonId = R.id.income_flg_off;
		}
		RadioButton bPRadioButton = (RadioButton) editCategoryRecordView
				.findViewById(checkTargetBPFRadioButtonId);
		bPRadioButton.setChecked(true);

		//表示順変更スピナーの設定
		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(),
				android.R.layout.simple_spinner_item);
		arrayAdapter.add(POSITION_NO_MOVE);
		for (int i = 1; i < categoryList.size(); i++) {
			arrayAdapter.add(Integer.toString(i));
		}
		arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		Spinner spinner = (Spinner) editCategoryRecordView.findViewById(R.id.category_position_spinner);
		spinner.setAdapter(arrayAdapter);

		//ダイアログの生成
		editCategoryDialogBuilder.setView(editCategoryRecordView);

		editCategoryDialogBuilder.setNegativeButton(R.string.DIALOG_CANCEL,
													new DialogInterface.OnClickListener() {
														@Override
														public void onClick(final DialogInterface dialog,
																final int which) {
															// なにもしない
														}
													});
		editCategoryDialogBuilder.setPositiveButton(R.string.DIALOG_DECIDE,
													new DialogInterface.OnClickListener() {

														@Override
														public void onClick(final DialogInterface dialog,
																final int which) {
															EditText editText = (EditText) editCategoryRecordView
																	.findViewById(R.id.input_value);
															final String categoryName = editText.getText()
																	.toString();
															if (TextUtils.isEmpty(categoryName)) {
																KakeiboUtils
																		.toastShow(
																					getApplicationContext(),
																					R.string.ERRORS_EMPTY_CATEGORY_NAME);
																return;

															}
															for (Category category : categoryList) {
																if (category.categoryName
																		.equals(categoryName)
																		&& !category.id
																				.equals(editTargetCategory.id)) {
																	KakeiboUtils
																			.toastShowLong(
																							getApplicationContext(),
																							R.string.ERRORS_DUPLICATE_CATEGORY_NAME);
																	return;
																}
															}
															updateCaregory(editCategoryRecordView);

															KakeiboUtils
																	.toastShow(
																				getApplicationContext(),
																				R.string.EDIT_COMPLETE_MESSAGE);

															updateCategoryList();

														}
													});

		final AlertDialog editCategoryDialog = editCategoryDialogBuilder.create();

		//ダイアログのタイトル設定
		editCategoryDialog.setTitle(R.string.DIALOG_TITLE_CATEGRY_EDIT);
		return editCategoryDialog;
	}

	/** カテゴリの更新　*/
	private void updateCaregory(final View editCategoryRecordView) {
		EditText editText = (EditText) editCategoryRecordView.findViewById(R.id.input_value);

		//⇒削除予定
		//		RadioButton dispFlgRadioButtonOn = (RadioButton) editCategoryRecordView
		//				.findViewById(R.id.disp_flg_on);
		//		int dispFlg = Category.DISP_FLG_OFF;
		//		if (dispFlgRadioButtonOn.isChecked()) {
		//			dispFlg = Category.DISP_FLG_ON;
		//		}
		int dispFlg = Category.DISP_FLG_ON;

		RadioButton incomeFlgRadioButtonOn = (RadioButton) editCategoryRecordView
				.findViewById(R.id.income_flg_on);
		int incomeFlg = Category.INCOME_FLG_OFF;
		if (incomeFlgRadioButtonOn.isChecked()) {
			incomeFlg = Category.INCOME_FLG_ON;
		}

		Spinner positionSpinner = (Spinner) editCategoryRecordView
				.findViewById(R.id.category_position_spinner);
		String positionStr = (String) positionSpinner.getSelectedItem();
		int movePosition = 0;
		if (!POSITION_NO_MOVE.equals(positionStr)) {
			movePosition = Integer.parseInt(positionStr);
		}
		Spinner positionOrientationSpinner = (Spinner) editCategoryRecordView
				.findViewById(R.id.category_position_orientation_spinner);
		String positionOrientation = (String) positionOrientationSpinner.getSelectedItem();

		if (getResources().getString(R.string.up).equals(positionOrientation)) {
			movePosition *= -1;
		}

		Category category = new Category();
		category.id = editTargetCategory.id;
		category.categoryName = editText.getText().toString();
		category.dispFlg = dispFlg;
		category.incomeFlg = incomeFlg;

		categoryDao.update(category, movePosition);

		if (Category.INCOME_FLG_ON.equals(category.incomeFlg)) {
			budgetDao.deleteByCategoryId(category.id);
		}

	}

	/** カテゴリの削除 */
	private void deleteCategory(final Integer categoryId) {

		categoryDao.deleteById(categoryId);
		budgetDao.deleteByCategoryId(categoryId);
		filterHelper.updateFilterForCategoryDeleted(getApplicationContext(), categoryId);

	}

	/** カテゴリリストを更新 */
	private void updateCategoryList() {
		ListView listView = (ListView) findViewById(R.id.category_name_list);
		categoryList = new ArrayList<Category>();
		Cursor c = categoryDao.findAllWithNonDisplay();
		startManagingCursor(c);
		while (c.moveToNext()) {
			Category category = categoryDxo.createFromCursol(c);
			categoryList.add(category);
		}
		CategoryAdapter adapter = new CategoryAdapter(this, R.layout.edit_category_row, categoryList);
		listView.setAdapter(adapter);

		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(final AdapterView<?> parent, final View view, final int position,
					final long id) {
				ListView listView = (ListView) parent;
				editTargetCategory = (Category) listView.getItemAtPosition(position);

				showDialog(SELECT_CATEGORY_NAME);

			}
		});

		adView.requestFreshAd();//Admobの更新
	}
}
