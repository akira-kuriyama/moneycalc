package com.marronst.moneycalc.helper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.util.Log;

import com.marronst.moneycalc.consts.KakeiboConsts;
import com.marronst.moneycalc.consts.KakeiboConsts.KakeiboListViewType;
import com.marronst.moneycalc.db.BudgetDao;
import com.marronst.moneycalc.db.HouseKeepingBookDao;
import com.marronst.moneycalc.dto.CategoryBudgetDto;
import com.marronst.moneycalc.dto.KakeiboTotalDto;
import com.marronst.moneycalc.dxo.HouseKeepingBookDxo;
import com.marronst.moneycalc.entity.Budget;
import com.marronst.moneycalc.entity.Category;
import com.marronst.moneycalc.entity.HouseKeepingBook;
import com.marronst.moneycalc.entity.HouseKeepingBook.IdxHouseKeepingBook;
import com.marronst.moneycalc.utils.KakeiboUtils;

public class CalcTotalHelper {

	private HouseKeepingBookDao houseKeepingBookDao;
	private BudgetDao budgetDao;

	private final HouseKeepingBookDxo houseKeepingBookDxo = new HouseKeepingBookDxo();

	/**　表示対象日付 */
	private Calendar nowDispCalendar;

	/** 表示タイプ */
	KakeiboListViewType kakeiboListViewType;

	/** 表示カテゴリーIdリスト */
	private List<Integer> targetViewCatgoryIdList;

	private final Calendar baseCalForGetCarryOverPrice = Calendar.getInstance();
	private final Calendar baseCalForGetPreviousYearMonth = Calendar.getInstance();

	private final Calendar nowDispCalendarForGetYearMonth = Calendar.getInstance();

	public void setDb(final HouseKeepingBookDao houseKeepingBookDao, final BudgetDao budgetDao) {
		this.houseKeepingBookDao = houseKeepingBookDao;
		this.budgetDao = budgetDao;
	}

	/** DBから条件に合うHouseKeepingBookリストを取得する */
	private List<HouseKeepingBook> getItemsForTotalAndPieGraph(final Activity activity) {
		return getItems(activity, false);
	}

	/** DBから条件に合うHouseKeepingBookリストを取得する */
	private List<HouseKeepingBook> getItemsForBarGraph(final Activity activity) {
		return getItems(activity, true);
	}

	/** DBから条件に合うHouseKeepingBookリストを取得する */
	private List<HouseKeepingBook> getItems(final Activity activity, final boolean isGetRegisterDate) {

		List<HouseKeepingBook> items = new ArrayList<HouseKeepingBook>();
		Cursor c = houseKeepingBookDao.findByMonth(nowDispCalendar, kakeiboListViewType,
													targetViewCatgoryIdList);
		//activity.startManagingCursor(c);
		Log.i("getItems", "start");
		while (c.moveToNext()) {
			HouseKeepingBook hkb = houseKeepingBookDxo.createFromCursor(c, isGetRegisterDate);
			int lastIndex = IdxHouseKeepingBook.lastIndex();
			hkb.title = c.getString(++lastIndex);
			hkb.incomeFlg = c.getInt(++lastIndex);
			items.add(hkb);
		}
		Log.i("getItems", "end");
		c.close();
		return items;
	}

	/** グラフ用にkakeiboTotalDtoListを加工する */
	public LinkedHashMap<String, Long> convertGraphMap(final List<KakeiboTotalDto> kakeiboTotalDtoList) {
		LinkedHashMap<String, Long> map = new LinkedHashMap<String, Long>();
		for (KakeiboTotalDto dto : kakeiboTotalDtoList) {
			if (!dto.isTotalRow && Category.INCOME_FLG_OFF == dto.incomeFlg) {
				map.put(dto.title, Math.abs(dto.expensePrice));
			}
		}

		return map;
	}

