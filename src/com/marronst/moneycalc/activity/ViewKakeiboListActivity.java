package com.marronst.moneycalc.activity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.marronst.moneycalc.R;
import com.marronst.moneycalc.adapter.HrAdapter;
import com.marronst.moneycalc.consts.KakeiboConsts;
import com.marronst.moneycalc.consts.KakeiboConsts.KakeiboListViewType;
import com.marronst.moneycalc.db.FilterDao;
import com.marronst.moneycalc.db.HouseKeepingBookDao;
import com.marronst.moneycalc.db.HouseKeepingBookOpenHelper;
import com.marronst.moneycalc.dxo.HouseKeepingBookDxo;
import com.marronst.moneycalc.entity.Category;
import com.marronst.moneycalc.entity.Filter;
import com.marronst.moneycalc.entity.HouseKeepingBook;
import com.marronst.moneycalc.entity.HouseKeepingBook.IdxHouseKeepingBook;
import com.marronst.moneycalc.helper.FilterHelper;
import com.marronst.moneycalc.helper.FooterHelper;
import com.marronst.moneycalc.helper.MenuHelper;
import com.marronst.moneycalc.helper.TotalAndListViewHelper;
import com.marronst.moneycalc.helper.MenuHelper.MenuParam;
import com.marronst.moneycalc.helper.MenuHelper.MenuType;
import com.marronst.moneycalc.utils.KakeiboUtils;

public class ViewKakeiboListActivity extends Activity {

	protected final String TAG = this.getClass().getSimpleName();
	protected SQLiteDatabase db;

	protected HouseKeepingBookDao houseKeepingBookDao;
	private FilterDao filterDao;

	private HouseKeepingBookOpenHelper houseKeepingBookOpenHelper;
	private final HouseKeepingBookDxo houseKeepingBookDxo = new HouseKeepingBookDxo();

	/** 選択対象のレコード */
	private HouseKeepingBook targetHouseKeepingBook;

	/** リ ストクリック時用 */
	private final int SELECT_RECORD_ID = 0;

	/**　表示対象日付 */
	private Calendar nowDispCalendar = Calendar.getInstance();

	/** 表示タイプ */
	private KakeiboListViewType kakeiboListViewType;

	/** 表示カテゴリーId */
	private Integer targetViewCategoryId;

	/** 表示カテゴリー名 */
	private String targetViewCategoryName;

	/** 適用フィルター*/
	private Filter filter;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		Log.i(TAG, "onCreate start");
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.setContentView(R.layout.view_kakeibo_list);

		houseKeepingBookOpenHelper = new HouseKeepingBookOpenHelper(getApplicationContext());

		try {
			db = houseKeepingBookOpenHelper.getWritableDatabase();
		} catch (SQLiteException e) {
			Log.e(TAG, "getWritableDatabase時にエラー", e);
			throw e;
		}

		houseKeepingBookDao = new HouseKeepingBookDao(db);
		filterDao = new FilterDao(db);

		// リストのクリック時の設定
		ListView listView = (ListView) findViewById(android.R.id.list);
		listView.setOnItemClickListener(new KakeiboListListener());

		// 共通下部ボタンの設定
		FooterHelper.setupCommonBottomButton(this);

