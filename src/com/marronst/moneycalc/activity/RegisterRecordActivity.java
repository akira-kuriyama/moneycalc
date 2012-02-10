package com.marronst.moneycalc.activity;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

import com.marronst.moneycalc.R;
import com.marronst.moneycalc.app.DateTimeDialogBuilder;
import com.marronst.moneycalc.consts.KakeiboConsts;
import com.marronst.moneycalc.consts.KakeiboConsts.KakeiboListViewType;
import com.marronst.moneycalc.db.CategoryDao;
import com.marronst.moneycalc.db.HouseKeepingBookDao;
import com.marronst.moneycalc.db.HouseKeepingBookOpenHelper;
import com.marronst.moneycalc.dxo.CategoryDxo;
import com.marronst.moneycalc.entity.Category;
import com.marronst.moneycalc.entity.HouseKeepingBook;
import com.marronst.moneycalc.exception.MoneyCalcException;
import com.marronst.moneycalc.helper.FooterHelper;
import com.marronst.moneycalc.helper.MenuHelper;
import com.marronst.moneycalc.helper.MenuHelper.MenuType;
import com.marronst.moneycalc.task.QuickToastTask;
import com.marronst.moneycalc.utils.KakeiboFormatUtils;
import com.marronst.moneycalc.utils.KakeiboUtils;

public class RegisterRecordActivity extends Activity {
	protected final String TAG = this.getClass().getSimpleName();
	protected SQLiteDatabase db;

	protected CategoryDao categoryDao;
	protected HouseKeepingBookDao houseKeepingBookDao;

	private HouseKeepingBookOpenHelper houseKeepingBookOpenHelper;
	private final CategoryDxo categoryDxo = new CategoryDxo();

	/** カテゴリリスト */
	private List<Category> categoryList;

	/** 購入日 */
	private Date registerDate;

	/** 位置情報 */
	//	private LocationManager mLocationManager;
	//	private String bestProvider;

	/** キーボードから入力された値の退避用フィールド */
	private Double temporalValue = 0D;
	/** キーボードから入力された値 */
	private Double inputPriceValue = 0D;
	/** キーボードから入力された演算子 */
	private int temporalOperand = OPERAND_PLUS;
	/** 小数深度 */
	private int decimalDeep;
	/** 直前に=が押されたか */
	private boolean isEqualInputed;
	/** 直前に演算子が押されたか */
	private boolean isOperandInputed;

	/** 少数点入力中 */
	private boolean isInputingDecimal;
	protected static final int OPERAND_PLUS = +1;
	protected static final int OPERAND_MINUS = -1;
	protected static final String OPERAND_PLUS_NAME = "＋";
	protected static final String OPERAND_MINUS_NAME = "－";

	/** 表示モード */
	private boolean isSimpleViewMode = true;

	/** Display情報 */
	private DisplayMetrics metrics;

	private NotificationManager notificationManager;

	//	private final LocationListener locationListener = new LocationListener() {
	//		@Override
	//		public void onStatusChanged(final String provider, final int status, final Bundle extras) {
	//			//
	//		}
	//
	//		@Override
	//		public void onProviderEnabled(final String provider) {
	//			// 
	//		}
	//
	//		@Override
	//		public void onProviderDisabled(final String provider) {
	//			// 
	//		}
	//
	//		@Override
	//		public void onLocationChanged(final Location location) {
	//			// 
	//		}
	//	};

	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		Log.i(TAG, "onCreate start");

		super.onCreate(savedInstanceState);

		this.getWindow()
				.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.setContentView(R.layout.register_record);

		houseKeepingBookOpenHelper = new HouseKeepingBookOpenHelper(getApplicationContext());

		try {
			db = houseKeepingBookOpenHelper.getWritableDatabase();
		} catch (SQLiteException e) {
			Log.e(TAG, "getWritableDatabase時にエラー", e);
			throw e;
		}

		categoryDao = new CategoryDao(db);
		houseKeepingBookDao = new HouseKeepingBookDao(db);

		metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);

		notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		SharedPreferences defaultSharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		Boolean isDispStatusBar = defaultSharedPreferences
				.getBoolean(KakeiboConsts.PREFERENCE_KEY_IS_DISP_STATUS_BAR, false);
		if (Boolean.TRUE.equals(isDispStatusBar)) {
			KakeiboUtils.startNotification(this, notificationManager);
		}

		//登録ボタンの設定
		Button button = (Button) findViewById(R.id.add_record);
		button.setOnClickListener(new AddRecordListener());

		//購入日時変更ボタンの設定
		setupEditRegisterDateButton();

		//  キーボードの設定
		setupKeyBoardButton();

		//  共通下部ボタンの設定
		FooterHelper.setupCommonBottomButton(this);

		Log.i(TAG, "onCreate end");
	}

	@Override
	protected void onPause() {
		Log.i(TAG, "onPause start");
		super.onPause();

		//位置情報更新停止
		//		mLocationManager.removeUpdates(locationListener);

		Log.i(TAG, "onPause end");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		houseKeepingBookOpenHelper.close();
	}

	@Override
	protected void onResume() {
		Log.i(TAG, "onResume start");
		super.onResume();

		//		Log.i("KakeiboUtils", "metrics.heightPixels=" + metrics.heightPixels);
		//		Log.i("KakeiboUtils", "metrics.widthPixels=" + metrics.widthPixels);

		Intent intent = getIntent();
		String registerDateStr = intent.getStringExtra(KakeiboConsts.INTENT_KEY_REGISTER_DATE);
		if (!TextUtils.isEmpty(registerDateStr)) {
			String[] cals = registerDateStr.split("/");
			Calendar cal = Calendar.getInstance();
			cal
					.set(Integer.parseInt(cals[0]), Integer.parseInt(cals[1]), Integer.parseInt(cals[2]), 12,
							0, 0);
			registerDate = cal.getTime();
		} else {
			registerDate = new Date();
		}

		//カテゴリリストの更新
		this.categoryList = getCategoryList();

		// タイトルの更新
		updateTitle();

		// カテゴリスピナーの更新
		setupCategorySpinner();

		//表示モードの更新
		if ((metrics.heightPixels <= 800 && KakeiboUtils.isPortrait(this)) //
				|| (metrics.heightPixels == 960 && metrics.widthPixels == 640)) {
			if (isSimpleViewMode) {
				switchSimpleInputMode();
			} else {
				switchDetailInputMode();
			}
		}

		//購入日時の設定
		setupRegisterDate();

		//支出・収入の表示設定
		setupIncomeExpenseName();

		//位置情報取得開始
		//		mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
		//		Criteria criteria = new Criteria();
		//		bestProvider = mLocationManager.getBestProvider(criteria, true);
		//		if (bestProvider == null) {
		//			bestProvider = LocationManager.GPS_PROVIDER;
		//			KakeiboUtils.toastShow(getApplicationContext(), "bestProvider == null");
		//		}
		//		mLocationManager.requestLocationUpdates(bestProvider, 1000, 0, locationListener);
		//		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);

		//金額表示
		final TextView priceValueText = (TextView) findViewById(R.id.price_value);
		priceValueText.setText(KakeiboFormatUtils.formatPriceForRegisterView(inputPriceValue,
																				getApplicationContext()));

		Log.i(TAG, "onResume end");
	}

	/** 購入日時の設定 */
	private void setupRegisterDate() {
		TextView registerDateTime = (TextView) findViewById(R.id.disp_register_date_and_time);
		registerDateTime.setText(KakeiboFormatUtils.formatDateTimeToString(getApplicationContext(),
																			registerDate));
	}

	/** 購入日時ボタンの設定 */
	private void setupEditRegisterDateButton() {
		Button editRegisterDateButton = (Button) findViewById(R.id.edit_register_date);
		editRegisterDateButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(final View v) {

				final DateTimeDialogBuilder dateTimeDialogBuilder = new DateTimeDialogBuilder();
				dateTimeDialogBuilder.init(RegisterRecordActivity.this,
											new DateTimeDialogBuilder.DateTimeDialogResultListener() {

												@Override
												public void onReturnResultDate(final Date date) {
													registerDate = date;
													TextView registerDateTextView = (TextView) findViewById(R.id.disp_register_date_and_time);
													registerDateTextView.setText(KakeiboFormatUtils
															.formatDateTimeToString(getApplicationContext(),
																					date));
												}
											}, registerDate);

				AlertDialog alertDialog = dateTimeDialogBuilder.getEditRegisterDateDialog();
				alertDialog.show();
			}
		});
	}

	/** キーボードボタンの設定 */
	private void setupKeyBoardButton() {
		final TextView priceValueText = (TextView) findViewById(R.id.price_value);
		final TextView operandName = (TextView) findViewById(R.id.operand_name);
		findViewById(R.id.key_0).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				updatePriceDisp(0, priceValueText, operandName);
			}
		});
		findViewById(R.id.key_1).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				updatePriceDisp(1, priceValueText, operandName);
			}
		});
		findViewById(R.id.key_2).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				updatePriceDisp(2, priceValueText, operandName);
			}

		});
		findViewById(R.id.key_3).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				updatePriceDisp(3, priceValueText, operandName);
			}
		});
		findViewById(R.id.key_4).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				updatePriceDisp(4, priceValueText, operandName);
			}
		});
		findViewById(R.id.key_5).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				updatePriceDisp(5, priceValueText, operandName);
			}
		});
		findViewById(R.id.key_6).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				updatePriceDisp(6, priceValueText, operandName);
			}
		});
		findViewById(R.id.key_7).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				updatePriceDisp(7, priceValueText, operandName);
			}
		});
		findViewById(R.id.key_8).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				updatePriceDisp(8, priceValueText, operandName);
			}
		});
		findViewById(R.id.key_9).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				updatePriceDisp(9, priceValueText, operandName);
			}
		});
		//クリア
		findViewById(R.id.key_clear).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				displog("key_clear start");
				inputPriceValue = 0D;
				temporalValue = 0D;
				temporalOperand = OPERAND_PLUS;
				decimalDeep = 0;
				isInputingDecimal = false;
				priceValueText.setText(KakeiboFormatUtils
						.formatPriceForRegisterView(inputPriceValue, getApplicationContext()));
				clearOperandName(operandName);
				displog("key_clear end");
			}
		});
		//ドット
		View keyDot = findViewById(R.id.key_dot);
		if (keyDot != null) {
			keyDot.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(final View v) {
					displog("key_dot start");
					if (!isInputingDecimal && !isEqualInputed) {
						isInputingDecimal = true;
						String priceValue = KakeiboFormatUtils
								.formatPriceForRegisterView(inputPriceValue, getApplicationContext());
						priceValue += ".";
						priceValueText.setText(priceValue);
						clearOperandName(operandName);
					}
					displog("key_dot end");
				}
			});
		}
		//バック
		findViewById(R.id.key_back).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				displog("key_back start");
				if (isEqualInputed || isOperandInputed) {
					return;
				}
				if (!isInputingDecimal) {
					inputPriceValue = new Double(((int) (inputPriceValue / 10)));
				} else {
					if (decimalDeep == 0) {
						isInputingDecimal = false;
						//なにもしない(ドットがとれる)
					} else if (decimalDeep == 1) {
						decimalDeep -= 1;
						isInputingDecimal = false;
						inputPriceValue = new Double(inputPriceValue.intValue());
					} else if (decimalDeep == 2) {
						decimalDeep -= 1;
						String format = new DecimalFormat("###0.00").format(inputPriceValue);
						inputPriceValue = Double.parseDouble(format.substring(0, format.length() - 1));
					}
				}
				priceValueText.setText(formatPriceValue());
				displog("key_back end");
			}
		});
		//プラス
		findViewById(R.id.key_plus).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				displog("key_plus start");
				//				if (temporalOperand == OPERAND_PLUS && !isOperandInputed) {
				//					BigDecimal result = new BigDecimal(temporalOperand).multiply(
				//																					new BigDecimal(
				//																							inputPriceValue))
				//							.add(new BigDecimal(temporalValue));
				//					if (result.compareTo(new BigDecimal(KakeiboConsts.PRICE_LIMIT)) > 0) {
				//						showOverMaxValueNotice();
				//						return;
				//					}
				//					inputPriceValue = result.doubleValue();
				//					temporalValue = inputPriceValue;
				//					priceValueText.setText(KakeiboFormatUtils.formatPriceForRegisterView(inputPriceValue));
				//					inputPriceValue = 0D;
				//				} else {
				//					temporalValue = inputPriceValue;
				//					if (!isOperandInputed) {
				//						inputPriceValue = 0D;
				//					}
				//				}
				if (!isOperandInputed) {
					BigDecimal result = calcTotal();
					if (result == null) {
						return;
					}
					inputPriceValue = result.doubleValue();
					priceValueText.setText(KakeiboFormatUtils
							.formatPriceForRegisterView(inputPriceValue, getApplicationContext()));
				}

				temporalOperand = OPERAND_PLUS;
				decimalDeep = 0;
				isInputingDecimal = false;
				isEqualInputed = false;
				isOperandInputed = true;
				updateOperandName(operandName);
				displog("key_plus end");
			}
		});
		//マイナス
		findViewById(R.id.key_minus).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				displog("key_minus start");

				//				if (temporalOperand == OPERAND_MINUS && !isOperandInputed) {
				//					BigDecimal result = new BigDecimal(temporalOperand).multiply(
				//																					new BigDecimal(
				//																							inputPriceValue))
				//							.add(new BigDecimal(temporalValue));
				//					if (result.compareTo(new BigDecimal(KakeiboConsts.PRICE_LIMIT)) > 0) {
				//						showOverMaxValueNotice();
				//						return;
				//					}
				//					inputPriceValue = result.doubleValue();
				//					temporalValue = inputPriceValue;
				//					priceValueText.setText(KakeiboFormatUtils.formatPriceForRegisterView(inputPriceValue));
				//					inputPriceValue = 0D;
				//				} else {
				//					temporalValue = inputPriceValue;
				//					if (!isOperandInputed) {
				//						inputPriceValue = 0D;
				//					}
				//				}
				if (!isOperandInputed) {
					BigDecimal result = calcTotal();
					if (result == null) {
						return;
					}
					inputPriceValue = result.doubleValue();
					priceValueText.setText(KakeiboFormatUtils
							.formatPriceForRegisterView(inputPriceValue, getApplicationContext()));
				}

				temporalOperand = OPERAND_MINUS;
				decimalDeep = 0;
				isInputingDecimal = false;
				isEqualInputed = false;
				isOperandInputed = true;
				updateOperandName(operandName);
				displog("key_minus end");
			}

		});
		//イコール
		findViewById(R.id.key_equal).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(final View v) {
				displog("key_equal start");
				if (!isOperandInputed) {
					BigDecimal result = calcTotal();
					if (result == null) {
						return;
					}
					inputPriceValue = result.doubleValue();
				}
				temporalValue = 0D;
				temporalOperand = OPERAND_PLUS;
				decimalDeep = 0;
				isInputingDecimal = false;
				isOperandInputed = false;
				isEqualInputed = true;
				priceValueText.setText(KakeiboFormatUtils
						.formatPriceForRegisterView(inputPriceValue, getApplicationContext()));
				clearOperandName(operandName);
				displog("key_equal end");
			}

		});

		if (metrics.heightPixels <= 800//
				|| (metrics.heightPixels == 960 && metrics.widthPixels == 640)) {
			ImageButton keyboardHide = (ImageButton) findViewById(R.id.keyboard_hide);
			if (keyboardHide != null) {
				keyboardHide.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(final View v) {
						switchDetailInputMode();
					}
				});
			}

			ImageButton keyboardShow = (ImageButton) findViewById(R.id.keyboard_show);
			if (keyboardShow != null) {
				keyboardShow.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(final View v) {
						switchSimpleInputMode();
					}
				});
			}
		}
	}

	private void displog(final String name) {

		//		Log.w(TAG, "------------------ " + name);
		//		Log.w(TAG, "inputPriceValue=" + inputPriceValue);
		//		Log.w(TAG, "temporalValue=" + temporalValue);
		//		Log.w(TAG, "temporalOperand=" + temporalOperand);
		//		Log.w(TAG, "decimalDeep=" + decimalDeep);
		//		Log.w(TAG, "isInputingDecimal=" + isInputingDecimal);
		//		Log.w(TAG, "isOperandInputed=" + isOperandInputed);
		//		Log.w(TAG, "isEqualInputed=" + isEqualInputed);
	}

	/** 現時点の合計を計算する。 限界金額を超えていたら、警告Toastをだし、nullを返す*/
	private BigDecimal calcTotal() {
		BigDecimal result = new BigDecimal(temporalOperand).multiply(new BigDecimal(inputPriceValue))
				.add(new BigDecimal(temporalValue));
		if (result.compareTo(new BigDecimal(KakeiboConsts.PRICE_LIMIT)) > 0) {
			showOverMaxValueNotice();
			return null;
		}
		return result;
	}

	/** 数値キーを押下された時、金額表示を更新する */
	private void updatePriceDisp(final int inputKeyValue, final TextView priceValueText,
			final TextView operandName) {
		displog("updatePriceDisp start");
		if (isOverMaxValue(inputKeyValue)) {
			showOverMaxValueNotice();
			return;
		}
		if (isEqualInputed) {
			isEqualInputed = false;
			inputPriceValue = 0D;
		}
		if (isOperandInputed) {
			isOperandInputed = false;
			temporalValue = inputPriceValue;
			inputPriceValue = 0D;
		}
		isOperandInputed = false;
		if (!isInputingDecimal) {
			//小数点押されていないとき
			inputPriceValue = inputPriceValue * 10 + inputKeyValue;
		} else {
			//小数点押されているとき
			if (decimalDeep >= 2) {
				return;
			}
			decimalDeep += 1;
			if (decimalDeep == 1) {
				inputPriceValue = inputPriceValue + (inputKeyValue / 10D);
			} else if (decimalDeep == 2) {
				inputPriceValue = inputPriceValue + (inputKeyValue / 100D);
			}

		}
		priceValueText.setText(formatPriceValue());
		clearOperandName(operandName);
		displog("updatePriceDisp end");
	}

	private String formatPriceValue() {
		String formatPriceValue = "";
		if (!isInputingDecimal) {
			formatPriceValue = KakeiboFormatUtils.formatPriceForRegisterView(inputPriceValue,
																				getApplicationContext());
		} else {
			if (decimalDeep == 0) {
				formatPriceValue = KakeiboFormatUtils.formatPriceForRegisterView(inputPriceValue,
																					getApplicationContext());
			} else if (decimalDeep == 1) {
				formatPriceValue = KakeiboFormatUtils
						.formatPriceWithDecimalOneForRegisterView(inputPriceValue, getApplicationContext());
			} else if (decimalDeep == 2) {
				formatPriceValue = KakeiboFormatUtils
						.formatPriceWithDecimalTwoForRegisterView(inputPriceValue, getApplicationContext());
			}
		}
		return formatPriceValue;
	}

	/** 詳細入力表示モードに切り替える */
	private void switchDetailInputMode() {
		isSimpleViewMode = false;
		View keyboard = findViewById(R.id.keyboard);
		//		Animation anim = AnimationUtils.loadAnimation(getApplicationContext(), android.R.anim.fade_out);
		//		keyboard.startAnimation(anim);
		keyboard.setVisibility(View.GONE);
		if (keyboard != null) {
			keyboard.setVisibility(View.GONE);
		}
		ImageButton keyboardHide = (ImageButton) findViewById(R.id.keyboard_hide);
		if (keyboardHide != null) {
			keyboardHide.setVisibility(View.GONE);
		}
		View keyboardShow = findViewById(R.id.keyboard_show);
		if (keyboardShow != null) {
			keyboardShow.setVisibility(View.VISIBLE);
		}

		View registerDateArea = findViewById(R.id.register_date_erea);
		registerDateArea.setVisibility(View.VISIBLE);
		View memoArea = findViewById(R.id.memo_erea);
		memoArea.setVisibility(View.VISIBLE);
	}

	/** シンプル入力表示モードに切り替える */
	private void switchSimpleInputMode() {
		try {
			isSimpleViewMode = true;
			ImageButton keyboardShow = (ImageButton) findViewById(R.id.keyboard_show);
			View keyboard = findViewById(R.id.keyboard);
			//		Animation anim = AnimationUtils.loadAnimation(getApplicationContext(), android.R.anim.fade_in);
			//		keyboard.startAnimation(anim);
			keyboard.setVisibility(View.VISIBLE);

			keyboardShow.setVisibility(View.GONE);
			findViewById(R.id.keyboard_hide).setVisibility(View.VISIBLE);

			View registerDateArea = findViewById(R.id.register_date_erea);
			registerDateArea.setVisibility(View.GONE);
			View memoArea = findViewById(R.id.memo_erea);
			memoArea.setVisibility(View.GONE);
		} catch (Exception e) {
			throw new MoneyCalcException("switchSimpleInputMode error, DeviceInfo="
					+ KakeiboUtils.getDeviceInfo(this), e);
		}
	}

	private boolean isOverMaxValue(final int plessKey) {
		if (isOperandInputed) {
			return false;
		}
		BigDecimal result = new BigDecimal(inputPriceValue).multiply(BigDecimal.TEN)
				.add(new BigDecimal(plessKey));
		return result.compareTo(new BigDecimal(KakeiboConsts.PRICE_LIMIT)) > 0;
	}

	/** 演算子のクリア */
	private void clearOperandName(final TextView operandName) {
		operandName.setText(KakeiboConsts.EMPTY);
	}

	/** 演算子の表示更新 */
	private void updateOperandName(final TextView operandName) {
		String name = "";
		switch (temporalOperand) {
			case OPERAND_MINUS:
				name = OPERAND_MINUS_NAME;
				break;
			case OPERAND_PLUS:
				name = OPERAND_PLUS_NAME;
				break;
			default:
				break;
		}
		operandName.setText(name);
	}

	private void showOverMaxValueNotice() {
		KakeiboUtils.toastShow(getApplicationContext(), R.string.ERRORS_MAX_PRICE_OVER);
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

	/** 支出・収入の表示の設定*/
	private void setupIncomeExpenseName() {
		Spinner spinner = (Spinner) findViewById(R.id.category_spinner);
		int itemPosition = spinner.getSelectedItemPosition();
		String categoryname = spinner.getItemAtPosition(itemPosition).toString();
		Category category = getCategory(categoryname);

		changeIncomeExpenseName(category);

	}

	/** 支出・収入の表示を変える*/
	private void changeIncomeExpenseName(final Category category) {
		TextView textView = (TextView) findViewById(R.id.incame_expense_name);
		if (category.incomeFlg == Category.INCOME_FLG_OFF) {
			textView.setTextColor(getResources().getColor(R.color.expense_color));
			textView.setText(KakeiboConsts.EMPTY);
		} else {
			textView.setText("(" + getResources().getString(R.string.INCOME_NAME) + ")");
			textView.setTextColor(getResources().getColor(R.color.income_color));
		}

	}

	private class AddRecordListener implements OnClickListener {
		@Override
		public void onClick(final View v) {

			if (inputPriceValue < 0) {
				KakeiboUtils.toastShow(RegisterRecordActivity.this, R.string.ERRORS_REIGSTER_MINUS_PRICE);
				return;
			}

			addRecorld();

			InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
			inputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);

			new QuickToastTask(getApplicationContext(), R.string.KAKEIBO_REGISTER_COMPLETE_MESSAGE)
					.execute("");//素早く消えるトースト

			SharedPreferences defaultSharedPreferences = PreferenceManager
					.getDefaultSharedPreferences(getApplicationContext());
			Boolean isMoveDayListAfterRegistered = defaultSharedPreferences
					.getBoolean(KakeiboConsts.PREFERENCE_KEY_IS_MOVE_DAY_LIST_AFTER_REGISTERED, false);
			if (isMoveDayListAfterRegistered) {
				//一日の一覧へ遷移する
				final KakeiboListViewType viewType = KakeiboListViewType.DAY;
				Intent i = new Intent(getApplicationContext(), ViewKakeiboListActivity.class);
				i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

				i.putExtra(KakeiboConsts.INTENT_KEY_LIST_VIEW_TYPE, viewType.name());

				//購入日時をViewから取得し、intentへセットする
				//				TextView registerDateAndTime = (TextView) findViewById(R.id.disp_register_date_and_time);
				//				Date registeredDate = KakeiboFormatUtils.formatStringToDateTime((String) registerDateAndTime
				//						.getText());
				Calendar startDate = Calendar.getInstance();
				startDate.setTime(registerDate);
				i.putExtra(KakeiboConsts.INTENT_KEY_START_DATE_VALUE, KakeiboUtils
						.getStartCal(startDate, viewType, getApplicationContext()));
				startActivity(i);
			}
		}
	}

	// レコードの追加をする
	private void addRecorld() {
		Log.i(TAG, "addRecorld");

		Spinner spinner = (Spinner) findViewById(R.id.category_spinner);
		int itemPosition = spinner.getSelectedItemPosition();
		String categoryname = spinner.getItemAtPosition(itemPosition).toString();
		Category category = getCategory(categoryname);
		Integer categoryId = category.id;
		EditText memoEditText = (EditText) findViewById(R.id.memo_value);
		String memo = memoEditText.getText().toString();

		int price;
		if (KakeiboUtils.isJapan()) {
			price = ((inputPriceValue)).intValue();
		} else {
			price = ((Double) (inputPriceValue * 100)).intValue();
		}
		String latitudeStr = "";
		String longitudeStr = "";
		String place = "";

		//		Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		//		Location location = mLocationManager.getLastKnownLocation(bestProvider);
		//
		//		if (location == null) {
		//			bestProvider = LocationManager.GPS_PROVIDER;
		//			mLocationManager.requestLocationUpdates(bestProvider, 1000, 0, locationListener);
		//			location = mLocationManager.getLastKnownLocation(bestProvider);
		//		}
		//
		//		if (location != null) {
		//			double latitude = location.getLatitude();
		//			double longitude = location.getLongitude();
		//			latitudeStr = Double.toString(latitude);
		//			longitudeStr = Double.toString(longitude);
		//			//test
		//			//			KakeiboFormatUtils.toastShowLong(getApplicationContext(), ""// "bestProvider =" + bestProvider
		//			//					+ "\nlongitude=" + longitude + " ,latitude=" + latitude);
		//
		//			Geocoder geo = new Geocoder(getApplicationContext(), Locale.JAPAN);
		//			try {
		//				List<Address> addressList = geo.getFromLocation(latitude, longitude, 1);
		//				StringBuilder b = new StringBuilder();
		//				for (int i = 0; i < addressList.size(); i++) {
		//					final Address address = addressList.get(i);
		//					for (int j = 0; j <= address.getMaxAddressLineIndex(); j++) {
		//						b.append(address.getAddressLine(j) + " ");
		//					}
		//				}
		//				//test
		//				//				AlertDialog.Builder ab = new AlertDialog.Builder(RegisterRecordActivity.this);
		//				//				ab.setMessage(b.toString()).show();
		//
		//				place = b.toString();
		//			} catch (IOException e) {
		//				e.printStackTrace();
		//			}
		//
		//		} else {
		//			KakeiboUtils.toastShow(getApplicationContext(), "location is null");
		//		}

		//位置情報更新停止
		//mLocationManager.removeUpdates(locationListener);

		HouseKeepingBook houseKeepingBook = new HouseKeepingBook();
		houseKeepingBook.price = price;
		houseKeepingBook.memo = memo;
		houseKeepingBook.place = place;
		houseKeepingBook.categoryId = categoryId;
		houseKeepingBook.registerDate = registerDate;// KakeiboFormatUtils.formatStringToDateTime(registerDateAndTimeStr);
		houseKeepingBook.latitude = latitudeStr;
		houseKeepingBook.longitude = longitudeStr;
		houseKeepingBookDao.insert(houseKeepingBook);

		inputPriceValue = 0D;// 入力値のクリアー
		temporalValue = 0D;
		temporalOperand = OPERAND_PLUS;
		TextView priceValueText = (TextView) findViewById(R.id.price_value);
		priceValueText.setText(KakeiboFormatUtils.formatPriceForRegisterView(inputPriceValue,
																				getApplicationContext()));
		clearOperandName((TextView) findViewById(R.id.operand_name));//演算子表示のクリア
		memoEditText.setText("");

	}

	/** カテゴリリストの取得 */
	private List<Category> getCategoryList() {
		List<Category> categoryList = new ArrayList<Category>();
		Cursor c = categoryDao.findAllWithNonDisplay();
		startManagingCursor(c);
		while (c.moveToNext()) {
			Category category = categoryDxo.createFromCursol(c);
			categoryList.add(category);
		}
		this.categoryList = categoryList;

		return categoryList;
	}

	/** タイトルの更新 */
	private void updateTitle() {
		TextView textView = (TextView) findViewById(R.id.this_month_text);
		textView.setText(KakeiboFormatUtils
				.formatDateToStringPlusYoubi(getApplicationContext(), registerDate));
	}

	/** カテゴリ選択するスピナーをセット */
	private void setupCategorySpinner() {

		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getApplicationContext(),
				android.R.layout.simple_spinner_item);

		for (Category category : categoryList) {
			if (Category.DISP_FLG_ON.equals(category.dispFlg)) {
				arrayAdapter.add(category.categoryName);
			}
		}

		arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		Spinner spinner = (Spinner) findViewById(R.id.category_spinner);
		spinner.setAdapter(arrayAdapter);
		spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(final AdapterView<?> parent, final View view, final int position,
					final long id) {
				String categoryName = (String) parent.getItemAtPosition(position);
				Category category = getCategory(categoryName);
				changeIncomeExpenseName(category);

			}

			@Override
			public void onNothingSelected(final AdapterView<?> parent) {
				// 
			}
		});
	}

	private Category getCategory(final String categoryName) {
		for (Category category : categoryList) {
			if (category.categoryName.equals(categoryName)) {
				return category;
			}
		}
		return null;
	}

}
