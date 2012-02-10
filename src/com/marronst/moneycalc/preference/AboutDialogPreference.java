package com.marronst.moneycalc.preference;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;

import com.marronst.moneycalc.R;

public class AboutDialogPreference extends DialogPreference {

	public AboutDialogPreference(final Context context, final AttributeSet attrs) {
		super(context, attrs);
		setDialogLayoutResource(R.layout.appli_info);
	}

	@Override
	protected void showDialog(final Bundle state) {
		Context context = getContext();

		Builder mBuilder = new AlertDialog.Builder(context)//
				.setTitle(getDialogTitle())//
				.setIcon(R.drawable.show_bunny)//
				.setPositiveButton(getPositiveButtonText(), this)//
		;

		View contentView = onCreateDialogView();
		if (contentView != null) {
			onBindDialogView(contentView);
			mBuilder.setView(contentView);
		} else {
			mBuilder.setMessage("");
		}

		onPrepareDialogBuilder(mBuilder);

		//	getPreferenceManager().registerOnActivityDestroyListener(this);

		// Create the dialog
		final Dialog dialog = mBuilder.create();
		if (state != null) {
			dialog.onRestoreInstanceState(state);
		}
		dialog.setOnDismissListener(this);
		dialog.show();
	}
}
