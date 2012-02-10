package com.marronst.moneycalc.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.marronst.moneycalc.R;
import com.marronst.moneycalc.helper.MenuHelper;
import com.marronst.moneycalc.helper.MenuHelper.MenuType;

public class EiSelectActionActivity extends Activity {
	protected final String TAG = this.getClass().getSimpleName();

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		Log.i(TAG, "onCreate start");
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.setContentView(R.layout.ei_select_action);

		Log.i(TAG, "onCreate end");
	}

	@Override
	protected void onResume() {
		Log.i(TAG, "onResume start");
		super.onResume();

		//データをエクスポートする場合
		Button exportSelectButton = (Button) findViewById(R.id.export_select_button);
		exportSelectButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(final View v) {
				Intent intent = new Intent(getApplicationContext(), EiExportActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			}
		});

		//データをインポートする場合
		Button importSelectButton = (Button) findViewById(R.id.import_select_button);
		importSelectButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(final View v) {
				Intent intent = new Intent(getApplicationContext(), EiImportActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			}
		});

		Log.i(TAG, "onResume end");
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

}
