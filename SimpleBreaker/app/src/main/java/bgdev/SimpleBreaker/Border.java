package bgdev.SimpleBreaker;

import android.graphics.RectF;

/**
 * Created by bryan on 1/24/2018.
 */

public class Border {

    // RectF is an object that holds four coordinates - just what we need
    private RectF rect;
    private RectF innerRect;

    // How long and high our paddle will be
    private float length;
    private float height;

    // X is the far left of the rectangle which forms our paddle
    private float x;

    // Y is the top coordinate
    private float y;



    // This the the constructor method
    // When we create an object from this class we will pass
    // in the screen width and height
    public Border(int screenX, int screenY){
        // 130 pixels wide and 20 pixels high
        length = screenX - (float)(screenX * .1);
        height = screenY - (float)(screenY * .05);

        // Start paddle in roughly the screen centre
        reset(screenX, screenY);


    }

    // This is a getter method to make the rectangle that
    // defines our paddle available in BreakoutView class
    public RectF getRect(){
        return rect;
    }

    public RectF getInnerRect(){
        return innerRect;
    }

    public void reset(int screenX, int screenY){
        x = (float)(screenX *.05);
        y = (float)(screenY * .025);

        rect = new RectF(x, y, x + length, y + height);
        innerRect = new RectF(x + 5, y + 5, x + length - 5, y + height - 5);
    }

}
