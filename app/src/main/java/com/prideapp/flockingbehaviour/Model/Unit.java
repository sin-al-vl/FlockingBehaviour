package com.prideapp.flockingbehaviour.Model;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.DrawableRes;

import com.prideapp.flockingbehaviour.R;

import math.geom2d.Vector2D;

/**
 * Created by Александр on 12.02.2017.
 */

public class Unit {

    private Vector2D position;
    private Vector2D velocity;

    private Bitmap bitmap;

    public Unit(int x, int y, int reqWidth, int reqHeight, Resources res){
        this.position = new Vector2D(x, y);
        velocity = new Vector2D(0, -20);

        bitmap = Bitmap.createScaledBitmap(
                decodeSampledBitmapFromResource(res, R.drawable.droplet_cutted, reqWidth, reqHeight),
                reqWidth, reqHeight, false);
    }

    public void update(float dt){
        velocity = velocity.times(dt/1000);
        position = position.plus(velocity);
        velocity = velocity.times(1000/dt);
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public Vector2D getPosition() {
        return position;
    }

    public short getAngle(){
        return (short) (velocity.angle()*180/Math.PI);
    }

    public Vector2D getVelocity() {
        return velocity;
    }

    public void setVelocity(Vector2D velocity) {
        this.velocity = velocity;
    }

    public static Bitmap decodeSampledBitmapFromResource(Resources res, @DrawableRes int id, int reqWidth, int reqHeight) {

        // Читаем с inJustDecodeBounds=true для определения размеров
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        BitmapFactory.decodeResource(res, id, options);

        // Вычисляем inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth,
                reqHeight);

        // Читаем с использованием inSampleSize коэффициента
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, id, options);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options,
                                            int reqWidth, int reqHeight) {
        // Реальные размеры изображения
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Вычисляем наибольший inSampleSize, который будет кратным двум
            // и оставит полученные размеры больше, чем требуемые
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