	/** 縦棒グラフ用のMapを取得する */
	public LinkedHashMap<String, Long> getBarGraphMap(final Activity activity,
			final Calendar nowDispCalendar, final KakeiboListViewType kakeiboListViewType,
			final List<Integer> targetViewCatgoryIdList) {
		this.nowDispCalendar = nowDispCalendar;
		this.kakeiboListViewType = kakeiboListViewType;
		this.targetViewCatgoryIdList = targetViewCatgoryIdList;

		List<HouseKeepingBook> items = getItemsForBarGraph(activity);

		LinkedHashMap<String, Long> barGraphMap = new LinkedHashMap<String, Long>();
		ArrayList<Calendar> xAxisList = getXAxisList();
		for (Calendar startCal : xAxisList) {
			Calendar endCal = Calendar.getInstance();
			endCal.setTime(startCal.getTime());
			forwardCalendar(endCal);

			final long startTime = startCal.getTime().getTime();
			final long endTime = endCal.getTime().getTime();

			Long totalPrice = 0L;
			for (HouseKeepingBook houseKeepingBook : items) {
				if (Category.INCOME_FLG_ON == houseKeepingBook.incomeFlg) {
					continue;
				}

				if (startTime <= houseKeepingBook.registerDate.getTime()
						&& endTime > houseKeepingBook.registerDate.getTime()) {
					totalPrice += houseKeepingBook.price;
				}
			}
			barGraphMap.put(getBarGraphXAxisTitle(startCal), totalPrice);
		}

		return barGraphMap;
	}

	private String getBarGraphXAxisTitle(final Calendar startCal) {
		String barGraphXAxisTitle = KakeiboConsts.EMPTY;
		boolean isJapan = KakeiboUtils.isJapan();
		String format = "";
		switch (kakeiboListViewType) {
			case YEAR:
				if (isJapan) {
					format = "M月";
				} else {
					format = "MMM";
				}
				break;
			case HALF_YEAR:
				if (isJapan) {
					format = "M月";
				} else {
					format = "MMM";
				}
				break;
			case THREE_MONTH:
				if (isJapan) {
					format = "M月";
				} else {
					format = "MMM";
				}
				break;
			case MONTH:
				format = "d";
				break;
			case WEEK:
				format = "M/d";
				break;
			case DAY:
				format = "k";
				break;
			default:
				break;
		}
		barGraphXAxisTitle = (String) DateFormat.format(format, startCal);
		return barGraphXAxisTitle;
	}

	private ArrayList<Calendar> getXAxisList() {

		ArrayList<Calendar> xAxisList = new ArrayList<Calendar>();
		Calendar startCal = Calendar.getInstance();
		startCal.setTime(nowDispCalendar.getTime());
		Calendar endCal = getEndCal(startCal);
		while (startCal.before(endCal)) {
			xAxisList.add((Calendar) startCal.clone());
			forwardCalendar(startCal);
		}
		return xAxisList;
	}

	private void forwardCalendar(final Calendar startCal) {
		switch (kakeiboListViewType) {
			case YEAR:
				startCal.add(Calendar.MONTH, 1);
				break;
			case HALF_YEAR:
				startCal.add(Calendar.MONTH, 1);
				break;
			case THREE_MONTH:
				startCal.add(Calendar.MONTH, 1);
				break;
			case MONTH:
				startCal.add(Calendar.DAY_OF_MONTH, 1);
				break;
			case WEEK:
				startCal.add(Calendar.DAY_OF_MONTH, 1);
				break;
			case DAY:
				startCal.add(Calendar.HOUR, 1);
				break;
			default:
				break;
		}

	}

	private Calendar getEndCal(final Calendar startCal) {
		Calendar endCal = Calendar.getInstance();
		endCal.setTime(startCal.getTime());
		switch (kakeiboListViewType) {
			case YEAR:
				endCal.add(Calendar.YEAR, 1);
				break;
			case HALF_YEAR:
				endCal.add(Calendar.MONTH, 6);
				break;
			case THREE_MONTH:
				endCal.add(Calendar.MONTH, 3);
				break;
			case MONTH:
				endCal.add(Calendar.MONTH, 1);
				break;
			case WEEK:
				endCal.add(Calendar.WEEK_OF_MONTH, 1);
				break;
			case DAY:
				endCal.add(Calendar.DAY_OF_MONTH, 1);
				break;
			default:
				break;
		}
		return endCal;
	}

