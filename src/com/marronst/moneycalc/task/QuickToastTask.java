package com.marronst.moneycalc.task;

import android.content.Context;
import android.os.AsyncTask;
import android.view.Gravity;
import android.widget.Toast;

/** 素早く消えるトーストためのタスク */
public class QuickToastTask extends AsyncTask<String, Integer, Integer> {
	private Toast toast;
	private final Context context;
	private final int msgResId;
	private int dispTime;
	private static final int SHORT = 800;
	private static final int LONG = 1200;

	public QuickToastTask(final Context context, final int msgResId) {
		this.msgResId = msgResId;
		this.context = context;
		this.dispTime = SHORT;
	}

	public QuickToastTask setLong() {
		this.dispTime = LONG;
		return this;
	}

	@Override
	protected void onPreExecute() {
		Toast toast = Toast.makeText(context, msgResId, Toast.LENGTH_SHORT);
		this.toast = toast;
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
	}

	@Override
	protected Integer doInBackground(final String... params) {
		try {
			Thread.sleep(dispTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return 0;
	}

	@Override
	protected void onPostExecute(final Integer i) {
		this.toast.cancel();
	}
}