package com.marronst.moneycalc.activity;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.marronst.moneycalc.R;
import com.marronst.moneycalc.adapter.BudgetAdapter;
import com.marronst.moneycalc.consts.KakeiboConsts;
import com.marronst.moneycalc.db.BudgetDao;
import com.marronst.moneycalc.db.HouseKeepingBookOpenHelper;
import com.marronst.moneycalc.entity.Budget;
import com.marronst.moneycalc.helper.MenuHelper;
import com.marronst.moneycalc.helper.MenuHelper.MenuType;
import com.marronst.moneycalc.listener.OnListViewButtonClickListener;
import com.marronst.moneycalc.utils.KakeiboFormatUtils;
import com.marronst.moneycalc.utils.KakeiboUtils;

public class ManageBudgetActivity extends Activity {

	protected final String TAG = this.getClass().getSimpleName();

	private SQLiteDatabase db;

	private HouseKeepingBookOpenHelper houseKeepingBookOpenHelper;

	private BudgetDao budgetDao;

	//内部プロパティ
	private Budget editTargetBudget;

	private List<Budget> budgetList;

	private Calendar nowDispCalendar;

	private static final int DIALOG_TYPE_TOTAL_BUDGET = 1;
	public static final int DIALOG_TYPE_ONE_BUDGET = 2;

	private final DecimalFormat budgetPriceFormat = new DecimalFormat("#,###.##");

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		Log.i(TAG, "onCreate start");
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.setContentView(R.layout.manage_budget);

		houseKeepingBookOpenHelper = new HouseKeepingBookOpenHelper(getApplicationContext());

		try {
			db = houseKeepingBookOpenHelper.getWritableDatabase();
		} catch (SQLiteException e) {
			Log.e(TAG, "getWritableDatabase時にエラー", e);
			throw e;
		}

		budgetDao = new BudgetDao(db);

		nowDispCalendar = Calendar.getInstance();

		//設定ボタンの設定
		setupSettingButton();

