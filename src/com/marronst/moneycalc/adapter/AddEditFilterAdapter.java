package com.marronst.moneycalc.adapter;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.marronst.moneycalc.R;
import com.marronst.moneycalc.dto.CategoryFilterDto;

public class AddEditFilterAdapter extends ArrayAdapter<CategoryFilterDto> {

	@SuppressWarnings("unused")
	private static final String TAG = AddEditFilterAdapter.class.getSimpleName();

	private final LayoutInflater inflater;

	public AddEditFilterAdapter(final Context context, final int textViewResourceId,
			final List<CategoryFilterDto> items) {
		super(context, textViewResourceId, items);
		this.inflater = LayoutInflater.from(context);
	}

	@Override
	public View getView(final int position, final View convertView, final ViewGroup parent) {
		// ビューを設定する
		View view = this.inflater.inflate(R.layout.add_edit_filter_row, null);
		// 表示すべきデータの取得
		final CategoryFilterDto categoryFilterDto = getItem(position);

		if (categoryFilterDto != null) {
			//カテゴリ名
			String categoryName = categoryFilterDto.categoryName;
			TextView categoryNameTextView = (TextView) view.findViewById(R.id.category_name);
			categoryNameTextView.setText(categoryName);

			//チェックボックス
			final CheckBox checkBox = (CheckBox) view.findViewById(R.id.is_select);
			checkBox.setChecked(categoryFilterDto.isSelect);

			//チェックボックスのチェックチェンジ時
			checkBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked) {
					categoryFilterDto.isSelect = isChecked;
				}
			});

			//カテゴリ名クリック時にもチェックボックスのONOFFを切り替える
			categoryNameTextView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					checkBox.setChecked(!checkBox.isChecked());

				}
			});
		}
		return view;
	}
}