package com.marronst.moneycalc.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.marronst.moneycalc.R;
import com.marronst.moneycalc.adapter.AddEditFilterAdapter;
import com.marronst.moneycalc.consts.KakeiboConsts;
import com.marronst.moneycalc.db.CategoryDao;
import com.marronst.moneycalc.db.FilterDao;
import com.marronst.moneycalc.db.HouseKeepingBookOpenHelper;
import com.marronst.moneycalc.dto.CategoryFilterDto;
import com.marronst.moneycalc.dxo.CategoryDxo;
import com.marronst.moneycalc.dxo.FilterDxo;
import com.marronst.moneycalc.entity.Category;
import com.marronst.moneycalc.entity.Filter;
import com.marronst.moneycalc.helper.MenuHelper;
import com.marronst.moneycalc.helper.MenuHelper.MenuType;
import com.marronst.moneycalc.utils.KakeiboUtils;

public class AddEditFilterActivity extends Activity {

	protected final String TAG = this.getClass().getSimpleName();

	private SQLiteDatabase db;

	private HouseKeepingBookOpenHelper houseKeepingBookOpenHelper;

	private FilterDao filterDao;

	private CategoryDao categoryDao;

	private final FilterDxo filterDxo = new FilterDxo();

	private final CategoryDxo categoryDxo = new CategoryDxo();

	private final List<CategoryFilterDto> categoryFilterDtoList = new ArrayList<CategoryFilterDto>();

	//編集対象フィルター
	private Filter editTargetFilter;

	//登録画面ならtrue
	private boolean isAddView = false;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		Log.i(TAG, "onCreate start");
		super.onCreate(savedInstanceState);

		this.getWindow()
				.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.setContentView(R.layout.add_edit_filter);

		houseKeepingBookOpenHelper = new HouseKeepingBookOpenHelper(getApplicationContext());

		try {
			db = houseKeepingBookOpenHelper.getWritableDatabase();
		} catch (SQLiteException e) {
			Log.e(TAG, "getWritableDatabase時にエラー", e);
			throw e;
		}
		filterDao = new FilterDao(db);
		categoryDao = new CategoryDao(db);

