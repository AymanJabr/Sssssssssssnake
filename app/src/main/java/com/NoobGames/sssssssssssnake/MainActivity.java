package com.NoobGames.sssssssssssnake;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.NoobGames.sssssssssssnake.GameActivity;

public class MainActivity extends AppCompatActivity {

    Canvas canvas;
    SnakeAnimView snakeAnimView;
    //snake head sprite sheet
    Bitmap headAnimBitmap;
    //the portion of the bitmap to be drawn in the current frame
    Rect rectToBeDrawn;
    //The dimension of a single frame
    //miraculously I found a picture that is  320 X 256, with 5 by 4 frames size, each
    // of size 64 X 64
    int frameHeight = 64;
    int frameWidth = 64;
    int numFramesWidth = 5;
    int numFramesHeight = 4;// maybe I won't use this.
    int frameNumber;

    Bitmap dinahBitmap;
    int currentImage;


    int screenWidth;
    int screenHeight;

    //stats
    long lastFrameTime;
    int fps;
    int hi;

    //to start the fame from onTouchEvent
    Intent i;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        //get the width and height of the screen
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenHeight = size.y;
        screenWidth = size.x;

        dinahBitmap = BitmapFactory.decodeResource(getResources(), getCorrectPicture(currentImage));
//        headAnimBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.snake_sprite_sheet);
        snakeAnimView = new SnakeAnimView(this);
        setContentView(snakeAnimView);
        i = new Intent(this, GameActivity.class);
    }

    private int getCorrectPicture(int currentPicture) {

        Log.i("CURREEEENT PICTURE : ", currentPicture + "");
        switch (currentPicture) {
            case 0:
                return R.drawable.snake_no_toungue;
            case 1:
                return R.drawable.snake_one_toungue;
            case 2:
                return R.drawable.snake_two_toungue;
            case 3:
                return R.drawable.snake_four_toungue;
        }

        return -1;
    }


    class SnakeAnimView extends SurfaceView implements Runnable {
        Thread ourThread = null;
        SurfaceHolder ourHolder;
        volatile boolean playingSnake;
        Paint paint;

        public SnakeAnimView(Context context) {

            super(context);
            ourHolder = getHolder();
            paint = new Paint();
//            frameWidth = headAnimBitmap.getWidth() / numFramesWidth;
//            frameHeight = headAnimBitmap.getHeight() / numFramesHeight;
            frameWidth = dinahBitmap.getWidth();
            frameHeight = dinahBitmap.getHeight();
        }


        @Override
        public void run() {
            while (playingSnake) {
                update();
                draw();
                controlFPS();
            }
        }

        public void update() {

            // change the value of current image
            if (currentImage == 3) {
                currentImage = 0;
            } else {
                currentImage++;
            }

            dinahBitmap = BitmapFactory.decodeResource(getResources(), getCorrectPicture(currentImage));




            //which frame should be drawn
            //This is going to be highly divergent from the tutorial
//            switch (frameNumber){
//                case 0:
//                case 1:
//                case 2:
//                case 3:
//                case 4:
//                    rectToBeDrawn = new Rect((frameNumber  * frameWidth) - 1, 0 ,
//                            ((frameNumber - 5) * frameWidth + frameWidth) - 1, frameHeight);
//                    break;
//                case 5:
//                case 6:
//                case 7:
//                case 8:
//                case 9:
//                    rectToBeDrawn = new Rect(((frameNumber - 5) * frameWidth) - 1, frameHeight ,
//                            ((frameNumber - 5) * frameWidth + frameWidth) - 1, frameHeight * 2);
//                    break;
//                case 10:
//                case 11:
//                case 12:
//                case 13:
//                case 14:
//                    rectToBeDrawn = new Rect(((frameNumber - 10) * frameWidth) - 1, frameHeight * 2 ,
//                            ((frameNumber - 10) * frameWidth + frameWidth) - 1, frameHeight * 3);
//                    break;
//                case 15:
//                case 16:
//                case 17:
//                case 18:
//                case 19:
//                    rectToBeDrawn = new Rect(((frameNumber - 15) * frameWidth) - 1, frameHeight * 3 ,
//                            ((frameNumber - 15) * frameWidth + frameWidth) - 1, frameHeight * 4);
//                    break;
//
//            }

//            frameNumber++;

//            if (frameNumber == 19) {
//                frameNumber = 0;
//            }



        }

        public void draw() {

            if (ourHolder.getSurface().isValid()) {
                canvas = ourHolder.lockCanvas();
                canvas.drawColor(Color.BLACK);
                paint.setColor(Color.argb(255, 255, 255, 255));
                paint.setTextSize(150);
                canvas.drawText("Snake: ", 10, 150, paint);
                paint.setTextSize(25);
                canvas.drawText("   Highscore: " + hi, 10, screenHeight - 10, paint);

                //draw the snake head
                //make this Rect whatever size and location you like ???????
                Rect destRect = new Rect(screenWidth / 2 - 500, screenHeight / 2 - 500,
                        screenWidth / 2 + 500, screenHeight / 2 + 500);

//                canvas.drawBitmap(headAnimBitmap,rectToBeDrawn,destRect,paint);
                canvas.drawBitmap(dinahBitmap,rectToBeDrawn,destRect,paint);

                ourHolder.unlockCanvasAndPost(canvas);
            }
        }

        public void controlFPS() {

            long timeThisFrame = (System.currentTimeMillis() - lastFrameTime);
            long timeToSleep = 500 - timeThisFrame;
            if (timeThisFrame > 0) {
                fps = (int) (1000 / timeThisFrame);
            }

            if (timeToSleep > 0) {

                try {
                    ourThread.sleep(timeToSleep);
                } catch (InterruptedException e) {
                    Log.e("EERrRRRRoooOOORRR:  ", "error in controlFPS thread sleep");
                }
            }

            lastFrameTime = System.currentTimeMillis();
        }

        public void pause() {
            playingSnake = false;
            try {
                ourThread.join();
            } catch (InterruptedException e) {
                Log.e("ERRROOOORRRRR: ", "error in pausing method, in joining ourThread");
            }
        }

        public void resume() {
            playingSnake = true;
            ourThread = new Thread(this);
            ourThread.start();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            startActivity(i);
            return true;
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        snakeAnimView.pause();
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        snakeAnimView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        snakeAnimView.pause();
    }

    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            snakeAnimView.pause();
            finish();
            return true;
        }
        return false;
    }
}

