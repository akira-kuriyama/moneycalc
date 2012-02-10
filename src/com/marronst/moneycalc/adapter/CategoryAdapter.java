package com.marronst.moneycalc.adapter;

import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.marronst.moneycalc.R;
import com.marronst.moneycalc.entity.Category;

public class CategoryAdapter extends ArrayAdapter<Category> {

	@SuppressWarnings("unused")
	private static final String TAG = CategoryAdapter.class.getSimpleName();

	private final LayoutInflater inflater;
	private final String NO_DISPLAY = "(非表示)";

	public CategoryAdapter(final Context context, final int textViewResourceId, final List<Category> items2) {
		super(context, textViewResourceId, items2);
		this.inflater = LayoutInflater.from(context);
	}

	@Override
	public View getView(final int position, final View convertView, final ViewGroup parent) {
		// ビューを設定する
		View view = this.inflater.inflate(R.layout.edit_category_row, null);
		// 表示すべきデータの取得
		Category category = getItem(position);

		if (category != null) {
			TextView categoryName = (TextView) view.findViewById(R.id.edit_category_row_category_name);
			if (Category.DISP_FLG_OFF.equals(category.dispFlg)) {
				categoryName.setTextColor(Color.GRAY);
				categoryName.setText(category.categoryName + NO_DISPLAY);
			} else {
				categoryName.setText(category.categoryName);

			}
		}
		return view;
	}
}