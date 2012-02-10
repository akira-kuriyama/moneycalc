package com.marronst.moneycalc.adapter;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.marronst.moneycalc.R;
import com.marronst.moneycalc.entity.Filter;

public class FilterAdapter extends ArrayAdapter<Filter> {

	@SuppressWarnings("unused")
	private static final String TAG = FilterAdapter.class.getSimpleName();

	private final LayoutInflater inflater;
	private final String DEFAULT_MSG;

	public FilterAdapter(final Context context, final int textViewResourceId, final List<Filter> items2) {
		super(context, textViewResourceId, items2);
		this.inflater = LayoutInflater.from(context);
		DEFAULT_MSG = context.getResources().getString(R.string.filter_default);
	}

	@Override
	public View getView(final int position, final View convertView, final ViewGroup parent) {
		// ビューを設定する
		View view = this.inflater.inflate(R.layout.manage_filter_row, null);
		// 表示すべきデータの取得
		Filter filter = getItem(position);

		if (filter != null) {
			String filterName = filter.filterName;
			TextView filterNameTextView = (TextView) view.findViewById(R.id.manage_filter_row_filter_name);
			if (Filter.DEFAULT_FLG_ON.equals(filter.isDefault)) {
				filterName += " " + DEFAULT_MSG;
			}
			filterNameTextView.setText(filterName);
		}
		return view;
	}
}