package com.marronst.moneycalc.dto;

public class KakeiboTotalDto {
	public Integer categoryId;//カテゴリId
	public String title;
	public long expensePrice;//支出
	public long incomePrice;//収入
	public boolean isDispIncomePrice;
	public int incomeFlg;
	public boolean isTotalRow;
	public long budgetPrice;//予算
	public long carryOverPrice;//先月持越し
	public long remainingValuePrice;//残高
	public int remainingValueMeterOnPixel;
	public int remainingValueMeterOffPixel;
	public Integer titleTextSize;
	public boolean isSetttingFilter;

	@Override
	public String toString() {
		return "KakeiboTotalDto [budgetPrice=" + budgetPrice + ", carryOverPrice=" + carryOverPrice
				+ ", categoryId=" + categoryId + ", expensePrice=" + expensePrice + ", incomeFlg="
				+ incomeFlg + ", incomePrice=" + incomePrice + ", isDispIncomePrice=" + isDispIncomePrice
				+ ", isSetttingFilter=" + isSetttingFilter + ", isTotalRow=" + isTotalRow
				+ ", remainingValueMeterOffPixel=" + remainingValueMeterOffPixel
				+ ", remainingValueMeterOnPixel=" + remainingValueMeterOnPixel + ", remainingValuePrice="
				+ remainingValuePrice + ", title=" + title + ", titleTextSize=" + titleTextSize + "]";
	}

}