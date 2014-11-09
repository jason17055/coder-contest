package dragonfin.contest.common;

import java.io.*;
import java.util.NoSuchElementException;

/**
 * Converts any of (\*\r|\*\r\n|\*\n) to \n.
 */
public class PlainTextFilter extends FilterInputStream
{
	int state = 0;
	Bytes mem = new Bytes();
	Bytes pending = new Bytes();

	static final int ST_EOF = -1;

	public PlainTextFilter(InputStream in)
	{
		super(in);
	}

	@Override
	public int read()
		throws IOException
	{
		while (pending.isEmpty()) {

			if (state == ST_EOF) { return -1; }

			int c = in.read();
			handle(c);
		}

		return (pending.unshift() & 0xff);
	}

	@Override
	public int read(byte[] buf, int off, int len)
		throws IOException
	{
		while (pending.isEmpty()) {

			if (state == ST_EOF) { return -1; }

			byte [] mbuf = new byte[2048];
			int nread = in.read(mbuf);
			if (nread == -1) {
				handle(-1);
			}
			else {
				for (int i = 0; i < nread; i++) {
					handle(mbuf[i]);
				}
			}
		}

		int rv = 0;
		while (rv < len && !pending.isEmpty()) {

			buf[off+rv] = pending.unshift();
			rv++;
		}

		return rv;
	}

	void handle(int c)
	{
		switch (state) {
		case 0: //initial state
			if (c == -1) {
				pending.add((byte)'\n');
				state = ST_EOF;
			}
			else if (c == ' ' || c == '\t') {
				mem.add((byte)c);
				state = 1;
			}
			else if (c == '\r') {
				pending.add((byte)'\n');
				state = 2;
			}
			else if (c == '\n') {
				pending.add((byte)'\n');
				state = 3;
			}
			else {
				pending.add((byte)c);
			}
			break;

		case 1: //whitespace
			if (c == -1) {
				mem.clear();
				pending.add((byte)'\n');
				state = ST_EOF;
			}
			else if (c == ' ' || c == '\t') {
				mem.add((byte)c);
			}
			else if (c == '\r') {
				mem.clear();
				pending.add((byte)'\n');
				state = 2;
			}
			else if (c == '\n') {
				mem.clear();
				pending.add((byte)'\n');
				state = 3;
			}
			else {
				pending.addAll(mem);
				mem.clear();
				pending.add((byte)c);
				state = 0;
			}
			break;
		case 2:
			if (c == -1) {
				state = ST_EOF;
			}
			else if (c == ' ' || c == '\t') {
				mem.add((byte)c);
				state = 1;
			}
			else if (c == '\r') {
				pending.add((byte)'\n');
				state = 2;
			}
			else if (c == '\n') {
				state = 3;
			}
			else {
				pending.add((byte)c);
				state = 0;
			}
			break;

		case 3:
			if (c == -1) {
				state = ST_EOF;
			}
			else if (c == ' ' || c == '\t') {
				mem.add((byte)c);
				state = 1;
			}
			else if (c == '\r') {
				pending.add((byte)'\n');
				state = 2;
			}
			else if (c == '\n') {
				pending.add((byte)'\n');
				state = 3;
			}
			else {
				pending.add((byte)c);
				state = 0;
			}
			break;

		case ST_EOF:
			break;

		}
	}

	static class Bytes
	{
		byte [] buf = new byte[1024];
		int off = 0;
		int len = 0;

		public void add(byte b)
		{
			if (off+len+1 >= buf.length) {
				int nsize = off < buf.length/4 ? buf.length*2 : buf.length;
				byte [] nbuf = new byte[nsize];
				System.arraycopy(buf, off, nbuf, 0, len);
				buf = nbuf;
				off = 0;
			}

			buf[off+len] = b;
			len++;
		}

		public void addAll(Bytes bb)
		{
			for (int i = 0; i < bb.len; i++) {
				add(bb.buf[bb.off+i]);
			}
		}

		public void clear()
		{
			off = 0;
			len = 0;
		}

		public byte unshift()
		{
			if (len == 0) {
				throw new NoSuchElementException();
			}
			len--;
			return buf[off++];
		}

		public int length()
		{
			return len;
		}

		public boolean isEmpty()
		{
			return len == 0;
		}
	}

	public static void main(String [] args) throws Exception
	{
		PlainTextFilter f = new PlainTextFilter(System.in);
		//int c;
		//while ( (c=f.read()) != -1 ) {
		//	System.out.write(c);
		//}

		int nread;
		byte [] buf = new byte[100];
		while ( (nread = f.read(buf)) != -1 ) {
			System.out.write(buf, 0, nread);
		}
	}
}