	/** 合計画面、および円グラフ用のKakeiboTotalDtoListを取得する */
	public List<KakeiboTotalDto> getKakeiboTotalDtoList(final Activity activity,
			final Calendar nowDispCalendar, final KakeiboListViewType kakeiboListViewType,
			final List<Integer> targetViewCatgoryIdList) {

		this.nowDispCalendar = nowDispCalendar;
		this.kakeiboListViewType = kakeiboListViewType;
		this.targetViewCatgoryIdList = targetViewCatgoryIdList;
		final Context context = activity.getApplicationContext();
		boolean isPortrait = KakeiboUtils.isPortrait(context);
		DisplayMetrics metrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
		int widthPixels = metrics.widthPixels;

		//DBから条件に合うHouseKeepingBookリストを取得する
		List<HouseKeepingBook> items = getItemsForTotalAndPieGraph(activity);

		// カテゴリごとに、合計金額を集計
		LinkedHashMap<CategoryBudgetDto, Long> totalPriceEachCategoryMap = calcEachCategoryTotal(items,
																									context);
		List<KakeiboTotalDto> kakeiboTotalDtoList = new ArrayList<KakeiboTotalDto>();

		Long expensePrice = 0L;
		Long incomePrice = 0L;

		final float scaledDensity = metrics.scaledDensity;

		boolean isUseCategoryCarryover = KakeiboUtils.isUseCategoryCarryover(context);
		int balanceCalcMethod = KakeiboUtils.getBalanceCalcMethod(context);

		boolean isDispIncomePrice = false;

		// カテゴリごとの処理(値のセット) ---------------------------------------------
		for (Entry<CategoryBudgetDto, Long> entry : totalPriceEachCategoryMap.entrySet()) {
			KakeiboTotalDto categoryRowDto = new KakeiboTotalDto();
			CategoryBudgetDto categoryBudgetDto = entry.getKey();

			if (categoryBudgetDto.dispFlg == Category.DISP_FLG_OFF && categoryRowDto.expensePrice == 0) {
				//非表示カテゴリで、合計金額が0であれば、リストに表示させない。
				continue;
			}

			categoryRowDto.categoryId = categoryBudgetDto.categoryId;
			categoryRowDto.title = categoryBudgetDto.categoryName;
			categoryRowDto.titleTextSize = 16;
			categoryRowDto.incomeFlg = categoryBudgetDto.incomeFlg;
			categoryRowDto.isSetttingFilter = KakeiboUtils.isNotEmpty(targetViewCatgoryIdList);
			if (Category.INCOME_FLG_ON == categoryRowDto.incomeFlg) {
				//収入カテゴリの場合----------
				isDispIncomePrice = true;
				//収入金額の設定
				categoryRowDto.incomePrice = entry.getValue();
				//合計に加算
				incomePrice += categoryRowDto.incomePrice;
			} else {
				//支出カテゴリの場合----------
				categoryRowDto.expensePrice = entry.getValue();
				//合計に加算
				expensePrice += categoryRowDto.expensePrice;
				if (KakeiboListViewType.MONTH.equals(kakeiboListViewType)) {
					//予算金額の設定
					categoryRowDto.budgetPrice = categoryBudgetDto.budgetPrice;
					if (isDisplayCategoryCarryover(isUseCategoryCarryover, categoryRowDto.budgetPrice,
													balanceCalcMethod, targetViewCatgoryIdList)) {
						//先月繰越金額を取得し、設定
						List<Integer> categoryIdList = new ArrayList<Integer>();
						categoryIdList.add(categoryBudgetDto.categoryId);
						categoryRowDto.carryOverPrice = getCarryOverPrice(categoryIdList,
																			categoryRowDto.incomeFlg,
																			balanceCalcMethod, context, false);
					}
					if (isDisplayCategoryRemainingValuePrice(categoryRowDto.budgetPrice, balanceCalcMethod)) {
						//残高金額の設定
						categoryRowDto.remainingValuePrice = getCategoryRemainingValuePrice(categoryRowDto,
																							balanceCalcMethod);
						//残高メーターのピクセルを計算し、設定
						setRemainingValueMeterPixels(scaledDensity, categoryRowDto, balanceCalcMethod,
														isPortrait, widthPixels);
					}
				}
			}

			kakeiboTotalDtoList.add(categoryRowDto);
		}

		// トータル行の処理(値のセット) ---------------------------------------------
		KakeiboTotalDto totalRowDto = new KakeiboTotalDto();
		totalRowDto.title = TotalAndListViewHelper.getKakeiboTopRowTitle(this.nowDispCalendar,
																			kakeiboListViewType, context);
		totalRowDto.titleTextSize = 18;

		totalRowDto.expensePrice = expensePrice;
		totalRowDto.incomePrice = incomePrice;
		totalRowDto.incomeFlg = Category.INCOME_FLG_OFF;
		totalRowDto.isTotalRow = true;
		totalRowDto.isDispIncomePrice = isDispIncomePrice;

		if (KakeiboListViewType.MONTH.equals(kakeiboListViewType)) {
			boolean isUseCarryover = KakeiboUtils.isUseCarryover(context);

			if (KakeiboUtils.isEmpty(this.targetViewCatgoryIdList)) {
				//フィルターが指定されていない時

				Budget budget = budgetDao.findOneMonthBudgetListByYearMonth(getYearMonth(context));
				totalRowDto.budgetPrice = budget == null ? 0L : budget.budgetPrice;
				if (isDisplayCarryover(isUseCarryover, totalRowDto.budgetPrice, balanceCalcMethod)) {
					//先月繰越金額を取得
					totalRowDto.carryOverPrice = getCarryOverPrice(null, null, balanceCalcMethod, context,
																	true);
				}
				if (isDisplayRemainingValuePrice(totalRowDto.budgetPrice, balanceCalcMethod)) {
					totalRowDto.remainingValuePrice = getRemainingValuePrice(totalRowDto, balanceCalcMethod);
					setRemainingValueMeterPixels(scaledDensity, totalRowDto, balanceCalcMethod, isPortrait,
													widthPixels);
				}
			} else {
				//フィルターが指定されている時
				totalRowDto.isSetttingFilter = true;
				for (KakeiboTotalDto kakeiboTotalDto : kakeiboTotalDtoList) {

					totalRowDto.budgetPrice += kakeiboTotalDto.budgetPrice;
					//totalRowDto.carryOverPrice += kakeiboTotalDto.carryOverPrice;
				}
				//				if (balanceCalcMethod == KakeiboConsts.BALANCE_CALC_METHOD_INCOME_MINUS_EXPENSE) {
				//					totalRowDto.carryOverPrice = getCarryOverPrice(this.targetViewCatgoryIdList, null,
				//																	balanceCalcMethod, context, true);
				//
				//				}
				totalRowDto.remainingValuePrice = getRemainingValuePrice(totalRowDto, balanceCalcMethod);
				setRemainingValueMeterPixels(scaledDensity, totalRowDto, balanceCalcMethod, isPortrait,
												widthPixels);
			}
		}

		kakeiboTotalDtoList.add(0, totalRowDto);
		return kakeiboTotalDtoList;
	}

