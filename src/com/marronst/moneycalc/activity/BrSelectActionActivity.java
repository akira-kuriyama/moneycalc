package com.marronst.moneycalc.activity;

import android.app.Activity;
import android.app.AlertDialog;
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
import com.marronst.moneycalc.utils.KakeiboUtils;

public class BrSelectActionActivity extends Activity {
	protected final String TAG = this.getClass().getSimpleName();

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		Log.i(TAG, "onCreate start");
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.setContentView(R.layout.br_select_action);

		Log.i(TAG, "onCreate end");
	}

	@Override
	protected void onResume() {
		Log.i(TAG, "onResume start");
		super.onResume();

		//データをバックアップする場合
		Button backupSelectButton = (Button) findViewById(R.id.backup_select_button);
		backupSelectButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(final View v) {
				Intent intent = new Intent(getApplicationContext(), BrBackupActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			}
		});

		//データをレストアする場合
		Button importSelectButton = (Button) findViewById(R.id.restore_select_button);
		importSelectButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(final View v) {

				//SDカードがあるかチェック
				if (!KakeiboUtils.isMediaMounted()) {
					AlertDialog.Builder builder = new AlertDialog.Builder(BrSelectActionActivity.this);
					builder.setMessage(R.string.backup_no_sdcard_error_msg);
					builder.setPositiveButton(android.R.string.ok, null);
					builder.create().show();
					return;
				}
				
				Intent intent = new Intent(getApplicationContext(), BrRestoreActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivity(intent);
			}
		});

		//バックアップデータを削除する場合
		Button backupDeleteSelectButton = (Button) findViewById(R.id.backup_delete_select_button);
		backupDeleteSelectButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(final View v) {
				
				//SDカードがあるかチェック
				if (!KakeiboUtils.isMediaMounted()) {
					AlertDialog.Builder builder = new AlertDialog.Builder(BrSelectActionActivity.this);
					builder.setMessage(R.string.backup_no_sdcard_error_msg);
					builder.setPositiveButton(android.R.string.ok, null);
					builder.create().show();
					return;
				}
				
				Intent intent = new Intent(getApplicationContext(), BrBackupDeleteActivity.class);
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
