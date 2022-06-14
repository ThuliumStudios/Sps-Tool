package com.sps.util;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Units {
	public static DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.ENGLISH);

	public static String today( ) {
		return dateFormat.format(LocalDateTime.now());
	}

	public static String today(char separator) {
		return DateTimeFormatter.ofPattern("MM" + separator + "dd" + separator + "yyyy", Locale.ENGLISH).format(LocalDateTime.now());
	}

	public static String yesterday( ) {
		return todayMinus(1);
	}

	public static String todayMinus(int days) {
		return dateFormat.format(LocalDateTime.now().minusDays(days));
	}

	public static String formatDate(DateTime date) {
		if (date == null)
			return "";

		DateTime dt = date.withZone(DateTimeZone.UTC);
		return dateFormat.format(LocalDate.of(dt.getYear(), dt.getMonthOfYear(), dt.getDayOfMonth()));
	}
}
