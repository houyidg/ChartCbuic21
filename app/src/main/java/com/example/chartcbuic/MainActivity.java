package com.example.chartcbuic;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;

import com.example.chartcbuic2.R;
import com.polonium.linechart.Line;
import com.polonium.linechart.LineChartView;
import com.polonium.linechart.LinePoint;
import com.polonium.linechart.TextAlign;

public class MainActivity extends Activity {
	ArrayList<Line> lines = new ArrayList<Line>();//所需要显示的line集合
	LineChartView   chart;
	private Integer maxY=0;//point最大的y轴值
	private Integer minY=0;//point最小的y轴值
	private Integer paddingBtm=56;//y轴方向曲线离画布低的最小距离
	private Integer paddingTop=56;//y轴方向曲线离画布顶的最小距离
	private Integer startX=0;//曲线图开始的位置
	private Integer endX=100;//
	private Integer subPoints = 4;//目前还没找到作用
	private boolean isAnim= true;//绘制曲线是否需要动画
	boolean isExsitNegativeNumber=false;//数据中是否存在负数
	private LinePoint.Type pointType = LinePoint.Type.TRIANGLE;
	private float radius=10;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_main);
			//首先初始化数据
			List<WeatherData> initdata = initdata();
		    //格式化数据
			formData(initdata);
			 //初始化chart
		    initchart();
		    //产生曲线图标
	        formLines2(initdata);
	        //开启曲线绘画
	        chart.startDraw(isAnim, 10);
	        //设置点击
	        findViewById(R.id.btn_refresh2).setOnClickListener(new OnClickListener() {
	        	@Override
	        	public void onClick(View v) {
	        		lines = generateLines(0, 100, initdata());
	        		chart.refreshData(lines, isAnim);
	        	}
	        });
	}

	/**
	 * 初始化chart
	 */
	private void initchart() {
		chart = (LineChartView) findViewById(R.id.chart);
		chart.setViewPort(0, 0, endX, maxY+paddingTop);//设置整个曲线的大小
		chart.setViewPortMargins(5,5, 5, 0);//設置內部曲線的margin
	}

	/**
	 * 格式化数据，获取最大值和最小值
	 * @param initdata
	 * @return
	 */
	private void formData(List<WeatherData> initdata) {
		List<Integer> list= new ArrayList<Integer>();
		if(initdata==null){return ;}
		for (WeatherData data : initdata) {
			if(minY>data.minDegree){
				minY=data.minDegree;
			}
			if(maxY<data.maxDegree){
				maxY=data.maxDegree;
			}
		}
		if(minY<0){
			maxY+= (Math.abs(minY)+paddingBtm);
			isExsitNegativeNumber =true;
			minY = Math.abs(minY)+paddingBtm;
		}else if(minY>=0 && minY<paddingBtm){//上移
			maxY+=paddingBtm-minY;
			minY=paddingBtm-minY;
			isExsitNegativeNumber =true;
		}
		list.add(maxY);
		list.add(minY);
	}

	/**
	 * 产生曲线图标
	 */
	private void formLines2(List<WeatherData> data) {
		lines = generateLines(startX, endX, data);
		if(lines!=null && lines.size()>0)
		for(Line line:lines){
			if(isAnim){
				chart.addAnimLine(line);
			}else{
				chart.addLine(line);
			}
		}
	}

	/**
	 * 初始化数据
	 * @return
	 */
	private List<WeatherData> initdata() {
		List<WeatherData> weatherDataList = new ArrayList<MainActivity.WeatherData>();
		weatherDataList.add(new WeatherData(20,-10));
		weatherDataList.add(new WeatherData(20,19));
		weatherDataList.add(new WeatherData(18,17));
		weatherDataList.add(new WeatherData(30,29));
		weatherDataList.add(new WeatherData(12,-0));
		weatherDataList.add(new WeatherData(29,3));
		return weatherDataList;
	}

	/**
	 * 装配曲线集合
	 * @param startX
	 * @param endX
	 * @param wData
	 * @return
	 */
	 private ArrayList<Line> generateLines(int startX, int endX, List<WeatherData> wData) {
		 Line hline = new Line(this);
		 Line lline = new Line(this);
		 ArrayList<Line> lines=  new ArrayList<Line>();
		 if(wData==null||wData.size()<3){return lines;}
		 int base = (wData.size()-2);
		 //计算x轴两点间距
		 float xDistance=(endX-startX)/(base*1f);
		 float changeX=0;
		 int changeMaxY=0;
		 int changeMinY=0;
		 LinePoint minPoint;
		 LinePoint maxPoint;
		 for (int i = 0; i < wData.size(); i++) {
			 WeatherData data = wData.get(i);
			 changeMinY = data.minDegree;
			 changeMaxY = data.maxDegree;
			 if(i==1 || i==wData.size()-1){//x轴第一块间距 xDistance/2;//x轴最后一块间距 xDistance/2;
				 changeX+=(xDistance/2);
			 }else if(i==0){
				 changeX = startX;
			 }else{
				 changeX+=xDistance;
			 }
			 //这里对负数做了处理
			 minPoint = new LinePoint(this, changeX, isExsitNegativeNumber?(data.minDegree+Math.abs(minY)):data.minDegree);
			 maxPoint = new LinePoint(this, changeX, isExsitNegativeNumber?(data.maxDegree+Math.abs(minY)):data.maxDegree);
			 
			 //如果是第一个点和最后一个点 就不显示
			 if(i==0 || i==wData.size()-1){//x轴第一块间距 xDistance/2;//x轴最后一块间距 xDistance/2;
				 minPoint.setVisible(false);
				 maxPoint.setTextVisible(false);
			 }else{
				 minPoint.setVisible(true);
				 minPoint.setTextVisible(true);
				 minPoint.setTextAlign(TextAlign.BOTTOM|TextAlign.HORIZONTAL_CENTER);
				 minPoint.setRadius(radius);
				 minPoint.setType(pointType);
				 minPoint.setStrokeColor(getResources().getColor(R.color.white));
				 minPoint.setTextColor(getResources().getColor(R.color.light_green));
				 minPoint.setText(""+changeMinY+"°");
				 maxPoint.setVisible(true);
				 maxPoint.setTextVisible(true);
				 maxPoint.setTextAlign(TextAlign.TOP|TextAlign.HORIZONTAL_CENTER);
				 maxPoint.setRadius(radius);
				 maxPoint.setType(pointType);
				 maxPoint.setStrokeColor(getResources().getColor(R.color.white));
				 maxPoint.setTextColor(getResources().getColor(R.color.white));
				 maxPoint.setText(""+changeMaxY+"°");
			 }
			 hline.addPoint(maxPoint);
			 lline.addPoint(minPoint);
		 }
		 //设置Line的属性
		 hline.setFilled(true)
			.setColor(getResources().getColor(R.color.white))
			.setStrokeWidth(1)
			.smoothLine(subPoints)
			.setFilledColor(getResources().getColor(R.color.light_gray));
		 
		 lline.setFilled(true)
			.setColor(getResources().getColor(R.color.white))
			.setStrokeWidth(1)
			.smoothLine(subPoints)
			.setFilledColor(getResources().getColor(R.color.light_light_gray));
		 lines.add(hline);
		 lines.add(lline);
		 return lines;
	 }
	 
	 private class WeatherData{
		 public WeatherData(int maxDegree, int minDegree) {
			super();
			this.maxDegree = maxDegree;
			this.minDegree = minDegree;
		}
		public int maxDegree;
		 public int minDegree;
	 }
}
