package org.example.search;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NorphoneTest {

	@Test
	public void testNorphoneEncoding() throws Exception {
		assertEquals(Norphone.encode("P�l"), Norphone.encode("Paal"));
		assertEquals(Norphone.encode("P�l"), Norphone.encode("Paul"));
	}
	
}