	/** カテゴリの残高金額を表示するかどうか */
	private boolean isDisplayCategoryRemainingValuePrice(final long budgetPrice, final int balanceCalcMethod) {

		boolean isDisplayRemainingValuePrice = false;

		switch (balanceCalcMethod) {
			case KakeiboConsts.BALANCE_CALC_METHOD_NONE:
				//ignore
				break;
			case KakeiboConsts.BALANCE_CALC_METHOD_INCOME_MINUS_EXPENSE:
				//ignore
				break;
			case KakeiboConsts.BALANCE_CALC_METHOD_BUDGET_MINUS_EXPENSE:
			case KakeiboConsts.BALANCE_CALC_METHOD_BUDGET_PLUS_INCOME_MINUS_EXPENSE:
				if (budgetPrice != 0L) {
					isDisplayRemainingValuePrice = true;
				}
				break;
			default:
				//ignore
				break;
		}
		return isDisplayRemainingValuePrice;
	}

	/** トータルの残高金額を表示するかどうか */
	private boolean isDisplayRemainingValuePrice(final long budgetPrice, final int balanceCalcMethod) {

		boolean isDisplayRemainingValuePrice = false;

		switch (balanceCalcMethod) {
			case KakeiboConsts.BALANCE_CALC_METHOD_NONE:
				//ignore
				break;
			case KakeiboConsts.BALANCE_CALC_METHOD_INCOME_MINUS_EXPENSE:
				isDisplayRemainingValuePrice = true;
				break;
			case KakeiboConsts.BALANCE_CALC_METHOD_BUDGET_MINUS_EXPENSE:
			case KakeiboConsts.BALANCE_CALC_METHOD_BUDGET_PLUS_INCOME_MINUS_EXPENSE:
				if (budgetPrice != 0L) {
					isDisplayRemainingValuePrice = true;
				}
				break;
			default:
				//ignore
				break;
		}
		return isDisplayRemainingValuePrice;
	}

