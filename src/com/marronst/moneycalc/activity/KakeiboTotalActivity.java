package com.marronst.moneycalc.activity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.app.Activity;
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
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.marronst.moneycalc.R;
import com.marronst.moneycalc.adapter.KakeiboTotalAdapter;
import com.marronst.moneycalc.consts.KakeiboConsts;
import com.marronst.moneycalc.consts.KakeiboConsts.KakeiboListViewType;
import com.marronst.moneycalc.db.BudgetDao;
import com.marronst.moneycalc.db.FilterDao;
import com.marronst.moneycalc.db.HouseKeepingBookDao;
import com.marronst.moneycalc.db.HouseKeepingBookOpenHelper;
import com.marronst.moneycalc.dto.KakeiboTotalDto;
import com.marronst.moneycalc.entity.Filter;
import com.marronst.moneycalc.helper.CalcTotalHelper;
import com.marronst.moneycalc.helper.FilterHelper;
import com.marronst.moneycalc.helper.FooterHelper;
import com.marronst.moneycalc.helper.MenuHelper;
import com.marronst.moneycalc.helper.TotalAndListViewHelper;
import com.marronst.moneycalc.helper.MenuHelper.MenuParam;
import com.marronst.moneycalc.helper.MenuHelper.MenuType;
import com.marronst.moneycalc.task.CalcTotalTask;
import com.marronst.moneycalc.utils.KakeiboUtils;

public class KakeiboTotalActivity extends Activity {
	protected final String TAG = this.getClass().getSimpleName();

	private SQLiteDatabase db;

	private HouseKeepingBookOpenHelper houseKeepingBookOpenHelper;
	private HouseKeepingBookDao houseKeepingBookDao;
	private BudgetDao budgetDao;
	private FilterDao filterDao;

	/**　表示対象日付 */
	private Calendar nowDispCalendar;

	/** 表示タイプ */
	private KakeiboListViewType kakeiboListViewType;

	/** 集計計算ヘルパー */
	private CalcTotalHelper calcTotalHelper;

	/** 一覧画面から戻ってきたらtrue */
	private boolean isReturnListView = false;

	/** 適用フィルター*/
	private Filter filter;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		Log.i(TAG, "onCreate start");
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.setContentView(R.layout.kakeibo_total);
		houseKeepingBookOpenHelper = new HouseKeepingBookOpenHelper(getApplicationContext());

		try {
			db = houseKeepingBookOpenHelper.getWritableDatabase();
		} catch (SQLiteException e) {
			Log.e(TAG, "getWritableDatabase時にエラー", e);
			throw e;
		}

		houseKeepingBookDao = new HouseKeepingBookDao(db);
		budgetDao = new BudgetDao(db);
		filterDao = new FilterDao(db);
		calcTotalHelper = new CalcTotalHelper();
		calcTotalHelper.setDb(houseKeepingBookDao, budgetDao);

		//  共通下部ボタンの設定
		FooterHelper.setupCommonBottomButton(this);

