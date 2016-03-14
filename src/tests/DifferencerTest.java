package tests;

import java.util.ArrayList;
import org.junit.Test;
import static org.junit.Assert.*;

import dragonfin.Differencer;
import dragonfin.Differencer.DiffSegment;

public class DifferencerTest
{
	static String[] makeList(int len)
	{
		ArrayList<String> list = new ArrayList<String>();
		for (int i = 0; i < len; i++) {
			list.add(String.format("%d.item.%d", i, i));
		}
		return list.toArray(new String[0]);
	}

	static DiffSegment[] allSegments(String[] a, String[] b)
	{
		Differencer d = new Differencer(a, b);
		ArrayList<DiffSegment> segments = new ArrayList<DiffSegment>();
		DiffSegment seg;
		while ((seg = d.nextSegment()) != null) {
			segments.add(seg);
		}
		return segments.toArray(new DiffSegment[0]);
	}

	String [] empty = new String[0];

	@Test
	public void emptyLists()
	{
		assertEquals(allSegments(empty, empty).length, 0);
	}

	@Test
	public void emptyVersusNonempty()
	{
		String [] a = makeList(1);
		DiffSegment [] ss = allSegments(empty, a);
		assertEquals(ss.length, 1);
		assertEquals(ss[0].type, '+');
		assertEquals(ss[0].offset1, 0);
		assertEquals(ss[0].length1, 0);
		assertEquals(ss[0].offset2, 0);
		assertEquals(ss[0].length2, 1);

		ss = allSegments(a, empty);
		assertEquals(ss.length, 1);
		assertEquals(ss[0].type, '-');
		assertEquals(ss[0].offset1, 0);
		assertEquals(ss[0].length1, 1);
		assertEquals(ss[0].offset2, 0);
		assertEquals(ss[0].length2, 0);
	}
}
