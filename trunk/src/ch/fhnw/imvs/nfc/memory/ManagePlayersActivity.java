/*
 * Copyright (c) 2012 Fachhochschule Nordwestschweiz (FHNW)
 * All Rights Reserved. 
 */

package ch.fhnw.imvs.nfc.memory;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;

public class ManagePlayersActivity
    extends ListActivity
{
    private ArrayAdapter<String> playersListAdapter;

    private List<String> playersList;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.set_players);

        playersList = new ArrayList<String>();

        playersListAdapter =
            new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,
                playersList);
        setListAdapter(playersListAdapter);
        registerForContextMenu(getListView());

        Button buttonAddNewPlayer =
            (Button) findViewById(R.id.buttonAddNewPlayer);
        buttonAddNewPlayer.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                showInputDialog("", new Command()
                {
                    @Override
                    public void execute(String input)
                    {
                        if (!(input == null || input.trim().isEmpty()))
                        {
                            playersList.add(input);
                            updateList();
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        try
        {
            // get existing player list form sharedPreferences
            playersList =
                JSONSharedPreferences.loadStringList(this,
                    SharedPreferencesKeys.PREF_NAME,
                    SharedPreferencesKeys.PLAYERS_KEY);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }
        updateList();
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        //save playersList
        JSONSharedPreferences.saveJSONArray(this,
            SharedPreferencesKeys.PREF_NAME, SharedPreferencesKeys.PLAYERS_KEY,
            new JSONArray(playersList));
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
        ContextMenuInfo menuInfo)
    {
        if (v.getId() == android.R.id.list)
        {
            AdapterView.AdapterContextMenuInfo info =
                (AdapterView.AdapterContextMenuInfo) menuInfo;
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.manage_players_activity_context, menu);
            menu.setHeaderTitle(playersList.get(info.position));
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        final AdapterView.AdapterContextMenuInfo info =
            (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId())
        {
        // user touches edit
        case R.id.edit_player:
            showInputDialog(playersList.get(info.position), new Command()
            {
                @Override
                public void execute(String input)
                {
                    if (!(input == null || input.trim().isEmpty()))
                    {
                        playersList.set(info.position, input);
                        updateList();
                    }
                }
            });
            break;
        // user touches delete
        case R.id.delete_player:
            playersList.remove(info.position);
            updateList();
            break;
        }
        return true;
    }

    private void showInputDialog(String existingValue, final Command command)
    {
        LayoutInflater factory =
            LayoutInflater.from(ManagePlayersActivity.this);
        final View textEntryView =
            factory.inflate(R.layout.dialog_add_new_player, null);
        final EditText input =
            (EditText) textEntryView.findViewById(R.id.inputPlayerName);
        input.setText(existingValue);
        new AlertDialog.Builder(ManagePlayersActivity.this)
            .setTitle(R.string.add_new_player)
            .setView(textEntryView)
            .setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int whichButton)
                    {
                        // User clicked OK
                        command.execute(input.getText().toString());
                    }
                })
            .setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int whichButton)
                    {
                        // nothing to do
                    }
                }).show();
    }

    /**
     * update the list with the players
     */
    private void updateList()
    {
        playersListAdapter.clear();
        for (String item : playersList)
        {
            playersListAdapter.add(item);
        }
    }
}
