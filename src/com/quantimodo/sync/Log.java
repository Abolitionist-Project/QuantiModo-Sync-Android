package com.quantimodo.sync;

/**
 * Created with IntelliJ IDEA.
 * User: Quint
 * Date: 12-5-13
 * Time: 14:37
 * To change this template use File | Settings | File Templates.
 */
public class Log
{
	public static void i(String log)
	{
		android.util.Log.i("Quantimodo", log);
	}

	public static void e(String log)
	{
		android.util.Log.e("Quantimodo", log);
	}
}