	/** 残高メーターのピクセルを計算し、Dtoのフィールドにセットする */
	private void setRemainingValueMeterPixels(final float scaledDensity,
			final KakeiboTotalDto kakeiboTotalDto, final int balanceCalcMethod, final boolean isPortrait,
			final int widthPixels) {
		final BigDecimal meterTotalLength;
		if (isPortrait) {
			meterTotalLength = new BigDecimal(150);
		} else {
			if (widthPixels <= 480) {
				meterTotalLength = new BigDecimal(180);
			} else {
				meterTotalLength = new BigDecimal(250);
			}
		}
		long bunbo = 0L;

		if (KakeiboConsts.BALANCE_CALC_METHOD_INCOME_MINUS_EXPENSE == balanceCalcMethod) {
			bunbo = kakeiboTotalDto.incomePrice + kakeiboTotalDto.carryOverPrice;
		} else if (KakeiboConsts.BALANCE_CALC_METHOD_BUDGET_MINUS_EXPENSE == balanceCalcMethod) {
			bunbo = kakeiboTotalDto.budgetPrice + kakeiboTotalDto.carryOverPrice;
		} else if (KakeiboConsts.BALANCE_CALC_METHOD_BUDGET_PLUS_INCOME_MINUS_EXPENSE == balanceCalcMethod) {
			bunbo = kakeiboTotalDto.budgetPrice + kakeiboTotalDto.incomePrice
					+ kakeiboTotalDto.carryOverPrice;
		}

		int per = meterTotalLength.intValue();
		if (bunbo > 0) {
			if (kakeiboTotalDto.expensePrice == 0) {
				per = 0;
			} else if (kakeiboTotalDto.remainingValuePrice > 0) {
				//残高がゼロより上で、かつ分母がゼロより上(zero divideをさけるため)の場合
				per = meterTotalLength.multiply(new BigDecimal(kakeiboTotalDto.expensePrice))
						.divide(new BigDecimal(bunbo), 0, BigDecimal.ROUND_HALF_UP).intValue();
			}
		}
		kakeiboTotalDto.remainingValueMeterOffPixel = (int) (per * scaledDensity);
		kakeiboTotalDto.remainingValueMeterOnPixel = (int) (meterTotalLength.intValue() * scaledDensity)
				- kakeiboTotalDto.remainingValueMeterOffPixel;

	}

	/** トータルの残高を取得する */
	private long getRemainingValuePrice(final KakeiboTotalDto totalRowDto, final int balanceCalcMethod) {
		long remainingValuePrice = 0L;

		switch (balanceCalcMethod) {
			case KakeiboConsts.BALANCE_CALC_METHOD_NONE:
				//ignore
				break;
			case KakeiboConsts.BALANCE_CALC_METHOD_INCOME_MINUS_EXPENSE:
				remainingValuePrice = totalRowDto.incomePrice + totalRowDto.carryOverPrice
						- Math.abs(totalRowDto.expensePrice);
				break;
			case KakeiboConsts.BALANCE_CALC_METHOD_BUDGET_MINUS_EXPENSE:
				remainingValuePrice = totalRowDto.budgetPrice + totalRowDto.carryOverPrice
						- Math.abs(totalRowDto.expensePrice);
				break;
			case KakeiboConsts.BALANCE_CALC_METHOD_BUDGET_PLUS_INCOME_MINUS_EXPENSE:
				remainingValuePrice = totalRowDto.budgetPrice + totalRowDto.incomePrice
						+ totalRowDto.carryOverPrice - Math.abs(totalRowDto.expensePrice);
				break;
			default:
				//ignore
				break;
		}

		return remainingValuePrice;
	}