		Log.i(TAG, "onCreate end");
	}

	@Override
	protected void onResume() {
		Log.i(TAG, "onResume start");
		super.onResume();

		//予算リストを更新
		updateBudgetList();

		//タイトルの更新
		updateTitle();

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

	/** タイトルの更新 */
	private void updateTitle() {
		TextView manageBudgetTitle = (TextView) findViewById(R.id.manage_budget_title);
		int iconId;
		if (KakeiboUtils.isJapan()) {
			iconId = R.drawable.yen_currency_sign_mini;
		} else {
			iconId = R.drawable.dollar_currency_sign;
		}
		Drawable icon = getResources().getDrawable(iconId);
		icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
		manageBudgetTitle.setCompoundDrawables(icon, null, null, null);
	}

	/**前月、次月ボタンの設定 */
	private void setupSettingButton() {

		//前月ボタン
		Button previousMonthButton = (Button) findViewById(R.id.previous_month);
		String previousMonth = getResources().getString(R.string.PREVIOUS_MONTH);
		String previousMonthButtonName = "";
		if (KakeiboUtils.isLandscape(this)) {
			if (!KakeiboUtils.isJapan()) {
				previousMonth = previousMonth.split(" ")[0];
			}
			for (char c : previousMonth.toCharArray()) {
				previousMonthButtonName += c + "\n";
			}
			previousMonthButtonName = previousMonthButtonName.substring(0, previousMonthButtonName.length());
		} else {
			previousMonthButtonName = previousMonth;
		}
		previousMonthButton.setText(previousMonthButtonName);
		previousMonthButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				nowDispCalendar.add(Calendar.MONTH, -1);
				//予算リストを更新
				updateBudgetList();

			}
		});

		//次月ボタン
		Button nextMonthButton = (Button) findViewById(R.id.next_month);
		String nextMonth = getResources().getString(R.string.NEXT_MONTH);
		String nextMonthButtonName = "";
		if (KakeiboUtils.isLandscape(this)) {
			if (!KakeiboUtils.isJapan()) {
				nextMonth = nextMonth.split(" ")[0];
			}
			for (char c : nextMonth.toCharArray()) {
				nextMonthButtonName += c + "\n";
			}
			nextMonthButtonName = nextMonthButtonName.substring(0, nextMonthButtonName.length());
		} else {
			nextMonthButtonName = nextMonth;
		}
		nextMonthButton.setText(nextMonthButtonName);
		nextMonthButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				nowDispCalendar.add(Calendar.MONTH, +1);
				//予算リストを更新
				updateBudgetList();

			}
		});

		//先月を同じ予算を設定するボタン
		Button copyPreviousMonthBudgetSettingButton = (Button) findViewById(R.id.copy_previous_month_budget_setting);
		copyPreviousMonthBudgetSettingButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				// 先月と同じ予算を設定する
				getCopyPreviousMonthBudgetDialog();
			}

		});
	}

	/** 先月と同じ予算を設定する */
	private void getCopyPreviousMonthBudgetDialog() {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setTitle(getResources()
				.getString(R.string.copy_previous_month_budget_dialog_title));
		alertDialogBuilder.setMessage(getResources()
				.getString(R.string.copy_previous_month_budget_dialog_message));
		alertDialogBuilder.setIcon(android.R.drawable.ic_dialog_info);
		alertDialogBuilder.setPositiveButton(R.string.DIALOG_DECIDE2, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(final DialogInterface dialog, final int which) {

				overwriteByPreviousBudget();

				//予算リストを更新
				updateBudgetList();

				//１か月の予算の表示更新
				//updateTotalBudgetPrice();

				KakeiboUtils.toastShow(getApplicationContext(), R.string.SETTING_COMPLETE_MESSAGE);
			}

		});
		alertDialogBuilder.setNegativeButton(R.string.DIALOG_CANCEL, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(final DialogInterface dialog, final int which) {
				// なにもしない
			}
		});

		alertDialogBuilder.create().show();
	}

	/** 先月と同じ予算で上書く処理 */
	private void overwriteByPreviousBudget() {
		Budget previousOneMonthBudget = budgetDao.findOneMonthBudgetListByYearMonth(getPreviousYearMonth());

		Budget oneMonthBudget = budgetDao.findOneMonthBudgetListByYearMonth(getYearMonth());
		if (oneMonthBudget != null) {
			budgetDao.deleteByCategoryIdAndYearMonth(oneMonthBudget.yearMonth,
																oneMonthBudget.categoryId);
		} else {
			oneMonthBudget = new Budget();
			oneMonthBudget.yearMonth = getYearMonth();
		}
		oneMonthBudget.budgetPrice = previousOneMonthBudget == null ? null
				: previousOneMonthBudget.budgetPrice;
		budgetDao.insert(oneMonthBudget);

		List<Budget> previousBudgetList = budgetDao.findCategoryBudgetListByYearMonth(getPreviousYearMonth(),
																						null);
		List<Budget> budgetList = budgetDao.findCategoryBudgetListByYearMonth(getYearMonth(), null);
		for (Budget previousBudget : previousBudgetList) {
			for (Budget budget : budgetList) {
				if (previousBudget.categoryId.equals(budget.categoryId)) {
					budget.budgetPrice = previousBudget.budgetPrice;
					budget.yearMonth = getYearMonth();
					budgetDao.deleteByCategoryIdAndYearMonth(budget.yearMonth, budget.categoryId);
					budgetDao.insert(budget);
				}
			}
		}

	}

	/** 予算の更新　*/
	private void updateBudget(final View editbudgetRecordView) {
		EditText editText = (EditText) editbudgetRecordView.findViewById(R.id.input_value);
		String budgetPriceStr = editText.getText().toString();
		Long budgetPrice = 0L;
		if (!TextUtils.isEmpty(budgetPriceStr)) {
			BigDecimal budgetPriceBigDecimal = new BigDecimal(budgetPriceStr)
					.setScale(2, BigDecimal.ROUND_HALF_UP);
			if (!KakeiboUtils.isJapan()) {
				budgetPriceBigDecimal = budgetPriceBigDecimal.multiply(new BigDecimal(100));
			}
			budgetPrice = budgetPriceBigDecimal.longValue();
		}
		if (editTargetBudget != null) {
			editTargetBudget.budgetPrice = budgetPrice;
			budgetDao.deleteByCategoryIdAndYearMonth(editTargetBudget.yearMonth,
																editTargetBudget.categoryId);
		} else {
			editTargetBudget = new Budget();
			editTargetBudget.budgetPrice = budgetPrice;
		}

		editTargetBudget.yearMonth = getYearMonth();
		budgetDao.insert(editTargetBudget);

	}

	/** 予算リストを更新 */
	private void updateBudgetList() {
		ListView listView = (ListView) findViewById(R.id.budget_list);

		budgetList = budgetDao.findCategoryBudgetListByYearMonth(getYearMonth(), null);

		List<BudgetDto> budgetDtoList = convertToBudgetDtoListFromBudgetList(budgetList);

		//Total行の設定
		Budget budget = budgetDao.findOneMonthBudgetListByYearMonth(getYearMonth());
		BudgetDto budgetTotal = new BudgetDto(budget);
		budgetTotal.isTotalRow = true;
		Long categoryTotalBudgetPrice = 0L;
		for (Budget budget2 : budgetList) {
			categoryTotalBudgetPrice += budget2.budgetPrice;
		}
		budgetTotal.categoryTotalBudgetPrice = categoryTotalBudgetPrice;

		budgetTotal.categoryName = getBudgetTotalTitle();
		budgetDtoList.add(0, budgetTotal);

		BudgetAdapter adapter = new BudgetAdapter(this, R.layout.manage_budget_row, budgetDtoList);
		adapter.previousBudgetList = budgetDao
				.findCategoryBudgetListByYearMonth(getPreviousYearMonth(), null);
		adapter.listener = new OnListViewButtonClickListener() {

			@Override
			public void onclick(final BudgetDto budgetDto, final Budget previousMonthBudget) {
				if (budgetDto.isTotalRow) {
					editTargetBudget = budgetDao.findOneMonthBudgetListByYearMonth(getYearMonth());
					Budget previousMonthTotalBudget = budgetDao
							.findOneMonthBudgetListByYearMonth(getPreviousYearMonth());
					getManageBudgetDialog(DIALOG_TYPE_TOTAL_BUDGET, previousMonthTotalBudget).show();
				} else {
					editTargetBudget = budgetDto;
					getManageBudgetDialog(DIALOG_TYPE_ONE_BUDGET, previousMonthBudget).show();
				}
			}
		};
		listView.setAdapter(adapter);

	}

	/** トータル行のタイトルを取得する */
	private String getBudgetTotalTitle() {
		String budgetTotalTitle;
		String budget_title_frag = getResources().getString(R.string.budget_title_frag);
		String year_format = getResources().getString(R.string.year_format);
		String month_format = getResources().getString(R.string.month_format);
		String day_format = getResources().getString(R.string.day_format);
		if (Locale.JAPAN.equals(Locale.getDefault())) {
			if (KakeiboUtils.isMonthStartDaySetting(getApplicationContext())) {
				Calendar tmpCal = Calendar.getInstance();
				tmpCal.setTime(nowDispCalendar.getTime());
				KakeiboUtils.changeToPreviousStartCalender(getApplicationContext(), tmpCal);
				String categoryNameFrom = (String) DateFormat.format(year_format + month_format + day_format,
																		tmpCal);
				tmpCal.add(Calendar.MONTH, +1);
				tmpCal.add(Calendar.DAY_OF_MONTH, -1);

				String categoryNameTo = (String) DateFormat.format(year_format + month_format + day_format,
																	tmpCal);
				budgetTotalTitle = categoryNameFrom + "～\n" + categoryNameTo + budget_title_frag;

			} else {
				budgetTotalTitle = (String) DateFormat.format(year_format + month_format, nowDispCalendar)
						+ budget_title_frag;
			}
		} else {
			if (KakeiboUtils.isMonthStartDaySetting(getApplicationContext())) {
				Calendar tmpCal = Calendar.getInstance();
				tmpCal.setTime(nowDispCalendar.getTime());
				KakeiboUtils.changeToPreviousStartCalender(getApplicationContext(), tmpCal);
				String categoryNameFrom = DateFormat.getMediumDateFormat(getApplicationContext())
						.format(tmpCal.getTime());
				tmpCal.add(Calendar.MONTH, +1);
				tmpCal.add(Calendar.DAY_OF_MONTH, -1);
				String categoryNameTo = DateFormat.getMediumDateFormat(getApplicationContext())
						.format(tmpCal.getTime());

				budgetTotalTitle = budget_title_frag;
				budgetTotalTitle += "\n";
				budgetTotalTitle += categoryNameFrom + " - " + categoryNameTo;
			} else {

				budgetTotalTitle = budget_title_frag
						+ "\n　　　　　"
						+ (String) DateFormat
								.format(" " + month_format + " ," + year_format, nowDispCalendar);
			}
		}
		return budgetTotalTitle;
	}

	//
	//	private View createHeader() {
	//
	//		View header = getLayoutInflater().inflate(R.layout.manage_budget_header, null);
	//
	//		//全体予算設定ボタン
	//		ImageButton settingTotalBudgetButton = (ImageButton) header.findViewById(R.id.setting_total_budget);
	//		settingTotalBudgetButton.setOnClickListener(new OnClickListener() {
	//			@Override
	//			public void onClick(final View v) {
	//				editTargetBudget = budgetDao.findOneMonthBudgetListByYearMonth(getYearMonth());
	//				Budget previousMonthBudget = budgetDao
	//						.findOneMonthBudgetListByYearMonth(getPreviousYearMonth());
	//				getManageBudgetDialog(DIALOG_TYPE_TOTAL_BUDGET, previousMonthBudget).show();
	//			}
	//		});
	//
	//		/** １か月の予算の表示更新 */
	//		Budget budget = budgetDao.findOneMonthBudgetListByYearMonth(getYearMonth());
	//		TextView totalBudgetPriceTextView = (TextView) header.findViewById(R.id.total_budget_price);
	//		String price = "";
	//		if (budget == null || budget.budgetPrice == null || budget.budgetPrice == 0) {
	//			price = KakeiboConsts.NO_SETTING_BUDGET;
	//		} else {
	//			price = KakeiboFormatUtils.formatPrice(budget.budgetPrice);
	//		}
	//		totalBudgetPriceTextView.setText(price);
	//
	//		/** カテゴリごとの合計予算の表示更新 */
	//		TextView categoryTotalBudgetPriceTextView = (TextView) header
	//				.findViewById(R.id.kakeibo_total_budget_price);
	//
	//		Integer categoryTotalBudgetPrice = 0;
	//		for (Budget budget2 : budgetList) {
	//			categoryTotalBudgetPrice += budget2.budgetPrice;
	//		}
	//		categoryTotalBudgetPriceTextView.setText(KakeiboFormatUtils.formatPrice(categoryTotalBudgetPrice));
	//
	//		return header;
	//	}

	/** 予算設定ダイアログの生成  */
	private AlertDialog getManageBudgetDialog(final int dialogType, final Budget previousMonthBudget) {
		AlertDialog.Builder editBudgetDialogBuilder = new AlertDialog.Builder(this);

		LayoutInflater inflater = LayoutInflater.from(this);
		final View editbudgetRecordView = inflater.inflate(R.layout.manage_budget_record, null);

		//予算金額テキストボックス
		final EditText editText = (EditText) editbudgetRecordView.findViewById(R.id.input_value);
		Long budgetPrice = null;
		if (editTargetBudget != null) {
			budgetPrice = editTargetBudget.budgetPrice;
		}
		if (budgetPrice == null || budgetPrice == 0) {
			editText.setText("");
		} else {
			String budgetPriceStr = KakeiboFormatUtils.formatPriceNoUnit(budgetPrice);
			editText.setText(budgetPriceStr.replace(",", ""));
		}

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
		TextView currencyUnitTextEnable = (TextView) editbudgetRecordView.findViewById(currnecyUnitEnableId);
		String currencyUnit = KakeiboUtils.getCurrencyUnit(getApplicationContext());
		currencyUnitTextEnable.setText(currencyUnit);

		TextView currencyUnitTextDisable = (TextView) editbudgetRecordView
				.findViewById(currnecyUnitDisableId);
		currencyUnitTextDisable.setVisibility(View.GONE);

		//先月の予算テキスト
		TextView previousMonthBudgetPriceTextView = (TextView) editbudgetRecordView
				.findViewById(R.id.previous_month_budget_price);
		Long previouBudgetPrice = null;
		if (previousMonthBudget != null) {
			previouBudgetPrice = previousMonthBudget.budgetPrice;
		}
		if (previouBudgetPrice == null || previouBudgetPrice == 0) {
			previousMonthBudgetPriceTextView.setText(R.string.NO_SETTING_BUDGET);
		} else {
			previousMonthBudgetPriceTextView.setText(KakeiboFormatUtils.formatPrice(previouBudgetPrice,
																					getApplicationContext()));
		}

		//先月の予算を設定ボタンの設定
		Button copyButton = (Button) editbudgetRecordView.findViewById(R.id.copy_previous_budget_price);
		copyButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				Long previouBudgetPrice = null;
				if (previousMonthBudget != null) {
					previouBudgetPrice = previousMonthBudget.budgetPrice;
				}
				if (previouBudgetPrice == null || previouBudgetPrice == 0) {
					editText.setText("");
				} else {
					String budgetPriceStr = KakeiboFormatUtils.formatPriceNoUnit(previouBudgetPrice);
					editText.setText(budgetPriceStr.replace(",", ""));
				}

			}
		});

		if (dialogType == DIALOG_TYPE_TOTAL_BUDGET) {
			//カテゴリごとの合計予算
			TextView categoryTotalBudgetPriceTextView = (TextView) editbudgetRecordView
					.findViewById(R.id.kakeibo_total_budget_price);
			Long categoryTotalBudgetPriceTmp = 0L;
			for (Budget budget : budgetList) {
				categoryTotalBudgetPriceTmp += budget.budgetPrice;
			}
			final Long categoryTotalBudgetPrice = categoryTotalBudgetPriceTmp;
			categoryTotalBudgetPriceTextView.setText(KakeiboFormatUtils.formatPrice(categoryTotalBudgetPrice,
																					getApplicationContext()));

			//カテゴリごとの合計予算の設定ボタンの設定

			Button copyCategoryTotalBudgetPriceButton = (Button) editbudgetRecordView
					.findViewById(R.id.copy_kakeibo_total_budget_price);
			copyCategoryTotalBudgetPriceButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					if (categoryTotalBudgetPrice == null || categoryTotalBudgetPrice == 0) {
						editText.setText("");
					} else {
						editText.setText(categoryTotalBudgetPrice.toString());
					}
				}
			});

		} else {
			View oneMonthBudgetSettingArea1 = editbudgetRecordView
					.findViewById(R.id.one_month_budget_setting_area_1);
			oneMonthBudgetSettingArea1.setVisibility(View.GONE);
			View oneMonthBudgetSettingArea2 = editbudgetRecordView
					.findViewById(R.id.one_month_budget_setting_area_2);
			if (oneMonthBudgetSettingArea2 != null) {//横向きの時はnullなので
				oneMonthBudgetSettingArea2.setVisibility(View.GONE);
			}
		}

		editBudgetDialogBuilder.setPositiveButton(R.string.DIALOG_DECIDE,
													new DialogInterface.OnClickListener() {
														@Override
														public void onClick(final DialogInterface dialog,
																final int which) {

															EditText editText = (EditText) editbudgetRecordView
																	.findViewById(R.id.input_value);
															String budgetPrice = editText.getText()
																	.toString();
															if (TextUtils.isEmpty(budgetPrice)) {
																budgetPrice = "0";
															}
															try {
																budgetPriceFormat.parse(budgetPrice);
															} catch (ParseException e) {
																editText.setText("");
																return;
															}

															BigDecimal budgetPriceBigDecimal = new BigDecimal(
																	budgetPrice);
															if (budgetPriceBigDecimal
																	.compareTo(new BigDecimal(
																			KakeiboConsts.PRICE_LIMIT)) > 0) {
																KakeiboUtils
																		.toastShow(
																					getApplicationContext(),
																					R.string.ERRORS_MAX_PRICE_OVER);
																return;
															}

															//更新
															updateBudget(editbudgetRecordView);

															KakeiboUtils
																	.toastShow(
																				getApplicationContext(),
																				R.string.SETTING_COMPLETE_MESSAGE);

															if (dialogType == DIALOG_TYPE_ONE_BUDGET) {
																//予算リストの更新
																updateBudgetList();
															} else if (dialogType == DIALOG_TYPE_TOTAL_BUDGET) {
																//一か月予算表示の更新
																//updateTotalBudgetPrice();
																updateBudgetList();
															}
														}

													});
		editBudgetDialogBuilder.setNegativeButton(R.string.DIALOG_CANCEL,
													new DialogInterface.OnClickListener() {
														@Override
														public void onClick(final DialogInterface dialog,
																final int which) {
															// なにもしない
														}

													});
		//ダイアログの生成
		editBudgetDialogBuilder.setView(editbudgetRecordView);
		final AlertDialog editBudgetDialog = editBudgetDialogBuilder.create();

		//ダイアログのタイトル設定
		String title;
		if (dialogType == DIALOG_TYPE_TOTAL_BUDGET) {
			title = getBudgetTotalTitle();
		} else {
			if (KakeiboUtils.isJapan()) {
				title = editTargetBudget.categoryName + "の予算設定";
			} else {
				title = "Budget of " + editTargetBudget.categoryName;
			}
		}

		editBudgetDialog.setTitle(title);
		return editBudgetDialog;

	}

	/** 表示対象１か月前の年月のyearMonthを取得 */
	private Integer getPreviousYearMonth() {
		int year;
		int month;
		if (KakeiboUtils.isMonthStartDaySetting(getApplicationContext())) {
			Calendar tempCal = Calendar.getInstance();
			tempCal.setTime(nowDispCalendar.getTime());
			KakeiboUtils.changeToPreviousStartCalender(getApplicationContext(), tempCal);
			year = tempCal.get(Calendar.YEAR);
			month = tempCal.get(Calendar.MONTH) + 1;
		} else {
			Calendar previousMonth = Calendar.getInstance();
			previousMonth.setTime(nowDispCalendar.getTime());
			previousMonth.add(Calendar.MONTH, -1);
			year = previousMonth.get(Calendar.YEAR);
			month = previousMonth.get(Calendar.MONTH) + 1;
		}
		return year * 100 + month;

	}

	/** 表示対象年月のyearMonthを取得 */
	private Integer getYearMonth() {

		int year;
		int month;
		if (KakeiboUtils.isMonthStartDaySetting(getApplicationContext())) {
			Calendar tempCal = Calendar.getInstance();
			tempCal.setTime(nowDispCalendar.getTime());
			KakeiboUtils.changeToPreviousStartCalender(getApplicationContext(), tempCal);
			tempCal.add(Calendar.MONTH, 1);
			year = tempCal.get(Calendar.YEAR);
			month = tempCal.get(Calendar.MONTH) + 1;
		} else {
			year = nowDispCalendar.get(Calendar.YEAR);
			month = nowDispCalendar.get(Calendar.MONTH) + 1;
		}
		return year * 100 + month;
	}

	private List<BudgetDto> convertToBudgetDtoListFromBudgetList(final List<Budget> budgetList) {
		List<BudgetDto> budgetDtoList = new ArrayList<BudgetDto>();
		for (Budget budget : budgetList) {
			BudgetDto budgetDto = new BudgetDto(budget);
			budgetDtoList.add(budgetDto);
		}
		return budgetDtoList;
	}

	public class BudgetDto extends Budget {

		public Long categoryTotalBudgetPrice;

		public boolean isTotalRow;

		public BudgetDto(final Budget budget) {
			if (budget != null) {
				this.budgetPrice = budget.budgetPrice;
				this.categoryId = budget.categoryId;
				this.categoryName = budget.categoryName;
				this.dispFlg = budget.dispFlg;
				this.id = budget.id;
				this.incomeFlg = budget.incomeFlg;
				this.yearMonth = budget.yearMonth;
			}
		}

	}

}
