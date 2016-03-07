package dragonfin.contest.common;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Logger;
import java.util.regex.*;

public class HtmlDateFormat
{
	private static final Logger log = Logger.getLogger(HtmlDateFormat.class.getName());

	private HtmlDateFormat() {}  //prevent instantiation

	static Pattern DATETIME_PATTERN1 = Pattern.compile("^[0-9]{2,}-[0-9]{2}-[0-9]{2}T[0-9][0-9]:[0-9][0-9]$");
	static final String DATETIME_FMT1 = "yyyy-MM-dd'T'HH:mm";

	static final String DATETIME_FMT2 = "yyyy-MM-dd'T'HH:mm:ss";

	public static Date parseDateTime(String s, String localTimeZone)
	{
		if (s == null || s.equals("")) {
			return null;
		}

		try {

		Matcher m;
		m = DATETIME_PATTERN1.matcher(s);
		if (m.matches()) {
			SimpleDateFormat f = new SimpleDateFormat(DATETIME_FMT1);
			f.setTimeZone(TimeZone.getTimeZone(localTimeZone));
			return f.parse(s);
		}

		SimpleDateFormat f = new SimpleDateFormat(DATETIME_FMT2);
		f.setTimeZone(TimeZone.getTimeZone(localTimeZone));
		return f.parse(s);

		}
		catch (ParseException e) {
			log.info("Date parsing exception: " + e.getMessage());
			return null;
		}
	}

	public static String formatDateTime(Date d, String localTimeZone)
	{
		if (d == null) {
			return null;
		}

		SimpleDateFormat formatter = new SimpleDateFormat(DATETIME_FMT2);
		formatter.setTimeZone(TimeZone.getTimeZone(localTimeZone));
		return formatter.format(d);
	}
}
