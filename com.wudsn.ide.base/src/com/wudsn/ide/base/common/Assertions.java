/**
 * Copyright (C) 2009 - 2021 <a href="https://www.wudsn.com" target="_top">Peter Dell</a>
 *
 * This file is part of WUDSN IDE.
 * 
 * WUDSN IDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * WUDSN IDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with WUDSN IDE.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.wudsn.ide.base.common;

public class Assertions {

	public static void assertFalse(boolean actual) {
		throw new RuntimeException("Actual values is not false");
	}

	public static void assertEquals(Object actual, Object expected) {
		if (actual == expected) {
			return;
		}
		if (actual == null && expected != null) {
			fail("Actual value is null");
		}
		if (actual != null && expected == null) {
			fail("Actual value " + actual + " is not null");
		}
		if (!actual.equals(expected)) {
			fail("Actual value " + actual + " is not equal to expected value " + expected + "");

		}
	}

	public static void fail(String message) {
		throw new RuntimeException(message);
	}

	public static void fail(Throwable throwable) {
		throw new RuntimeException(throwable);
	}

}
