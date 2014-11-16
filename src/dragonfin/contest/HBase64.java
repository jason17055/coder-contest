package dragonfin.contest;

import java.nio.charset.Charset;
import java.io.*;

public class HBase64
{
	static final Charset UTF8 = Charset.forName("UTF-8");
	static final String HTML64_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

	public static String html64(String str)
	{
		byte[] bb = str.getBytes(UTF8);
		return html64(bb);
	}

	public static String html64(byte [] bb)
	{
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < bb.length; i += 3) {
			int a = bb[i] & 255;
			int b = i+1 < bb.length ? bb[i+1] & 255 : 0;
			int c = i+2 < bb.length ? bb[i+2] & 255 : 0;
			int t = (a << 16) | (b << 8) | c;

			sb.append(HTML64_CHARS.charAt((t >> 18)&63));
			sb.append(HTML64_CHARS.charAt((t >> 12)&63));
			if (i+1 < bb.length)
			sb.append(HTML64_CHARS.charAt((t >> 6) &63));
			if (i+2 < bb.length)
			sb.append(HTML64_CHARS.charAt((t >> 0) &63));
		}
		return sb.toString();
	}

	public static void main(String [] args) throws Exception
	{
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String s;
		while ( (s=in.readLine()) != null ) {
			System.out.println(html64(s));
		}
	}
}
