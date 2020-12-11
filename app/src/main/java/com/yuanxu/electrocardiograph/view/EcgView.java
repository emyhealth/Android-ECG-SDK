package com.yuanxu.electrocardiograph.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.yuanxu.electrocardiograph.R;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class EcgView extends SurfaceView implements SurfaceHolder.Callback {

    private Context mContext;
    private SurfaceHolder surfaceHolder;
    public boolean isRunning;
    private Canvas mCanvas;

    private float ecgMax = 4096;//心电的最大值
    private String bgColor = "#ffffff";
    private int wave_speed = 25;//波速: 25mm/s
    private int px1s = 0;       //每秒多少px
    private int sleepTime = 10; //每次锁屏的时间间距，单位:ms
    private float lockWidth;//每次锁屏需要画的
    private int ecgPerCount = 5;//每次画心电数据的个数，心电每秒有100个数据包
    private List<Integer> waiting = new ArrayList<>();

    private volatile ConcurrentLinkedQueue<Integer> ecg0Datas = new ConcurrentLinkedQueue<Integer>();

    private Paint mPaint;//画波形图的画笔
    private int mWidth;//控件宽度
    private int mHeight;//控件高度
    private float ecgYRatio;   //Y轴的压缩比
    private float startY1;
    private Rect rect;
    //是否重置
    private boolean isreSet = false;

    private int startX;//每次画线的X坐标起点
    private double ecgXOffset =1;//每次X坐标偏移的像素
    private int blankLineWidth = 6;//右侧空白点的宽度


    public EcgView(Context context, AttributeSet attrs){
        super(context, attrs);
        this.mContext = context;
        this.surfaceHolder = getHolder();
        this.surfaceHolder.addCallback(this);
        rect = new Rect();
        converXOffset();
    }

    private void init() {
        mPaint = new Paint();
        mPaint.setColor(getResources().getColor(R.color.colorF34558));
        mPaint.setStrokeWidth(2);
        startY1 = mHeight & (1/ 2);
        defaultPaint();
    }

    /**
     * 重置
     */
    public void reSet(){
        startX=0;
        isreSet = true;
    }

    /**
     * 根据波速计算每次X坐标增加的像素
     *
     * 计算出每次锁屏应该画的px值
     */
    private void converXOffset(){
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int width = dm.widthPixels;
        int height = dm.heightPixels;
        //获取屏幕对角线的长度，单位:px
        double diagonalMm = Math.sqrt(width * width + height * height) / dm.densityDpi;//单位：英寸
        diagonalMm = diagonalMm * 2.54 * 10;//转换单位为：毫米
        double diagonalPx = width * width + height * height;
        diagonalPx = Math.sqrt(diagonalPx);
        //每毫米有多少px
        double px1mm = diagonalPx / diagonalMm;
        //每秒画多少px
        double px1spir = wave_speed * px1mm;

//        lockWidth = (float) (px1spir * (sleepTime / 1000f));
        //lockWidth = 5;

        px1s = Integer.parseInt(new DecimalFormat("0").format(px1spir));
        ecgXOffset = px1spir / 100;
        //每次锁屏所需画的宽度
        lockWidth =  (float)ecgXOffset * ecgPerCount;
        ecgMax =(float) height/ 2;
        //Y轴的压缩比
        ecgYRatio = (float) px1mm *10 / 20971;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Canvas canvas = holder.lockCanvas();
        canvas.drawColor(Color.parseColor(bgColor));
        drawBg(canvas);
        holder.unlockCanvasAndPost(canvas);
        startThread();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h ;
        ecgMax = mHeight/2;
        isRunning = true;
        init();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopThread();
    }

    private void startThread() {
        isRunning = true;
        new Thread(drawRunnable).start();
    }

    private void stopThread(){
        isRunning = false;
        ecg0Datas.clear();
    }

    Runnable drawRunnable = new Runnable() {
        @Override
        public void run() {
            while(isRunning){
                long startTime = System.currentTimeMillis();

                startDrawWave();

                long endTime = System.currentTimeMillis();
                if(endTime - startTime < sleepTime){
                    try {
                        Thread.sleep(sleepTime - (endTime - startTime));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    private void startDrawWave(){
        if (isreSet){
            rect.set(startX, 0, mWidth, mHeight);
            isreSet = false;
        }else {
            rect.set(startX, 0, Integer.parseInt(new DecimalFormat("0").format(startX + lockWidth + blankLineWidth)), mHeight);
        }
        mCanvas = surfaceHolder.lockCanvas(rect);
        if(mCanvas == null) return;
        mCanvas.drawColor(Color.parseColor(bgColor));

        drawBg(mCanvas);
        for (int i=0;i<ecgPerCount;i++){
            Integer value = ecg0Datas.poll();
            if (value !=null){
                waiting.add(value);
            }
            if (waiting.size() ==5){
                break;
            }
        }
        if (waiting.size() ==5) {
            drawWave1();
            //startX = (int) (startX + lockWidth);
            startX = Integer.parseInt ( new DecimalFormat("0").format(startX + lockWidth));
            waiting.clear();
        }
        surfaceHolder.unlockCanvasAndPost(mCanvas);

        if(startX > mWidth){
            startX = 0;
        }
    }
    private void drawBg(Canvas canvas){
        float base = px1s / 25.0f;
        int line =0;
        for (float i=base;i<mHeight;i=i+base){
            line = line +1;
            if (line % 25 ==0){
                canvas.drawLine(0, i, mWidth, i, horizontalBigBoldLine);
            }else if (line % 5 ==0){
                canvas.drawLine(0, i, mWidth, i, horizontalBoldLine);
            }else{
                canvas.drawLine(0, i, mWidth, i, horizontalLine);
            }
        }
        line =0;
        for (float i=base;i<mWidth;i=i+base){
            line = line +1;
            if (line % 25 ==0){
                canvas.drawLine(i, 0, i, mHeight, horizontalBigBoldLine);
            }else if (line % 5 ==0){
                canvas.drawLine(i, 0, i, mHeight, horizontalBoldLine);
            }else{
                canvas.drawLine(i, 0, i, mHeight, horizontalLine);
            }
        }
        canvas.drawLine(60, mHeight / 2, 80, mHeight / 2, criterionLine);
        canvas.drawLine(80, mHeight / 2, 80, mHeight / 2 - 50, criterionLine);
        canvas.drawLine(80, mHeight / 2 - 50, 105, mHeight / 2 - 50, criterionLine);
        canvas.drawLine(105, mHeight / 2 - 50, 105, mHeight / 2, criterionLine);
        canvas.drawLine(105, mHeight / 2, 125, mHeight / 2, criterionLine);

        canvas.drawText("25mm/s 10mm/mv",mWidth-px1s,mHeight-80,textLine);
    }
    //横线
    private Paint horizontalLine;
    //横线加粗
    private Paint horizontalBoldLine;
    //横线加更粗
    private Paint horizontalBigBoldLine;
    //字体样式
    private Paint textLine;
    //竖线
    private Paint verticalLine;
    //竖线加粗
    private Paint verticalBoldLine;
    //竖线加更粗
    private Paint verticalBigBoldLine;
    //凸凸线
    private Paint criterionLine;
    /**
     * 初始化画笔
     */
    private void defaultPaint() {
        horizontalLine = new Paint();
        horizontalLine.setColor(getResources().getColor(R.color.colorF0E8E9));
        horizontalLine.setStrokeWidth(1);
        horizontalLine.setAntiAlias(true);

        horizontalBoldLine = new Paint();
        horizontalBoldLine.setColor(getResources().getColor(R.color.colorE1CED1));
        horizontalBoldLine.setStrokeWidth(2);
        horizontalBoldLine.setAntiAlias(true);

        horizontalBigBoldLine = new Paint();
        horizontalBigBoldLine.setColor(getResources().getColor(R.color.colorD49BAC));
        horizontalBigBoldLine.setStrokeWidth(2);
        horizontalBigBoldLine.setAntiAlias(true);

        verticalLine = new Paint();
        verticalLine.setColor(getResources().getColor(R.color.colorF0E8E9));
        verticalLine.setStrokeWidth(1);
        verticalLine.setAntiAlias(true);

        verticalBoldLine = new Paint();
        verticalBoldLine.setColor(getResources().getColor(R.color.colorE1CED1));
        verticalBoldLine.setStrokeWidth(2);
        verticalBoldLine.setAntiAlias(true);

        verticalBigBoldLine = new Paint();
        verticalBigBoldLine.setColor(getResources().getColor(R.color.colorD49BAC));
        verticalBigBoldLine.setStrokeWidth(2);
        verticalBigBoldLine.setAntiAlias(true);

        criterionLine = new Paint();
        criterionLine.setColor(getResources().getColor(R.color.color33AFFF));
        criterionLine.setStrokeWidth(2);
        criterionLine.setAntiAlias(true);

        textLine = new Paint();
        textLine.setTextSize(45f);
        textLine.setColor(getResources().getColor(R.color.colorPrimary));
    }

    /**
     * 画波
     */
    private void drawWave1(){
        try{
//            drawBg(mCanvas);
            float mStartX = startX;
            for(int i=0;i<ecgPerCount;i++){
                float newX = (float) (mStartX + ecgXOffset);

                Integer value = waiting.get(i);
                float newY = ecgConver(value);
                mCanvas.drawLine(mStartX, startY1, newX, newY, mPaint);
                mStartX = newX;
                startY1 = newY;

            }
        }catch (NoSuchElementException e){}
    }

    /**
     * 将心电数据转换成用于显示的Y坐标
     * @param data
     * @return
     */
    private float ecgConver(int data){
        float y = 0f;
        y = ecgMax - data * ecgYRatio;
        //data = (int) (ecgMax - data);
        return y;
    }

    public void addEcgData0(int data){
        boolean result = ecg0Datas.add(data);
        if (!result){
            ecg0Datas.clear();
            ecg0Datas.add(data);
        }
    }
    public void addEcgData0(int[] data){
        for (int item : data){
            ecg0Datas.add(item);
        }
    }

}
