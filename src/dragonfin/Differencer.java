package dragonfin;

import java.io.*;
import java.util.*;

public class Differencer
{
	String [] lines1;
	String [] lines2;
	int off1;
	int off2;

	public class DiffSegment
	{
		public final char type;
		final int offset;
		public final int length;

		DiffSegment(char type, int offset, int length)
		{
			this.type = type;
			this.offset = offset;
			this.length = length;
		}

		public String getLine(int i) {
			if (type == '-') {
				return lines1[offset+i];
			}
			else {
				return lines2[offset+i];
			}
		}
	}

	public Differencer(String [] lines1, String [] lines2)
	{
		this.lines1 = lines1;
		this.lines2 = lines2;
	}

	public DiffSegment nextSegment()
	{
		if (off1 >= lines1.length) {
			if (off2 >= lines2.length) {
				return null;
			}
			DiffSegment seg = new DiffSegment('+', off2, lines2.length-off2);
			off2 = lines2.length;
			return seg;
		}
		if (off2 >= lines2.length) {
			DiffSegment seg = new DiffSegment('-', off1, lines1.length-off1);
			off1 = lines1.length;
			return seg;
		}

		if (lines1[off1].equals(lines2[off2])) {
			// same lines
			int len = 1;
			while (off1+len < lines1.length && off2+len < lines2.length && lines1[off1+len].equals(lines2[off2+len])) {
				len++;
			}
			DiffSegment seg = new DiffSegment('=', off2, len);
			off1 += len;
			off2 += len;
			return seg;
		}

		// find a line common to both files
		for (int i = 1; off1+i < lines1.length || off2+i < lines2.length; i++) {
			for (int j = 0; j <= i; j++) {
				if (off1+i-j < lines1.length && off2+j < lines2.length
					&& lines1[off1+i-j].equals(lines2[off2+j]))
				{
					if (i==j) {
						// file2 has extra lines
						DiffSegment seg = new DiffSegment('+', off2, j);
						off2 += j;
						return seg;
					}
					else if (j == 0) {
						// file2 is missing lines
						DiffSegment seg = new DiffSegment('-', off1, i);
						off1 += i;
						return seg;
					}
					DiffSegment seg = new DiffSegment('!', off2, j);
					off1 += i-j;
					off2 += j;
					return seg;
				}
			}
		}

		// rest of file has no common lines
		DiffSegment seg = new DiffSegment('!', off2, lines2.length-off2);
		off1 = lines1.length;
		off2 = lines2.length;
		return seg;
	}

	void dump()
	{
		DiffSegment seg;
		while ( (seg = nextSegment()) != null )
		{
			for (int i = 0; i < seg.length; i++) {
				System.out.printf("%c%s\n",
					seg.type,
					seg.getLine(i)
					);
			}
		}
	}

	static String [] readFile(String fileName)
		throws IOException
	{
		BufferedReader in = new BufferedReader(new FileReader(fileName));
		ArrayList<String> list = new ArrayList<String>();
		String s;
		while ( (s=in.readLine()) != null)
		{
			list.add(s);
		}
		in.close();
		return list.toArray(new String[0]);
	}

	public static void main(String [] args) throws Exception
	{
		String [] lines1 = readFile(args[0]);
		String [] lines2 = readFile(args[1]);

		Differencer me = new Differencer(lines1, lines2);
		me.dump();
	}
}
