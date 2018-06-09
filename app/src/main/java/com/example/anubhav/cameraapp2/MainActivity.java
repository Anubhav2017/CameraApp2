package com.example.anubhav.cameraapp2;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureRequest.Builder;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import java.util.Collections;

import static android.hardware.camera2.CameraCaptureSession.*;
import static android.hardware.camera2.params.MeteringRectangle.METERING_WEIGHT_DONT_CARE;


public class MainActivity extends AppCompatActivity {
    Button takepic;
    TextureView textureView;
    CameraManager cameraManager;
    CameraDevice phonecamera;
    private static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    private static final int REQUEST_CAMERA_PERMISSION = 1;
    String frontCamera;
    String rearCamera;
    private static final String FRAGMENT_DIALOG = "dialog";
    CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            phonecamera = camera;
            createPreviewSession();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            camera.close();


        }
    };;
    HandlerThread handlerThread;
    Handler handler;
    String cameraId;

    Size size;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    private void openBackgroundThread() {
        handlerThread = new HandlerThread("CameraApp2");

        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    private void closeBackgroundThread() {
        handlerThread.quit();
        handler = null;
    }

    private void setupCamera() {
        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            if(ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.CAMERA
                },REQUEST_CAMERA_PERMISSION);
            }
            
            cameraId = cameraManager.getCameraIdList()[0];
            CameraCharacteristics characteristics=cameraManager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap configs=characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            size=configs.getOutputSizes(ImageFormat.JPEG)[1];
            textureView.setFocusable(true);



        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
     }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        takepic = findViewById(R.id.picbutton);
        takepic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SurfaceTexture surfaceTexture=textureView.getSurfaceTexture();
                final Surface surface=new Surface(surfaceTexture);
                try {
                    phonecamera.createCaptureSession(Collections.singletonList(surface), new StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            try {
                                Builder clickBuilder=phonecamera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                                clickBuilder.addTarget(surface);
                                session.capture(clickBuilder.build(),null,handler);
                            } catch (CameraAccessException e) {
                                e.printStackTrace();
                            }

                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                        }
                    },handler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
        });
        textureView = findViewById(R.id.textureview);
        textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                openBackgroundThread();
                setupCamera();
                //Log.d("tag1","setupdone");

                openCamera();
               // Log.d("2","opened");
            }


            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                closeBackgroundThread();
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });


    }



    private void openCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        try {
            cameraManager.openCamera(cameraId, stateCallback, handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private void createPreviewSession(){
        final SurfaceTexture surfaceTexture=textureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(size.getWidth(),size.getHeight());
        final Surface surface=new Surface(surfaceTexture);
        try {
            phonecamera.createCaptureSession(Collections.singletonList(surface), new StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        Builder builder= phonecamera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        builder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        builder.addTarget(surface);
                        int rotation=getWindowManager().getDefaultDisplay().getRotation();
                        builder.set(CaptureRequest.JPEG_ORIENTATION,ORIENTATIONS.get(rotation));

                        session.setRepeatingRequest(builder.build(),null,handler);
                        Log.d("3","built");
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {

                }
            },handler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }





}
