package com.marronst.moneycalc.activity;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import android.app.Activity;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.marronst.moneycalc.R;
import com.marronst.moneycalc.consts.KakeiboConsts;
import com.marronst.moneycalc.consts.KakeiboConsts.GraphType;
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
import com.marronst.moneycalc.task.GraphImageDownloadTask;
import com.marronst.moneycalc.utils.ImageCacheUtils;
import com.marronst.moneycalc.utils.KakeiboFormatUtils;
import com.marronst.moneycalc.utils.KakeiboUtils;

public class ViewGraphActivity extends Activity {
	protected final String TAG = this.getClass().getSimpleName();

	private SQLiteDatabase db;

	private HouseKeepingBookOpenHelper houseKeepingBookOpenHelper;

	private FilterDao filterDao;

	/**　表示対象日付 */
	private Calendar nowDispCalendar;

	/** 表示タイプ */
	private KakeiboListViewType kakeiboListViewType;

	/** 表示カテゴリーId */
	private Integer targetViewCategoryId;

	/** 円グラフデータ */
	private LinkedHashMap<String, Long> pieGraphMap;

	/** 棒グラフグラフデータ */
	private LinkedHashMap<String, Long> barGraphMap;

	/** 集計結果Dtoリスト */
	private List<KakeiboTotalDto> kakeiboTotalDtoList;

	/** 集計計算ヘルパー */
	private final CalcTotalHelper calcTotalHelper = new CalcTotalHelper();

	/** グラフタイプ */
	private GraphType graphType;

	/** カテゴリ名 */
	private String targetViewCategoryName;

	/** Display情報 */
	DisplayMetrics metrics;

	/** 適用フィルター*/
	private Filter filter;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		Log.i(TAG, "onCreate start");
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.setContentView(R.layout.view_graph);
		houseKeepingBookOpenHelper = new HouseKeepingBookOpenHelper(getApplicationContext());

		try {
			db = houseKeepingBookOpenHelper.getWritableDatabase();
		} catch (SQLiteException e) {
			Log.e(TAG, "getWritableDatabase時にエラー", e);
			throw e;
		}

		HouseKeepingBookDao houseKeepingBookDao = new HouseKeepingBookDao(db);
		BudgetDao budgetDao = new BudgetDao(db);
		filterDao = new FilterDao(db);

		calcTotalHelper.setDb(houseKeepingBookDao, budgetDao);

		//  共通下部ボタンの設定
		FooterHelper.setupCommonBottomButton(this);

		metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);

		Log.i(TAG, "onCreate end");
	}

	@Override
	protected void onResume() {
		Log.i(TAG, "onResume start");
		super.onResume();
		Intent intent = getIntent();
		String nowDispCalendarStr = intent.getStringExtra(KakeiboConsts.INTENT_KEY_START_DATE_VALUE);
		String[] cals = nowDispCalendarStr.split("/");
		Calendar cal = Calendar.getInstance();
		cal.set(Integer.parseInt(cals[0]), Integer.parseInt(cals[1]), Integer.parseInt(cals[2]), 0, 0, 0);
		nowDispCalendar = cal;
		graphType = (GraphType) intent.getSerializableExtra(KakeiboConsts.INTENT_KEY_GRAPH_TYPE);
		targetViewCategoryId = (Integer) intent
				.getSerializableExtra(KakeiboConsts.INTENT_KEY_TARGET_VIEW_CATEGORY_ID);
		String KakeiboListViewTypeStr = intent.getStringExtra(KakeiboConsts.INTENT_KEY_LIST_VIEW_TYPE);
		kakeiboListViewType = KakeiboListViewType.valueOf(KakeiboListViewTypeStr);
		targetViewCategoryName = intent.getStringExtra(KakeiboConsts.INTENT_KEY_CATEGORY_NAME);
		int filterId = intent.getIntExtra(KakeiboConsts.INTENT_KEY_FILTER_ID, Filter.FILTER_NONE);
		if (!Filter.FILTER_NONE.equals(filterId)) {
			filter = filterDao.findById(filterId);
		}
		// タイトルの更新
		updateTitle();

		// グラフの更新
		updateGraph();

		// 前の年月週、次の年月週の設定
		setupNextAndPreviousButton();

		Log.i(TAG, "onResume end");
	}

	//
	//	//警告を消すためのメソッド
	//	@SuppressWarnings("unchecked")
	//	private LinkedHashMap<String, Long> castToHashMap(final Serializable serializableExtra) {
	//		LinkedHashMap result = new LinkedHashMap();
	//		if (serializableExtra == null) {
	//			return result;
	//		}
	//		HashMap map = (HashMap<String, Long>) serializableExtra;
	//		result.putAll(map);
	//		return result;
	//	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		houseKeepingBookOpenHelper.close();
	}

	@Override
	protected void onPause() {
		super.onPause();

	};

	/** メニューの設定 */
	@Override
	public boolean onCreateOptionsMenu(final Menu menu) {
		boolean result = super.onCreateOptionsMenu(menu);
		ArrayList<MenuType> menuTypeList = new ArrayList<MenuType>();
		menuTypeList.add(MenuType.CHANGE_INTERVAL);
		menuTypeList.add(MenuType.PREFERENCE);
		if (GraphType.Bar.equals(graphType) && targetViewCategoryId == null) {
			menuTypeList.add(MenuType.VIEW_PIE_GRAPH);
		} else if (GraphType.Pie.equals(graphType)) {
			menuTypeList.add(MenuType.VIEW_BAR_GRAPH);
		}
		if (targetViewCategoryId == null) {
			menuTypeList.add(MenuType.CHANGE_FILTER);
		}

		MenuHelper.createOptionsMenu(this, menu, menuTypeList.toArray(new MenuType[] {}));
		return result;
	}

	/** メニュー項目の選択時 */
	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		MenuParam menuParam = new MenuParam();
		menuParam.nowDispCalendar = nowDispCalendar;
		menuParam.categoryId = targetViewCategoryId;
		menuParam.categoryName = targetViewCategoryName;
		menuParam.kakeiboListViewType = kakeiboListViewType;
		menuParam.viewingGraphtype = graphType;
		menuParam.viewingActivity = this.getClass();
		menuParam.nowFilterId = filter == null ? null : filter.id;
		if (MenuHelper.optionsItemSelected(this, item, menuParam)) {
			return true;
		} else {
			return super.onOptionsItemSelected(item);
		}
	}

	/** タイトルの更新 */
	private void updateTitle() {
		TextView textView = (TextView) findViewById(R.id.title);
		StringBuilder title = new StringBuilder();
		if (KakeiboUtils.isJapan()) {
			title.append(TotalAndListViewHelper.getTitle(kakeiboListViewType));
			title.append("の");
			switch (graphType) {
				case Bar:
					if (targetViewCategoryId != null) {
						title.append(targetViewCategoryName);
					}
					title.append("棒");
					break;
				case Pie:
					title.append("円");
					break;

				default:
					break;
			}
			title.append("グラフ");
		} else {
			if (targetViewCategoryId != null) {
				title.append(targetViewCategoryName + " ");
			}
			switch (graphType) {
				case Bar:
					title.append("Bar chart of ");
					break;
				case Pie:
					title.append("Pie chart of ");
					break;
				default:
					break;
			}
			title.append(TotalAndListViewHelper.getTitle(kakeiboListViewType));
		}

		if (filter != null) {
			title = new StringBuilder(FilterHelper.getAddFilterTitle(filter, title.toString()));
		}

		textView.setText(title.toString());

		switch (graphType) {
			case Bar:
				textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.chart_mini, 0, 0, 0);
				break;
			case Pie:
				textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.chart_pie_mini, 0, 0, 0);
				break;

			default:
				break;
		}

	}

	/** グラフの更新 */
	public void updateGraph() {

		// グラフタイトルの更新
		updateGraphTitle();

		// グラフの描画
		drowGraphImage();
	}

	/** グラフタイトルの更新  */
	private void updateGraphTitle() {
		//グラフタイトルの更新
		TextView graphTitleTextView = (TextView) findViewById(R.id.graph_title);
		boolean isSingleLine = true;
		if (GraphType.Bar.equals(graphType) && KakeiboListViewType.WEEK.equals(kakeiboListViewType)) {
			isSingleLine = false;
		}
		graphTitleTextView.setText(TotalAndListViewHelper.getKakeiboTopRowTitle(nowDispCalendar,
																				kakeiboListViewType, this,
																				isSingleLine));

		//平均金額ラベルの更新
		TextView averageLabelTitleTextView = (TextView) findViewById(R.id.average_label);
		if (GraphType.Bar.equals(graphType)) {
			String averageLabel = KakeiboConsts.EMPTY;
			boolean isJapan = KakeiboUtils.isJapan();
			switch (kakeiboListViewType) {
				case YEAR:
				case HALF_YEAR:
				case THREE_MONTH:
					if (isJapan) {
						averageLabel = "月";
					} else {
						averageLabel = "Monthly";
					}
					break;
				case MONTH:
				case WEEK:
					if (isJapan) {
						averageLabel = "日";
					} else {
						averageLabel = "Daily";
					}
					break;
				default:
					if (isJapan) {
						averageLabel = "時間";
					} else {
						averageLabel = "1 hour";
					}
					break;
			}
			if (isJapan) {
				averageLabelTitleTextView.setText("(" + averageLabel + "の平均)");
			} else {
				averageLabelTitleTextView.setText("(" + averageLabel + " Avg)");
			}
			averageLabelTitleTextView.setVisibility(View.VISIBLE);
		} else {
			averageLabelTitleTextView.setVisibility(View.GONE);
		}
	}

	/** グラフの描画(非同期で行う) */
	private void drowGraphImage() {
		final ImageView imageView = (ImageView) findViewById(R.id.graph_image);
		final String url = createGraphUrl();
		//	Log.i(TAG, "url= " + url);

		final View retryGraphDownloadArea = findViewById(R.id.retry_graph_download_area);
		retryGraphDownloadArea.setVisibility(View.GONE);
		final Button retryGraphDownloadButton = (Button) findViewById(R.id.retry_graph_download);
		retryGraphDownloadButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(final View v) {
				Intent i = getIntent();
				i.putExtra(KakeiboConsts.INTENT_KEY_START_DATE_VALUE, KakeiboUtils
						.getStartCal(nowDispCalendar, kakeiboListViewType, getApplicationContext()));
				startActivity(i);
			}
		});
		Bitmap cacheBitmap = ImageCacheUtils.get(url);
		if (cacheBitmap != null) {
			imageView.setImageBitmap(cacheBitmap);
		} else {

			final GraphImageDownloadTask graphImageDownloadTask = new GraphImageDownloadTask(this, imageView,
					retryGraphDownloadArea);
			graphImageDownloadTask.execute(url);
		}

	}

	/** グラフURLの生成 */
	private String createGraphUrl() {
		String graphUrl = KakeiboConsts.EMPTY;
		if (GraphType.Pie.equals(graphType)) {
			graphUrl = createPieGraphUrl();
		} else if (GraphType.Bar.equals(graphType)) {
			graphUrl = createBarGraphUrl();
		}

		return graphUrl;
	}

	/** 縦棒グラフのURL生成 */
	private String createBarGraphUrl() {
		if (barGraphMap == null || barGraphMap.isEmpty()) {
			barGraphMap = createBarGraphMap();
		}

		Set<Entry<String, Long>> entrySet = barGraphMap.entrySet();
		Long total = 0L;
		for (Entry<String, Long> entry : entrySet) {
			Long value = entry.getValue();
			total += value;
		}

		//合計金額の更新
		TextView expensePriceTextView = (TextView) findViewById(R.id.expense_price);
		expensePriceTextView.setText(KakeiboFormatUtils.formatPrice(total, getApplicationContext()));

		//平均金額の更新
		TextView averagePriceTitleTextView = (TextView) findViewById(R.id.average_price);
		if (GraphType.Bar.equals(graphType)) {
			averagePriceTitleTextView.setText(KakeiboFormatUtils.formatPrice(total / entrySet.size(),
																				getApplicationContext()));
			averagePriceTitleTextView.setVisibility(View.VISIBLE);
		} else {
			averagePriceTitleTextView.setVisibility(View.GONE);
		}

		StringBuilder builder = new StringBuilder();
		//240x320 Aria?
		//480x800 Desire	
		//480x854 Xperia 
		//320x480 HT-03A
		//480×960 IS01 SH-10B 
		//1024×600 GALAXY Tab
		int widthPixels = metrics.widthPixels;//640;// 
		int heightPixels = metrics.heightPixels;//960;//
		int imageHight = 0;
		int imageWidth = 0;

		if (heightPixels >= 700) {
			imageHight = 500;
		} else {
			if (KakeiboListViewType.MONTH.equals(kakeiboListViewType)) {
				imageHight = 305;
			} else {
				imageHight = 265;
			}

		}

		if (widthPixels >= 400) {
			imageWidth = widthPixels;
			if (imageWidth >= 800) {
				imageWidth = 800;
			}
		} else {
			if (KakeiboListViewType.MONTH.equals(kakeiboListViewType)) {
				imageWidth = 460;
			} else {
				imageWidth = 300;
			}
		}
		//pixelsを300000以下に収めるように調整
		while (imageHight * imageWidth > 300000) {
			imageHight--;
			imageWidth--;
		}

		builder.append("chs=" + imageWidth + "x" + imageHight);

		Long maxPrice = 0L;
		StringBuilder chxl = new StringBuilder();
		StringBuilder chd = new StringBuilder();
		final StringBuilder chxlSeparater = new StringBuilder("|");
		final StringBuilder chdSeparater = new StringBuilder(",");
		for (Entry<String, Long> entry : entrySet) {
			String label = entry.getKey();
			Long price = entry.getValue();
			chxl.append(URLEncoder.encode(label)).append(chxlSeparater);
			chd.append(price).append(chdSeparater);
			if (maxPrice < price) {
				maxPrice = price;
			}
		}

		builder.append("&chd=t:").append(chd.substring(0, chd.length() - 1));
		builder.append("&chxl=0:|").append(chxl.substring(0, chxl.length() - 1));

		builder.append("&chxr=0,0,0|1,0," + maxPrice.toString());
		builder.append("&chds=0," + maxPrice.toString());

		builder.append("&chxs=0,3C3C3C,11,0,l,676767|1,3C3C3C,11.5,0,l,676767");
		builder.append("&chxt=x,y");
		builder.append("&chbh=a,10,40");
		builder.append("&cht=bvg");
		builder.append("&chco=6161D8");
		builder.append("&chdlp=b");
		builder.append("&chg=0,10,0,0");
		//Log.i(TAG, "url=  " + builder.toString());
		return builder.toString();
	}

	/** 円グラフのURL生成 */
	private String createPieGraphUrl() {

		if (pieGraphMap == null || pieGraphMap.isEmpty()) {
			pieGraphMap = createPeiGraphMap();
		}
		Set<Entry<String, Long>> entrySet = pieGraphMap.entrySet();

		Long total = 0L;
		for (Entry<String, Long> entry : entrySet) {
			Long value = entry.getValue();
			total += value;
		}

		//合計金額の更新
		TextView expensePriceTextView = (TextView) findViewById(R.id.expense_price);
		expensePriceTextView.setText(KakeiboFormatUtils.formatPrice(total, getApplicationContext()));

		//平均金額の更新
		TextView averagePriceTitleTextView = (TextView) findViewById(R.id.average_price);
		if (GraphType.Bar.equals(graphType)) {
			averagePriceTitleTextView.setText(KakeiboFormatUtils.formatPrice(total / entrySet.size(),
																				getApplicationContext()));
			averagePriceTitleTextView.setVisibility(View.VISIBLE);
		} else {
			averagePriceTitleTextView.setVisibility(View.GONE);
		}

		BigDecimal totalBigDecimal = new BigDecimal(total);
		String[] marks = new String[] { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "n", "m",
				"o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y",
				"z"//
				, "a1", "b1", "c1", "d1", "e1", "f1", "g1", "h1", "i1", "j1", "k1", "l1", "n1", "m1", "o1",
				"p1", "q1", "r1", "s1", "t1", "u1", "v1", "w1", "x1", "y1", "z1" };
		String[] colors = new String[] { "ff7f7f", "ff7fbf", "ff7fff", "7f7fff", "7fffff", "7fff7f",
				"ffff7f", "bf7fff", "7fbfff", "7fffbf", "bfff7f", "ffbf7f" //ここまでビビットカラー
				, "dccb18", "38a1db", "c0c6c9", "fcc800", "3b7960", "493759", "460e44", "16160e", "c53d43" };

		StringBuilder builder = new StringBuilder();

		//480x800 Desire	
		//480x854 Xperia 
		//320x480 HT-03A
		//480x960 IS01 SH-10B
		//1024×600 GALAXY Tab
		boolean isLandscape = KakeiboUtils.isLandscape(this);

		int widthPixels = metrics.widthPixels;
		int heightPixels = metrics.heightPixels;
		int imageHight = 0;
		int imageWidth = 0;

		if (heightPixels > 800) {
			imageHight = 550;
		} else if (heightPixels > 700) {
			imageHight = 500;
		} else {
			if (isLandscape) {
				if (heightPixels > 320) {
					imageHight = 330;
				} else {
					imageHight = 230;

				}
			} else {
				imageHight = 280;
			}
		}

		if (widthPixels >= 800) {
			imageWidth = 650;
		} else if (widthPixels > 400) {

			if (isLandscape) {
				if (widthPixels > 480) {
					imageWidth = 480;
				} else {
					imageWidth = 360;
				}
			} else {
				imageWidth = 450;
			}
		} else {
			imageWidth = 310;
		}

		//pixelsを300000以下に収めるように調整
		while (imageHight * imageWidth > 300000) {
			imageHight--;
			imageWidth--;
		}

		builder.append("chs=" + imageWidth + "x" + imageHight + "&");

		if (widthPixels > 400) {
			builder.append("chma=80,80&");
		} else {
			if (entrySet.size() < 6) {
				builder.append("chma=60,60&");

			} else {
				builder.append("chma=40,40&");
			}
		}

		builder.append("chp=4.7&");

		StringBuilder chd = new StringBuilder("chd=t:");
		StringBuilder chdl = new StringBuilder("chdl=");
		StringBuilder chl = new StringBuilder("chl=");
		StringBuilder chco = new StringBuilder("chco=");
		int marksCounter = 0;
		int colorsCounter = 0;

		for (Entry<String, Long> entry : entrySet) {
			Long value = entry.getValue();
			String key = entry.getKey();

			String per = "0";
			String perForTitle = "0";
			if (!BigDecimal.ZERO.equals(totalBigDecimal)) {
				BigDecimal v = new BigDecimal(value);
				per = v.divide(totalBigDecimal, 2, BigDecimal.ROUND_HALF_UP).toString();
				perForTitle = v.multiply(new BigDecimal(100)).divide(totalBigDecimal, 0,
																		BigDecimal.ROUND_HALF_UP).toString();
			}
			String title = "";
			title = getUrlEncoded(key + "(" + perForTitle + "%)");
			String price = getUrlEncoded(KakeiboFormatUtils.formatPrice(value, getApplicationContext()));

			chd.append(per).append(",");
			chdl.append(marks[marksCounter] + ":").append(title).append("|");
			chl.append(marks[marksCounter] + ":").append(price).append("|");
			chco.append(colors[colorsCounter]).append("|");

			marksCounter++;
			if (marksCounter > marks.length - 1) {
				marksCounter = 0;
			}
			colorsCounter++;
			if (colorsCounter > colors.length - 1) {
				colorsCounter = 0;
			}

		}
		builder.append("chco=3072F3&");
		builder.append(chd.substring(0, chd.length() - 1)).append("&");
		builder.append(chdl.substring(0, chdl.length() - 1)).append("&");
		builder.append(chl.substring(0, chl.length() - 1)).append("&");
		builder.append(chco.substring(0, chco.length() - 1)).append("&");

		//	builder.append("chtt=" + title + "&");//タイトルは未設定
		builder.append("chts=323232,14.5&");
		builder.append("chxs=0,464646,11.5&");
		builder.append("chxt=x&");
		builder.append("cht=p&");
		builder.append("chdls=333333,14&");
		if (!isLandscape) {
			//縦向きならラベルを下に出す。
			builder.append("chdlp=b|");
		} else {
			builder.append("chdlp=r|");
		}
		for (int i = 0; i < entrySet.size(); i++) {
			builder.append(i);
			if (0 != entrySet.size() - 1) {
				builder.append(",");
			}
		}

		//Log.i(TAG, "url=  " + builder.toString());
		return builder.toString();

	}

	/** 円グラフ用集計結果マップの生成 */
	private LinkedHashMap<String, Long> createPeiGraphMap() {
		//集計結果を取得
		kakeiboTotalDtoList = calcTotalHelper.getKakeiboTotalDtoList(this, nowDispCalendar,
																		kakeiboListViewType,
																		getCatgoryIdList());
		LinkedHashMap<String, Long> graphMap = calcTotalHelper.convertGraphMap(kakeiboTotalDtoList);
		return graphMap;
	}

	/** 縦棒グラフ用集計結果マップの生成 */
	private LinkedHashMap<String, Long> createBarGraphMap() {
		//集計結果を取得
		List<Integer> catgoryIdList = getCatgoryIdList();
		LinkedHashMap<String, Long> graphMap = calcTotalHelper.getBarGraphMap(this, nowDispCalendar,
																				kakeiboListViewType,
																				catgoryIdList);
		return graphMap;
	}

	/** 前の年月週、次の年月週の設定 */
	private void setupNextAndPreviousButton() {

		// 前の年月週ボタンのonClick時の設定
		Button previousButton = (Button) findViewById(R.id.previous);
		previousButton.setText(TotalAndListViewHelper.previousButtonName(kakeiboListViewType, this));
		previousButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				pieGraphMap = null;//グラフデータ削除
				barGraphMap = null;//グラフデータ削除
				nowDispCalendar.add(TotalAndListViewHelper.getField(kakeiboListViewType), -1
						* TotalAndListViewHelper.getFieldValue(kakeiboListViewType));
				updateGraph();
				updateTitle();
			}

		});
		// 次の年月週ボタンのonClick時の設定
		Button nextButton = (Button) findViewById(R.id.next);
		nextButton.setText(TotalAndListViewHelper.nextButtonName(kakeiboListViewType, this));
		nextButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				pieGraphMap = null;//グラフデータ削除
				barGraphMap = null;//グラフデータ削除
				nowDispCalendar.add(TotalAndListViewHelper.getField(kakeiboListViewType),
									TotalAndListViewHelper.getFieldValue(kakeiboListViewType));
				updateGraph();
				updateTitle();
			}
		});
	}

	/** URLエンコードを行う*/
	private String getUrlEncoded(final String key) {
		String str = "";
		try {
			str = URLEncoder.encode(key, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			String deviceInfo = KakeiboUtils.getDeviceInfo(ViewGraphActivity.this);
			throw new IllegalStateException("UnsupportedEncodingException###deviceInfo=" + deviceInfo, e1);
		}
		return str;
	}

	/** フィルタリング対象のカテゴリIDのリストを取得する */
	private List<Integer> getCatgoryIdList() {
		List<Integer> catgoryIdList = new ArrayList<Integer>();
		if (targetViewCategoryId != null) {
			catgoryIdList.add(targetViewCategoryId);
		} else if (filter != null) {
			String[] categoryIdStrArray = filter.categoryIdList.split(",");
			for (String idStr : categoryIdStrArray) {
				catgoryIdList.add(Integer.valueOf(idStr));
			}
		}
		return catgoryIdList;
	}
}
