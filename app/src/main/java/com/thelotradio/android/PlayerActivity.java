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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.thelotradio.android.media.AudioPlayer;

/**
 * An activity that plays media using {@link AudioPlayer}.
 */
public class PlayerActivity extends Activity implements View.OnClickListener {

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

        startService(new Intent(this, AudioPlaybackService.class));
    }


    @Override
    public void onClick(View view) {
        Intent pauseIntent = new Intent(this, AudioPlaybackService.class);
        pauseIntent.setAction(AudioPlaybackService.ACTION_PAUSE);
        startService(pauseIntent);
        playerStatus.setText(R.string.paused);
    }
}