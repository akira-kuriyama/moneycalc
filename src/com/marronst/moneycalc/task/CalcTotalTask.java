package com.marronst.moneycalc.task;

import java.util.Calendar;
import java.util.List;

import android.app.Activity;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.marronst.moneycalc.R;
import com.marronst.moneycalc.adapter.KakeiboTotalAdapter;
import com.marronst.moneycalc.consts.KakeiboConsts.KakeiboListViewType;
import com.marronst.moneycalc.db.BudgetDao;
import com.marronst.moneycalc.db.HouseKeepingBookDao;
import com.marronst.moneycalc.db.HouseKeepingBookOpenHelper;
import com.marronst.moneycalc.dto.KakeiboTotalDto;
import com.marronst.moneycalc.helper.CalcTotalHelper;

public class CalcTotalTask extends AsyncTask<String, Integer, List<KakeiboTotalDto>> {
	protected final String TAG = this.getClass().getSimpleName();

	private final Activity activity;
	private final CalcTotalHelper calcTotalHelper;
	private final Calendar nowDispCalendar;
	private final KakeiboListViewType kakeiboListViewType;
	private final List<Integer> catgoryIdList;
	private final ListView listView;
	private final View totalTallyProgressBarArea;
	private final SQLiteDatabase db;

	public CalcTotalTask(final Activity activity, final Calendar nowDispCalendar,
			final KakeiboListViewType kakeiboListViewType, final List<Integer> catgoryIdList,
			final ListView listView) {
		this.activity = activity;
		this.listView = listView;
		HouseKeepingBookOpenHelper houseKeepingBookOpenHelper = new HouseKeepingBookOpenHelper(activity
				.getApplicationContext());
		db = houseKeepingBookOpenHelper.getReadableDatabase();
		this.calcTotalHelper = new CalcTotalHelper();
		this.calcTotalHelper.setDb(new HouseKeepingBookDao(db), new BudgetDao(db));
		this.nowDispCalendar = nowDispCalendar;
		this.kakeiboListViewType = kakeiboListViewType;
		this.catgoryIdList = catgoryIdList;
		totalTallyProgressBarArea = activity.findViewById(R.id.total_tally_progress_bar_area);
		Log.i("CalcTotalTask", "start");
	}

	@Override
	protected void onPreExecute() {
		totalTallyProgressBarArea.setVisibility(View.VISIBLE);
	}

	@Override
	protected List<KakeiboTotalDto> doInBackground(final String... params) {
		//Debug.startMethodTracing("moneyCalc");
		//集計結果を取得
		List<KakeiboTotalDto> kakeiboTotalDtoList = calcTotalHelper
				.getKakeiboTotalDtoList(activity, nowDispCalendar, kakeiboListViewType, catgoryIdList);
		db.close();
		return kakeiboTotalDtoList;
	}

	@Override
	protected void onPostExecute(final List<KakeiboTotalDto> kakeiboTotalDtoList) {
		//ListViewにセット
		totalTallyProgressBarArea.setVisibility(View.GONE);
		KakeiboTotalAdapter kakeiboTotalAdapter = new KakeiboTotalAdapter(activity,
				R.layout.kakeibo_total_row, kakeiboTotalDtoList);
		listView.setAdapter(kakeiboTotalAdapter);
		//Debug.stopMethodTracing();
		Log.i("CalcTotalTask", "end");
	}
}
