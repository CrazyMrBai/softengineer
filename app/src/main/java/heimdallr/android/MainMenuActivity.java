package heimdallr.android;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

public class MainMenuActivity extends FragmentActivity
        implements HomeFragment.OnFragmentInteractionListener ,
        NotificationFragment.OnFragmentInteractionListener ,
        MineFragment.OnFragmentInteractionListener {

    private TextView mTextMessage;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    showHomeFragment();
                    return true;
                case R.id.navigation_dashboard:
                    mTextMessage.setText(R.string.title_dashboard);
                    return true;
                case R.id.navigation_notifications:
                    mTextMessage.setText(R.string.title_notifications);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    //fragment之间的切换
    private HomeFragment mHomeFragment;
    private NotificationFragment mNotificationFragment;
    private MineFragment mMineFragment;

    private void showHomeFragment(){
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        if(mHomeFragment == null){
            mHomeFragment = new HomeFragment();
            transaction.add(R.id.fragment_container,mHomeFragment);
        }
        hideFragment(transaction);
        transaction.show(mHomeFragment);
        transaction.commit();
    }

    private void showNotificationsFragment(){
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        if(mNotificationFragment == null){
            mNotificationFragment = new NotificationFragment();
            transaction.add(R.id.fragment_container,mNotificationFragment);
        }
        hideFragment(transaction);
        transaction.show(mNotificationFragment);
        transaction.commit();
    }
    private void showMineFragment(){
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        if(mMineFragment == null){
            mMineFragment = new MineFragment();
            transaction.add(R.id.fragment_container,mMineFragment);
        }
        hideFragment(transaction);
        transaction.show(mMineFragment);
        transaction.commit();
    }
    private void hideFragment(FragmentTransaction transaction){
        if(mHomeFragment != null){
            transaction.hide(mHomeFragment);
        }

        if(mNotificationFragment != null){
            transaction.hide(mNotificationFragment);
        }
        if(mMineFragment != null){
            transaction.hide(mMineFragment);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("MainmenuActivity","reA:"+requestCode);
        if(mHomeFragment == null)
            return;

        mHomeFragment.onActivityResult(requestCode,resultCode,data);
    }
}
