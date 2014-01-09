package com.quantimodo.sync.su;

/*public class SU
{
	public static Process startProcess()
	{
		try
		{
			Log.i("Starting SU process");
			return Runtime.getRuntime().exec("su");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public static void stopProcess(Process p, boolean waitFor)
	{
		try
		{
			Log.i("Exiting SU process");
			DataOutputStream outputS = new DataOutputStream(p.getOutputStream());

			outputS.writeBytes("exit\n");

			outputS.flush();
			outputS.close();

			if (waitFor)
			{
				p.waitFor();
			}
		}
		catch (Exception ignored)
		{
		}
	}

	public static boolean copyToCache(DataOutputStream outputS, BufferedReader inputS, String filePath, String cachePath)
	{
		try
		{
			outputS.writeBytes("cp \"" + filePath + "\" \"" + cachePath + "\"\n");

			waitForEnd(inputS, outputS);

			return true;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return false;
	}

	private static void waitForEnd(BufferedReader inputS, DataOutputStream outputS) throws IOException
	{
		outputS.writeBytes("echo \"--ENDOFTASK--\"\n");
		outputS.flush();

		String line;

		//noinspection StatementWithEmptyBody
		while ((line = inputS.readLine()) != null && !line.equals("--ENDOFTASK--"))
		{
		}
	}
}*/
