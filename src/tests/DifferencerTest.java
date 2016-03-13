package tests;

import java.util.ArrayList;
import org.junit.Test;
import static org.junit.Assert.*;

import dragonfin.Differencer;
import dragonfin.Differencer.DiffSegment;

public class DifferencerTest
{
	static ArrayList<DiffSegment> allSegments(Differencer d)
	{
		ArrayList<DiffSegment> segments = new ArrayList<DiffSegment>();
		DiffSegment seg;
		while ((seg = d.nextSegment()) != null) {
			segments.add(seg);
		}
		return segments;
	}

	@Test
	public void emptyLists()
	{
		String [] empty = new String[0];
		Differencer d = new Differencer(empty, empty);
		assertEquals(allSegments(d).size(), 0);
	}
}
