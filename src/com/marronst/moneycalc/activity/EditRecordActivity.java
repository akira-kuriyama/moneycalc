package com.marronst.moneycalc.activity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

import com.marronst.moneycalc.R;
import com.marronst.moneycalc.app.DateTimeDialogBuilder;
import com.marronst.moneycalc.app.DateTimeDialogBuilder.DateTimeDialogResultListener;
import com.marronst.moneycalc.consts.KakeiboConsts;
import com.marronst.moneycalc.consts.KakeiboConsts.KakeiboListViewType;
import com.marronst.moneycalc.db.CategoryDao;
import com.marronst.moneycalc.db.HouseKeepingBookDao;
import com.marronst.moneycalc.db.HouseKeepingBookOpenHelper;
import com.marronst.moneycalc.dxo.CategoryDxo;
import com.marronst.moneycalc.entity.Category;
import com.marronst.moneycalc.entity.HouseKeepingBook;
import com.marronst.moneycalc.helper.MenuHelper;
import com.marronst.moneycalc.helper.MenuHelper.MenuType;
import com.marronst.moneycalc.utils.KakeiboFormatUtils;
import com.marronst.moneycalc.utils.KakeiboUtils;

public class EditRecordActivity extends Activity {
	protected final String TAG = this.getClass().getSimpleName();

	public static final String TARGET_HOUSE_KEEPING_BOOK = "TARGET_HOUSE_KEEPING_BOOK";

	protected SQLiteDatabase db;

	private HouseKeepingBookOpenHelper houseKeepingBookOpenHelper;
	private HouseKeepingBookDao houseKeepingBookDao;
	private CategoryDao categoryDao;

	private final CategoryDxo categoryDxo = new CategoryDxo();

	private HouseKeepingBook targetHouseKeepingBook;

	private List<Category> categoryList;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		Log.i(TAG, "onCreate start");

		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.setContentView(R.layout.edit_record);

		houseKeepingBookOpenHelper = new HouseKeepingBookOpenHelper(getApplicationContext());

		try {
			db = houseKeepingBookOpenHelper.getWritableDatabase();
		} catch (SQLiteException e) {
			Log.e(TAG, "getWritableDatabase時にエラー", e);
			throw e;
		}

		houseKeepingBookDao = new HouseKeepingBookDao(db);
		categoryDao = new CategoryDao(db);

		targetHouseKeepingBook = (HouseKeepingBook) getIntent().getParcelableExtra(TARGET_HOUSE_KEEPING_BOOK);

