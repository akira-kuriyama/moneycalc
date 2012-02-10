package com.marronst.moneycalc.task;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.marronst.moneycalc.R;
import com.marronst.moneycalc.utils.ImageCacheUtils;
import com.marronst.moneycalc.utils.KakeiboUtils;

public class GraphImageDownloadTask extends AsyncTask<String, Integer, Bitmap> {
	protected final String TAG = this.getClass().getSimpleName();

	private final View retryGraphDownloadArea;
	private final ImageView imageView;
	private final ProgressDialog progressDialog;
	private final String message;
	private Boolean isComplete;
	private final String deviceInfo;

	public GraphImageDownloadTask(final Activity activity, final ImageView imageView,
			final View retryGraphDownloadArea) {
		this.imageView = imageView;
		this.progressDialog = new ProgressDialog(activity);
		this.retryGraphDownloadArea = retryGraphDownloadArea;
		this.message = activity.getResources().getString(R.string.GRAPH_CREATING_MESSAGE);
		this.deviceInfo = KakeiboUtils.getDeviceInfo(activity);
	}

	@Override
	protected void onPreExecute() {
		progressDialog.setMessage(message);
		progressDialog.setIndeterminate(true);
		progressDialog.setProgress(ProgressDialog.STYLE_SPINNER);
		progressDialog.show();
	}

	@Override
	protected void onCancelled() {
		Log.i("GraphImageDownloadTask", "onCancelled ");
		super.onCancelled();
		imageView.setImageBitmap(null);
		progressDialog.dismiss();
	}

	@Override
	protected Bitmap doInBackground(final String... params) {
		Log.i("GraphImageDownloadTask", "doInBackground start");
		final String url = params[0];
		Bitmap bt = null;

		new GraphImageDownloadWatchTask(this, retryGraphDownloadArea).execute();
		try {

			URL urltest = new URL("http://chart.apis.google.com/chart");
			URLConnection urlConnection = urltest.openConnection();
			urlConnection.setDoOutput(true);

			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(urlConnection.getOutputStream());
			BufferedWriter bufferedWriter = new BufferedWriter(outputStreamWriter);
			bufferedWriter.write(url);
			bufferedWriter.close();
			outputStreamWriter.close();

			final InputStream inputStream = urlConnection.getInputStream();
			bt = BitmapFactory.decodeStream(inputStream);
			inputStream.close();
			//	InputStream inputStream = new URL(url).openStream();
			//			bt = BitmapFactory.decodeStream(inputStream);

		} catch (MalformedURLException e) {
			//Log.e(TAG, "MalformedURLException グラフ画像取得中にエラー", e);
			throw new IllegalStateException(
					"MalformedURLException url=" + url + ", deviceInfo=" + deviceInfo, e);
		} catch (IOException e) {

			Class<? extends IOException> exceptionClass = e.getClass();
			if (exceptionClass == FileNotFoundException.class) {
				throw new IllegalStateException("IOException url=" + url + ", deviceInfo=" + deviceInfo, e);
			}
			//Log.e(TAG, "IOException グラフ画像取得中にエラー", e);

		}
		if (bt != null) {
			ImageCacheUtils.put(url, bt);
		}

		Log.i("GraphImageDownloadTask", "doInBackground end");
		return bt;
	}

	@Override
	protected void onPostExecute(final Bitmap bt) {

		Log.i("GraphImageDownloadTask", "onPostExecute");
		try {
			if (bt != null) {
				isComplete = true;
			} else {
				isComplete = false;
			}
			progressDialog.dismiss();
		} catch (Exception e) {
			// 横向きでグラフ表示状態で、縦向きにするとエラーになるので、握りつぶす。。
			//e.printStackTrace();
		}
		imageView.setImageBitmap(bt);
	}

	public Boolean isComplete() {
		return this.isComplete;
	}
}