		Log.i(TAG, "onCreate end");
	}

	@Override
	protected void onResume() {
		Log.i(TAG, "onResume start");
		super.onResume();

		//Debug.startMethodTracing("moneyCalc");

		final Intent intent = getIntent();
		String nowDispCalendarStr = intent.getStringExtra(KakeiboConsts.INTENT_KEY_START_DATE_VALUE);
		String[] cals = nowDispCalendarStr.split("/");
		Calendar cal = Calendar.getInstance();
		cal.set(Integer.parseInt(cals[0]), Integer.parseInt(cals[1]), Integer.parseInt(cals[2]), 0, 0, 0);
		nowDispCalendar = cal;
		String KakeiboListViewTypeStr = intent.getStringExtra(KakeiboConsts.INTENT_KEY_LIST_VIEW_TYPE);
		kakeiboListViewType = KakeiboListViewType.valueOf(KakeiboListViewTypeStr);
		String categoryIdStr = intent.getStringExtra(KakeiboConsts.INTENT_KEY_CATEGORY_ID);
		targetViewCategoryId = categoryIdStr == null ? null : Integer.parseInt(categoryIdStr);
		targetViewCategoryName = intent.getStringExtra(KakeiboConsts.INTENT_KEY_CATEGORY_NAME);
		int filterId = intent.getIntExtra(KakeiboConsts.INTENT_KEY_FILTER_ID, Filter.FILTER_NONE);
		if (!Filter.FILTER_NONE.equals(filterId)) {
			filter = filterDao.findById(filterId);
		}
		// タイトルの更新
		updateTitle();

		// リストの更新
		updateList();

		// 前の年月週、次の年月週の設定 
		setupNextAndPreviousButton();

		//Debug.stopMethodTracing();
		Log.i(TAG, "onResume end");
	}

	@Override
	public boolean onKeyDown(final int keyCode, final KeyEvent event) {
		if (keyCode != KeyEvent.KEYCODE_BACK) {
			return super.onKeyDown(keyCode, event);
		} else {

			Intent i = new Intent();
			i.putExtra(KakeiboConsts.INTENT_KEY_START_DATE_VALUE, KakeiboUtils
					.getStartCal(nowDispCalendar, kakeiboListViewType, getApplicationContext()));
			i.putExtra(KakeiboConsts.INTENT_KEY_LIST_VIEW_TYPE, kakeiboListViewType.name());
			setResult(RESULT_OK, i);
			finish();
			return false;
		}
	}

	@Override
	protected void onPause() {
		Log.i(TAG, "onPause start");
		super.onPause();

		Log.i(TAG, "onPause end");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		houseKeepingBookOpenHelper.close();
	}

	private class KakeiboListListener implements OnItemClickListener {

		@Override
		public void onItemClick(final AdapterView<?> parent, final View view, final int position,
				final long id) {
			ListView listView = (ListView) parent;
			targetHouseKeepingBook = (HouseKeepingBook) listView.getItemAtPosition(position);

			if (((HouseKeepingBookDto) targetHouseKeepingBook).isTotalRow) {
				return;
			}
			showDialog(SELECT_RECORD_ID);

			Log.i(TAG, "onItemLongClick end");
		}

	}

	/** メニューの設定 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		ArrayList<MenuType> menuTypeList = new ArrayList<MenuType>();
		menuTypeList.add(MenuType.REGISTER);
		menuTypeList.add(MenuType.CHANGE_INTERVAL);
		menuTypeList.add(MenuType.PREFERENCE);
		menuTypeList.add(MenuType.VIEW_BAR_GRAPH);
		if (targetViewCategoryId == null) {
			menuTypeList.add(MenuType.VIEW_PIE_GRAPH);
			menuTypeList.add(MenuType.CHANGE_FILTER);
		}

		MenuHelper.createOptionsMenu(this, menu, menuTypeList.toArray(new MenuType[] {}));
		return result;
	}

	/** メニュー項目の選択時 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		boolean result = false;

		MenuParam menuParam = new MenuParam();
		menuParam.kakeiboListViewType = kakeiboListViewType;
		menuParam.nowDispCalendar = nowDispCalendar;
		menuParam.categoryId = targetViewCategoryId;
		menuParam.categoryName = targetViewCategoryName;
		menuParam.viewingActivity = this.getClass();
		menuParam.nowFilterId = filter == null ? null : filter.id;

		MenuType menuType = MenuType.getMenuType(item.getItemId());
		if (MenuType.VIEW_PIE_GRAPH.equals(menuType) || MenuType.VIEW_BAR_GRAPH.equals(menuType)
				|| MenuType.CHANGE_INTERVAL.equals(menuType) || MenuType.CHANGE_FILTER.equals(menuType)
				|| MenuType.REGISTER.equals(menuType)) {

			result = MenuHelper.optionsItemSelected(this, item, menuParam);
		} else {
			result = MenuHelper.optionsItemSelected(this, item);
		}

		if (result) {
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	@Override
	protected Dialog onCreateDialog(final int id) {
		switch (id) {
			case SELECT_RECORD_ID:
				String[] dialogList = new String[] { getResources().getString(R.string.DIALOG_EDIT),
						getResources().getString(R.string.DIALOG_DELETE) };
				final DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {

					public void onClick(final DialogInterface dialog, final int which) {

						switch (which) {
							case 0:// 編集
								Intent i = new Intent(getApplicationContext(), EditRecordActivity.class);
								i.putExtra(EditRecordActivity.TARGET_HOUSE_KEEPING_BOOK,
											targetHouseKeepingBook);
								startActivity(i);
								break;
							//							case 1:// 買った場所を地図で見る
							//								if (TextUtils.isEmpty(targetHouseKeepingBook.latitude)
							//										|| TextUtils.isEmpty(targetHouseKeepingBook.longitude)) {
							//									KakeiboUtils.toastShow(getApplicationContext(), "位置情報が記録されていません。");
							//									break;
							//								}
							//								Intent mapi = new Intent(getApplicationContext(), ViewBoughtMapActivity.class);
							//								mapi.putExtra(ViewBoughtMapActivity.TARGET_HOUSE_KEEPING_BOOK,
							//												targetHouseKeepingBook);
							//								Cursor c = houseKeepingBookDao.findRecordOfSeveralMonths(nowDispCalendar, 1);
							//								ArrayList<HouseKeepingBook> items = new ArrayList<HouseKeepingBook>();
							//								startManagingCursor(c);
							//								while (c.moveToNext()) {
							//									HouseKeepingBook hkb = houseKeepingBookDxo.createFromCursor(c);
							//									hkb.categoryName = c.getString(c
							//											.getColumnIndex(CnCategory.categoryName()));
							//									items.add(hkb);
							//								}
							//								mapi
							//										.putParcelableArrayListExtra(
							//																		ViewBoughtMapActivity.SEVERAL_MONTHS_HOUSE_KEEPING_BOOKS,
							//																		items);
							//								startActivity(mapi);
							//								break;
							case 1:// 削除
								deleteRecord();
								break;

							default:
								break;
						}
					}

				};
				return new AlertDialog.Builder(this)//
						.setTitle(R.string.DIALOG_TITLE_CHOICE)//
						.setItems(dialogList, listener)//
						.setNegativeButton(R.string.DIALOG_CANCEL, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(final DialogInterface dialog, final int which) {
								// キャンセルなのでなにもしない
							}
						}).create();

			default:
				return null;
		}

	}

	/** 一覧の更新 */
	private void updateList() {
		Log.i(TAG, "updateList start");

		Cursor c = houseKeepingBookDao.findByMonth(nowDispCalendar, kakeiboListViewType, getCatgoryIdList());
		ArrayList<HouseKeepingBookDto> items = new ArrayList<HouseKeepingBookDto>();
		startManagingCursor(c);
		int totalExpencePrice = 0;
		int totalIncomePrice = 0;
		boolean isDispIncomePrice = false;
		while (c.moveToNext()) {
			HouseKeepingBook hkb = houseKeepingBookDxo.createFromCursor(c, true);
			HouseKeepingBookDto dto = convertHouseKeepingBookToDto(hkb);
			int lastIndex = IdxHouseKeepingBook.lastIndex();
			dto.title = c.getString(++lastIndex);
			dto.incomeFlg = c.getInt(++lastIndex);
			if (Category.INCOME_FLG_ON == dto.incomeFlg) {
				dto.incomePrice = hkb.price;
				isDispIncomePrice = true;
				totalIncomePrice += hkb.price;

			} else {
				dto.expensePrice = hkb.price;
				totalExpencePrice += hkb.price;
			}

			items.add(dto);
		}

		HouseKeepingBookDto totalRow = new HouseKeepingBookDto();
		String title = TotalAndListViewHelper.getKakeiboTopRowTitle(nowDispCalendar, kakeiboListViewType,
																	this);
		totalRow.title = title;
		totalRow.expensePrice = totalExpencePrice;
		totalRow.incomePrice = totalIncomePrice;
		totalRow.isTotalRow = true;
		totalRow.isDispIncomePrice = isDispIncomePrice;
		totalRow.iKakeiboListViewType = kakeiboListViewType;
		items.add(0, totalRow);

		HrAdapter hrAdapter = new HrAdapter(this, R.layout.view_kakeibo_list_row, items);
		ListView listview = (ListView) findViewById(android.R.id.list);
		listview.setAdapter(hrAdapter);
		Log.i(TAG, "updateList end");
	}

	private HouseKeepingBookDto convertHouseKeepingBookToDto(final HouseKeepingBook hkb) {
		HouseKeepingBookDto dto = new HouseKeepingBookDto();
		dto.id = hkb.id;
		dto.categoryId = hkb.categoryId;
		dto.price = hkb.price;
		dto.memo = hkb.memo;
		dto.place = hkb.place;
		dto.registerDate = hkb.registerDate;
		dto.latitude = hkb.latitude;
		dto.longitude = hkb.longitude;

		return dto;
	}

	/** タイトルの更新 */
	private void updateTitle() {
		TextView textView = (TextView) findViewById(R.id.this_month_text);
		String title;
		String categoryName = KakeiboConsts.EMPTY;
		if (targetViewCategoryId != null) {
			categoryName = targetViewCategoryName;
		}
		if (KakeiboUtils.isJapan()) {
			title = TotalAndListViewHelper.getTitle(kakeiboListViewType) + "の" + categoryName + "一覧";
		} else {
			String front = "";
			if (TextUtils.isEmpty(categoryName)) {
				front = "List of ";
			} else {
				front = categoryName + " list of ";
			}
			title = front + TotalAndListViewHelper.getTitle(kakeiboListViewType);
		}

		if (targetViewCategoryId == null && filter != null) {
			title = FilterHelper.getAddFilterTitle(filter, title);
		}
		textView.setText(title);
	}

	/** 前の年月週、次の年月週の設定 */
	private void setupNextAndPreviousButton() {
		// 前の年月週ボタンのonClick時の設定
		Button previousButton = (Button) findViewById(R.id.previous);
		previousButton.setText(TotalAndListViewHelper.previousButtonName(kakeiboListViewType, this));
		previousButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				nowDispCalendar.add(TotalAndListViewHelper.getField(kakeiboListViewType), -1
						* TotalAndListViewHelper.getFieldValue(kakeiboListViewType));
				updateList();
				updateTitle();
			}

		});
		// 次の年月週ボタンのonClick時の設定
		Button nextButton = (Button) findViewById(R.id.next);
		nextButton.setText(TotalAndListViewHelper.nextButtonName(kakeiboListViewType, this));
		nextButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				nowDispCalendar.add(TotalAndListViewHelper.getField(kakeiboListViewType),
									TotalAndListViewHelper.getFieldValue(kakeiboListViewType));
				updateList();
				updateTitle();
			}
		});
	}

	// レコードを削除する
	private void deleteRecord() {
		// 削除確認ダイアログ
		new AlertDialog.Builder(ViewKakeiboListActivity.this).setIcon(R.drawable.alert_dialog_icon)
				.setTitle(R.string.DIALOG_TITLE_DELETE_CONFIRM)
				.setPositiveButton(R.string.BUTTON_YES, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(final DialogInterface dialog, final int which) {
						// 削除実行
						deleteHouseKeepingBook(targetHouseKeepingBook.id);

						KakeiboUtils.toastShow(getApplicationContext(), R.string.DELETE_COMPLETE_MESSAGE);

						// 削除後、表示リストを更新
						updateList();
					}

				}).setNegativeButton(R.string.BUTTON_NO, new DialogInterface.OnClickListener() {

					@Override
					public void onClick(final DialogInterface dialog, final int which) {
						//
					}
				}).show();
	}

	// レコード削除の実行
	private void deleteHouseKeepingBook(final Integer id) {
		houseKeepingBookDao.deleteById(id);
	}

	public class HouseKeepingBookDto extends HouseKeepingBook {

		public long expensePrice;
		public long incomePrice;
		public boolean isTotalRow;
		public boolean isDispIncomePrice;
		public KakeiboListViewType iKakeiboListViewType;
	}

	/** フィルタリング対象のカテゴリIDのリストを取得する */
	private List<Integer> getCatgoryIdList() {

		List<Integer> catgoryIdList = new ArrayList<Integer>();
		if (targetViewCategoryId != null) {
			catgoryIdList.add(targetViewCategoryId);
		} else {
			if (filter != null) {
				String[] categoryIdStrArray = filter.categoryIdList.split(",");
				for (String idStr : categoryIdStrArray) {
					catgoryIdList.add(Integer.valueOf(idStr));
				}
			}
		}
		return catgoryIdList;
	}

}
