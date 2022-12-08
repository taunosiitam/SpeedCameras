package net.tralls.speedcameras;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class MirrorLayout extends RelativeLayout {

    private boolean isMirrored = false;

    public MirrorLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    void toggleMirror() {
        isMirrored = !isMirrored;
        postInvalidate();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.save();
        if (isMirrored) canvas.scale(1, -1, 0, getHeight() / 2f);
        super.dispatchDraw(canvas);
        canvas.restore();
    }

}
