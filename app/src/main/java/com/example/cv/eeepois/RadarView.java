package com.example.cv.eeepois;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.SweepGradient;
import android.location.Location;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

public class RadarView extends View {

    int startColor;
    int endColor;
    int mRadarBgColor;
    int mRadarLineColor;
    int radarCircleCount;
    int mRadarRadius;
    int mRadarAccentColor;
    int mPoiRadius = 5;

    Paint mRadarAccent;
    Paint mRadarPaint;
    Paint mRadarBg;
    float degress;
    float rotateAngel;
    Matrix matrix;
    SweepGradient radarShader;
    boolean isScan;

    AppCompatActivity mainActivity = null;

    TextView txtLat;
    TextView txtLon;
    TextView txtAlt;

    HashMap<String, Location> hmLoc;

    private static final int DEFAULT_WIDTH = 200;
    private static final int DEFAULT_HEIGHT = 200;
    private static final int MSG_WHAT = 0x111;
    private static final int DELAY_TIME = 5;

    public RadarView(Context context) {
        super(context);
    }

    public RadarView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.RadarView);
        startColor = ta.getColor(R.styleable.RadarView_startColor, startColor);
        endColor = ta.getColor(R.styleable.RadarView_endColor, endColor);
        mRadarBgColor = ta.getColor(R.styleable.RadarView_bgColor, mRadarBgColor);
        mRadarLineColor = ta.getColor(R.styleable.RadarView_lineColor, mRadarLineColor);
        radarCircleCount = ta.getColor(R.styleable.RadarView_circleCount, radarCircleCount);
        mRadarAccentColor = ta.getColor(R.styleable.RadarView_colorAccent, mRadarAccentColor);
        init();
        ta.recycle();
    }

    public RadarView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void init() {
        mRadarBg = new Paint(Paint.ANTI_ALIAS_FLAG);
        mRadarBg.setColor(mRadarBgColor);
        mRadarAccent = new Paint(Paint.ANTI_ALIAS_FLAG);
        mRadarAccent.setColor(mRadarAccentColor);
        mRadarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mRadarPaint.setColor(mRadarLineColor);
        mRadarPaint.setStrokeWidth(2);
        mRadarPaint.setStyle(Paint.Style.STROKE);
        mRadarRadius = 100;
        radarShader = new SweepGradient(0, 0, startColor, endColor);
        matrix = new Matrix();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = measureSize(1, DEFAULT_WIDTH, widthMeasureSpec);
        int height = measureSize(0, DEFAULT_HEIGHT, heightMeasureSpec);
        int measureSize = Math.max(width, height);
        setMeasuredDimension(measureSize, measureSize);
    }

    private int measureSize(int specType, int contentSize, int measureSpec) {
        int result;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        if (specMode == MeasureSpec.EXACTLY) {
            result = Math.max(contentSize, specSize);
        } else {
            result = contentSize;
            if (specType == 1) {
                result += (getPaddingLeft() + getPaddingRight());
            } else {
                result += (getPaddingTop() + getPaddingBottom());
            }
        }

        return result;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.translate(mRadarRadius, mRadarRadius);
        mRadarBg.setShader(null);

        canvas.drawCircle(0, 0, mRadarRadius, mRadarBg);

        for (int i = 0; i <= radarCircleCount; i++) {
            canvas.drawCircle(0, 0, (float) (i * 1.0 / radarCircleCount * mRadarRadius), mRadarPaint);
        }

        canvas.drawLine(-mRadarRadius, 0, mRadarRadius, 0, mRadarPaint);
        canvas.drawLine(0, mRadarRadius, 0, -mRadarRadius, mRadarPaint);

        // loop for each point
        /*
        for (Map.Entry<String, Location> entry : hmLoc.entrySet()) {
            String key = entry.getKey();
            Location value = entry.getValue();

            double lon = value.getLongitude();
            double x = (lon+180)*(DEFAULT_WIDTH/360);

            double lat = value.getLatitude();
            double latrad = lat*Math.PI/180;

            double mercN = Math.log(Math.tan((Math.PI/4)+(latrad/2)));
            double y = (DEFAULT_HEIGHT / 2) - ((DEFAULT_WIDTH*mercN)/(2*Math.PI));

            // canvas.translate(mRadarRadius / 2, mRadarRadius / 2);
            canvas.drawCircle(0, 0, mPoiRadius, mRadarAccent);
            // canvas.translate(-(mRadarRadius / 2), -(mRadarRadius / 2));
        }
        */

        mRadarBg.setShader(radarShader);
        canvas.concat(matrix);
        canvas.drawCircle(0, 0, mRadarRadius, mRadarBg);

        canvas.rotate(degress);
        canvas.concat(matrix);
       // matrix.preRotate(rotateAngel, 0, 0);
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            rotateAngel += 3;
            postInvalidate();

            matrix.reset();
            matrix.preRotate(rotateAngel, 0, 0);
            mHandler.sendEmptyMessageDelayed(MSG_WHAT, DELAY_TIME);
        }
    };

    public void scan() {
        if (!isScan) {
            isScan = true;
            mHandler.sendEmptyMessageDelayed(MSG_WHAT, DELAY_TIME);
        } else {
            isScan = false;
            mHandler.removeMessages(MSG_WHAT);
        }
    }

    public void setup(AppCompatActivity activity, HashMap<String, Location> hm, TextView T1, TextView T2, TextView T3) {
        mainActivity = activity;
        hmLoc = hm;
        txtLat = T1;
        txtLon = T2;
        txtAlt = T3;
    }
}
