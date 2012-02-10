package com.marronst.moneycalc.adapter;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.marronst.moneycalc.R;
import com.marronst.moneycalc.activity.ViewKakeiboListActivity.HouseKeepingBookDto;
import com.marronst.moneycalc.consts.KakeiboConsts.KakeiboListViewType;
import com.marronst.moneycalc.entity.Category;
import com.marronst.moneycalc.utils.KakeiboFormatUtils;

public class HrAdapter extends ArrayAdapter<HouseKeepingBookDto> {

	@SuppressWarnings("unused")
	private static final String TAG = HrAdapter.class.getSimpleName();

	private final LayoutInflater inflater;
	private final int expenseColor;
	private final int incomeColor;
	private final Context context;

	public HrAdapter(final Context context, final int textViewResourceId,
			final List<HouseKeepingBookDto> items2) {
		super(context, textViewResourceId, items2);
		this.inflater = LayoutInflater.from(context);
		expenseColor = context.getResources().getColor(R.color.expense_color);
		incomeColor = context.getResources().getColor(R.color.income_color);
		this.context = context;
	}

	@Override
	public View getView(final int position, final View convertView, final ViewGroup parent) {
		// ビューを受け取る
		View view = convertView;
		if (view == null) {
			view = this.inflater.inflate(R.layout.view_kakeibo_list_row, null);
		}
		// 表示すべきデータの取得
		HouseKeepingBookDto item = getItem(position);

		if (item.isTotalRow) {
			view.setBackgroundResource(R.color.WHITE);
		}

		if (item != null) {
			view.setClickable(false);

			if (item.isTotalRow || Category.INCOME_FLG_OFF == item.incomeFlg) {
				TextView expensePrice = (TextView) view.findViewById(R.id.expense_price);
				expensePrice.setText(KakeiboFormatUtils.formatPrice(item.expensePrice, context));
				expensePrice.setTextColor(expenseColor);
			}
			if (item.isTotalRow || Category.INCOME_FLG_ON == item.incomeFlg) {
				TextView incomePrice = (TextView) view.findViewById(R.id.income_price);
				incomePrice.setText(KakeiboFormatUtils.formatPrice(item.incomePrice, context));
				incomePrice.setTextColor(incomeColor);
			}

			if (!item.isTotalRow) {
				if (Category.INCOME_FLG_ON == item.incomeFlg) {
					view.findViewById(R.id.income_area).setVisibility(View.VISIBLE);
					view.findViewById(R.id.expense_area).setVisibility(View.GONE);
				} else {
					view.findViewById(R.id.income_area).setVisibility(View.GONE);
					view.findViewById(R.id.expense_area).setVisibility(View.VISIBLE);
				}

				view.findViewById(R.id.memo_register_date_area).setVisibility(View.VISIBLE);

			} else {
				if (item.isDispIncomePrice) {
					view.findViewById(R.id.income_area).setVisibility(View.VISIBLE);
				} else {
					view.findViewById(R.id.income_area).setVisibility(View.GONE);
				}
				view.findViewById(R.id.expense_area).setVisibility(View.VISIBLE);

				if (KakeiboListViewType.WEEK.equals(item.iKakeiboListViewType)) {
					view.findViewById(R.id.memo_register_date_area).setVisibility(View.GONE);
				} else {
					view.findViewById(R.id.memo_register_date_area).setVisibility(View.VISIBLE);
				}
			}

			TextView rowTitle = (TextView) view.findViewById(R.id.row_title);
			rowTitle.setText(item.title);

			TextView memo = (TextView) view.findViewById(R.id.memo);
			memo.setText(item.memo);

			TextView registerDate = (TextView) view.findViewById(R.id.registerDate);
			registerDate.setText(KakeiboFormatUtils.formatDateTimeToString(context, item.registerDate));

		}
		return view;
	}
}