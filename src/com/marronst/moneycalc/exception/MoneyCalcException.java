package com.marronst.moneycalc.exception;

public class MoneyCalcException extends RuntimeException {

	private static final long serialVersionUID = 5421702262922975548L;

	public MoneyCalcException() {
		super();
	}

	public MoneyCalcException(final String detailMessage, final Throwable throwable) {
		super(detailMessage, throwable);
	}

	public MoneyCalcException(final String detailMessage) {
		super(detailMessage);
	}

	public MoneyCalcException(final Throwable throwable) {
		super(throwable);
	}

}
