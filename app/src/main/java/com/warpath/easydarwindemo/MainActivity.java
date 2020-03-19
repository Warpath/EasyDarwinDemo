package com.warpath.easydarwindemo;

import android.arch.lifecycle.Observer;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import io.reactivex.Single;
import io.reactivex.functions.Consumer;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.easydarwin.push.MediaStream;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_MEDIA_PROJECTION = 1000;
    public static final String HOST = "192.168.1.4";
    private MediaStream mediaStream;

    private Single<MediaStream> getMediaStream() {
        Single<MediaStream> single = RxHelper.single(MediaStream.getBindedMediaStream(this, this), mediaStream);
        if (mediaStream == null) {
            return single.doOnSuccess(new Consumer<MediaStream>() {
                @Override
                public void accept(MediaStream ms) throws Exception {
                    mediaStream = ms;
                }
            });
        } else {
            return single;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //启动服务
        Intent intent = new Intent(this, MediaStream.class);
        startService(intent);

        getMediaStream().subscribe(new Consumer<MediaStream>() {
            @Override
            public void accept(final MediaStream ms) throws Exception {
                final TextView pushingStateText = findViewById(R.id.pushing_state);
                final Button pushingDesktop = findViewById(R.id.pushing_desktop);
                ms.observePushingState(MainActivity.this, new Observer<MediaStream.PushingState>() {
                    @Override
                    public void onChanged(@Nullable MediaStream.PushingState pushingState) {
                        if (pushingState.screenPushing) {
                            pushingStateText.setText("屏幕推送");

                            if (ms.isScreenPushing()) {
                                pushingDesktop.setText("取消推送");
                            } else {
                                pushingDesktop.setText("推送屏幕");
                            }
                            pushingDesktop.setEnabled(true);
                        }

                        pushingStateText.append(":\t" + pushingState.msg);
                        if (pushingState.state > 0) {
                            pushingStateText.append(pushingState.url);
                            pushingStateText.append("\n");
                            if ("avc".equals(pushingState.videoCodec)) {
                                pushingStateText.append("视频编码方式：" + "H264硬编码");
                            }else if ("hevc".equals(pushingState.videoCodec)) {
                                pushingStateText.append("视频编码方式："  + "H265硬编码");
                            }else if ("x264".equals(pushingState.videoCodec)) {
                                pushingStateText.append("视频编码方式："  + "x264");
                            }
                        }
                    }
                });

            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                Toast.makeText(MainActivity.this, "创建服务出错!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void onPushScreen(final View view) {
        getMediaStream().subscribe(new Consumer<MediaStream>() {
            @Override
            public void accept(MediaStream mediaStream) throws Exception {
                if (mediaStream.isScreenPushing()) {//正在推送，那取消推送
                    mediaStream.stopPushScreen();
                } else {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) { //lollipop 以前版本不支持
                        return;
                    }
                    MediaProjectionManager mMpMngr = (MediaProjectionManager)getApplicationContext().getSystemService(MEDIA_PROJECTION_SERVICE);
                    startActivityForResult(mMpMngr.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION);
                    //防止多次点击
                    view.setEnabled(false);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, final int resultCode, @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            getMediaStream().subscribe(new Consumer<MediaStream>() {
                @Override
                public void accept(MediaStream mediaStream) throws Exception {
                    mediaStream.pushScreen(resultCode, data, HOST, "554", "screen111");
                }
            });
        }
    }
}
