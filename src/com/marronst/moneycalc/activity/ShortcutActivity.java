package com.marronst.moneycalc.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;

import com.marronst.moneycalc.R;

public class ShortcutActivity extends Activity {

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent shortcutIntent = new Intent(this, RegisterRecordActivity.class);

		// 作成したショートカットを設定するIntent。ここでショートカット名とアイコンも設定。
		Intent intent = new Intent();
		intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
		Parcelable iconResource = Intent.ShortcutIconResource.fromContext(this, R.drawable.super_mario_coin2);
		intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);

		String shortcutName = getResources().getString(R.string.SHORTCUT_NAME);
		intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutName);

		// ショートカット設定
		setResult(RESULT_OK, intent);
		finish();

	}
}
