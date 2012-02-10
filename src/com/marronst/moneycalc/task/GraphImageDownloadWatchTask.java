package com.marronst.moneycalc.task;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;

public class GraphImageDownloadWatchTask extends AsyncTask<String, Integer, Void> {
	protected final String TAG = this.getClass().getSimpleName();

	private final GraphImageDownloadTask graphImageDownloadTask;
	private final View retryGraphDownloadArea;

	public GraphImageDownloadWatchTask(final GraphImageDownloadTask graphImageDownloadTask,
			final View retryGraphDownloadArea) {
		this.graphImageDownloadTask = graphImageDownloadTask;
		this.retryGraphDownloadArea = retryGraphDownloadArea;
	}

	@Override
	protected Void doInBackground(final String... params) {
		Log.i("GraphImageDownloadWatchTask", "doInBackground start");
		Boolean isComplete = graphImageDownloadTask.isComplete();
		if (!Boolean.TRUE.equals(isComplete)) {

			for (int i = 0; i < 14; i++) {
				try {
					Thread.sleep(500);
					if (graphImageDownloadTask.isComplete() != null) {
						break;
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		}

		Log.i("GraphImageDownloadWatchTask", "doInBackground end");
		return null;

	}

	@Override
	protected void onPostExecute(final Void result) {
		Log.i("GraphImageDownloadWatchTask", "onPostExecute start");
		if (!Boolean.TRUE.equals(graphImageDownloadTask.isComplete())) {
			graphImageDownloadTask.cancel(true);
			retryGraphDownloadArea.setVisibility(View.VISIBLE);
		} else {
			retryGraphDownloadArea.setVisibility(View.GONE);
		}

		Log.i("GraphImageDownloadWatchTask", "onPostExecute end");
	}

}
