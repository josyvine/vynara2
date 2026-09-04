package com.example.ui;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.MainActivity;
import com.example.R;
import com.example.character.Character;
import com.example.engine.GLTFImporter;
import com.example.engine.Scene;
import com.example.engine.SceneObject;
import com.example.engine.StudioGLRenderer;
import com.example.engine.ThreeDEngine;
import com.example.export.GLTFExporter;
import com.example.runtime.ProjectRuntime;
import com.example.utils.VynaraLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

public class StudioFragment extends Fragment {

    private GLSurfaceView glSurfaceView;
    private StudioGLRenderer renderer;
    private ProjectRuntime runtime;
    private ThreeDEngine engine;
    
    private TextView tvStats;
    private TextView tvSelectedInfo;
    private TextView tvAnimTime;
    private SeekBar seekbarTimeline;
    private ImageButton btnAnimPlay;
    private boolean isPlaying = false;
    private android.os.Handler animHandler;
    private Runnable animRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_studio, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Phase 1 Alignment: Fetch the single, unified shared project runtime instance
        if (getActivity() instanceof MainActivity) {
            runtime = ((MainActivity) getActivity()).getProjectRuntime();
        } else {
            runtime = ProjectRuntime.getInstance(requireContext());
        }

        engine = runtime.getEngine();
        animHandler = new android.os.Handler(android.os.Looper.getMainLooper());

        glSurfaceView = view.findViewById(R.id.gl_surface_view);
        tvStats = view.findViewById(R.id.tv_studio_poly_stats);
        tvSelectedInfo = view.findViewById(R.id.tv_selected_object_info);
        tvAnimTime = view.findViewById(R.id.tv_anim_time);
        seekbarTimeline = view.findViewById(R.id.seekbar_timeline);
        btnAnimPlay = view.findViewById(R.id.btn_anim_play);

