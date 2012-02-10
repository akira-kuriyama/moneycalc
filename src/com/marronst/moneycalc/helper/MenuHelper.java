package com.marronst.moneycalc.helper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.view.Menu;
import android.view.MenuItem;

import com.marronst.moneycalc.R;
import com.marronst.moneycalc.activity.BrSelectActionActivity;
import com.marronst.moneycalc.activity.EditCategoryActivity;
import com.marronst.moneycalc.activity.EiExportActivity;
import com.marronst.moneycalc.activity.KakeiboPreferenceActivity;
import com.marronst.moneycalc.activity.KakeiboTotalActivity;
import com.marronst.moneycalc.activity.ManageBudgetActivity;
import com.marronst.moneycalc.activity.ManageFilterActivity;
import com.marronst.moneycalc.activity.RegisterRecordActivity;
import com.marronst.moneycalc.activity.ViewGraphActivity;
import com.marronst.moneycalc.activity.ViewKakeiboListActivity;
import com.marronst.moneycalc.consts.KakeiboConsts;
import com.marronst.moneycalc.consts.KakeiboConsts.GraphType;
import com.marronst.moneycalc.consts.KakeiboConsts.KakeiboListViewType;
import com.marronst.moneycalc.entity.Filter;
import com.marronst.moneycalc.utils.KakeiboUtils;

public class MenuHelper {

	/** メニューの設定 */
	public static void createOptionsMenu(final Activity activity, final Menu menu,
			final MenuType... menuTypes) {

		MenuType[] values = MenuType.values();
		for (int i = 0; i < values.length; i++) {
			for (MenuType menuTypeTmp : menuTypes) {
				if (values[i].equals(menuTypeTmp)) {
					MenuItem menuItem = menu.add(0, menuTypeTmp.getId(), i, menuTypeTmp.getTitleRes());
					menuItem.setIcon(menuTypeTmp.getIconRes());
				}
			}
		}

	}

