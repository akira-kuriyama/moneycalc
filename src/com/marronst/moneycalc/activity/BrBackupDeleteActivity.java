package com.marronst.moneycalc.activity;

import java.io.File;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.marronst.moneycalc.R;
import com.marronst.moneycalc.helper.BackupRestoreHelper;
import com.marronst.moneycalc.helper.MenuHelper;
import com.marronst.moneycalc.helper.MenuHelper.MenuType;
import com.marronst.moneycalc.task.QuickToastTask;

public class BrBackupDeleteActivity extends Activity {
	protected final String TAG = this.getClass().getSimpleName();

	private BackupRestoreHelper backupRestoreHelper;

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		Log.i(TAG, "onCreate start");

		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.setContentView(R.layout.br_backup_delete);

		Log.i(TAG, "onCreate end");
	}

	@Override
	protected void onResume() {
		Log.i(TAG, "onResume start");
		super.onResume();
		backupRestoreHelper = new BackupRestoreHelper();
		//選択して削除ボタン
		final Button selectedDataDeleteButton = (Button) findViewById(R.id.do_backup_delete_button);
		//全て削除ボタン
		final Button allDataDeleteButton = (Button) findViewById(R.id.do_all_backup_delete_button);

		final String backupDataIsNothingMsg = getResources().getString(R.string.backup_data_is_nothing_msg);

		List<String> backupFileNameList = backupRestoreHelper.getBackupFileNameList();
		if (backupFileNameList.size() == 0) {
			backupFileNameList.add(backupDataIsNothingMsg);
			allDataDeleteButton.setEnabled(false);
			selectedDataDeleteButton.setEnabled(false);
		}
		final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(),
				android.R.layout.simple_spinner_item, backupFileNameList);
		arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		final Spinner backupDataSpinner = (Spinner) findViewById(R.id.backup_data_spinner);
		backupDataSpinner.setAdapter(arrayAdapter);

		final Builder selectedDataDeleteButtonBuilder = new AlertDialog.Builder(this);
		//選択して削除ボタン押下時
		selectedDataDeleteButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(final View v) {

				selectedDataDeleteButtonBuilder.setTitle(R.string.DIALOG_TITLE_DELETE_CONFIRM);
				selectedDataDeleteButtonBuilder.setIcon(R.drawable.alert_dialog_icon);
				selectedDataDeleteButtonBuilder.setPositiveButton(android.R.string.ok,
																	new DialogInterface.OnClickListener() {

																		@Override
																		public void onClick(
																				final DialogInterface dialog,
																				final int which) {
																			deleteSelectedData(
																								selectedDataDeleteButton,
																								allDataDeleteButton,
																								backupDataIsNothingMsg,
																								arrayAdapter,
																								backupDataSpinner);

																		}

																	});
				selectedDataDeleteButtonBuilder.setNegativeButton(android.R.string.no,
																	new DialogInterface.OnClickListener() {

																		@Override
																		public void onClick(
																				final DialogInterface dialog,
																				final int which) {
																			// cancel
																		}
																	});
				selectedDataDeleteButtonBuilder.create().show();
			}
		});

		final Builder allDataDeleteButtonBuilder = new AlertDialog.Builder(this);
		//全て削除ボタン押下時
		allDataDeleteButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(final View v) {

				allDataDeleteButtonBuilder.setTitle(R.string.DIALOG_TITLE_DELETE_CONFIRM);
				allDataDeleteButtonBuilder.setIcon(R.drawable.alert_dialog_icon);
				allDataDeleteButtonBuilder.setPositiveButton(android.R.string.ok,
																new DialogInterface.OnClickListener() {

																	@Override
																	public void onClick(
																			final DialogInterface dialog,
																			final int which) {
																		deleteAllData(
																						selectedDataDeleteButton,
																						allDataDeleteButton,
																						backupDataIsNothingMsg,
																						backupDataSpinner);

																	}

																});
				allDataDeleteButtonBuilder.setNegativeButton(android.R.string.no,
																new DialogInterface.OnClickListener() {

																	@Override
																	public void onClick(
																			final DialogInterface dialog,
																			final int which) {
																		// cancel
																	}
																});
				allDataDeleteButtonBuilder.create().show();
			}
		});

		Log.i(TAG, "onResume end");
	}

	private void deleteSelectedData(final Button selectedDataDeleteButton, final Button allDataDeleteButton,
			final String backupDataIsNothingMsg, final ArrayAdapter<String> arrayAdapter,
			final Spinner backupDataSpinner) {
		int selectedItemPosition = backupDataSpinner.getSelectedItemPosition();

		File[] backupDirectories = backupRestoreHelper.getBackupDirectories();
		if (selectedItemPosition == AdapterView.INVALID_POSITION || backupDirectories == null) {
			return;
		}
		File file = backupDirectories[selectedItemPosition];
		String path = file.getPath();
		backupRestoreHelper.deleteFiles(path);

		arrayAdapter.remove(backupDataSpinner.getSelectedItem().toString());
		int count = arrayAdapter.getCount();
		if (count == 0) {
			arrayAdapter.add(backupDataIsNothingMsg);
			allDataDeleteButton.setEnabled(false);
			selectedDataDeleteButton.setEnabled(false);
		}
		arrayAdapter.setNotifyOnChange(true);

		new QuickToastTask(getApplicationContext(), R.string.DELETE_COMPLETE_MESSAGE).setLong().execute();
	}

	private void deleteAllData(final Button selectedDataDeleteButton, final Button allDataDeleteButton,
			final String backupDataIsNothingMsg, final Spinner backupDataSpinner) {

		File[] backupDirectories = backupRestoreHelper.getBackupDirectories();
		for (File file : backupDirectories) {
			String path = file.getPath();
			backupRestoreHelper.deleteFiles(path);
		}

		final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(),
				android.R.layout.simple_spinner_item, new String[] { backupDataIsNothingMsg });
		arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		backupDataSpinner.setAdapter(arrayAdapter);
		allDataDeleteButton.setEnabled(false);
		selectedDataDeleteButton.setEnabled(false);

		new QuickToastTask(getApplicationContext(), R.string.DELETE_COMPLETE_MESSAGE).setLong().execute();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
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
