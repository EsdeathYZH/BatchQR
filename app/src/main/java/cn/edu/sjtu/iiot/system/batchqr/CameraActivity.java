package cn.edu.sjtu.iiot.system.batchqr;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;

public class CameraActivity extends AppCompatActivity {

    Camera2BasicFragment mfragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        mfragment = Camera2BasicFragment.newInstance();
        if (null == savedInstanceState) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, mfragment)
                    .commit();
        }
    }


    /**
     * specifiy the controller's key event
     */

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    mfragment.takePictureFromCenter();
                    break;
                default:
                    break;
            }
        }
        return super.dispatchKeyEvent(event);
    }

}