	/** メニュー項目の選択時 */
	public static boolean optionsItemSelected(final Activity activity, final MenuItem item,
			final MenuParam menuParam) {
		MenuType menuType = MenuType.getMenuType(item.getItemId());
		Intent intent;

		final Resources resources = activity.getResources();
		switch (menuType) {

			case VIEW_PIE_GRAPH://円グラフ
				intent = getPieGraphIntent(activity, menuParam);
				activity.startActivity(intent);
				return true;

			case VIEW_BAR_GRAPH://棒グラフ
				intent = getBarGraphIntent(activity, menuParam);
				activity.startActivity(intent);
				return true;

			case CHANGE_INTERVAL://表示期間変更

				final List<KakeiboListViewType> list = new ArrayList<KakeiboListViewType>();
				KakeiboListViewType[] values = KakeiboListViewType.values();
				for (KakeiboListViewType kakeiboListViewType : values) {
					list.add(kakeiboListViewType);
				}

				String[] listNames = new String[list.size()];

				int index = 0;
				for (int i = 0; i < list.size(); i++) {
					KakeiboListViewType kakeiboListViewType = list.get(i);
					listNames[i] = resources.getString(kakeiboListViewType.getNameRes());
					if (kakeiboListViewType.equals(menuParam.kakeiboListViewType)) {
						index = i;
					}
				}
				AlertDialog.Builder builder = new AlertDialog.Builder(activity);
				builder.setTitle(R.string.DIALOG_TITLE_CHANGE_INTERVAL);
				builder.setSingleChoiceItems(listNames, index, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(final DialogInterface dialog, final int which) {
						Intent intent = null;
						KakeiboListViewType kakeiboListViewType = list.get(which);
						menuParam.kakeiboListViewType = kakeiboListViewType;
						Class<? extends Activity> viewingActivity = menuParam.viewingActivity;

						if (KakeiboTotalActivity.class.equals(viewingActivity)) {
							intent = getKakeiboTotalIntent(activity, menuParam);
						} else if (ViewKakeiboListActivity.class.equals(viewingActivity)) {
							intent = getKakeiboListIntent(activity, menuParam);
						} else if (ViewGraphActivity.class.equals(viewingActivity)) {
							if (GraphType.Bar.equals(menuParam.viewingGraphtype)) {
								intent = getBarGraphIntent(activity, menuParam);
							} else if (GraphType.Pie.equals(menuParam.viewingGraphtype)) {
								intent = getPieGraphIntent(activity, menuParam);
							}
						}

						activity.startActivity(intent);
						dialog.dismiss();
					}

				});
				builder.create().show();

				return true;

			case CHANGE_FILTER://フィルター変更

				FilterHelper filterHelper = new FilterHelper();
				List<Filter> filterList = filterHelper.getAllFilterList(activity.getApplicationContext());
				filterList.add(0, filterHelper.getFilterNone(activity.getApplicationContext()));

				String[] filterNameArray = new String[filterList.size()];
				final Integer[] filterIdArray = new Integer[filterList.size()];

				int cfIndex = 0;
				for (int i = 0; i < filterList.size(); i++) {
					Filter filter = filterList.get(i);
					filterIdArray[i] = filter.id;
					filterNameArray[i] = filter.filterName;
					if (filter.id.equals(menuParam.nowFilterId)) {
						cfIndex = i;
					}
				}

				AlertDialog.Builder cfBuilder = new AlertDialog.Builder(activity);
				cfBuilder.setTitle(R.string.DIALOG_TITLE_CHANGE_FILTER);
				cfBuilder.setSingleChoiceItems(filterNameArray, cfIndex,
												new DialogInterface.OnClickListener() {
													@Override
													public void onClick(final DialogInterface dialog,
															final int which) {
														Intent intent = null;
														Integer filterId = filterIdArray[which];
														menuParam.nowFilterId = filterId;
														Class<? extends Activity> viewingActivity = menuParam.viewingActivity;

														if (KakeiboTotalActivity.class
																.equals(viewingActivity)) {
															intent = getKakeiboTotalIntent(activity,
																							menuParam);
														} else if (ViewKakeiboListActivity.class
																.equals(viewingActivity)) {
															intent = getKakeiboListIntent(activity, menuParam);
														} else if (ViewGraphActivity.class
																.equals(viewingActivity)) {
															if (GraphType.Bar
																	.equals(menuParam.viewingGraphtype)) {
																intent = getBarGraphIntent(activity,
																							menuParam);
															} else if (GraphType.Pie
																	.equals(menuParam.viewingGraphtype)) {
																intent = getPieGraphIntent(activity,
																							menuParam);
															}
														}

														activity.startActivity(intent);
														dialog.dismiss();
													}
												});

				cfBuilder.create().show();

				return true;

			case PREFERENCE://設定
				String[] prefNameArray = new String[PreferenceMenuType.values().length];
				PreferenceMenuType[] preferenceMenuTypeArray = PreferenceMenuType.values();

				for (int i = 0; i < preferenceMenuTypeArray.length; i++) {
					prefNameArray[i] = resources.getString(preferenceMenuTypeArray[i].getTitleResId());
				}
				new AlertDialog.Builder(activity).setTitle(R.string.DIALOG_TITLE_CHOICE)
						.setItems(prefNameArray, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(final DialogInterface dialog, final int which) {
								PreferenceMenuType preferenceMenuType = getPreferenceMenuType(which);
								Intent intent;
								switch (preferenceMenuType) {
									case EDIT_CATEGORY:
										intent = new Intent(activity.getApplicationContext(),
												EditCategoryActivity.class);
										intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
										activity.startActivity(intent);
										break;
									case MANAGE_BUDGET:
										intent = new Intent(activity.getApplicationContext(),
												ManageBudgetActivity.class);
										intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
										activity.startActivity(intent);
										break;

									case EDIT_FILTER:
										intent = new Intent(activity.getApplicationContext(),
												ManageFilterActivity.class);
										intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
										activity.startActivity(intent);
										break;
									case TOTAL_PREFERENCE:
										intent = new Intent(activity.getApplicationContext(),
												KakeiboPreferenceActivity.class);
										intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
										activity.startActivity(intent);
										break;

									case DATA_EXPORT_AND_IMPORT:
										intent = new Intent(activity.getApplicationContext(),
										//												EiSelectActionActivity.class);//エクスポート、インポート選択画面が出来たら、コメントを外す
												EiExportActivity.class);
										intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
										activity.startActivity(intent);
										break;
									case DATA_BACKUP_AND_IMPORT:
										intent = new Intent(activity.getApplicationContext(),
												BrSelectActionActivity.class);
										intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
										activity.startActivity(intent);
										break;
									default:
										break;
								}
							}

							private PreferenceMenuType getPreferenceMenuType(final int which) {
								for (PreferenceMenuType preferenceMenuType : PreferenceMenuType.values()) {
									if (preferenceMenuType.ordinal() == which) {
										return preferenceMenuType;
									}
								}
								return null;
							}
						}).create().show();