		Log.i(TAG, "onCreate end");
	}

	@Override
	protected void onResume() {
		Log.i(TAG, "onResume start");
		super.onResume();

		//カテゴリリストの初期化
		categoryList = getCategoryList();

		// 金額のセット
		EditText editText = (EditText) findViewById(R.id.input_value);
		String price = KakeiboFormatUtils.formatPriceNoUnit(targetHouseKeepingBook.price.longValue());
		editText.setText(price.replace(",", ""));

		// 金額単位のセット
		int currnecyUnitEnableId;
		int currnecyUnitDisableId;
		if (KakeiboUtils.isCurrencyUnitPositionFront(getApplicationContext())) {
			currnecyUnitEnableId = R.id.currency_unit_front;
			currnecyUnitDisableId = R.id.currency_unit_back;
		} else {
			currnecyUnitEnableId = R.id.currency_unit_back;
			currnecyUnitDisableId = R.id.currency_unit_front;
		}
		TextView currencyUnitTextEnable = (TextView) findViewById(currnecyUnitEnableId);
		String currencyUnit = KakeiboUtils.getCurrencyUnit(getApplicationContext());
		currencyUnitTextEnable.setText(currencyUnit);

		TextView currencyUnitTextDisable = (TextView) findViewById(currnecyUnitDisableId);
		currencyUnitTextDisable.setVisibility(View.GONE);

		//支出・収入の表示設定
		for (Category category : categoryList) {
			if (category.id.equals(targetHouseKeepingBook.categoryId)) {
				changeIncomeExpenseName(category);
			}
		}

		//メモのセット
		EditText memoEditText = (EditText) findViewById(R.id.memo_value);
		memoEditText.setText(targetHouseKeepingBook.memo);

		// 編集ボタンの設定
		Button editButton = (Button) findViewById(R.id.edit_record);
		editButton.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(final View v) {
				Log.i(TAG, "edit_record onclick");

				EditText priceInput = (EditText) findViewById(R.id.input_value);
				String priceString = priceInput.getText().toString();
				if (TextUtils.isEmpty(priceString)) {
					priceString = Integer.toString(0);
				}
				priceString = priceString.replace(" ", "");
				if (new BigDecimal(priceString).compareTo(BigDecimal.ZERO) == -1) {
					KakeiboUtils.toastShow(EditRecordActivity.this, R.string.ERRORS_REIGSTER_MINUS_PRICE);
					return;
				}
				if (updateRecord()) {
					KakeiboUtils.toastShow(getApplicationContext(), R.string.EDIT_COMPLETE_MESSAGE);
					final KakeiboListViewType viewType = KakeiboListViewType.DAY;
					Intent i = new Intent(getApplicationContext(), ViewKakeiboListActivity.class);
					i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
					Calendar cal = Calendar.getInstance();
					cal.setTime(targetHouseKeepingBook.registerDate);
					i.putExtra(KakeiboConsts.INTENT_KEY_START_DATE_VALUE, KakeiboUtils
							.getStartCal(cal, viewType, EditRecordActivity.this));
					i.putExtra(KakeiboConsts.INTENT_KEY_LIST_VIEW_TYPE, viewType.name());
					startActivity(i);
				}
			}

		});

		// キャンセルボタンの設定
		Button cancelButton = (Button) findViewById(R.id.edit_cancel);
		cancelButton.setOnClickListener(new Button.OnClickListener() {
			@Override
			public void onClick(final View v) {
				Log.i(TAG, "edit_cancel onclick");
				finish();
			}
		});

		// カテゴリ選択するスピナーの設定
		setupCategorySpinner();

		// 購入日時の表示更新
		updateRegisterDateAndTime();

		//購入日付の編集ボタンの設定
		Button editRegisterButton = (Button) findViewById(R.id.edit_register_date);
		editRegisterButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				final DateTimeDialogBuilder dateTimeDialogBuilder = new DateTimeDialogBuilder();
				dateTimeDialogBuilder.init(EditRecordActivity.this, new DateTimeDialogResultListener() {

					@Override
					public void onReturnResultDate(final Date date) {
						targetHouseKeepingBook.registerDate = date;
						updateRegisterDateAndTime();
					}

				}, targetHouseKeepingBook.registerDate);
				dateTimeDialogBuilder.getEditRegisterDateDialog().show();
			}
		});

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

	/** レコードの編集 */
	/**
	 * @return
	 */
	private boolean updateRecord() {
		//カテゴリ
		Spinner spinner = (Spinner) findViewById(R.id.category_spinner);
		int itemPosition = spinner.getSelectedItemPosition();
		String categoryname = spinner.getItemAtPosition(itemPosition).toString();
		Category category = categoryDao.findIdByCategoryName(categoryname);

		//メモ
		EditText memoInput = (EditText) findViewById(R.id.memo_value);
		String memo = memoInput.getText().toString();

		//金額
		EditText priceInput = (EditText) findViewById(R.id.input_value);
		String priceString = priceInput.getText().toString();
		if (TextUtils.isEmpty(priceString)) {
			priceString = Integer.toString(0);
		}
		priceString = priceString.replace(" ", "");
		BigDecimal priceBD = new BigDecimal(priceString);
		if (priceBD.compareTo(new BigDecimal(KakeiboConsts.PRICE_LIMIT)) > 0) {
			KakeiboUtils.toastShow(getApplicationContext(), R.string.ERRORS_MAX_PRICE_OVER);
			return false;
		}
		int price;
		if (KakeiboUtils.isJapan()) {
			price = priceBD.intValue();
		} else {
			priceBD = priceBD.multiply(new BigDecimal(100));
			priceBD.setScale(BigDecimal.ROUND_HALF_UP);
			price = priceBD.intValue();
		}

		targetHouseKeepingBook.categoryId = category.id;
		targetHouseKeepingBook.price = price;
		targetHouseKeepingBook.memo = memo;

		houseKeepingBookDao.update(targetHouseKeepingBook);

		return true;
	}

	/** 購入日時の表示更新*/
	private void updateRegisterDateAndTime() {
		TextView dateTextView = (TextView) findViewById(R.id.disp_register_date_and_time);
		dateTextView.setText(KakeiboFormatUtils.formatDateTimeToString(getApplicationContext(),
																		targetHouseKeepingBook.registerDate));
	}

	/**  カテゴリ選択するスピナーをセット*/
	protected void setupCategorySpinner() {

		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(),
				android.R.layout.simple_spinner_item);

		int targetCategoryPosition = 0;

		for (Category category : categoryList) {
			arrayAdapter.add(category.categoryName);
			if (targetHouseKeepingBook.title.equals(category.categoryName)) {
				targetCategoryPosition = category.position;
			}

		}

		arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		Spinner spinner = (Spinner) findViewById(R.id.category_spinner);
		spinner.setAdapter(arrayAdapter);
		spinner.setSelection(targetCategoryPosition, true);
		spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(final AdapterView<?> parent, final View view, final int position,
					final long id) {
				String categoryName = (String) parent.getItemAtPosition(position);
				for (Category category : categoryList) {
					if (category.categoryName.equals(categoryName)) {
						changeIncomeExpenseName(category);
					}
				}
			}

			@Override
			public void onNothingSelected(final AdapterView<?> parent) {
				// 
			}
		});

	}

	/** 支出・収入の表示を変える*/
	private void changeIncomeExpenseName(final Category category) {
		TextView textView = (TextView) findViewById(R.id.incame_expense_name);
		if (category.incomeFlg == Category.INCOME_FLG_OFF) {
			textView.setText(R.string.EXPENSE_NAME);
			textView.setTextColor(getResources().getColor(R.color.expense_color));
		} else {
			textView.setText(R.string.INCOME_NAME);
			textView.setTextColor(getResources().getColor(R.color.income_color));
		}

	}

	/** カテゴリリストの取得 */
	private List<Category> getCategoryList() {
		List<Category> categoryList = new ArrayList<Category>();
		Cursor c = categoryDao.findAll();
		startManagingCursor(c);
		while (c.moveToNext()) {
			categoryList.add(categoryDxo.createFromCursol(c));
		}
		return categoryList;
	}

}
