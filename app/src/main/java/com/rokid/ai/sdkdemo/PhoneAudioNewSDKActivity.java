package com.rokid.ai.sdkdemo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.rokid.ai.audioai.AudioAiConfig;
import com.rokid.ai.audioai.AudioAiService;
import com.rokid.ai.audioai.aidl.IRokidAudioAiListener;
import com.rokid.ai.audioai.aidl.IRokidAudioAiService;
import com.rokid.ai.audioai.aidl.ServerConfig;
import com.rokid.ai.audioai.socket.base.ClientSocketManager;
import com.rokid.ai.audioai.socket.business.preprocess.IReceiverPcmListener;
import com.rokid.ai.audioai.socket.business.preprocess.PcmClientManager;
import com.rokid.ai.audioai.socket.business.record.RecordClientManager;
import com.rokid.ai.audioai.util.Logger;
import com.rokid.ai.sdkdemo.presenter.AsrControlPresenter;
import com.rokid.ai.sdkdemo.presenter.AsrControlPresenterImpl;
import com.rokid.ai.sdkdemo.util.PerssionManager;
import com.rokid.ai.sdkdemo.view.IAsrUiView;

import java.io.File;

public class PhoneAudioNewSDKActivity extends AppCompatActivity {

    private IRokidAudioAiService mAudioAiService;
    private final static String CONFIG_FILE_NAME = "Rokid_Ai_SDK_Config.txt";
    public static final String IGNORE_SUPPRESS_AUDIO_VOLUME = "Ignore_Suppress_Audio_Volume";
    private static final String TAG = PhoneAudioNewSDKActivity.class.getSimpleName();
    private boolean mIgnoreSuppressAudioVolume = false;
    private AsrControlPresenter mAsrControlPresenter;
    private Context mContext = null;
    private int mSingleDoubleStatus;


    private TextView mAsrStateTV;
    private TextView mAsrResultTv;
    private TextView mNLPTv;
    private TextView mActionTv;
    private TextView mActivationTv;
    private TextView mErrorTv;
    private TextView mPcmTv;

    private TextView mTotalStateTv;
    private TextView mFileStateTv;


    private int mActivationCount;
    private long mPcmCount;
    private int mBadPcmCount;

    private File[] testFileList;
    private int testFileIndex = 0;
    private int mTestCode;
    private boolean mSocketConnect;

    private boolean mCanSendPcm;

    private boolean isRecording;

    private boolean isBindService;

    private ServiceConnection mAiServiceConnection;

    private RecordClientManager mRecordClientManager;

    private PcmClientManager mPcmSocketManager;

    private Intent mServiceIntent;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_audio_new_sdk);
        mContext = this;

        initView();

