package personal.nfl.networkcapture.activity;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import personal.nfl.networkcapture.R;
import personal.nfl.vpn.service.VpnTestService;

public class TestActivity extends AppCompatActivity {

    private final int REQUEST_CODE_START_VPN = 10 ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
    }

    public void onClick(View view) {
        Intent intent = VpnTestService.prepare(this);
        if (intent != null) {
            Log.i("NFL" , "请求 VPN 权限") ;
            startActivityForResult(intent, REQUEST_CODE_START_VPN);
        } else {
            Log.i("NFL" , "VPN 已打开") ;
            onActivityResult(REQUEST_CODE_START_VPN, RESULT_OK, null);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            switch (requestCode){
                case REQUEST_CODE_START_VPN:
                    Log.i("NFL" , "vpn 启动成功") ;
                    startService(new Intent(this , VpnTestService.class)) ;
                    break;
                default:
                    break;
            }
        }
    }
}
