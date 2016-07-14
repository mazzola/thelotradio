/*
 * Copyright (C) 2014 The Android Open Source Project
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
package com.thelotradio.android;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.thelotradio.android.media.AudioPlayer;

/**
 * An activity that plays media using {@link AudioPlayer}.
 */
public class PlayerActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "TheLotRadio";

    private TextView playerStatus;
    private ImageView logo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        logo = (ImageView) findViewById(R.id.logo)  ;
        playerStatus = (TextView) findViewById(R.id.player_status);
        logo.setOnClickListener(this);
        playerStatus.setOnClickListener(this);
        bindService(new Intent(this, AudioPlaybackService.class), serviceConnection, BIND_AUTO_CREATE);
    }


    @Override
    public void onClick(View view) {
        MediaControllerCompat controller = getSupportMediaController();
        if (controller != null) {
            int state = controller.getPlaybackState().getState();
            if (state == PlaybackStateCompat.STATE_PAUSED) {
                controller.getTransportControls().play();
            } else if (state == PlaybackStateCompat.STATE_PLAYING) {
                controller.getTransportControls().pause();
            }
        }
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            AudioPlaybackService.LocalBinder binder = (AudioPlaybackService.LocalBinder) service;
            try {
                setSupportMediaController(new MediaControllerCompat(PlayerActivity.this,
                        binder.getSessionToken()));
                getSupportMediaController().registerCallback(controllerCallback);
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to create media controller", e);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    private MediaControllerCompat.Callback controllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state) {
            if (state.getState() == PlaybackStateCompat.STATE_PLAYING) {
                playerStatus.setText(getString(R.string.playing));
            } else if (state.getState() == PlaybackStateCompat.STATE_PAUSED) {
                playerStatus.setText(getString(R.string.paused));
            }
        }
    };
}