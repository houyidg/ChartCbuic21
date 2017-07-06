package com.polonium.linechart;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import com.example.chartcbuic2.R;
import com.polonium.linechart.LinePoint.Type;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * View for drawing line charts.
 */
public class LineChartView extends View implements Runnable {
    private static final boolean Debug = true;
	private static final String TAG = "LineChartView TAg";
    private ArrayList<Line> mLines = new ArrayList<Line>();
    private float mViewPortLeft = 0;
    private float mViewPortRight = 0;
    private float mViewPortTop = 0;
    private float mViewPortBottom = 0;
    private float mViewPortMarginLeft, mViewPortMarginRight, mViewPortMarginTop, mViewPortMarginBottom;
    private Shader cropViewPortShader;
    private float mScaleX;
    private float mScaleY;
    private Matrix mMatrix = new Matrix();
    private float mMaxX = Float.MIN_VALUE;
    private float mMaxY = Float.MIN_VALUE;
    private float mMinX = Float.MAX_VALUE;
    private float mMinY = Float.MAX_VALUE;
    private int BASE_ANIMTIME=10;//ondraw一次最长的停滞时间
    private int ACCELERATE_RATE=1;//每次ondraw y轴增量的 加速度  
    private int INCREMENT_NUMBER=5;//每次ondraw y轴的增量
    private int SOOMTH_CUBIC_SUB_POINT_NUM=5;//曲线里子点数量
    /**
     * Instantiates a new line chart view.
     * @param context the context of activity
     */
    public LineChartView(Context context) {
        super(context);
        initDefault(context);
    }

