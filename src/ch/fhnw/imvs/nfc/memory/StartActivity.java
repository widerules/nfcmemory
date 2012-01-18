/*
 * Copyright (c) 2012 Fachhochschule Nordwestschweiz (FHNW)
 * All Rights Reserved. 
 */

package ch.fhnw.imvs.nfc.memory;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

public class StartActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start);

        Button buttonManagePlayers = (Button) findViewById(R.id.manage_players);
        buttonManagePlayers.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startActivity(new Intent(StartActivity.this,
                    ManagePlayersActivity.class));
            }
        });

        Button buttonManageCards = (Button) findViewById(R.id.manage_cards);
        buttonManageCards.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startActivity(new Intent(StartActivity.this,
                    ManageCardsActivity.class));
            }
        });

        Button buttonStartGame = (Button) findViewById(R.id.start_game);
        buttonStartGame.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startActivity(new Intent(StartActivity.this, GameActivity.class));
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.start_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
        case R.id.settings:
            Intent settingsActivity =
                new Intent(getBaseContext(), Preferences.class);
            startActivity(settingsActivity);
            return true;
        case R.id.faq:
            WebView wv = new WebView(getBaseContext());
            wv.loadData(getString(R.string.faq), "text/html", "utf-8");
            wv.setBackgroundColor(Color.WHITE);
            wv.getSettings().setDefaultTextEncodingName("utf-8");
            wv.setWebViewClient(new WebViewClient()
            {
                public boolean shouldOverrideUrlLoading(WebView view, String url)
                {
                    if (url != null && (url.startsWith("http://")
                        || url.startsWith("https://")))
                    {
                        StartActivity.this.startActivity(
                            new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                        return true;
                    }
                    else
                    {
                        return false;
                    }
                }
            });
            new AlertDialog.Builder(this).setTitle(R.string.faq_title)
                .setView(wv).setPositiveButton(android.R.string.ok, null)
                .show();
        default:
            return super.onOptionsItemSelected(item);

        }

    }

}