		Log.i(TAG, "onCreate end");
	}

	@Override
	protected void onResume() {
		Log.i(TAG, "onResume start");
		super.onResume();

		//Debug.startMethodTracing("moneyCalc");

		//インテントから情報を取り出す
		if (!isReturnListView) {
			Intent intent = getIntent();
			String nowDispCalendarStr = intent.getStringExtra(KakeiboConsts.INTENT_KEY_START_DATE_VALUE);
			String[] cals = nowDispCalendarStr.split("/");
			Calendar cal = Calendar.getInstance();
			cal.set(Integer.parseInt(cals[0]), Integer.parseInt(cals[1]), Integer.parseInt(cals[2]), 0, 0, 0);
			nowDispCalendar = cal;
			String KakeiboListViewTypeStr = intent.getStringExtra(KakeiboConsts.INTENT_KEY_LIST_VIEW_TYPE);
			kakeiboListViewType = KakeiboListViewType.valueOf(KakeiboListViewTypeStr);
			int filterId = intent.getIntExtra(KakeiboConsts.INTENT_KEY_FILTER_ID, Filter.FILTER_NONE);
			if (!Filter.FILTER_NONE.equals(filterId)) {
				filter = filterDao.findById(filterId);
			}

		} else {
			isReturnListView = false;
		}

		// タイトルの更新
		updateTitle();

		// カテゴリ別合計リストの更新
		updateKakeiboTotalList();

		// 前の年月週、次の年月週の設定
		setupNextAndPreviousButton();

		//Debug.stopMethodTracing();

		Log.i(TAG, "onResume end");
	}

	@Override
	protected void onPause() {
		//Debug.stopMethodTracing();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		db.close();
		houseKeepingBookOpenHelper.close();
	}

	@Override
	protected void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
		Log.i(TAG, "onActivityResult start");
		if (requestCode == KakeiboConsts.REQUEST_CODE_VIEW_LIST && resultCode == RESULT_OK) {
			Log
					.i(TAG,
						"onActivityResult requestCode == KakeiboConsts.REQUEST_CODE_VIEW_LIST && resultCode == RESULT_OK");
			String nowDispCalendarStr = intent.getStringExtra(KakeiboConsts.INTENT_KEY_START_DATE_VALUE);
			String[] cals = nowDispCalendarStr.split("/");
			Calendar cal = Calendar.getInstance();
			cal.set(Integer.parseInt(cals[0]), Integer.parseInt(cals[1]), Integer.parseInt(cals[2]), 0, 0, 0);
			nowDispCalendar = cal;
			String KakeiboListViewTypeStr = intent.getStringExtra(KakeiboConsts.INTENT_KEY_LIST_VIEW_TYPE);
			kakeiboListViewType = KakeiboListViewType.valueOf(KakeiboListViewTypeStr);

			isReturnListView = true;
		}
		Log.i(TAG, "onActivityResult end");
	}

	/** メニューの設定 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		MenuHelper
				.createOptionsMenu(this, menu, MenuType.CHANGE_INTERVAL, MenuType.CHANGE_FILTER,
									MenuType.PREFERENCE, MenuType.VIEW_PIE_GRAPH, MenuType.VIEW_BAR_GRAPH);
		return result;
	}

	/** メニュー項目の選択時 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		boolean result = false;

		MenuParam menuParam = new MenuParam();
		menuParam.kakeiboListViewType = kakeiboListViewType;
		menuParam.nowDispCalendar = nowDispCalendar;
		menuParam.viewingActivity = this.getClass();
		menuParam.nowFilterId = filter == null ? null : filter.id;

		MenuType menuType = MenuType.getMenuType(item.getItemId());
		if (MenuType.VIEW_PIE_GRAPH.equals(menuType) || //
				MenuType.VIEW_BAR_GRAPH.equals(menuType) || //
				MenuType.CHANGE_INTERVAL.equals(menuType) || //
				MenuType.CHANGE_FILTER.equals(menuType)) {
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

	/** カテゴリ別合計リストの更新 */
	private void updateKakeiboTotalList() {
		Log.i(TAG, "updateKakeiboTotalList start");

		ListView listView = (ListView) findViewById(R.id.kakeibo_total_list);

		//ListViewにセット
		KakeiboTotalAdapter kakeiboTotalAdapter = new KakeiboTotalAdapter(this, R.layout.kakeibo_total_row,
				new ArrayList<KakeiboTotalDto>());
		listView.setAdapter(kakeiboTotalAdapter);

		listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(final AdapterView<?> parent, final View view, final int position,
					final long id) {
				ListView listView = (ListView) parent;
				KakeiboTotalDto kakeiboTotalDto = (KakeiboTotalDto) listView.getItemAtPosition(position);
				final KakeiboListViewType viewType = kakeiboListViewType;

				Intent i = new Intent(getApplicationContext(), ViewKakeiboListActivity.class);
				i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				i.putExtra(KakeiboConsts.INTENT_KEY_START_DATE_VALUE, KakeiboUtils
						.getStartCal(nowDispCalendar, viewType, getApplicationContext()));
				i.putExtra(KakeiboConsts.INTENT_KEY_LIST_VIEW_TYPE, viewType.name());
				i.putExtra(KakeiboConsts.INTENT_KEY_CATEGORY_ID, kakeiboTotalDto.categoryId == null ? null
						: kakeiboTotalDto.categoryId.toString());
				i.putExtra(KakeiboConsts.INTENT_KEY_CATEGORY_NAME, kakeiboTotalDto.title);

				if (kakeiboTotalDto.categoryId == null && filter != null) {
					i.putExtra(KakeiboConsts.INTENT_KEY_FILTER_ID, filter.id);
				}
				startActivityForResult(i, KakeiboConsts.REQUEST_CODE_VIEW_LIST);
			}
		});

		CalcTotalTask calcTotalTask = new CalcTotalTask(this, nowDispCalendar, kakeiboListViewType,
				getCatgoryIdList(), listView);
		calcTotalTask.execute();
		Log.i(TAG, "updateKakeiboTotalList end");

	}

	/** フィルタリング対象のカテゴリIDのリストを取得する */
	private List<Integer> getCatgoryIdList() {
		List<Integer> catgoryIdList = new ArrayList<Integer>();
		if (filter != null) {
			String[] categoryIdStrArray = filter.categoryIdList.split(",");
			for (String idStr : categoryIdStrArray) {
				catgoryIdList.add(Integer.valueOf(idStr));
			}
		}
		return catgoryIdList;
	}

	/** 前の年月週、次の年月週の設定 */
	private void setupNextAndPreviousButton() {
		// 前の年月週ボタンのonClick時の設定
		Button previousButton = (Button) findViewById(R.id.previous);
		previousButton.setText(TotalAndListViewHelper.previousButtonName(kakeiboListViewType, this));
		previousButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				TotalAndListViewHelper
						.changeToPreviousStartCalenderByKakeiboListViewType(getApplicationContext(),
																			nowDispCalendar,
																			kakeiboListViewType);
				updateKakeiboTotalList();
				updateTitle();
			}

		});
		// 次の年月週ボタンのonClick時の設定
		Button nextButton = (Button) findViewById(R.id.next);
		nextButton.setText(TotalAndListViewHelper.nextButtonName(kakeiboListViewType, this));
		nextButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				TotalAndListViewHelper
						.changeToNextStartCalenderByKakeiboListViewType(getApplicationContext(),
																		nowDispCalendar, kakeiboListViewType);
				updateKakeiboTotalList();
				updateTitle();
			}
		});
	}

	/** タイトルの更新 */
	private void updateTitle() {
		TextView textView = (TextView) findViewById(R.id.kakeibo_total_title);

		String title;
		if (KakeiboUtils.isJapan()) {
			title = TotalAndListViewHelper.getTitle(kakeiboListViewType) + "の合計";
		} else {
			title = "Total of " + TotalAndListViewHelper.getTitle(kakeiboListViewType);
		}
		if (filter != null) {
			title = FilterHelper.getAddFilterTitle(filter, title);
		}

		textView.setText(title);

	}
}
