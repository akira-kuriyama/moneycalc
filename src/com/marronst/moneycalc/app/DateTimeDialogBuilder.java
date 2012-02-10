package com.marronst.moneycalc.app;

import java.util.Calendar;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.DatePicker;
import android.widget.TimePicker;

import com.marronst.moneycalc.R;

public class DateTimeDialogBuilder {

	private Date setupDate = null;

	private Activity mActivity;

	public interface DateTimeDialogResultListener {

		void onReturnResultDate(Date date);
	}

	private DateTimeDialogResultListener listener;

	public void init(final Activity activity, final DateTimeDialogResultListener listener,
			final Date setupDate) {
		this.mActivity = activity;
		this.listener = listener;
		this.setupDate = setupDate;
	}

	public AlertDialog getEditRegisterDateDialog() {
		AlertDialog.Builder editRegisterDateDialogBuilder = new AlertDialog.Builder(mActivity);
		LayoutInflater inflater = LayoutInflater.from(mActivity);

		final View inputDateAndTimeView = inflater.inflate(R.layout.input_date_and_time, null);
		Calendar cal = Calendar.getInstance();
		cal.setTime(setupDate);

		final DatePicker datePicker = (DatePicker) inputDateAndTimeView.findViewById(R.id.date_picker);
		datePicker
				.updateDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));

		final TimePicker timePicker = (TimePicker) inputDateAndTimeView.findViewById(R.id.time_picker);
		timePicker.setIs24HourView(true);
		timePicker.setCurrentHour(cal.get(Calendar.HOUR_OF_DAY));
		timePicker.setCurrentMinute(cal.get(Calendar.MINUTE));
		editRegisterDateDialogBuilder.setView(inputDateAndTimeView);
		editRegisterDateDialogBuilder.setPositiveButton(R.string.DIALOG_DECIDE,
														new DialogInterface.OnClickListener() {

															@Override
															public void onClick(final DialogInterface dialog,
																	final int which) {
																Integer currentHour = timePicker
																		.getCurrentHour();
																Integer currentMinute = timePicker
																		.getCurrentMinute();

																int year = datePicker.getYear();
																int month = datePicker.getMonth();
																int dayOfMonth = datePicker.getDayOfMonth();
																Calendar registerDateCal = Calendar
																		.getInstance();
																registerDateCal.set(year, month, dayOfMonth,
																					currentHour,
																					currentMinute);
																listener.onReturnResultDate(registerDateCal
																		.getTime());
															}
														});
		editRegisterDateDialogBuilder.setNegativeButton(R.string.DIALOG_CANCEL,
														new DialogInterface.OnClickListener() {
															@Override
															public void onClick(final DialogInterface dialog,
																	final int which) {
																// 	キャンセル
															}
														});
		editRegisterDateDialogBuilder.setTitle(R.string.DIALOG_TITLE_REGISTER_DATE_EDIT);
		final AlertDialog editRegisterDateDialog = editRegisterDateDialogBuilder.create();

		return editRegisterDateDialog;
	}

}
