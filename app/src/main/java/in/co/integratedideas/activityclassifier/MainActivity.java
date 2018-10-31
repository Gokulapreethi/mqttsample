package in.co.integratedideas.activityclassifier;

import android.app.Activity;
import android.app.ActivityGroup;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.math.BigDecimal;

public class MainActivity extends Activity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor sensor;
    EditText edServer;
    Button btnConnect;
    TextView tvStatus;


    String sAddress = "iot.eclipse.org";
    String sUserName = null;
    String sPassword = null;
    String sDestination = null;
    String sMessage = null;

    String topic        = "rupam/activity";
    String content      = "FORWARD";
    int qos             = 0;
    String broker       = "tcp://iot.eclipse.org:1883";
    //String broker       = "tcp://192.168.1.103:1883";
    String clientId     = "RUPAM_DAS";
    MemoryPersistence persistence = new MemoryPersistence();
    IMqttClient sampleClient;
    ////////CONNECTION CLASS/////
    public class ConnectionClass extends AsyncTask<ActivityGroup, String, String>
    {
        Exception exc = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
        @Override protected String doInBackground(ActivityGroup... params) {
            try
            {
                if (Looper.myLooper()==null)
                    Looper.prepare();
                sampleClient = new MqttClient(broker, clientId, persistence);

                //    sampleClient=new MqttClient(broker,clientId);
                MqttConnectOptions connOpts = new MqttConnectOptions();
                connOpts.setCleanSession(true);
                IMqttToken imt=sampleClient.connectWithResult(connOpts);

                Log.d("MQTT MODULE.....","....DONE..."+sampleClient.getServerURI()+"--"+imt.getResponse().getPayload());


                if(sampleClient.isConnected()) {
                    return "CONNECTED";
                }
                else
                {
                    return "Connection Failed.....";
                }

            }
            catch(Exception ex )
            {

                Log.d("MQTT MODULE", "CONNECTION FAILED " + ex.getMessage() + " broker: " + broker + " clientId " + clientId);
                //   Toast.makeText(MainActivity.this, "FAILED", Toast.LENGTH_LONG).show();
                // tv2.setText("Failed!!");
                return "FAILED";
            }
            // return null;
        }
        @Override protected void onPostExecute(String result) {
            super.onPostExecute(result);
            // Utils.usrMessage(getApplicationContext(), "Oh Noo!:\n"+exc.getMessage());
            //Utils.logErr(getApplicationContext(), exc);
            // finish();

            if(result!= null)
            {
                isConnected=true;
                tvStatus.setText(result);
                // setContentView(result);
            }
        }
    }
    boolean isConnected;
    void Send(String content)
    {
        final String data=content;
      //  isConnected =sampleClient.isConnected();
        AsyncTask.execute(new Runnable()
        {
            @Override
            public void run()
            {

                try
                {
                    if(sampleClient.isConnected())
                    {
                        MqttMessage message = new MqttMessage(data.getBytes());
                        message.setQos(qos);
                        sampleClient.publish(topic, message);
                    }
                    else
                    {

                    }
                    Log.d("MQTT MODULE",data+" SENT");
                }
                catch(Exception ex)
                {

                }
                //TODO your background code
            }


        });
    }


    ////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), sensorManager.SENSOR_DELAY_UI);
        edServer=(EditText)findViewById(R.id.edServer);
        btnConnect=(Button)findViewById(R.id.btnConnect);
        tvStatus=(TextView)findViewById(R.id.tvStatus);
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Connect();;
                broker="tcp://"+edServer.getText().toString().trim()+":1883";
                ConnectionClass con=new ConnectionClass();
                con.execute();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    public  float Round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

    float prevX=-1,prevY=-1,prevZ=-1;
    float DX=0;
    long lastUpdate = System.currentTimeMillis();
    int WAIT=1000;
    boolean detected=false;
    @Override
    public void onSensorChanged(SensorEvent event) {

        String command="";
        if(!isConnected)
        {
          //  return;
        }
        float[] values = event.values;

        // Movement
        float x = Round(values[0], 1);
        float y = Round(values[1], 1);
        float z = Round(values[2], 1);
        command="";
        long actualTime = System.currentTimeMillis();
        if ((actualTime - lastUpdate) > WAIT)
        {

            long diffTime = (actualTime - lastUpdate);
            lastUpdate = actualTime;
     boolean sit_stand_sleep=false;

            ///// Make the Calculations Here//////
            float diffX=x-prevX;
            float diffY=y-prevY;
            float diffz=z-prevZ;

            //////////////////////

            double xz=Math.abs(diffX)* Math.abs(diffz);
            double xyz=Math.abs(diffX)+Math.abs(diffz)+Math.abs(diffY);
            if(xz>1.1 )
            {
                if(xz>40)
                {
                    Log.d("RECOGNIZED", "RUNNING "+xz);
                    command="RUNNING_"+xz;
                }
                else {
                    Log.d("RECOGNIZED", "WALKING " + xz);
                    command="WALKING_"+xz;
                }
                sit_stand_sleep=true;
            }
            else
            {
                if(xz>.1)
                {
                    Log.d("RECOGNIZED", "SHAKING LEGS");
                    command="SHAKING LEGS";
                }
                else
                {
                    if(xyz<.3)
                    {
                        Log.d("RECOGNIZED", "MOBILE NOT WITH USER");
                        command="MOBILE NOT WITH USER";
                    }
                    else {
                        Log.d("RECOGNIZED", "RESTING");
                        command="RESTING";
                    }
                }

            }
            /// Finally Update the past values with Current Values
            if(prevX!=-1)
            {



                DX=DX+diffX;


                prevX = x;
                prevZ = y;
                prevZ = z;

            }
            else
            {

                prevX = x;
                prevY = y;
                prevZ = z;

            }

                if(command.length()>1)
                {

                    Send(command);
                    command = "";
                }

        }
        ///////////////
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
