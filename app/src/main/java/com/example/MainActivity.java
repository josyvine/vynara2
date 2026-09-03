package com.example;

import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.runtime.ProjectRuntime;
import com.example.ui.AssetsFragment;
import com.example.ui.CreateFragment;
import com.example.ui.InAppFloatingConsoleView;
import com.example.ui.LandingFragment;
import com.example.ui.ProductionFragment;
import com.example.ui.ProjectsFragment;
import com.example.ui.SettingsFragment;
import com.example.ui.StudioFragment;
import com.example.utils.VynaraLogger;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private ProjectRuntime projectRuntime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Phase 1 Alignment: Initialize unified shared 3D project runtime instance
        projectRuntime = ProjectRuntime.getInstance(getApplicationContext());

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        if (savedInstanceState == null) {
            loadFragment(new LandingFragment());
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_create) {
                loadFragment(new CreateFragment());
                return true;
            } else if (id == R.id.nav_projects) {
                loadFragment(new ProjectsFragment());
                return true;
            } else if (id == R.id.nav_assets) {
                loadFragment(new AssetsFragment());
                return true;
            } else if (id == R.id.nav_studio) {
                loadFragment(new StudioFragment());
                return true;
            } else if (id == R.id.nav_settings) {
                loadFragment(new SettingsFragment());
                return true;
            }
            return false;
        });

        // Instantiate and dynamically attach the custom in-app overlay diagnostic console
        InAppFloatingConsoleView floatingConsole = new InAppFloatingConsoleView(this);
        
        FrameLayout.LayoutParams consoleParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        consoleParams.gravity = Gravity.TOP | Gravity.END;
        consoleParams.topMargin = 120; // Safe vertical boundary offset from system status bar
        consoleParams.rightMargin = 20;

        ViewGroup rootContainer = findViewById(android.R.id.content);
        if (rootContainer != null) {
            rootContainer.addView(floatingConsole, consoleParams);
        }

        // Write boot diagnostic log line
        VynaraLogger.system("Vynara initialized cleanly.");
    }

    public void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    public void navigateToCreate() {
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_create);
        }
    }

    public void navigateToStudio() {
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_studio);
        }
    }

    public void startProduction(String prompt) {
        startProduction(prompt, "Photorealistic", "OpenGL ES / GLTF", new ArrayList<>());
    }

    public void startProduction(String prompt, String style, String targetEngine, List<String> referenceImageUris) {
        loadFragment(ProductionFragment.newInstance(prompt, style, targetEngine, referenceImageUris));
    }

    public ProjectRuntime getProjectRuntime() {
        if (projectRuntime == null) {
            projectRuntime = ProjectRuntime.getInstance(getApplicationContext());
        }
        return projectRuntime;
    }
}