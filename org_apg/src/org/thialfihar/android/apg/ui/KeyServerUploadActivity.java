/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2011 Senecaso
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.thialfihar.android.apg.ui;

import org.thialfihar.android.apg.Constants;
import org.thialfihar.android.apg.R;
import org.thialfihar.android.apg.helper.Preferences;
import org.thialfihar.android.apg.service.ApgIntentServiceHandler;
import org.thialfihar.android.apg.service.ApgIntentService;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

/**
 * gpg --send-key activity
 * 
 * Sends the selected public key to a key server
 */
public class KeyServerUploadActivity extends SherlockFragmentActivity {

    // Not used in sourcode, but listed in AndroidManifest!
    public static final String ACTION_EXPORT_KEY_TO_SERVER = Constants.INTENT_PREFIX
            + "EXPORT_KEY_TO_SERVER";

    public static final String EXTRA_KEYRING_ROW_ID = "keyId";

    private Button export;
    private Spinner keyServer;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

        case android.R.id.home:
            // app icon in Action Bar clicked; go home
            Intent intent = new Intent(this, KeyListPublicActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;

        default:
            break;

        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.key_server_export_layout);

        export = (Button) findViewById(R.id.btn_export_to_server);
        keyServer = (Spinner) findViewById(R.id.keyServer);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, Preferences.getPreferences(this)
                        .getKeyServers());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        keyServer.setAdapter(adapter);
        if (adapter.getCount() > 0) {
            keyServer.setSelection(0);
        } else {
            export.setEnabled(false);
        }

        export.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadKey();
            }
        });
    }

    private void uploadKey() {
        // Send all information needed to service to upload key in other thread
        Intent intent = new Intent(this, ApgIntentService.class);

        intent.putExtra(ApgIntentService.EXTRA_ACTION, ApgIntentService.ACTION_UPLOAD_KEY);

        // fill values for this action
        Bundle data = new Bundle();

        int keyRingId = getIntent().getIntExtra(EXTRA_KEYRING_ROW_ID, -1);
        data.putInt(ApgIntentService.UPLOAD_KEY_KEYRING_ROW_ID, keyRingId);

        String server = (String) keyServer.getSelectedItem();
        data.putString(ApgIntentService.UPLOAD_KEY_SERVER, server);

        intent.putExtra(ApgIntentService.EXTRA_DATA, data);

        // Message is received after uploading is done in ApgService
        ApgIntentServiceHandler saveHandler = new ApgIntentServiceHandler(this, R.string.progress_exporting,
                ProgressDialog.STYLE_HORIZONTAL) {
            public void handleMessage(Message message) {
                // handle messages by standard ApgHandler first
                super.handleMessage(message);

                if (message.arg1 == ApgIntentServiceHandler.MESSAGE_OKAY) {

                    Toast.makeText(KeyServerUploadActivity.this, R.string.keySendSuccess,
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
            };
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(ApgIntentService.EXTRA_MESSENGER, messenger);

        // show progress dialog
        saveHandler.showProgressDialog(this);

        // start service with intent
        startService(intent);
    }
}
