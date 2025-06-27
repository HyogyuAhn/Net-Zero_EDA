package insa.eda;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ActivityHome extends AppCompatActivity {

    private static final String TAG = "ActivityHome";
    private NavController navController = null;
    private AppBarConfiguration appBarConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        
        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);
        
        try {
            FragmentManager fragmentManager = getSupportFragmentManager();
            NavHostFragment navHostFragment = 
                    (NavHostFragment) fragmentManager.findFragmentById(R.id.nav_host_fragment);
                    
            if (navHostFragment != null) {
                Log.d(TAG, "NavHostFragment found, initializing NavController");
                navController = navHostFragment.getNavController();
                
                appBarConfiguration = new AppBarConfiguration.Builder(
                        R.id.navigation_home, 
                        R.id.navigation_drive,
                        R.id.navigation_history,
                        R.id.navigation_report,
                        R.id.navigation_profile)
                        .build();
                
                NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
                
                BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
                NavigationUI.setupWithNavController(bottomNav, navController);
            } else {
                Log.e(TAG, "NavHostFragment is null");
                Toast.makeText(this, "Navigation 초기화 실패", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception initializing NavController: " + e.getMessage());
            Toast.makeText(this, "Navigation 초기화 중 오류 발생: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        if (navController != null) {
            return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp();
        }
        return super.onSupportNavigateUp();
    }
}
