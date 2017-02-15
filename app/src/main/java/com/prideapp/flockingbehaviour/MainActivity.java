package com.prideapp.flockingbehaviour;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.prideapp.flockingbehaviour.Controle.Intelligence;
import com.prideapp.flockingbehaviour.Model.Unit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import math.geom2d.Point2D;
import math.geom2d.Vector2D;

public class MainActivity extends AppCompatActivity {

    public static String MY_TAG = "MyTag";

    public static byte UNITS_AMOUNT = 6;
    public static byte OBSTACLE_AMOUNT = 3;
    public static long DELTA_TIME_MILLIS = 100;

    public static int WIDTH = 480, HEIGHT = 800, UNIT_WIDTH = 36, UNIT_HEIGHT = 20;
    private float widthCoefficient, heightCoefficient; //1&1


    private ArrayList<Unit> units;
    private HashMap<Point2D, Integer> circleObstacles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(new DrawView(this));


        if(getSupportActionBar() != null)
            getSupportActionBar().hide();

        DisplayMetrics metrics;
        metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        int displayH = metrics.heightPixels;
        int displayW = metrics.widthPixels;

        widthCoefficient = (float) displayW /WIDTH;
        heightCoefficient = (float) displayH /HEIGHT;

        Log.e(MY_TAG, "Display w h :" + displayW + " " + displayH
                + "\nCoefficients w h :" + widthCoefficient + " " + heightCoefficient);