        // Setup OpenGL ES 2.0 Viewport Renderer
        glSurfaceView.setEGLContextClientVersion(2);
        renderer = new StudioGLRenderer(engine.getSceneManager(), engine.getCameraManager(), engine.getLightManager());
        glSurfaceView.setRenderer(renderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // Enable 360-degree Touch Viewport Camera Orbit Navigation
        setupViewportTouchOrbitGesture();

        updateStudioStatsUI();

        // Phase 13 Alignment: Undo & Redo transaction history
        View btnUndo = view.findViewById(R.id.btn_undo);
        if (btnUndo != null) {
            btnUndo.setOnClickListener(v -> {
                if (runtime.getUndoManager().undo()) {
                    updateStudioStatsUI();
                    Toast.makeText(getContext(), "Undo Successful", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Nothing to undo", Toast.LENGTH_SHORT).show();
                }
            });
        }

        View btnRedo = view.findViewById(R.id.btn_redo);
        if (btnRedo != null) {
            btnRedo.setOnClickListener(v -> {
                if (runtime.getRedoManager().redo()) {
                    updateStudioStatsUI();
                    Toast.makeText(getContext(), "Redo Successful", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Nothing to redo", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Phase 18 Alignment: Real GLTF scene exporter
        View btnExport = view.findViewById(R.id.btn_export_gltf);
        if (btnExport != null) {
            btnExport.setOnClickListener(v -> exportActiveSceneToLocalGltf());
        }

        // Viewport Transform Tool Controls
        View btnSelect = view.findViewById(R.id.btn_tool_select);
        if (btnSelect != null) {
            btnSelect.setOnClickListener(v -> {
                SceneObject selected = engine.getSceneManager().getSelectedObject();
                if (selected != null) {
                    tvSelectedInfo.setText("Selected: " + selected.getName() + " (" + selected.getSemanticType() + ")");
                } else {
                    List<SceneObject> objs = engine.getSceneManager().getAllObjects();
                    if (!objs.isEmpty()) {
                        engine.getSceneManager().selectObject(objs.get(0));
                        tvSelectedInfo.setText("Selected: " + objs.get(0).getName());
                    } else {
                        tvSelectedInfo.setText("No object selected");
                    }
                }
            });
        }

        View btnMove = view.findViewById(R.id.btn_tool_move);
        if (btnMove != null) {
            btnMove.setOnClickListener(v -> {
                SceneObject selected = engine.getSceneManager().getSelectedObject();
                if (selected != null) {
                    runtime.getTransactionManager().beginTransaction("Translate Object");
                    selected.getTransform().translate(0.5f, 0f, 0f); // Translate along positive X axis
                    runtime.getTransactionManager().commitTransaction();
                    Toast.makeText(getContext(), "Translated selected object (+0.5 X)", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Please select an object first", Toast.LENGTH_SHORT).show();
                }
            });
        }

        View btnRotate = view.findViewById(R.id.btn_tool_rotate);
        if (btnRotate != null) {
            btnRotate.setOnClickListener(v -> {
                SceneObject selected = engine.getSceneManager().getSelectedObject();
                if (selected != null) {
                    runtime.getTransactionManager().beginTransaction("Rotate Object");
                    selected.getTransform().rotate(0f, 15f, 0f); // Rotate along Yaw Y axis
                    runtime.getTransactionManager().commitTransaction();
                    Toast.makeText(getContext(), "Rotated selected object (+15 deg Yaw)", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Please select an object first", Toast.LENGTH_SHORT).show();
                }
            });
        }

        View btnScale = view.findViewById(R.id.btn_tool_scale);
        if (btnScale != null) {
            btnScale.setOnClickListener(v -> {
                SceneObject selected = engine.getSceneManager().getSelectedObject();
                if (selected != null) {
                    runtime.getTransactionManager().beginTransaction("Scale Object");
                    selected.getTransform().scaleBy(1.1f, 1.1f, 1.1f); // Increment scale uniformly
                    runtime.getTransactionManager().commitTransaction();
                    Toast.makeText(getContext(), "Scaled selected object (+10% Uniform)", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "Please select an object first", Toast.LENGTH_SHORT).show();
                }
            });
        }

        View btnHierarchy = view.findViewById(R.id.btn_tool_hierarchy);
        if (btnHierarchy != null) {
            btnHierarchy.setOnClickListener(v -> {
                int totalObjects = engine.getSceneManager().getAllObjects().size();
                int totalLights = engine.getLightManager().getLights().size();
                Toast.makeText(getContext(), "Scene Graph: " + totalObjects + " Nodes, " + totalLights + " Lights, 1 Camera", Toast.LENGTH_LONG).show();
            });
        }

        // Timeline and animation loop setup
        animRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPlaying) {
                    // Update animation playback time across active characters
                    for (Character c : runtime.getCharacterManager().getCharacterMap().values()) {
                        if (c.getAnimationPlayer() != null && c.getAnimationPlayer().isPlaying()) {
                            c.getAnimationPlayer().update(0.033f); // Symmetrical 30 FPS update steps
                            float seconds = c.getAnimationPlayer().getCurrentTimeSeconds();
                            if (seekbarTimeline != null) {
                                int progress = (int) ((seconds / 5.0f) * 100);
                                seekbarTimeline.setProgress(progress);
                            }
                        }
                    }
                    animHandler.postDelayed(this, 33);
                }
            }
        };

        if (btnAnimPlay != null) {
            btnAnimPlay.setOnClickListener(v -> {
                isPlaying = !isPlaying;
                btnAnimPlay.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
                
                for (Character c : runtime.getCharacterManager().getCharacterMap().values()) {
                    if (c.getAnimationPlayer() != null) {
                        if (isPlaying) {
                            c.getAnimationPlayer().resume();
                        } else {
                            c.getAnimationPlayer().pause();
                        }
                    }
                }
                
                if (isPlaying) {
                    animHandler.post(animRunnable);
                } else {
                    animHandler.removeCallbacks(animRunnable);
                }
            });
        }

        if (seekbarTimeline != null) {
            seekbarTimeline.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    float seconds = (progress / 100.0f) * 5.0f;
                    tvAnimTime.setText(String.format(java.util.Locale.US, "%.1fs / 5.0s", seconds));
                    
                    if (fromUser) {
                        // Scrub keyframes manually when seekBar changes
                        for (Character c : runtime.getCharacterManager().getCharacterMap().values()) {
                            if (c.getAnimationPlayer() != null) {
                                c.getAnimationPlayer().seek(seconds);
                            }
                        }
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }

        // AI Assistant Dialog Launcher
        View btnAi = view.findViewById(R.id.btn_ai_studio_assistant);
        if (btnAi != null) {
            btnAi.setOnClickListener(v -> {
                AiAssistantDialogFragment dialog = new AiAssistantDialogFragment();
                dialog.show(getChildFragmentManager(), "AiAssistantDialog");
            });
        }
    }

    /**
     * Touch Event Handler: Translates touch drag physics on the 3D surface view directly into spherical camera orbit rotation.
     */
    private void setupViewportTouchOrbitGesture() {
        if (glSurfaceView == null) return;

        glSurfaceView.setOnTouchListener(new View.OnTouchListener() {
            private float previousTouchX;
            private float previousTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event == null) return false;

                float x = event.getX();
                float y = event.getY();

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        previousTouchX = x;
                        previousTouchY = y;
                        v.performClick();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float deltaX = x - previousTouchX;
                        float deltaY = y - previousTouchY;

                        if (engine != null && engine.getCameraManager() != null) {
                            com.example.engine.Camera camera = engine.getCameraManager().getActiveCamera();
                            if (camera != null) {
                                float[] eye = camera.getEye();
                                float[] target = camera.getTarget();

                                if (eye != null && target != null && eye.length >= 3 && target.length >= 3) {
                                    float relX = eye[0] - target[0];
                                    float relY = eye[1] - target[1];
                                    float relZ = eye[2] - target[2];

                                    float radius = (float) Math.sqrt(relX * relX + relY * relY + relZ * relZ);
                                    if (radius < 0.001f) radius = 5.0f;

                                    float yaw = (float) Math.atan2(relZ, relX);
                                    float pitch = (float) Math.asin(Math.max(-0.99f, Math.min(0.99f, relY / radius)));

                                    yaw += deltaX * 0.008f;
                                    pitch += deltaY * 0.008f;

                                    float maxPitch = 1.52f; // ~87 degrees limit
                                    if (pitch > maxPitch) pitch = maxPitch;
                                    if (pitch < -maxPitch) pitch = -maxPitch;

                                    float newX = target[0] + radius * (float) (Math.cos(pitch) * Math.cos(yaw));
                                    float newY = target[1] + radius * (float) Math.sin(pitch);
                                    float newZ = target[2] + radius * (float) (Math.cos(pitch) * Math.sin(yaw));

                                    camera.setEye(newX, newY, newZ);
                                }
                            }
                        }

                        previousTouchX = x;
                        previousTouchY = y;
                        return true;
                }
                return false;
            }
        });
    }

    public void loadAndDisplayGLBFile(File glbFile) {
        if (glbFile == null || !glbFile.exists() || engine == null) return;

        try {
            VynaraLogger.system("StudioFragment: Loading external GLB into active scene: " + glbFile.getName());
            GLTFImporter.ImportResult result = GLTFImporter.loadFromFile(glbFile);

            runtime.getTransactionManager().beginTransaction("Import GLB Model");

            for (SceneObject obj : result.getSceneObjects()) {
                engine.getSceneManager().getActiveScene().addObject(obj);
            }

            for (Character ch : result.getCharacters()) {
                runtime.getCharacterManager().registerCharacter(ch);
            }

            engine.getSceneManager().updateWorldTransforms();
            runtime.getTransactionManager().commitTransaction();

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    updateStudioStatsUI();
                    Toast.makeText(getContext(), "Imported: " + glbFile.getName(), Toast.LENGTH_SHORT).show();
                });
            }

        } catch (Exception e) {
            VynaraLogger.e("StudioFragment: Failed loading GLB file", e);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Import error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }
    }

