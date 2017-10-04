package edu.berkeley.cs.jqf.examples.wise.driver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Driver {

	private static BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

	public static String readString() {
		try {
			return br.readLine();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static int readInteger() {
		return Integer.parseInt(readString());
	}

	public static char readCharacter() {
		return readString().charAt(0);
	}

	public static void exit() {
		
	}
}