		Log.i(TAG, "onCreate end");
	}

	@Override
	protected void onResume() {
		Log.i(TAG, "onResume start");
		super.onResume();

		Intent intent = getIntent();
		int filterId = intent.getIntExtra(KakeiboConsts.INTENT_KEY_FILTER_ID, Filter.FILTER_NONE);
		isAddView = filterId == Filter.FILTER_NONE;
		if (!isAddView) {
			editTargetFilter = filterDao.findById(filterId);
			if (editTargetFilter == null) {
				throw new IllegalStateException("filter is null. filterId = " + filterId);
			}
		}

		//タイトルの更新
		updateTitle();

		//フィルター名テキストボックスの設定
		setupFilterNameTextBox();

		//デフォルトフィルター設定チェックボックスの設定
		setupDefaultFilterCheckBox();

		//カテゴリリストを更新
		setupCategoryList();

		//Okボタンの設定
		setupOkButton();

		//キャンセルボタンの設定
		setupCancelButton();

		Log.i(TAG, "onResume end");
	}

	/** デフォルトフィルター設定チェックボックスの設定 */
	private void setupDefaultFilterCheckBox() {
		final CheckBox isDefaultCheckBox = (CheckBox) findViewById(R.id.is_default_check_box);
		if (!isAddView) {
			isDefaultCheckBox.setChecked(editTargetFilter.isDefault.equals(Filter.DEFAULT_FLG_ON));
		}

		TextView isDefaultCheckBoxLabel = (TextView) findViewById(R.id.is_default_check_box_label);
		isDefaultCheckBoxLabel.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(final View v) {
				isDefaultCheckBox.setChecked(!isDefaultCheckBox.isChecked());
			}
		});
	}

	/** カテゴリリスト設定 */
	private void setupCategoryList() {

		List<Integer> categoryIdList = new ArrayList<Integer>();
		if (!isAddView) {
			String categoryIdListStr = editTargetFilter.categoryIdList;
			if (!TextUtils.isEmpty(categoryIdListStr)) {
				String[] split = categoryIdListStr.split(",");
				if (split != null && split.length > 0) {
					for (String id : split) {
						categoryIdList.add(Integer.valueOf(id));
					}
				}
			}
		}

		ListView listView = (ListView) findViewById(R.id.filter_name_list);
		Cursor c = categoryDao.findAllWithNonDisplay();
		startManagingCursor(c);
		while (c.moveToNext()) {
			Category category = categoryDxo.createFromCursol(c);

			CategoryFilterDto categoryFilterDto = new CategoryFilterDto();
			categoryFilterDto.categoryName = category.categoryName;
			categoryFilterDto.categoryId = category.id;
			if (categoryIdList.contains(category.id)) {
				categoryFilterDto.isSelect = true;
			}
			categoryFilterDtoList.add(categoryFilterDto);
		}

		AddEditFilterAdapter adapter = new AddEditFilterAdapter(this, R.layout.add_edit_filter_row,
				categoryFilterDtoList);
		listView.setAdapter(adapter);

	}

	/** フィルター名テキストボックスの設定 */
	private void setupFilterNameTextBox() {
		if (!isAddView) {
			EditText editText = (EditText) findViewById(R.id.filter_name_text_box);
			editText.setText(editTargetFilter.filterName);
		}

	}

	/** キャンセルボタンの設定 */
	private void setupCancelButton() {
		Button cancelButton = (Button) findViewById(R.id.cancel_button);
		cancelButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(final View v) {
				finish();
			}
		});

	}

	/** Okボタンの設定 */
	private void setupOkButton() {
		Button okButton = (Button) findViewById(R.id.ok_button);
		int okButtonResId;
		if (isAddView) {
			okButtonResId = R.string.ADD;
		} else {
			okButtonResId = R.string.EDIT;
		}
		okButton.setText(okButtonResId);
		okButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(final View v) {

				EditText editText = (EditText) findViewById(R.id.filter_name_text_box);
				String filterName = editText.getText().toString();
				if (TextUtils.isEmpty(filterName)) {
					KakeiboUtils.toastShow(getApplicationContext(), R.string.required_filter_name);
					return;
				}

				String categoryIdList = "";
				for (int i = 0; i < categoryFilterDtoList.size(); i++) {
					CategoryFilterDto categoryFilterDto = categoryFilterDtoList.get(i);
					if (categoryFilterDto.isSelect) {
						categoryIdList += categoryFilterDto.categoryId + ",";
					}
				}
				if (categoryIdList.length() > 0) {
					categoryIdList = categoryIdList.substring(0, categoryIdList.length() - 1);
				} else {
					KakeiboUtils.toastShow(getApplicationContext(), R.string.required_filter_category);
					return;
				}

				CheckBox isDefaultCheckBox = (CheckBox) findViewById(R.id.is_default_check_box);
				int isDefault = isDefaultCheckBox.isChecked() ? Filter.DEFAULT_FLG_ON
						: Filter.DEFAULT_FLG_OFF;

				if (isAddView) {
					//登録処理
					Filter filter = new Filter();
					filter.filterName = filterName;
					filter.isDefault = isDefault;
					filter.categoryIdList = categoryIdList;
					filterDao.insert(filter);
				} else {
					//編集処理
					Filter filter = editTargetFilter;
					filter.filterName = filterName;
					if (isDefault == Filter.DEFAULT_FLG_ON && filter.isDefault == Filter.DEFAULT_FLG_OFF) {
						filterDao.updateAllDefaultFlgOff();
					}
					filter.isDefault = isDefault;
					filter.categoryIdList = categoryIdList;
					filterDao.update(filter);
				}
				finish();
			}
		});
	}

	/** タイトルの更新 */
	private void updateTitle() {
		TextView tilteTextView = (TextView) findViewById(R.id.title);

		int titleResId;
		if (isAddView) {
			titleResId = R.string.add_filter_title;
		} else {
			titleResId = R.string.edit_filter_title;
		}
		tilteTextView.setText(titleResId);
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

}