				return true;

			case REGISTER:

				intent = new Intent(activity.getApplicationContext(), RegisterRecordActivity.class);
				StringBuilder registerDateStr = new StringBuilder();
				final String slash = "/";
				registerDateStr.append(menuParam.nowDispCalendar.get(Calendar.YEAR));
				registerDateStr.append(slash);
				registerDateStr.append(menuParam.nowDispCalendar.get(Calendar.MONTH));
				registerDateStr.append(slash);
				registerDateStr.append(menuParam.nowDispCalendar.get(Calendar.DAY_OF_MONTH));
				intent.putExtra(KakeiboConsts.INTENT_KEY_REGISTER_DATE, registerDateStr.toString());
				activity.startActivity(intent);

				return true;

			default:
		}
		return false;
	}

	/** 一覧画面へ飛ばすためのIntentを生成し、返す*/
	private static Intent getKakeiboListIntent(final Activity activity, final MenuParam menuParam) {
		final KakeiboListViewType viewType = menuParam.kakeiboListViewType;
		Intent i = new Intent(activity.getApplicationContext(), ViewKakeiboListActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		i.putExtra(KakeiboConsts.INTENT_KEY_START_DATE_VALUE, KakeiboUtils
				.getStartCal(menuParam.nowDispCalendar, viewType, activity));
		i.putExtra(KakeiboConsts.INTENT_KEY_LIST_VIEW_TYPE, viewType.name());
		i.putExtra(KakeiboConsts.INTENT_KEY_FILTER_ID, menuParam.nowFilterId);
		return i;
	}

	/** 合計画面へ飛ばすためのIntentを生成し、返す*/
	private static Intent getKakeiboTotalIntent(final Activity activity, final MenuParam menuParam) {
		final KakeiboListViewType viewType = menuParam.kakeiboListViewType;
		Intent i = new Intent(activity.getApplicationContext(), KakeiboTotalActivity.class);
		i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		i.putExtra(KakeiboConsts.INTENT_KEY_START_DATE_VALUE, KakeiboUtils
				.getStartCal(menuParam.nowDispCalendar, viewType, activity.getApplicationContext()));
		i.putExtra(KakeiboConsts.INTENT_KEY_LIST_VIEW_TYPE, viewType.name());
		i.putExtra(KakeiboConsts.INTENT_KEY_FILTER_ID, menuParam.nowFilterId);
		return i;
	}

	/** 円グラフ画面へ飛ばすためのIntentを生成し、返す*/
	private static Intent getPieGraphIntent(final Activity activity, final MenuParam menuParam) {
		final KakeiboListViewType viewType = menuParam.kakeiboListViewType;
		Intent intent = new Intent(activity.getApplicationContext(), ViewGraphActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra(KakeiboConsts.INTENT_KEY_START_DATE_VALUE, KakeiboUtils
				.getStartCal(menuParam.nowDispCalendar, viewType, activity));
		intent.putExtra(KakeiboConsts.INTENT_KEY_LIST_VIEW_TYPE, viewType.name());
		intent.putExtra(KakeiboConsts.INTENT_KEY_TARGET_VIEW_CATEGORY_ID, menuParam.categoryId);
		intent.putExtra(KakeiboConsts.INTENT_KEY_GRAPH_TYPE, GraphType.Pie);
		intent.putExtra(KakeiboConsts.INTENT_KEY_FILTER_ID, menuParam.nowFilterId);
		return intent;
	}

	/** 棒グラフ画面へ飛ばすためのIntentを生成し、返す*/
	private static Intent getBarGraphIntent(final Activity activity, final MenuParam menuParam) {
		final KakeiboListViewType viewType = menuParam.kakeiboListViewType;
		Intent intent = new Intent(activity.getApplicationContext(), ViewGraphActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		intent.putExtra(KakeiboConsts.INTENT_KEY_START_DATE_VALUE, KakeiboUtils
				.getStartCal(menuParam.nowDispCalendar, viewType, activity));
		intent.putExtra(KakeiboConsts.INTENT_KEY_LIST_VIEW_TYPE, viewType.name());
		intent.putExtra(KakeiboConsts.INTENT_KEY_TARGET_VIEW_CATEGORY_ID, menuParam.categoryId);
		intent.putExtra(KakeiboConsts.INTENT_KEY_CATEGORY_NAME, menuParam.categoryName);
		intent.putExtra(KakeiboConsts.INTENT_KEY_GRAPH_TYPE, GraphType.Bar);
		intent.putExtra(KakeiboConsts.INTENT_KEY_FILTER_ID, menuParam.nowFilterId);
		return intent;
	}

	public static class MenuParam {

		/**　表示対象日付 */
		public Calendar nowDispCalendar;

		/** 表示タイプ */
		public KakeiboListViewType kakeiboListViewType;

		/**  表示カテゴリ */
		public Integer categoryId;

		/** カテゴリ名*/
		public String categoryName;

		/** 表示しているグラフタイプ */
		public GraphType viewingGraphtype;

		/** 表示中のアクティビティ */
		public Class<? extends Activity> viewingActivity;

		/** 現在のフィルターId */
		public Integer nowFilterId = Filter.FILTER_NONE;

	}

	/** メニュー項目の選択時 */
	public static boolean optionsItemSelected(final Activity activity, final MenuItem item) {
		return optionsItemSelected(activity, item, null);
	}

	private enum PreferenceMenuType {
		MANAGE_BUDGET(R.string.menu_title_manage_budget), //
			EDIT_CATEGORY(R.string.menu_title_edit_category), //
			EDIT_FILTER(R.string.menu_title_edit_filter), //
			TOTAL_PREFERENCE(R.string.menu_title_total_preference), //
			DATA_EXPORT_AND_IMPORT(R.string.menu_title_data_export_and_import), //
			DATA_BACKUP_AND_IMPORT(R.string.menu_title_data_backup_and_restore), //
		;

		private final int mTitleResId;

		private PreferenceMenuType(final int titleResId) {
			this.mTitleResId = titleResId;
		}

		public int getTitleResId() {
			return mTitleResId;
		}
	}

	public enum MenuType {
		REGISTER(android.R.drawable.ic_menu_edit, R.string.MENU_REGISTER), //
			VIEW_BAR_GRAPH(R.drawable.ic_menu_chart, R.string.MENU_VIEW_BAR_GRAPH), //
			VIEW_PIE_GRAPH(R.drawable.ic_menu_pie_chart, R.string.MENU_VIEW_PIE_GRAPH), //
			CHANGE_INTERVAL(android.R.drawable.ic_menu_month, R.string.MENU_CHANGE_INTERVAL), //
			CHANGE_FILTER(R.drawable.ic_menu_filter, R.string.MENU_CHANGE_FILTER), //
			PREFERENCE(android.R.drawable.ic_menu_manage, R.string.MENU_PREFERENCE), //
		;
		private final int mIconRes;
		private final int mTitleRes;

		private MenuType(final int iconRes, final int titleRes) {
			this.mIconRes = iconRes;
			this.mTitleRes = titleRes;
		}

		public int getId() {
			return this.ordinal();
		}

		public int getIconRes() {
			return mIconRes;
		}

		public int getTitleRes() {
			return mTitleRes;
		}

		public static MenuType getMenuType(final int id) {
			for (MenuType menuType : MenuType.values()) {
				if (id == menuType.getId()) {
					return menuType;
				}
			}
			throw new AssertionError();
		}
	};

}
