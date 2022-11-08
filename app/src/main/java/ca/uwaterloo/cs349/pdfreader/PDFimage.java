package ca.uwaterloo.cs349.pdfreader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.os.Parcelable;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

@SuppressLint("AppCompatCustomView")
public class PDFimage extends ImageView {

    final String LOGNAME = "pdf_image";

    // drawing path
    Path path = null;
    //HashMap<Integer, Path> paths;
    ArrayList<Path> paths = new ArrayList();
    ArrayList<Path> hPaths = new ArrayList();
    //ArrayList<Path> ePaths = new ArrayList();

    //HashMap<String, Path> ePaths = new HashMap<>();


    // image to display
    Bitmap bitmap;
    Paint paint = new Paint(Color.BLUE);
    Paint hPaint = new Paint(Color.YELLOW);

    ToggleButton mDraw;
    ToggleButton mEraser;
    ToggleButton mHighlight;
    Button mUndo;
    Button mRedo;

    Stack<Pair<String, Pair<ArrayList<Path>, ArrayList<Path>>>> undoStack = new Stack<>();
    Stack<Pair<String, Pair<ArrayList<Path>, ArrayList<Path>>>> redoStack = new Stack<>();

    ArrayList<Path> drawArr = new ArrayList<>();
    ArrayList<Path> highlightArr = new ArrayList<>();


    // constructor
    public PDFimage(Context context, ToggleButton draw, ToggleButton eraser, ToggleButton highlight, Button undo,
                    Button redo) {
        super(context);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(7);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.BLUE);

        hPaint.setAntiAlias(true);
        hPaint.setStrokeWidth(37);
        hPaint.setStrokeCap(Paint.Cap.ROUND);
        hPaint.setStyle(Paint.Style.STROKE);
        hPaint.setColor(Color.YELLOW);
        hPaint.setAlpha(35);

        mDraw = draw;
        mEraser = eraser;
        mHighlight = highlight;

        mUndo = undo;
        mRedo = redo;
    }

    public void addToUndoStack(String s, ArrayList<Path> drawArr, ArrayList<Path> highlightArr) {
        Log.d(LOGNAME, "undoStack size originally is: " + undoStack.size());
        if (undoStack.size() == 5) {
            undoStack.remove(0);
            Log.d(LOGNAME, "undoStack remove 0");
        }

        undoStack.push(new Pair<>(s, new Pair<>(drawArr, highlightArr)));


        Log.d(LOGNAME, "undoStack size now is: " + undoStack.size());
        Log.d(LOGNAME, "push msg : " + s);
    }

    boolean pathIntersect(Path p2) {
        Region clip = new Region(0, 0, 2000, 2560);
        Region region1 = new Region();
        region1.setPath(path, clip);
        Region region2 = new Region();
        region2.setPath(p2, clip);

        return region1.op(region2, Region.Op.INTERSECT);
    }



    // capture touch events (down/move/up) to create a path
    // and use that to create a stroke that we can draw
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mHighlight.isChecked()) {
            //paint.setAlpha(35);
            //paint.setStrokeWidth(50);
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.d(LOGNAME, "Action down");
                    //int id = event.getPointerId(0);
                    path = new Path();
                    path.moveTo(event.getX(), event.getY());
                    hPaths.add(path);
                    addToUndoStack("h", new ArrayList<Path>(), new ArrayList<Path>());
                    //paths.put(id, path)
                    break;

                case MotionEvent.ACTION_MOVE:
                    Log.d(LOGNAME, "Action move");
                    path.lineTo(event.getX(), event.getY());
                    break;
                case MotionEvent.ACTION_UP:
                    Log.d(LOGNAME, "Action up");
                    break;

            }
        }
        else if (mDraw.isChecked()) {
            //paint.setAlpha(250);
            //paint.setStrokeWidth(7);
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.d(LOGNAME, "Action down");
                    //int id = event.getPointerId(0);
                    path = new Path();
                    path.moveTo(event.getX(), event.getY());
                    paths.add(path);
                    addToUndoStack("d", new ArrayList<Path>(), new ArrayList<Path>());
                    //paths.put(id, path)
                    break;

                case MotionEvent.ACTION_MOVE:
                    Log.d(LOGNAME, "Action move");
                    path.lineTo(event.getX(), event.getY());
                    break;
                case MotionEvent.ACTION_UP:
                    Log.d(LOGNAME, "Action up");
                    break;

            }
        } else if (mEraser.isChecked()) {

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.d(LOGNAME, "Action down");
                    //int id = event.getPointerId(0);
                    path = new Path();
                    path.moveTo(event.getX(), event.getY());
                    //ePaths.put(paths.size() + "," + hPaths.size(), path);

                    for (int i = 0; i < paths.size(); i++) {
                        if (pathIntersect(paths.get(i))) {
                            drawArr.add(paths.get(i));
                            paths.remove(i);
                            i--;
                        }
                    }
                    for (int i = 0; i < hPaths.size(); i++) {
                        if (pathIntersect(hPaths.get(i))) {
                            highlightArr.add(hPaths.get(i));
                            hPaths.remove(i);
                            i--;
                        }
                    }
                    //paths.put(id, path)
                    break;

                case MotionEvent.ACTION_MOVE:
                    Log.d(LOGNAME, "Action move");
                    path.lineTo(event.getX(), event.getY());

                    for (int i = 0; i < paths.size(); i++) {
                        if (pathIntersect(paths.get(i))) {
                            drawArr.add(paths.get(i));
                            paths.remove(i);
                            i--;
                        }
                    }
                    for (int i = 0; i < hPaths.size(); i++) {
                        if (pathIntersect(hPaths.get(i))) {
                            highlightArr.add(hPaths.get(i));
                            hPaths.remove(i);
                            i--;
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    Log.d(LOGNAME, "Action up");
                    addToUndoStack("e", drawArr, highlightArr);
                    drawArr = new ArrayList<>();
                    highlightArr = new ArrayList<>();
                    break;

            }
        }

        return true;
    }

    // set image as background
    public void setImage(Bitmap bitmap) {
        this.bitmap = bitmap;
    }

    // set brush characteristics
    // e.g. color, thickness, alpha
    public void setBrush(Paint paint) {
        this.paint = paint;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // draw background
        if (bitmap != null) {
            this.setImageBitmap(bitmap);
        }

        //erase
        /*for (String key: ePaths.keySet()) {
            String[] arr = key.split(",");
            int drawNum = Integer.parseInt(arr[0]);
            int highlightNum = Integer.parseInt(arr[1]);
            for (int i = 0; i < drawNum; i++) {
                paths.get(i).op(ePaths.get(key), Path.Op.DIFFERENCE);
            }
            for (int i = 0; i < highlightNum; i++) {
                hPaths.get(i).op(ePaths.get(key), Path.Op.DIFFERENCE);
            }

        }*/

        //ePaths.clear();



        // draw lines over it
        for (Path path : paths) {
            canvas.drawPath(path, paint);
        }


        for (Path path : hPaths) {
            canvas.drawPath(path, hPaint);
        }
        super.onDraw(canvas);
    }


}