    public LineChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initDefault(context);
        setAttr(context, attrs);
    }

    /**
     * Instantiates a new line chart view.
     * @param context the context
     * @param attrs the attrs
     */
    public LineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initDefault(context);
        setAttr(context, attrs);
    }

    private void setAttr(Context context, AttributeSet attrs) {
       
    }

    /**
     * Initialization after instantiation
     * @param context  the context
     *            这里面初始化布局的各种参数
     */
    private void initDefault(Context context) {
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        setViewPortMargins(0,
                           TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 20, getResources().getDisplayMetrics()),
                           0,
                           0);
       
        setViewPort(0, 0, 100, 100);
    }


    /**
     * Removes all lines.
     * @see #addLine
     */
    public void removeAllLines() {
        while (mLines.size() > 0) {
            mLines.remove(0);
        }
        postInvalidate();
    }

    /**
     * Adds the line for drawing.
     * @param line
     */
    public void addLine(Line line) {
        mLines.add(line);
        for (LinePoint point : line.getPoints()) {
            mMaxX = point.getX() > mMaxX ? point.getX() : mMaxX;
            mMaxY = point.getY() > mMaxY ? point.getY() : mMaxY;
            mMinX = point.getX() < mMinX ? point.getX() : mMinX;
            mMinY = point.getY() < mMinY ? point.getY() : mMinY;
        }
    }

    public void addAnimLine(Line line) {
    	mLines.add(line);
    	//初始化矩阵
    	for (LinePoint point : line.getPoints()) {
    		point.predictY= point.getY();
    		point.setY(0f);
    		point.setVisible(false);
    		point.setTextVisible(false);
    		mMaxX = Math.max(point.getX(), mMaxX);
    		mMaxY =Math.max(point.getY(), mMaxY); 
    		mMinX = point.getX() < mMinX ? point.getX() : mMinX;
    		mMinY = point.getY() < mMinY ? point.getY() : mMinY;
    	}
    }

    /**
     * 开始绘画曲线
     * @param isDrawAnim
     * @param delay 
     */
    public void startDraw(boolean isDrawAnim,int delay){
    	if(isDrawAnim){
    		//开启runnable
        	postDelayed(this, delay);
    	}else{
    		 postInvalidate();
    	}
    }
   
    /**
     * Correct min and max values for ViewPort moving limits.
     */
    private void limitsCorrection() {
        mMaxX = Float.MIN_VALUE;
        mMaxY = Float.MIN_VALUE;
        mMinX = Float.MAX_VALUE;
        mMinY = Float.MAX_VALUE;
        for (Line line : mLines)
            for (LinePoint point : line.getPoints()) {
                mMaxX = point.getX() > mMaxX ? point.getX() : mMaxX;
                mMaxY = point.getY() > mMaxY ? point.getY() : mMaxY;
                mMinX = point.getX() < mMinX ? point.getX() : mMinX;
                mMinY = point.getY() < mMinY ? point.getY() : mMinY;
            }

        mMaxX = (mMaxX < mViewPortRight) ? mViewPortRight : mMaxX;
        mMaxY = (mMaxY < mViewPortTop) ? mViewPortTop : mMaxY;
        mMinX = (mMinX > mViewPortLeft) ? mViewPortLeft : mMinX;
        mMinY = (mMinY > mViewPortBottom) ? mViewPortBottom : mMinY;
    }

    /**
     * @param index the index
     * @return {@link Line}
     */
    public Line getLine(int index) {
        return mLines.get(index);
    }

    /**
     * @return lines count
     */
    public int getLinesCount() {
        return mLines.size();
    }

    /**
     * Sets ViewPort - part of full chart for drawing
     * 
     * @param left
     *            minimal horizontal value of chart to draw
     * @param bottom
     *            minimal vertical value of chart to draw
     * @param right
     *            maximal horizontal value of chart to draw
     * @param top
     *            maximal horizontal value of chart to draw
     */
    public void setViewPort(float left, float bottom, float right, float top) {
        mViewPortLeft = left;
        mViewPortRight = right;
        mViewPortTop = top;
        mViewPortBottom = bottom;
        limitsCorrection();
    }

    /**
     * Sets ViewPort margins from view sides in pixels.
     * 
     * @param left
     *            left
     * @param bottom
     *            bottom
     * @param right
     *            right
     * @param top
     *            top
     * @see #setViewPortMargins
     */
    public void setViewPortMargins(float left, float bottom, float right, float top) {
        mViewPortMarginLeft = left;
        mViewPortMarginRight = right;
        mViewPortMarginTop = top;
        mViewPortMarginBottom = bottom;
        cropViewPortShader = null;
        scaleCorrection();
    }

    /**
     * Sets ViewPort margins from view sides in dip.
     * 
     * @param left the left
     * @param bottom the bottom
     * @param right  the right
     * @param top  the top
     * @see #setViewPortMargins
     */
    public void setViewPortMarginsDP(float left, float bottom, float right, float top) {
        mViewPortMarginLeft = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                                                        left,
                                                        getResources().getDisplayMetrics());
        mViewPortMarginRight = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                                                         right,
                                                         getResources().getDisplayMetrics());
        mViewPortMarginTop = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                                                       top,
                                                       getResources().getDisplayMetrics());
        mViewPortMarginBottom = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                                                          bottom,
                                                          getResources().getDisplayMetrics());
        cropViewPortShader = null;
        scaleCorrection();
    }

    /*
     * (non-Javadoc)
     * @see android.view.View#onMeasure(int, int)
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Log.i("", "onMeasure");
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        scaleCorrection();
    }

    /**
     * Scale correction.
     */
    private void scaleCorrection() {
        mScaleX = (mViewPortRight - mViewPortLeft != 0) ? 
		(getMeasuredWidth() - mViewPortMarginLeft - mViewPortMarginRight) / (mViewPortRight - mViewPortLeft)  : 1;
		
		if(Debug){
			Log.e(TAG, "mScaleY:"+mScaleY);
		}
        mScaleY = (mViewPortTop - mViewPortBottom != 0) ? 
		(getMeasuredHeight() - mViewPortMarginTop - mViewPortMarginBottom) / (mViewPortTop - mViewPortBottom) : 1;
    }

    /*
     * (non-Javadoc)
     * @see android.view.View#onDraw(android.graphics.Canvas)
     */
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isInEditMode()) {
            return;
        }
        //核心代码开始、先cropCanvas（先修剪一块画布）
        if (cropViewPortShader == null) {
            Bitmap cropBitmap = Bitmap.createBitmap(getMeasuredWidth(), getMeasuredHeight(), Config.ALPHA_8);
            Canvas cropCanvas = new Canvas(cropBitmap);
            Paint cropPaint = new Paint();
            cropPaint.setColor(0xffffffff);
            cropPaint.setStyle(Paint.Style.FILL);
            cropCanvas.drawRect(mViewPortMarginLeft - 1,
                                mViewPortMarginTop - 1,//top
                                getWidth() - mViewPortMarginRight + 1,//Right
                                getHeight() - mViewPortMarginBottom + 1,//bottom
                                cropPaint);
            //把位图放于着色器，进行渲染
            /**
             * BitmapShader    : 主要用来渲染图像
				LinearGradient  :用来进行线性渲染
				RadialGradient  : 用来进行环形渲染
				SweepGradient   : 扫描渐变---围绕一个中心点扫描渐变就像电影里那种雷达扫描，用来梯度渲染。
				ComposeShader   : 组合渲染，可以和其他几个子类组合起来使用。
             */
            cropViewPortShader = new BitmapShader(cropBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        }
        for (Line line : mLines) {
            drawLine(canvas, line);
        }
        drawPoints(canvas);
    }


	/**
     * Draw line.
     * 含有覆盖的部分
     * @param canvas canvas
     * @param line line
     */
    private void drawLine(Canvas canvas, Line line) {
        Path pathCopy;
        if (line.isFilled()) {
            pathCopy = new Path(line.getFilledPath());
            mMatrix.reset();
            mMatrix.setScale(mScaleX, -mScaleY);//x为宽度、y为高的负数，表明是倒置

            pathCopy.transform(mMatrix);//适应matrix
            mMatrix.reset();
            mMatrix.setTranslate(-mViewPortLeft * mScaleX + mViewPortMarginLeft, (mViewPortTop) * mScaleY + mViewPortMarginTop);
            pathCopy.transform(mMatrix);
            line.getFilledPaint().setShader(cropViewPortShader);
            canvas.drawPath(pathCopy, line.getFilledPaint());
        }

        pathCopy = new Path(line.getPath());
        mMatrix.reset();
        mMatrix.setScale(mScaleX, -mScaleY);
        pathCopy.transform(mMatrix);
        mMatrix.reset();
        mMatrix.setTranslate(-mViewPortLeft * mScaleX + mViewPortMarginLeft, (mViewPortTop) * mScaleY
                                                                             + mViewPortMarginTop);
        pathCopy.transform(mMatrix);
        line.getPaint().setShader(cropViewPortShader);
        canvas.drawPath(pathCopy, line.getPaint());
    }

    private void drawPoints(Canvas canvas) {
        for (Line line : mLines) {
            for (LinePoint point : line.getPoints()) {
                drawPoint(canvas, point);
                drawPointText(canvas, point);
            }
        }
    }

    private void drawPoint(Canvas canvas, LinePoint point) {
        if (point.isVisible()) {
            float x = point.getX() * mScaleX - mViewPortLeft * mScaleX + mViewPortMarginLeft;
            float y = point.getY() * (-mScaleY) + mViewPortTop * mScaleY + mViewPortMarginTop;
            if (x + (point.getRadius() + point.getStrokePaint().getStrokeWidth()) > mViewPortMarginLeft 
                    && x - (point.getRadius() + point.getStrokePaint().getStrokeWidth()) < getWidth() - mViewPortMarginRight
                && y + (point.getRadius() + point.getStrokePaint().getStrokeWidth()) > mViewPortMarginTop
                && y - (point.getRadius() + point.getStrokePaint().getStrokeWidth()) < getHeight() - mViewPortMarginBottom) {
                point.getFillPaint().setShader(cropViewPortShader);
                point.getStrokePaint().setShader(cropViewPortShader);
                if (point.getType() == Type.CIRCLE) {
                    canvas.drawCircle(x, y, point.getRadius(), point.getFillPaint());
                    canvas.drawCircle(x, y, point.getRadius(), point.getStrokePaint());
                } else if (point.getType() == Type.SQUARE) {
                    canvas.drawRect(x - point.getRadius(),
                                    y - point.getRadius(),
                                    x + point.getRadius(),
                                    y + point.getRadius(),
                                    point.getFillPaint());
                    canvas.drawRect(x - point.getRadius(),
                                    y - point.getRadius(),
                                    x + point.getRadius(),
                                    y + point.getRadius(),
                                    point.getStrokePaint());
                } else if (point.getType() == Type.TRIANGLE) {
                    Path path = new Path();
                    path.moveTo(x, y - point.getRadius());
                    path.lineTo(x - 0.86f * point.getRadius(), y + 0.5f * point.getRadius());
                    path.lineTo(x + 0.86f * point.getRadius(), y + 0.5f * point.getRadius());
                    path.close();
                    canvas.drawPath(path, point.getFillPaint());
                    canvas.drawPath(path, point.getStrokePaint());
                }
            }
        }
    }

    private void drawPointText(Canvas canvas, LinePoint point) {
        float x = point.getX() * mScaleX - mViewPortLeft * mScaleX + mViewPortMarginLeft;
        float y = point.getY() * (-mScaleY) + mViewPortTop * mScaleY + mViewPortMarginTop;
        if (point.isTextVisible()) {
            point.getTextPaint().setTextAlign(Align.CENTER);
            point.getTextPaint().setShader(cropViewPortShader);
            float txtX = x;
            float txtY = y + (point.getTextPaint().getTextSize() - point.getTextPaint().descent()) / 2;
            if ((point.getTextAlign() & TextAlign.LEFT) > 0) {
                point.getTextPaint().setTextAlign(Align.RIGHT);
                txtX = x - point.getRadius() - point.getTextPaint().descent();
            } else if ((point.getTextAlign() & TextAlign.RIGHT) > 0) {
                point.getTextPaint().setTextAlign(Align.LEFT);
                txtX = x + point.getRadius() + point.getTextPaint().descent();
            }

            if ((point.getTextAlign() & TextAlign.TOP) > 0) {
                txtY = y - point.getRadius() - point.getTextPaint().descent();
            } else if ((point.getTextAlign() & TextAlign.BOTTOM) > 0) {
                txtY = y + point.getRadius() + point.getTextPaint().descent() + point.getTextPaint().getTextSize();
            }
            canvas.drawText(point.getText(), txtX, txtY, point.getTextPaint());
        }
    }
   
	@Override
	public void run() {
		// TODO Auto-generated method stub
		//1、每10ms更新y轴数据+1
		boolean isFinished=true;
		float maxOverPlus=1;//最大的剩餘值，用于缓冲加速
		float maxPredictY=1;
		for(Line line:mLines){
			for(LinePoint point:line.getPoints()){
				if(point.predictY>point.getY()){//目前的y值还未达到预期的y，则没完成，继续+1
					isFinished=false;
					point.setY(point.getY()+INCREMENT_NUMBER);
					if(maxOverPlus<(point.predictY-point.getY())){
						maxOverPlus=(int) (point.predictY-point.getY());
					}
					if(maxPredictY<(point.predictY)){
						maxPredictY=(int) (point.predictY);
					}
				}else{
					if(point==line.getPoints().get(0)||point==line.getPoints().get(line.getPoints().size()-1)){
						
					}else{
						point.setY(point.predictY);
						point.setVisible(true);
						point.setTextVisible(true);
					}
				}
			}
			//计算一次曲线path
			line.smoothLine(SOOMTH_CUBIC_SUB_POINT_NUM);
		} 
		int extraValue = Math.round(ACCELERATE_RATE*BASE_ANIMTIME*(maxOverPlus/maxPredictY));//maxOverPlus是y轴剩余要上升的value，value越小extraValue越小，也就是到最后速度越快
		//2、判断是否全都达到预期值，如果达到则不用postDelay()
		if(!isFinished){
			if(Debug){
				Log.e(TAG, "baseAnimTime+extraValue:"+(BASE_ANIMTIME+extraValue));
			}
			postDelayed(this, BASE_ANIMTIME+extraValue);//缓冲加速、
		}
		invalidate();
	}
	/**
	 * 刷新界面
	 * @param lines 对应的曲线集合
	 * @param isAnim 是否需要动画 刷新
	 */
	public void refreshData(ArrayList<Line> lines,boolean isAnim){
		if(lines!=null){
			this.removeAllLines();
			for(Line line:lines){
				if(isAnim){
					addAnimLine(line);
				}else{
					addLine(line);
				}
			}
			startDraw(isAnim,0);
		}
	}
}