        units = new ArrayList<>(UNITS_AMOUNT);
        circleObstacles = new HashMap<>(OBSTACLE_AMOUNT);
    }

    class DrawView extends SurfaceView implements SurfaceHolder.Callback {

        private DrawThread drawThread;

        private Point2D aim;

        public DrawView(Context context) {
            super(context);
            getHolder().addCallback(this);

            aim = new Point2D(0, 0);

            setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    switch (motionEvent.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            aim = new Point2D(motionEvent.getX()/widthCoefficient,
                                    motionEvent.getY()/heightCoefficient);
                            break;
                        case MotionEvent.ACTION_UP:
                            aim = new Point2D(0, 0);
                            break;
                    }
                    return true;
                }
            });
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height) {}

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            drawThread = new DrawThread(getHolder());
            drawThread.setRunning(true);
            drawThread.start();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            boolean retry = true;
            drawThread.setRunning(false);
            while (retry) {
                try {
                    drawThread.join();
                    retry = false;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        class DrawThread extends Thread {

            private boolean running = false;
            private SurfaceHolder surfaceHolder;

            private Paint paint;
            private Matrix matrix;

            private long prevTime;

            private void creatFloackStartPosition(){
                units.add(new Unit(140, HEIGHT - UNIT_HEIGHT,
                        (int)(UNIT_WIDTH*widthCoefficient), (int)(UNIT_HEIGHT*heightCoefficient), getResources()));
                units.add(new Unit(140 + 2*UNIT_WIDTH, (int)(HEIGHT-UNIT_HEIGHT),
                        (int)(UNIT_WIDTH*widthCoefficient), (int)(UNIT_HEIGHT*heightCoefficient), getResources()));
                units.add(new Unit(140 + 4*UNIT_WIDTH, (int)(HEIGHT-UNIT_HEIGHT),
                        (int)(UNIT_WIDTH*widthCoefficient), (int)(UNIT_HEIGHT*heightCoefficient), getResources()));
                units.add(new Unit(180, HEIGHT-2*UNIT_HEIGHT,
                        (int)(UNIT_WIDTH*widthCoefficient), (int)(UNIT_HEIGHT*heightCoefficient), getResources()));
                units.add(new Unit(180 + 2*UNIT_WIDTH, HEIGHT-2*UNIT_HEIGHT,
                        (int)(UNIT_WIDTH*widthCoefficient), (int)(UNIT_HEIGHT*heightCoefficient), getResources()));
                units.add(new Unit(220, HEIGHT-3*UNIT_HEIGHT,
                        (int)(UNIT_WIDTH*widthCoefficient), (int)(UNIT_HEIGHT*heightCoefficient), getResources()));
            }

            private void creatCrossStartPosition(){
                units.add(new Unit(200, 200,
                        (int)(UNIT_WIDTH*widthCoefficient), (int)(UNIT_HEIGHT*heightCoefficient), getResources()));
                units.add(new Unit(200, 200,
                        (int)(UNIT_WIDTH*widthCoefficient), (int)(UNIT_HEIGHT*heightCoefficient), getResources()));
                units.add(new Unit(200, 200,
                        (int)(UNIT_WIDTH*widthCoefficient), (int)(UNIT_HEIGHT*heightCoefficient), getResources()));
                units.add(new Unit(200, 200,
                        (int)(UNIT_WIDTH*widthCoefficient), (int)(UNIT_HEIGHT*heightCoefficient), getResources()));
                units.get(0).setVelocity(new Vector2D(1, 0));
                units.get(1).setVelocity(new Vector2D(0, 1));
                units.get(2).setVelocity(new Vector2D(-1, 0));
                units.get(3).setVelocity(new Vector2D(0, -1));
            }

            private void createObstacles(){
                circleObstacles.put(new Point2D(140, 280), 70);
                circleObstacles.put(new Point2D(400, 600), 30);
                circleObstacles.put(new Point2D(320, 140), 70);
            }

            public DrawThread(SurfaceHolder surfaceHolder) {
                this.surfaceHolder = surfaceHolder;
                paint = new Paint(Paint.ANTI_ALIAS_FLAG);

                createObstacles();

                creatFloackStartPosition();

                //creatCrossStartPosition();

                prevTime = System.currentTimeMillis();
            }

            public void setRunning(boolean running) {
                this.running = running;
            }

            @Override
            public void run() {
                Canvas canvas;
                while (running) {
                    canvas = null;
                    try {
                        canvas = surfaceHolder.lockCanvas(null);
                        if (canvas == null)
                            continue;

                        synchronized (surfaceHolder) {

                            render(canvas);

                            //recalculate velocities every DELTA_TIME_MILLIS
                            long now = System.currentTimeMillis();
                            long elapsedTime = now - prevTime;
                            if (elapsedTime >= DELTA_TIME_MILLIS){

                                prevTime = now;
                                Intelligence.moveAllUnitsAsync(units, circleObstacles, aim);
                            }
                        }

                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } finally {
                        if (canvas != null) {
                            surfaceHolder.unlockCanvasAndPost(canvas);
                        }
                    }
                }
            }

            private void render(Canvas canvas){
                canvas.drawARGB(80, 102, 204, 255);

                paint.setColor(Color.BLACK);
                for(Map.Entry<Point2D, Integer> circle : circleObstacles.entrySet())
                    canvas.drawCircle((float) circle.getKey().getX()*widthCoefficient,
                            (float) circle.getKey().getY()*heightCoefficient,
                            (widthCoefficient+heightCoefficient)/2*circle.getValue(), paint);

                paint.setColor(Color.RED);
                canvas.drawCircle(0, 0, 40*(widthCoefficient+heightCoefficient)/2, paint);
                canvas.drawCircle(0, 800*heightCoefficient, 40*(widthCoefficient+heightCoefficient)/2, paint);
                canvas.drawCircle(480*widthCoefficient, 0, 40*(widthCoefficient+heightCoefficient)/2, paint);
                canvas.drawCircle(480*widthCoefficient, 800*heightCoefficient, 40*(widthCoefficient+heightCoefficient)/2, paint);

                for(Unit unit : units) {
                    matrix = new Matrix();
                    matrix.postTranslate((float) (unit.getPosition().getX() - UNIT_WIDTH/2)*widthCoefficient,
                            (float) (unit.getPosition().getY() - UNIT_HEIGHT/2)*heightCoefficient);
                    matrix.postRotate(unit.getAngle(), (float) (unit.getPosition().getX())*widthCoefficient,
                            (float) (unit.getPosition().getY())*heightCoefficient);

                    canvas.drawBitmap(unit.getBitmap(), matrix, paint);

                    paint.setStrokeWidth(3);
                    canvas.drawPoint((float) (unit.getPosition().getX())*widthCoefficient,
                            (float) (unit.getPosition().getY())*heightCoefficient, paint);
                }


            }
        }

    }
}
