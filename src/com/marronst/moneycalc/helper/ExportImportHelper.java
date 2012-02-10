package com.marronst.moneycalc.helper;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.os.Environment;
import android.text.format.DateFormat;

public class ExportImportHelper {

	private static final String SLASH = "/";
	private static final String appDirPath = Environment.getExternalStorageDirectory().toString()
			+ "/moneyCalc/";
	/** エクスポート用ディレクトリ */
	public final String exportDirPath = appDirPath + "export/";

	/** インポート用ディレクトリ */
	public final String importDirPath = appDirPath + "import/";

	/** 家計簿データCSVファイル名*/
	public String kakeiboDataCsvFileName = "kakeiboData.csv";

	private SimpleDateFormat stringToDateTimeFormat = null;
	private StringBuilder csvDateFormat = null;

	private String exportTimeStr;

	private File exportFile;

	/** エクスポート用フォルダがなければ作成する */
	public void makeExportDireIfNotExists() throws SecurityException {
		try {
			File appDirPathFile = new File(appDirPath);
			if (!appDirPathFile.exists()) {
				appDirPathFile.mkdir();
			}
			File exportDirPathFile = new File(exportDirPath);
			if (!exportDirPathFile.exists()) {
				exportDirPathFile.mkdir();
			}
		} catch (SecurityException e) {
			throw e;
		}
	}

	public String formatDateTimeToStringForCsv(final Context context, final Date date) {
		String formatter = getCsvDateFormat(context);
		String dateStr = (String) DateFormat.format(formatter, date);
		return dateStr;
	}

	public Date formatStringToDateTimeForCsv(final String dateStr, final String formatter) {
		if (stringToDateTimeFormat == null) {
			stringToDateTimeFormat = new SimpleDateFormat(formatter);
		}
		Date date = null;
		try {
			date = stringToDateTimeFormat.parse(dateStr);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return date;
	}

	public String getCsvDateFormat(final Context context) {
		if (csvDateFormat != null) {
			return csvDateFormat.toString();
		}

		csvDateFormat = new StringBuilder();
		char[] dateFormatOrder = DateFormat.getDateFormatOrder(context);
		for (char c : dateFormatOrder) {
			switch (c) {
				case DateFormat.DATE:
					csvDateFormat.append("dd");
					break;
				case DateFormat.MONTH:
					csvDateFormat.append("MM");

					break;
				case DateFormat.YEAR:
					csvDateFormat.append("yyyy");
					break;
				default:
					break;
			}
			csvDateFormat.append("/");
		}
		csvDateFormat = new StringBuilder(csvDateFormat.substring(0, csvDateFormat.length() - 1));
		csvDateFormat.append(" kk:mm");
		return csvDateFormat.toString();
	}

	public File makeExportFile(final Context context) throws IOException {

		StringBuilder dateFormat = new StringBuilder();
		char[] dateFormatOrder = DateFormat.getDateFormatOrder(context);
		for (char c : dateFormatOrder) {
			switch (c) {
				case DateFormat.DATE:
					dateFormat.append("dd");
					break;
				case DateFormat.MONTH:
					dateFormat.append("MM");

					break;
				case DateFormat.YEAR:
					dateFormat.append("yyyy");
					break;
				default:
					break;
			}
			dateFormat.append("-");
		}
		dateFormat = new StringBuilder(dateFormat.substring(0, dateFormat.length() - 1));
		dateFormat.append("_kk-mm-ss");
		exportTimeStr = (String) DateFormat.format(dateFormat.toString(), new Date());

		String fileName = exportDirPath + getExportCsvFileName();
		exportFile = new File(fileName);
		exportFile.createNewFile();
		return exportFile;
	}

	public void deleteExportFile() {
		try {
			if (exportFile != null && exportFile.exists()) {
				exportFile.delete();
			}
		} catch (SecurityException e) {
			throw e;
		}
	}

	/** ユーザ向けのエクスポートファイル名を返す */
	public String getExportFileNameForUser() {
		return "/moneyCalc/export/" + getExportCsvFileName();
	}

	private String getExportCsvFileName() {
		return "kakeiboData_" + exportTimeStr + ".csv";
	}
}