	/** カテゴリの残高を取得する */
	private long getCategoryRemainingValuePrice(final KakeiboTotalDto categoryRowDto,
			final int balanceCalcMethod) {

		long categoryRemainingValuePrice = 0L;

		switch (balanceCalcMethod) {
			case KakeiboConsts.BALANCE_CALC_METHOD_NONE:
				//ignore
				break;
			case KakeiboConsts.BALANCE_CALC_METHOD_INCOME_MINUS_EXPENSE:
				//ignore
				break;
			case KakeiboConsts.BALANCE_CALC_METHOD_BUDGET_MINUS_EXPENSE:
			case KakeiboConsts.BALANCE_CALC_METHOD_BUDGET_PLUS_INCOME_MINUS_EXPENSE:
				categoryRemainingValuePrice = categoryRowDto.budgetPrice + categoryRowDto.carryOverPrice
						- Math.abs(categoryRowDto.expensePrice);
				break;
			default:
				//ignore
				break;
		}

		return categoryRemainingValuePrice;
	}

	/** トータルの残高繰越を表示するかどうか */
	private boolean isDisplayCarryover(final boolean isUseCarryover, final long budgetPrice,
			final int balanceCalcMethod) {

		if (!isUseCarryover) {
			return false;
		}
		boolean isDisplayCarryover = false;

		switch (balanceCalcMethod) {
			case KakeiboConsts.BALANCE_CALC_METHOD_NONE:
				isDisplayCarryover = false;
				break;
			case KakeiboConsts.BALANCE_CALC_METHOD_INCOME_MINUS_EXPENSE:
				isDisplayCarryover = true;
				break;
			case KakeiboConsts.BALANCE_CALC_METHOD_BUDGET_MINUS_EXPENSE:
			case KakeiboConsts.BALANCE_CALC_METHOD_BUDGET_PLUS_INCOME_MINUS_EXPENSE:
				if (budgetPrice != 0L) {
					isDisplayCarryover = true;
				}
				break;

			default:
				isDisplayCarryover = false;
				break;
		}
		return isDisplayCarryover;

	}

	/** カテゴリごとの残高繰越を表示するかどうか */
	private boolean isDisplayCategoryCarryover(final boolean isUseCategoryCarryover, final long budgetPrice,
			final int balanceCalcMethod, final List<Integer> categoryIdList) {
		return isUseCategoryCarryover
				&& budgetPrice != 0L
				&& (balanceCalcMethod == KakeiboConsts.BALANCE_CALC_METHOD_BUDGET_MINUS_EXPENSE || balanceCalcMethod == KakeiboConsts.BALANCE_CALC_METHOD_BUDGET_PLUS_INCOME_MINUS_EXPENSE)
				&& KakeiboUtils.isEmpty(this.targetViewCatgoryIdList);
	}

	/** 先月持越し金額を取得する */
	private Long getCarryOverPrice(final List<Integer> categoryIdList, Integer incomeFlg,
			final int balanceCalcMethod, final Context context, final boolean isTotal) {
		baseCalForGetCarryOverPrice.setTime(nowDispCalendar.getTime());

		Long carryOverPrice = 0L;

		if (balanceCalcMethod == KakeiboConsts.BALANCE_CALC_METHOD_NONE) {
			//残高表示なしの場合---
			return carryOverPrice;
		} else if (balanceCalcMethod == KakeiboConsts.BALANCE_CALC_METHOD_INCOME_MINUS_EXPENSE) {
			//残高=収入-支出 の場合---

			//ある表示対象の月からそれ以前のトータル金額を求めるために、第一引数にnullをセットする
			Integer totalPrice = houseKeepingBookDao.findPreviousMonthTotalPriceByMonth(null,
																						nowDispCalendar,
																						categoryIdList,
																						incomeFlg);
			carryOverPrice = new Long(-1 * totalPrice);
		} else if (balanceCalcMethod == KakeiboConsts.BALANCE_CALC_METHOD_BUDGET_MINUS_EXPENSE
				|| balanceCalcMethod == KakeiboConsts.BALANCE_CALC_METHOD_BUDGET_PLUS_INCOME_MINUS_EXPENSE) {
			//残高=予算-支出 の場合 or 残高=予算+収入-支出 の場合---

			if (balanceCalcMethod == KakeiboConsts.BALANCE_CALC_METHOD_BUDGET_MINUS_EXPENSE) {
				//残高=予算-支出 の場合は、支出しか取得しないようにする
				incomeFlg = Category.INCOME_FLG_OFF;
			}

			Integer previousYearMonth = getPreviousYearMonth(baseCalForGetCarryOverPrice, context);

			//			String formatDate = KakeiboFormatUtils.formatDate(nowDispCalendar.getTime(),
			//																DataFormatType.YYYYMMDDHHMMSS);

			List<Budget> budgetList = budgetDao.findAllMonthBudgetListByYearMonthBefore(previousYearMonth,
																						categoryIdList);
			Long allBudgetPrice = 0L;
			for (Budget budget : budgetList) {
				if (!previousYearMonth.equals(budget.yearMonth) || budget.budgetPrice == 0L) {
					break;
				}
				allBudgetPrice += budget.budgetPrice;

				baseCalForGetCarryOverPrice.add(Calendar.MONTH, -1);
				previousYearMonth = getPreviousYearMonth(baseCalForGetCarryOverPrice, context);
			}
			if (allBudgetPrice <= 0) {
				return 0L;
			}
			Integer totalPrice = houseKeepingBookDao
					.findPreviousMonthTotalPriceByMonth(baseCalForGetCarryOverPrice, nowDispCalendar,
														categoryIdList, incomeFlg);
			carryOverPrice = allBudgetPrice - totalPrice;
		}
		return carryOverPrice;

	}

