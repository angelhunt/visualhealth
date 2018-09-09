package com.example.nemo.mapdemo.map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import com.example.nemo.mapdemo.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Description:  mapview
 *
 * @author nemo
 * @version 2.0
 * @since 16/4/12
 */
public class ChinaMapView extends View {

    private static final String TAG = "ChinaMapView";

    private Paint paint;

    private int miniWidth;
    private int miniHeight;
    private int provinceTextSize;
    private int provinceMargin;
    private int numberMargin;
    private int bottomPadding;

    private float scale = 1;

    private RectF mapSize;

    private ProvinceItem selectedItem;

    private List<ProvinceItem> itemList;

    private Collection<? extends IProvinceData> dataList;

    private Drawable drawable;

    //ARGB, each two number is a dimension
    private int[] colorArray = new int[]{0xFFd3ffce, 0xFFFFF68F, 0xFFFFc0cb, 0xFFcc0000};
//    private int[] colorArray = new int[]{0xFF008000, 0xFFF6546a, 0xFF990000, 0xFF000000};

    private GestureDetectorCompat gestureDetectorCompat;

    public ChinaMapView(Context context) {
        super(context);
        init(null, 0);
    }

    public ChinaMapView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public ChinaMapView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }



    /**
     * 初始化加载地图数据并设置相关手势监听
     *
     * @param attrs    属性
     * @param defStyle 默认属性
     */
    private void init(AttributeSet attrs, int defStyle) {
        paint = new Paint();
        paint.setAntiAlias(true);
        miniWidth = getContext().getResources().getDimensionPixelSize(R.dimen.map_min_width);
        miniHeight = getContext().getResources().getDimensionPixelSize(R.dimen.map_min_height);
        provinceTextSize = getResources().getDimensionPixelSize(R.dimen.map_province_text_size);
        provinceMargin = getResources().getDimensionPixelSize(R.dimen.map_province_margin);
        numberMargin = getResources().getDimensionPixelSize(R.dimen.map_number_margin);

        gestureDetectorCompat = new GestureDetectorCompat(getContext(), new GestureDetector.SimpleOnGestureListener() {

            @Override
            public boolean onDown(MotionEvent e) {
                float x = e.getX();
                float y = e.getY();
                handlerTouch((int) x, (int) y);
                return true;
            }
//
//            @Override
//            public boolean onSingleTapUp(MotionEvent e) {
//                float x = e.getX();
//                float y = e.getY();
//                return handlerTouch((int) x, (int) y);
//            }

//            @Override
//            public void onShowPress(MotionEvent e) {
//                float x = e.getX();
//                float y = e.getY();
//                handlerTouch((int) x, (int) y);
//            }
        });
        drawable = getResources().getDrawable(R.drawable.scale_rule);

        bottomPadding = getResources().getDimensionPixelSize(R.dimen.map_bottom_padding);

        if (!isInEditMode()) {
            //获取地图svg封装信息
            MapSVGManager.getInstance(getContext()).getProvincePathListAsync((provincePathList, size) -> {

                List<ProvinceItem> list = new ArrayList<>();
                for (ProvincePath provincePath : provincePathList) {
                    ProvinceItem item = new ProvinceItem();
                    item.setPath(provincePath.getPath());
                    item.setProvinceCode(provincePath.getCode());
                    item.setProvinceName(provincePath.getName());
                    list.add(item);
                    Log.i(TAG, "init: get province " + item.provinceName);
                }

                if (dataList != null) {
                    setMapColor(list, dataList);
                }
                mapSize = size;
                itemList = list;
                //刷新布局
                requestLayout();
                postInvalidate();
            });
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        int viewWidth = width;
        int viewHeight = height;

        switch (widthMode) {
            case MeasureSpec.EXACTLY:
                viewWidth = width > miniWidth ? width : miniWidth;
                break;
            case MeasureSpec.AT_MOST:
            case MeasureSpec.UNSPECIFIED:
                viewWidth = miniWidth;
                break;
        }

        int computeHeight;
        if (mapSize != null) {
            double mapWidth = mapSize.width();
            double mapHeight = mapSize.height();
            scale = (float) (viewWidth / mapWidth);
            computeHeight = (int) (mapHeight * viewWidth / mapWidth);
        } else {
            computeHeight = (miniHeight * viewWidth / miniWidth);
        }

        switch (heightMode) {
            case MeasureSpec.EXACTLY:
                viewHeight = height;
                break;
            case MeasureSpec.AT_MOST:
            case MeasureSpec.UNSPECIFIED:
                viewHeight = miniHeight > computeHeight ? miniHeight : computeHeight;
                break;
        }

        if (mapSize != null) {
            double mapWidth = mapSize.width();
            scale = (float) (viewWidth / mapWidth);
        }

        setMeasuredDimension(MeasureSpec.makeMeasureSpec(viewWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(viewHeight + bottomPadding, MeasureSpec.EXACTLY));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetectorCompat.onTouchEvent(event);
    }

    /**
     * 处理手势触摸
     *
     * @param x 当前x
     * @param y 当前y
     * @return 是否触摸到区域部分
     */
    private boolean handlerTouch(int x, int y) {
        ProvinceItem provinceItem = null;
        final List<ProvinceItem> list = itemList;
        if (list == null) {
            return false;
        }
        for (ProvinceItem temp : list) {
            if (temp.isTouched((int) (x / scale), (int) (y / scale))) {
                provinceItem = temp;
                break;
            }
        }

        if (provinceItem != null && !provinceItem.equals(selectedItem)) {
            selectedItem = provinceItem;
            postInvalidate();
        }
        return provinceItem != null;
    }

    /**
     * 设置显示数据
     *
     * @param list 加载数据集合
     */
    public <T extends IProvinceData> void setData(Collection<T> list) {
        if (itemList != null) {
            //重新设置绘制区域信息
            setMapColor(itemList, list);
            postInvalidate();
        }
        dataList = list;
    }

    /**
     * 设置地图区域颜色，根据所占比例来绘制区域颜色深浅即初始化区域绘制信息
     *
     * @param itemList 省份区域集合
     * @param dataList 实际解析数据集合
     */
    private void setMapColor(List<ProvinceItem> itemList, Collection<? extends IProvinceData> dataList) {
        Map<Integer, Integer> map = new HashMap<>();
        if (dataList != null) {
            for (IProvinceData data : dataList) {
                map.put(data.getProvinceCode(), data.getPersonNumber());
            }
        }
        for (ProvinceItem item : itemList) {
            int code = item.getProvinceCode();
            int number = 0;
            if (map.containsKey(code)) {
                //TODO:
                number = map.get(code);
            }
            item.setPersonNumber(number);

            //Log.i("setcolor", String.valueOf(totalNumber));
            int color = Color.WHITE;
            if (number > 0 && number < 30)//50
                color=  colorArray[0];
            else if(number > 30 &&  number < 60)//100
                color = colorArray[1];
            else if(number > 60 && number < 90)//150
                color = colorArray[2];
            else if(number > 90 && number < 120)//200
                color = colorArray[3];
            item.setDrawColor(color);
        }
    }



    public void loadMapData() {

        List<ProvinceData> data = parseDemoData();
        setData(data);
//                .subscribeOn(Schedulers.io())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(
//                        dataList -> this.setData(dataList),
//                        throwable -> Toast.makeText(getContext(), "加载演示数据失败！", Toast.LENGTH_SHORT).show()
//                );
    }

    /**
     * 异步解析演示数据
     *
     * @return 异步结果
     */
    private List<ProvinceData> parseDemoData() {

//        return Observable.create(subscriber -> {

            List<ProvinceData> demoDataList = new ArrayList<>();
            try {
                InputStreamReader isr = new InputStreamReader(getContext().getAssets().open("result.json"), "UTF-8");
                BufferedReader br = new BufferedReader(isr);
                String line;
                StringBuilder builder = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    builder.append(line);
                }
                br.close();
                isr.close();

                JSONObject root = new JSONObject(builder.toString());
                JSONArray dataList = root.optJSONArray("province");
                if (dataList != null && dataList.length() != 0) {
                    for (int i = 0; i < dataList.length(); i++) {

                        ProvinceData item = new ProvinceData();
                        JSONObject obj = dataList.optJSONObject(i);

                        item.setNumber(obj.optInt("number"));
                        item.setProvinceId(obj.optInt("provinceName"));
                        item.setProvinceId(obj.optInt("provinceId"));
                        demoDataList.add(item);
                    }
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }
            return demoDataList;

//            subscriber.onNext(demoDataList);
//            subscriber.onCompleted();
//        });
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final List<ProvinceItem> list = itemList;
        //loadMapData(this);
        if (list != null) {

            int width = getWidth();
            int height = getHeight();
            canvas.save();
            canvas.scale(scale, scale);
            loadMapData();
            for (ProvinceItem item : list) {
                if (!item.equals(selectedItem)) {
//                    item.setDrawColor(colorArray[1]);
                    item.drawItem(canvas, paint, false);
                }
            }
            if (selectedItem != null) {
                selectedItem.drawItem(canvas, paint, true);
            }

            canvas.restore();

            if (selectedItem != null) {

                paint.setTypeface(Typeface.DEFAULT);
                paint.setColor(0xFF000000);
                paint.setTextAlign(Paint.Align.CENTER);
                paint.clearShadowLayer();
                paint.setTextSize(provinceTextSize);
                String provinceName = selectedItem.getProvinceName();
                canvas.drawText(provinceName, width / 2, provinceMargin, paint);

                int number = selectedItem.getPersonNumber();
                canvas.drawText("aqi " + number, width / 2, provinceMargin + provinceTextSize + numberMargin, paint);

            }

            if (drawable instanceof BitmapDrawable) {
                Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                int bitmapHeight = bitmap.getHeight();
                int bitmapWidth = bitmap.getWidth();
                drawable.setBounds(0, height - bitmapHeight, bitmapWidth, height);
                drawable.draw(canvas);
            }

        }
    }

    /**
     * 地图绘制省份区域信息
     */
    private static class ProvinceItem {
        /**
         * 区域路径
         */
        private Path path;

        /**
         * 区域背景色，默认白色
         */
        private int drawColor = Color.WHITE;

        /**
         * 区域省份名称
         */
        private String provinceName;

        /**
         * 区域省份编码
         */
        private int provinceCode;

        /**
         * 区域省份人数
         */
        private int personNumber;

        /**
         * 区域绘制方法
         *
         * @param canvas     画布
         * @param paint      画笔
         * @param isSelected 是否选中
         */
        void drawItem(Canvas canvas, Paint paint, boolean isSelected) {

            //选中时绘制阴影描边效果
            if (isSelected) {
                paint.setStrokeWidth(2);
                paint.setColor(Color.BLACK);
                paint.setStyle(Paint.Style.FILL);
                paint.setShadowLayer(8, 0, 0, 0xFFFFFFFF);
                canvas.drawPath(path, paint);

                paint.clearShadowLayer();
                paint.setColor(drawColor);
                paint.setStyle(Paint.Style.FILL);
                paint.setStrokeWidth(2);
                canvas.drawPath(path, paint);

            } else {
                //非选中时，绘制描边效果
                paint.clearShadowLayer();
                paint.setStrokeWidth(1);
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(drawColor);
                canvas.drawPath(path, paint);

                paint.setStyle(Paint.Style.STROKE);
                int strokeColor = 0xFFD0E8F4;
                paint.setColor(strokeColor);
                canvas.drawPath(path, paint);
            }
        }

        /**
         * 判断该区域是否处于touch状态
         *
         * @param x 当前x
         * @param y 当前y
         * @return 是否处于touch状态
         */
        boolean isTouched(int x, int y) {
            RectF r = new RectF();
            path.computeBounds(r, true);

            Region region = new Region();
            region.setPath(path, new Region((int) r.left, (int) r.top, (int) r.right, (int) r.bottom));
            return region.contains(x, y);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            ProvinceItem item = (ProvinceItem) o;

            return provinceCode == item.provinceCode;

        }

        @Override
        public int hashCode() {
            return provinceCode;
        }

        public Path getPath() {
            return path;
        }

        void setPath(Path path) {
            this.path = path;
        }

        public int getDrawColor() {
            return drawColor;
        }

        void setDrawColor(int drawColor) {
            this.drawColor = drawColor;
        }

        String getProvinceName() {
            return provinceName;
        }

        void setProvinceName(String provinceName) {
            this.provinceName = provinceName;
        }

        int getProvinceCode() {
            return provinceCode;
        }

        void setProvinceCode(int provinceCode) {
            this.provinceCode = provinceCode;
        }

        int getPersonNumber() {
            return personNumber;
        }

        void setPersonNumber(int personNumber) {
            this.personNumber = personNumber;
        }
    }
}
