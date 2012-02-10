package com.marronst.moneycalc.helper;

import java.io.IOException;

public class SendLogHelper extends AbstractOutputHelper {

	/** ログ送信用ディレクトリ */
	public final String sendLogDirPath = appDirPath + "log/";

	/** ログZIPファイル名 */
	final String LOG_ZIP_FILE_NAME = "app.log";

	/**　
	 * コンストラクタ
	 */
	public SendLogHelper() {
		//最初にログを保管するディレクトリを作成しておく。
		makeDirIfNotExists(sendLogDirPath);
	}

	public String getTempDir() {
		return sendLogDirPath + tmpDirPath;
	}

	public String getCategoryCsvFilePath() throws SecurityException, IOException {
		if (tmpDirPath == null) {
			throw new IllegalStateException("tmpDirPath is null");
		}
		String categoryCsvFilePath = sendLogDirPath + tmpDirPath + SLASH + categoryCsvFileName;
		createFileIfNotExists(categoryCsvFilePath);
		return categoryCsvFilePath;
	}

	public String getBudgetCsvFilePath() throws SecurityException, IOException {
		if (tmpDirPath == null) {
			throw new IllegalStateException("tmpDirPath is null");
		}
		String budgetCsvFilePath = sendLogDirPath + tmpDirPath + SLASH + budgetCsvFileName;
		createFileIfNotExists(budgetCsvFilePath);
		return budgetCsvFilePath;
	}

	public String getFilterCsvFilePath() throws SecurityException, IOException {
		if (tmpDirPath == null) {
			throw new IllegalStateException("tmpDirPath is null");
		}
		String filterCsvFilePath = sendLogDirPath + tmpDirPath + SLASH + filterCsvFileName;
		createFileIfNotExists(filterCsvFilePath);
		return filterCsvFilePath;
	}

	public String getKakeiboDataCsvFilePath() throws SecurityException, IOException {
		if (tmpDirPath == null) {
			throw new IllegalStateException("tmpDirPath is null");
		}
		String kakeiboDataCsvFilePath = sendLogDirPath + tmpDirPath + SLASH + kakeiboDataCsvFileName;
		createFileIfNotExists(kakeiboDataCsvFilePath);
		return kakeiboDataCsvFilePath;
	}

	public String getFormatStringToDateTimeForCsvFilePath() throws SecurityException, IOException {
		String formatStringToDateTimeForCsvFilePath = sendLogDirPath + tmpDirPath + SLASH
				+ CSV_DATE_FORMAT_TXT_FILE_NAME;
		createFileIfNotExists(formatStringToDateTimeForCsvFilePath);
		return formatStringToDateTimeForCsvFilePath;
	}

	public String getDeviceInfoFilePath() throws SecurityException, IOException {
		String deviceInfoTxtPath = sendLogDirPath + tmpDirPath + SLASH + DEVICE_INFO_TXT_FILE_NAME;
		createFileIfNotExists(deviceInfoTxtPath);
		return deviceInfoTxtPath;
	}

	public String getPreferenceCsvFilePath() throws SecurityException, IOException {
		String preferenceCsvFilePath = sendLogDirPath + tmpDirPath + SLASH + PREFERENCE_CSV_FILE_NAME;
		createFileIfNotExists(preferenceCsvFilePath);
		return preferenceCsvFilePath;
	}

	public String getAppLogFilePath() {
		return getTempDir() + SLASH + LOG_ZIP_FILE_NAME;
	}
}