    private void updateStudioStatsUI() {
        if (tvStats != null && engine != null) {
            Scene activeScene = engine.getSceneManager().getActiveScene();
            int triangles = activeScene != null ? activeScene.getTotalTriangleCount() : 0;
            int vertices = activeScene != null ? activeScene.getTotalVertexCount() : 0;
            tvStats.setText("Tris: " + triangles + " | Verts: " + vertices);
        }
    }

    private void exportActiveSceneToLocalGltf() {
        if (getContext() == null || engine == null) return;

        try {
            Scene activeScene = engine.getSceneManager().getActiveScene();
            String gltfJson = GLTFExporter.exportSceneToGLTFJson(activeScene);

            File exportDir = new File(getContext().getExternalFilesDir(null), "exports");
            if (!exportDir.exists() && !exportDir.mkdirs()) {
                Toast.makeText(getContext(), "Failed to create export folder", Toast.LENGTH_SHORT).show();
                return;
            }

            File exportFile = new File(exportDir, "vynara_scene_" + System.currentTimeMillis() + ".gltf");
            FileOutputStream fos = new FileOutputStream(exportFile);
            fos.write(gltfJson.getBytes());
            fos.close();

            Toast.makeText(getContext(), "Scene exported to: " + exportFile.getName(), Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(getContext(), "GLTF Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (glSurfaceView != null) glSurfaceView.onResume();
        updateStudioStatsUI();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (glSurfaceView != null) glSurfaceView.onPause();
        isPlaying = false;
        animHandler.removeCallbacks(animRunnable);
    }
}