package com.example.engine;

import android.opengl.Matrix;

public class Camera {
    private float[] eye = new float[] { 0f, 4f, 8f };
    private float[] target = new float[] { 0f, 1f, 0f };
    private float[] up = new float[] { 0f, 1f, 0f };

    private float fov = 45f;
    private float near = 0.1f;
    private float far = 100f;
    private int viewportWidth = 1080;
    private int viewportHeight = 1920;

    private final float[] viewMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewProjectionMatrix = new float[16];

    public Camera() {
        updateViewMatrix();
        updateProjectionMatrix(viewportWidth, viewportHeight);
    }

    public void updateViewMatrix() {
        Matrix.setLookAtM(viewMatrix, 0, eye[0], eye[1], eye[2], target[0], target[1], target[2], up[0], up[1], up[2]);
        updateViewProjectionMatrix();
    }

    public void updateProjectionMatrix(int width, int height) {
        this.viewportWidth = width > 0 ? width : 1;
        this.viewportHeight = height > 0 ? height : 1;
        float aspect = (float) this.viewportWidth / (float) this.viewportHeight;
        Matrix.perspectiveM(projectionMatrix, 0, fov, aspect, near, far);
        updateViewProjectionMatrix();
    }

    private void updateViewProjectionMatrix() {
        Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
    }

    public void setEye(float x, float y, float z) {
        eye[0] = x; eye[1] = y; eye[2] = z;
        updateViewMatrix();
    }

    public void setTarget(float x, float y, float z) {
        target[0] = x; target[1] = y; target[2] = z;
        updateViewMatrix();
    }

    public void setUp(float x, float y, float z) {
        up[0] = x; up[1] = y; up[2] = z;
        updateViewMatrix();
    }

    public void setFov(float fovDegrees) {
        this.fov = Math.max(10f, Math.min(120f, fovDegrees));
        updateProjectionMatrix(viewportWidth, viewportHeight);
    }

    public void setClippingPlanes(float nearPlane, float farPlane) {
        this.near = Math.max(0.01f, nearPlane);
        this.far = Math.max(near + 1.0f, farPlane);
        updateProjectionMatrix(viewportWidth, viewportHeight);
    }

    /**
     * Phase 16 Alignment: Frames camera eye and look target around a 3D bounding box.
     */
    public void frameBounds(float[] minBounds, float[] maxBounds) {
        if (minBounds == null || maxBounds == null || minBounds.length < 3 || maxBounds.length < 3) return;

        float centerX = (minBounds[0] + maxBounds[0]) / 2f;
        float centerY = (minBounds[1] + maxBounds[1]) / 2f;
        float centerZ = (minBounds[2] + maxBounds[2]) / 2f;

        float sizeX = maxBounds[0] - minBounds[0];
        float sizeY = maxBounds[1] - minBounds[1];
        float sizeZ = maxBounds[2] - minBounds[2];
        float maxExtent = Math.max(sizeX, Math.max(sizeY, sizeZ));

        float distance = (float) (maxExtent / Math.tan(Math.toRadians(fov / 2.0)));
        distance = Math.max(2.0f, distance * 1.5f);

        setTarget(centerX, centerY, centerZ);
        setEye(centerX, centerY + distance * 0.4f, centerZ + distance);
    }

    public float[] getEye() { return eye; }
    public float[] getTarget() { return target; }
    public float[] getUp() { return up; }
    public float getFov() { return fov; }
    public float getNear() { return near; }
    public float getFar() { return far; }
    
    public float[] getViewMatrix() { return viewMatrix; }
    public float[] getProjectionMatrix() { return projectionMatrix; }
    public float[] getViewProjectionMatrix() { return viewProjectionMatrix; }
}