	/** HouseKeepingBookリストをMapへ加工する */
	private LinkedHashMap<CategoryBudgetDto, Long> calcEachCategoryTotal(final List<HouseKeepingBook> items,
			final Context context) {

		LinkedHashMap<CategoryBudgetDto, Long> kakeiboTotalMap = new LinkedHashMap<CategoryBudgetDto, Long>();

		Integer yearMonth = getYearMonth(context);
		List<Budget> budgetList = budgetDao.findCategoryBudgetListByYearMonth(yearMonth,
																				targetViewCatgoryIdList);
		for (Budget budget : budgetList) {
			CategoryBudgetDto dto = new CategoryBudgetDto();
			dto.categoryId = budget.categoryId;
			dto.budgetPrice = budget.budgetPrice;
			dto.categoryName = budget.categoryName;
			dto.dispFlg = budget.dispFlg;
			dto.incomeFlg = budget.incomeFlg;
			kakeiboTotalMap.put(dto, 0L);
		}

		for (Map.Entry<CategoryBudgetDto, Long> entry : kakeiboTotalMap.entrySet()) {
			final CategoryBudgetDto key = entry.getKey();
			for (HouseKeepingBook houseKeepingBook : items) {
				if (key.categoryId.equals(houseKeepingBook.categoryId)) {
					entry.setValue(entry.getValue() + houseKeepingBook.price);
				}
			}
		}

		return kakeiboTotalMap;
	}

	/** 表示対象１か月前の年月のyearMonthを取得 */
	private Integer getPreviousYearMonth(final Calendar cal, final Context context) {
		baseCalForGetPreviousYearMonth.setTime(cal.getTime());
		if (!KakeiboUtils.isMonthStartDaySetting(context)) {
			baseCalForGetPreviousYearMonth.add(Calendar.MONTH, -1);
		}

		int pYear = baseCalForGetPreviousYearMonth.get(Calendar.YEAR);
		int pMonth = baseCalForGetPreviousYearMonth.get(Calendar.MONTH) + 1;
		return pYear * 100 + pMonth;
	}

	/** 表示対象年月のyearMonthを取得 */
	private Integer getYearMonth(final Context context) {
		int year;
		int month;
		if (KakeiboUtils.isMonthStartDaySetting(context)) {
			nowDispCalendarForGetYearMonth.setTime(nowDispCalendar.getTime());
			nowDispCalendarForGetYearMonth.add(Calendar.MONTH, 1);
			year = nowDispCalendarForGetYearMonth.get(Calendar.YEAR);
			month = nowDispCalendarForGetYearMonth.get(Calendar.MONTH) + 1;
		} else {
			year = nowDispCalendar.get(Calendar.YEAR);
			month = nowDispCalendar.get(Calendar.MONTH) + 1;
		}
		return year * 100 + month;
	}
}
