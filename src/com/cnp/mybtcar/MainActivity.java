package com.cnp.mybtcar;



import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class MainActivity extends Activity implements SensorEventListener  {

	    private MjpegView mv;
	   	private SensorManager mSensorManager;
	   	private Sensor mSensor; 
	   	
	   	private float mX, mY;
	   	private boolean misStatr = false;

	   	private int mbpX, mbpY;
	   	
	   	
	   	private KalmanFilter mKalmanAccX;
	   	private KalmanFilter mKalmanAccY;
	   	private RelativeLayout  controller_view;
        private Button SetPosButton;
        
        private BluetoothService mBTservice;
        private final Handler mHandler = new Handler() {

    		@Override
    		public void handleMessage(Message msg) {
    			super.handleMessage(msg);
    		}
    		
    	};

    	private static final int REQUEST_CONNECT_DEVICE = 1;
    	private static final int REQUEST_ENABLE_BT = 2;
    	
       public void onCreate(Bundle icicle) {

       super.onCreate(icicle);

       //sample public cam

       FrameLayout main_view = new FrameLayout(this);

       String URL = "http://175.199.55.73:8080/video";

       requestWindowFeature(Window.FEATURE_NO_TITLE);

       getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,

                            WindowManager.LayoutParams.FLAG_FULLSCREEN);

      
       //ImjpegViewListener listener;
       mv = new MjpegView(this, new ImjpegViewListener() {
			
			@Override
			public void sucess() {
				// TODO Auto-generated method stub
				//Log.d("MV", "Sucess");
			}
			
			@Override
			public void hasBitmap(Bitmap bm) {
				// TODO Auto-generated method stub
				//Log.d("MV", "hasBitmap");
			}
			
			@Override
			public void error() {
				// TODO Auto-generated method stub
				//Log.d("MV", "error");
			}
		});

       
       RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
       params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);

       RelativeLayout.LayoutParams params1 = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.FILL_PARENT, RelativeLayout.LayoutParams.FILL_PARENT);

       
       //       params.addRule(RelativeLayout.ALIGN_PARENT_LEFT, RelativeLayout.TRUE);
//       Button button1;
//       button1.setLayoutParams(params);

//       params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
//       params.addRule(RelativeLayout.RIGHT_OF, button1.getId());
//       Button button2;
//       button2.setLayoutParams(params);
       
       controller_view = new RelativeLayout (this);

       controller_view.setLayoutParams(params1);
       
       
       SetPosButton = new Button(this);
     
       SetPosButton.setLayoutParams(params);
       //SetPosButton.setWidth(100);
       
       SetPosButton.setOnClickListener(new OnClickListener() {
		
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(misStatr) {
					misStatr = false;
					SetPosButton.setText("시작");
					mv.stopPlayback();
					controller_view.setScrollX(mbpX);
					controller_view.setScrollY(mbpY);
				} else {
					misStatr = true;
					SetPosButton.setText("정지");
					//mv.startPlayback();
				}
			}
       });


       SetPosButton.setWidth(300);
       SetPosButton.setText("시작");

       controller_view.addView(SetPosButton);       
       
       main_view.addView(mv);
       main_view.addView(controller_view);

       setContentView(main_view);
       
       mbpX = controller_view.getScrollX();
       mbpY = controller_view.getScrollY();

       mv.setSource(URL);

       mv.setDisplayMode(MjpegView.SIZE_BEST_FIT);
       
       //mv.stopPlayback();

       //mv.showFps(true);
       
       //칼만필터 초기화
   		mKalmanAccX = new KalmanFilter(0.0f);
   		mKalmanAccY = new KalmanFilter(0.0f);
   		
   		//기울기 센서 등록
   		mSensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);		
   		mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
   		mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_GAME);       

   		
   		//controller_view.scrollTo(-200, -200);
   		if( mBTservice == null )
   		{
   			mBTservice = new BluetoothService(this, mHandler);
   		}
   		
   		if(!mBTservice.getDeviceState()) {
   			Log.d("MV", "블루투스 지원 불가");
   		}
   		
   		mBTservice.enableBluetooth();
   		
       }

      

       public void onPause() {

               super.onPause();

               mv.stopPlayback();

       }



	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		float x = event.values[0];
		float y = event.values[1];
		
    //칼만필터를 적용한다
		float filteredX = (float) mKalmanAccX.update(x);
		float filteredY = (float) mKalmanAccY.update(y);
                
                //이주석을 풀면 칼만필터를 사용하지 않는다
    //filteredX = x;
		//filteredY = y;
		
                //부모 레이아웃을 스크롤시켜 마치 뷰객체(오브젝트)가 움직이는것처럼 보이게 한다
                //저장해둔 예전값과 현재값의 차를 넣어 변화를 감지한다
                //여기에 100을 곱하는것은 차의 숫자가 워낙 작아 움직임이 보이지 않기 때문이다.
                //즉, 스피드라고도 보면 된다ㅎㅎ 더 큰숫자를 넣으면 더 빠르게 움직인다.
		if(misStatr) {
			controller_view.scrollBy((int)((mX - filteredX) * 100), (int)((mY - filteredY) * 100));
			if(mBTservice.getState()==3)
			{
				String t = "A";
				mBTservice.write(t.getBytes());
			}
		}
	
                //예전값을 저장한다
		mX = filteredX;
		mY = filteredY;
		
	}
	

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("MV", "onActivityResult " + resultCode);
        
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
            	mBTservice.getDeviceInfo(data);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
            	mBTservice.scanDevice();
            } else {

                Log.d("MV", "Bluetooth is not enabled");
            }
            break;
        }
	}	

}
