package com.marronst.moneycalc.helper;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.marronst.moneycalc.utils.KakeiboUtils;

public class BackupRestoreHelper extends AbstractOutputHelper {

	/** バックアップ用ディレクトリ */
	public final String backupDirPath = appDirPath + "backup/";

	private SimpleDateFormat stringToDateTimeFormat = null;

	public BackupRestoreHelper() {
		//最初にバックアップファイルを保管するディレクトリを作成しておく。
		makeDirIfNotExists(backupDirPath);
	}

	public String getCategoryCsvFilePath() throws SecurityException, IOException {
		if (tmpDirPath == null) {
			throw new IllegalStateException("tmpDirPath is null");
		}
		String categoryCsvFilePath = backupDirPath + tmpDirPath + SLASH + categoryCsvFileName;
		createFileIfNotExists(categoryCsvFilePath);
		return categoryCsvFilePath;
	}

	public String getBudgetCsvFilePath() throws SecurityException, IOException {
		if (tmpDirPath == null) {
			throw new IllegalStateException("tmpDirPath is null");
		}
		String budgetCsvFilePath = backupDirPath + tmpDirPath + SLASH + budgetCsvFileName;
		createFileIfNotExists(budgetCsvFilePath);
		return budgetCsvFilePath;
	}

	public String getFilterCsvFilePath() throws SecurityException, IOException {
		if (tmpDirPath == null) {
			throw new IllegalStateException("tmpDirPath is null");
		}
		String filterCsvFilePath = backupDirPath + tmpDirPath + SLASH + filterCsvFileName;
		createFileIfNotExists(filterCsvFilePath);
		return filterCsvFilePath;
	}

	public String getKakeiboDataCsvFilePath() throws SecurityException, IOException {
		if (tmpDirPath == null) {
			throw new IllegalStateException("tmpDirPath is null");
		}
		String kakeiboDataCsvFilePath = backupDirPath + tmpDirPath + SLASH + kakeiboDataCsvFileName;
		createFileIfNotExists(kakeiboDataCsvFilePath);
		return kakeiboDataCsvFilePath;
	}

	public String getPreferenceCsvFilePath() throws SecurityException, IOException {
		String preferenceCsvFilePath = backupDirPath + tmpDirPath + SLASH + PREFERENCE_CSV_FILE_NAME;
		createFileIfNotExists(preferenceCsvFilePath);
		return preferenceCsvFilePath;
	}

	public String getTempDir() {
		return backupDirPath + tmpDirPath;
	}

	public String getFormatStringToDateTimeForCsvFilePath() throws SecurityException, IOException {
		String formatStringToDateTimeForCsvFilePath = backupDirPath + tmpDirPath + SLASH
				+ CSV_DATE_FORMAT_TXT_FILE_NAME;
		createFileIfNotExists(formatStringToDateTimeForCsvFilePath);
		return formatStringToDateTimeForCsvFilePath;
	}

	public Date formatStringToDateTimeForCsv(final String dateStr, final String formatter) {
		if (stringToDateTimeFormat == null) {
			stringToDateTimeFormat = new SimpleDateFormat(formatter);
		}
		Date date = null;
		try {
			date = stringToDateTimeFormat.parse(dateStr);
		} catch (ParseException e) {
			throw new IllegalStateException("IOException###dateStr=" + dateStr + ", formatter=" + formatter,
					e);
		}
		return date;
	}

	public void clearDir() {
		deleteFiles("/sdcard/moneyCalc/backup/");

	}

	public List<String> getBackupFileNameList() {
		File[] backupDirectories = this.getBackupDirectories();
		List<String> backupFileNames = new ArrayList<String>();
		if (backupDirectories != null && backupDirectories.length > 0) {
			for (int i = 0; i < backupDirectories.length; i++) {
				File file = backupDirectories[i];
				String path = file.getPath();
				path = path.replace(backupDirPath, "");
				String[] fragments = path.split("_");
				String fileName = "";
				fileName += fragments[0];
				fileName += "/";
				fileName += fragments[1];
				fileName += "/";
				fileName += fragments[2];
				fileName += " ";
				fileName += fragments[3];
				fileName += ":";
				fileName += fragments[4];
				fileName += ".";
				fileName += fragments[5];

				if (KakeiboUtils.isJapan()) {
					fileName += "のデータ";
				}
				backupFileNames.add(fileName);
			}
		}
		return backupFileNames;
	}

	public File[] getBackupDirectories() {
		File file = new File(backupDirPath + "/");
		File[] listFiles = file.listFiles();
		List<File> fileList = Arrays.asList(listFiles);
		//更新日新しい順に並べる
		Collections.sort(fileList, new Comparator<File>() {

			@Override
			public int compare(final File file1, final File file2) {
				if (file1.lastModified() > file2.lastModified()) {
					return -1;
				} else {
					return 1;
				}

			}
		});
		return listFiles;
	}
}