//        bindService(new Intent(mContext, AudioAiService.class), mAiServiceConnection, BIND_AUTO_CREATE);
//        mAsrControlPresenter = new AsrControlPresenterImpl(mContext, mAsrUiView);


        requestPermission();

        mServiceIntent = AudioAiConfig.getIndependentIntent(this);
        mRecordClientManager = new RecordClientManager();
        mPcmSocketManager = new PcmClientManager();
    }


    public void initView() {


        mAsrStateTV = findViewById(R.id.main_ast_state_tv);
        mAsrResultTv = findViewById(R.id.main_ast_result_tv);
        mNLPTv = findViewById(R.id.main_ast_npl_tv);
        mActionTv = findViewById(R.id.main_ast_action_tv);
        mActivationTv = findViewById(R.id.main_ast_activation_tv);
        mErrorTv = findViewById(R.id.main_ast_error_tv);
        mPcmTv = findViewById(R.id.main_ast_pcm_tv);

        mTotalStateTv = findViewById(R.id.main_total_state_tv);
        mFileStateTv = findViewById(R.id.main_file_state_tv);

        ((TextView) findViewById(R.id.main_test_tv)).setText("Other : " + mTestCode);
    }



    private void showPcmData(final long len, byte[] bytes) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mPcmCount = mPcmCount + 1;
                if (len < 100) {
                    mBadPcmCount = mBadPcmCount + 1;
                }

                if (mPcmTv != null) {
                    mPcmTv.setText("pcm流数：" + mPcmCount + "         数据Len：" + len
                            + "       错误数据：" + mBadPcmCount);
                }
            }
        });
    }


    public void requestPermission() {
        PerssionManager.requestPerrion(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        PerssionManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void createConnection() {
        mAiServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                isBindService = true;
                Logger.d(TAG, "the onServiceConenct is called");
                if (service != null) {
                    Logger.d(TAG, "the onServiceConenct is called111");
                    mAudioAiService = IRokidAudioAiService.Stub.asInterface(service);
                    try {
                        Logger.d(TAG, "the onServiceConenct is called222");
                        mAudioAiService.srartAudioAiServer(getServiceConfig(mSingleDoubleStatus), mAudioAiListener);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mAudioAiService = null;
            }
        };
    }

    private IRokidAudioAiListener mAudioAiListener = new IRokidAudioAiListener.Stub() {

        @Override
        public void onIntermediateSlice(String asr) throws RemoteException {
            String s = "onIntermediateSlice(): asr = " + asr;
            Logger.d(TAG, s);
            if (mAsrControlPresenter != null) {
                mAsrControlPresenter.showAsrResultText(asr, true);
            }

        }

        @Override
        public void onIntermediateEntire(String asr) throws RemoteException {

            final String mTemp = asr;

            Logger.d(TAG, "onIntermediateEntire(): asr = " + asr);

            if (mAsrControlPresenter != null) {
                mAsrControlPresenter.showAsrResultText(asr, true);
            }
        }

        @Override
        public void onCompleteNlp(String nlp, String action) throws RemoteException {

            String s = "onCompleteNlp(): nlp = " + nlp + " action = " + action + "\n\r";
            Logger.d(TAG, s);

            if (mAsrControlPresenter != null) {
                mAsrControlPresenter.showAsrNlpText(nlp, action);
            }

        }

        @Override
        public void onVoiceEvent(int event, float sl, float energy) throws RemoteException {


            String s = "onVoiceEvent(): event = " + event + ", sl = " + sl + ", energy = " + energy + "\n\r";
            Logger.d(TAG, s);
            if (mAsrControlPresenter != null) {
                mAsrControlPresenter.showAsrEvent(event, sl, energy);
            }
        }

        @Override
        public void onRecognizeError(int errorCode) throws RemoteException {

            String s = "onRecognizeError(): errorCode = " + errorCode + "\n\r";
            Logger.d(TAG, s);
            if (mAsrControlPresenter != null) {
                mAsrControlPresenter.showRecognizeError(errorCode);
            }

        }

        @Override
        public void onServerSocketCreate(String ip, int port) throws RemoteException {

            Logger.d(TAG,"onServerSocketCreate(): ip = " + ip + ", port = " + port);
            if (mRecordClientManager != null) {
                mRecordClientManager.startSocket(ip, port, mConnnectListener);
            }

            new Thread(new Runnable() {
                @Override
                public void run() {

                    int bufferSize = 0;
                    if(mSingleDoubleStatus == 0) {
                        bufferSize = AudioRecord.getMinBufferSize(FREQUENCY, CHANNEL, AudioFormat.ENCODING_PCM_16BIT);
                    } else if(mSingleDoubleStatus == 1) {
                        bufferSize = AudioRecord.getMinBufferSize(FREQUENCY, CHANNELDOUBLE, AudioFormat.ENCODING_PCM_16BIT);
                    }
                    if(mAudioRecord == null) {

                        if(mSingleDoubleStatus == 0) {
                            mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC
                                    , FREQUENCY
                                    , CHANNEL
                                    , ENCODING_BIT
                                    , bufferSize);
                        } else if(mSingleDoubleStatus == 1) {
                            mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC
                                    , FREQUENCY
                                    , CHANNELDOUBLE
                                    , ENCODING_BIT
                                    , bufferSize);
                        }

                        isRecording = true;
                        int length = 2560;
                        byte[] buffer = new byte[length];
                        mAudioRecord.startRecording();
                        Logger.d(TAG, "the pcm data is" + bufferSize);
                        int bufferReadResult;
                        while (isRecording) {
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            bufferReadResult = mAudioRecord.read(buffer, 0, length);
                            if (mCanSendPcm) {
//                                Logger.d(TAG, "sendRecordData data is" + bufferSize);
                                if (mRecordClientManager != null) {
                                    mRecordClientManager.sendRecordData(buffer);
                                }
                            }
                        }

                    }

                }
            }).start();
        }

        @Override
        public void onPcmServerPrepared() throws RemoteException {
            Logger.d(TAG,"onPcmServerPrepared(): called");
            if (mPcmSocketManager != null) {
                mPcmSocketManager.startSocket(null, mPcmReceiver);
            }
        }

    };

    private IReceiverPcmListener mPcmReceiver = new IReceiverPcmListener() {
        @Override
        public void onPcmReceive(int length, byte[] data) {
            String s = "onPcmReceive(): len = " + length + "\n\r";
//            Logger.d(TAG, s);
            showPcmData(length, data);
        }
    };

    private ServerConfig getServiceConfig(int status) {

        ServerConfig config = null;
        if(status == 0) {
            config = new ServerConfig(
                    "workdir_asr_cn", "phonetest", true);
        } else if(status == 1) {
            config = new ServerConfig(
                    "workdir_asr_cn", "phonetest2", true);
        }

        config.setLogConfig(Logger.LEVEL_D, true, true);
        String key = "BBF450D04CC14DBD88E960CF5D4DD697";
        String secret = "29F84556B84441FC885300CD6A85CA70";
        String deviceTypeId = "3301A6600C6D44ADA27A5E58F5838E02";
        String deviceId = "57E741770A1241CP";
        String ignoreMoveConfig = "false";

        config.setKey(key).setSecret(secret).setDeviceTypeId(deviceTypeId).setDeviceId(deviceId);

        if ("true".equals(ignoreMoveConfig)) {
            // 忽略移动文件
            config.setIgnoreMoveConfig(true);
        }
        if (mIgnoreSuppressAudioVolume) {
            config.setIgnoreSuppressAudioVolume(true);
        }

        return config;
    }

    private IAsrUiView mAsrUiView = new IAsrUiView() {

        @Override
        public void showAsrResultText(final String str) {
            Logger.d(TAG, "onVoiceEvent(): showAsrResultText = " + str);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mAsrResultTv != null) {
                        mAsrResultTv.setText(str);
                    }
                }
            });
        }

        @Override
        public void showAsrStateText(final String str) {
            Logger.d(TAG, "onVoiceEvent(): event = " + str);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mAsrStateTV != null) {
                        mAsrStateTV.setText(str);
                    }
                }
            });
        }

        @Override
        public void showAsrActivation(final boolean isActivation) {
            Logger.d(TAG, "onVoiceEvent(): event = " + isActivation);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mActivationCount = mActivationCount + 1;
                    if (mActivationTv != null) {
                        mActivationTv.setText("激活访问次数：" + mActivationCount);
                    }
                    if (isActivation) {
                        Toast.makeText(mContext, "激活了", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(mContext, "拒绝了", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        @Override
        public void showAsrNlpText(final String nlp, final String action) {
            Logger.d(TAG, "onVoiceEvent(): event = " + nlp + "action is" + action);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mNLPTv != null) {
                        mNLPTv.setText("NLP：" + nlp);
                    }
                    if (mActionTv != null) {
                        mActionTv.setText("Action：" + action);
                    }
                }
            });
        }

        @Override
        public void showRecognizeError(final String errorType) {
            Logger.d(TAG, "onVoiceEvent(): event = " + errorType);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mErrorTv != null) {
                        mErrorTv.setBackgroundColor(getResources().getColor(R.color.colorRed));
                        mErrorTv.setText("errorType：" + errorType);

                        mErrorTv.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (mErrorTv != null) {
                                    mErrorTv.setBackgroundColor(getResources().getColor(R.color.colorWhite));
                                }
                            }
                        }, 2000);
                    }
                }
            });
        }
    };

    private static final int FREQUENCY = 16000;
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private static final int CHANNELDOUBLE = AudioFormat.CHANNEL_IN_STEREO;
    private static final int ENCODING_BIT = AudioFormat.ENCODING_PCM_16BIT;
    private AudioRecord mAudioRecord;


    private ClientSocketManager.IConnnectListener mConnnectListener = new ClientSocketManager.IConnnectListener() {
        @Override
        public void onConnectSuccess(ClientSocketManager socketManager) {
            mCanSendPcm = true;
            Logger.d(TAG,"IConnnectListener(): onConnectSuccess");

        }

        @Override
        public void onConnectFailed(ClientSocketManager socketManager) {
            mCanSendPcm = false;
            Logger.d(TAG,"IConnnectListener(): onConnectFailed");
        }
    };

    @Override
    protected void onDestroy() {
        isRecording = false;
        mCanSendPcm = false;
        mContext = null;

        mAsrUiView = null;

        if (mAsrControlPresenter != null) {
            mAsrControlPresenter.releaseMediaPlay();
            mAsrControlPresenter = null;
        }

        Logger.d(TAG, "onDestroy(): is called");
        if (mAudioRecord != null) {
            mAudioRecord.stop();
        }

        try {
            if (isBindService) {
                unbindService(mAiServiceConnection);
            }
//            stopService(new Intent(mContext, AudioAiService.class));
        } catch (Exception e) {
            e.printStackTrace();
        }

        mPcmSocketManager.onDestroy();
        mPcmSocketManager = null;

        mRecordClientManager.onDestroy();
        mRecordClientManager = null;

        super.onDestroy();
    }

    public void onClick(View view) {

        Logger.d(TAG, "the Phone AudioNewSDKActivity called");
        switch (view.getId()) {
            case R.id.btn_start_signle_phone_audio_new_sdk:
                mSingleDoubleStatus = 0;
                createConnection();
                Logger.d(TAG, "the Phone AudioNewSDKActivity called11111");
                bindService(mServiceIntent, mAiServiceConnection, BIND_AUTO_CREATE);
                mAsrControlPresenter = new AsrControlPresenterImpl(mContext, mAsrUiView);
                findViewById(R.id.btn_start_signle_phone_audio_new_sdk).setClickable(false);
                findViewById(R.id.btn_start_doubel_phone_audio_new_sdk).setVisibility(View.GONE);
                break;
            case R.id.btn_start_doubel_phone_audio_new_sdk:
                mSingleDoubleStatus = 1;
                createConnection();
                Logger.d(TAG, "the Phone AudioNewSDKActivity calledw2222");
                bindService(mServiceIntent, mAiServiceConnection, BIND_AUTO_CREATE);
                mAsrControlPresenter = new AsrControlPresenterImpl(mContext, mAsrUiView);
                findViewById(R.id.btn_start_doubel_phone_audio_new_sdk).setClickable(false);
                findViewById(R.id.btn_start_signle_phone_audio_new_sdk).setVisibility(View.GONE);
                break;
        }

    }

}