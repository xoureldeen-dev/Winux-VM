/*
Simple DirectMedia Layer
Java source code (C) 2009-2014 Sergii Pylypenko

This software is provided 'as-is', without any express or implied
warranty.  In no event will the authors be held liable for any damages
arising from the use of this software.

Permission is granted to anyone to use this software for any purpose,
including commercial applications, and to alter it and redistribute it
freely, subject to the following restrictions:

1. The origin of this software must not be misrepresented; you must not
   claim that you wrote the original software. If you use this software
   in a product, an acknowledgment in the product documentation would be
   appreciated but is not required. 
2. Altered source versions must be plainly marked as such, and must not be
   misrepresented as being the original software.
3. This notice may not be removed or altered from any source distribution.
*/

package x.org.server;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;


public class RunFromOtherApp extends Activity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		Log.i("SDL", "Run from another app, getCallingActivity() is " +( getCallingActivity() == null ? "null" : "not null" ));

		Intent main = new Intent(this, MainActivity.class);
		main.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		if( getIntent().getScheme() != null && getIntent().getScheme().equals("x11") )
		{
			int port = getIntent().getData().getPort();
			if (port >= 0)
			{
				if (port >= 6000)
					port -= 6000;
				//Globals.CommandLine = Globals.CommandLine + " :" + port;
				main.putExtra(RestartMainActivity.SDL_RESTART_PARAMS, ":" + port);
			}
		}
		startActivity(main);

		new Thread(new Runnable()
		{
			public void run()
			{
				Log.i("SDL", "Waiting for env vars to be set");
				while( System.getenv("DISPLAY") == null || System.getenv("PULSE_SERVER") == null )
				{
					try {
						Thread.sleep(300);
					} catch (InterruptedException e) {}
				}
				Log.i("SDL", "Env vars set, returning result, getCallingActivity() is " + (getCallingActivity() == null ? "null" : "not null"));

				if( getCallingActivity() != null )
				{
					final ComponentName callingActivity = getCallingActivity().clone();
					Log.i("SDL", "Launching calling activity: " + getCallingActivity().toString());
					new Thread(new Runnable()
					{
						public void run()
						{
							try {
								Thread.sleep(500);
							} catch (InterruptedException e) {}
							Intent caller = new Intent();
							caller.setComponent(callingActivity);
							caller.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							Log.i("SDL", "Launching calling activity: " + caller.toString());
							startActivity(caller);
						}
					}).start();
				}

				Intent intent = new Intent(Intent.ACTION_RUN, Uri.parse("x11://run?DISPLAY=" + Uri.encode(System.getenv("DISPLAY")) + "&PULSE_SERVER=" + Uri.encode(System.getenv("PULSE_SERVER"))));
				intent.putExtra("DISPLAY", System.getenv("DISPLAY"));
				intent.putExtra("PULSE_SERVER", System.getenv("PULSE_SERVER"));
				intent.putExtra("run", "export DISPLAY=" + System.getenv("DISPLAY") + " ; export PULSE_SERVER=" + System.getenv("PULSE_SERVER"));
				setResult(Activity.RESULT_OK, intent);
				finish();
			}
		}).start();
	}
}
