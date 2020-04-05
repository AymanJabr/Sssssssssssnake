package com.NoobGames.sssssssssssnake;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.NoobGames.sssssssssssnake.MainActivity;

import java.io.IOException;
import java.util.Random;

public class GameActivity extends Activity {

    SharedPreferences prefs;
    SharedPreferences.Editor editor;

    Canvas canvas;
    SnakeView snakeView;

    Bitmap headBitmap;
    Bitmap bodyBitmap;
    Bitmap tailBitmap;
    Bitmap appleBitmap;

    int screenWidth;
    int screenHeight;
    int topGap;

    //create and initialise sound variables
    private SoundPool soundPool;
    int sample1 = -1;
    int sample2 = -1;
    int sample3 = -1;
    int sample4 = -1;

    //up = 0, right = 1, down = 2, left = 3
    int directionOfTravel = 0;

    //stats
    long lastFrameTime;
    int fps;
    int score;
    int hi;

    //game objects
    int[] snakeX;
    int[] snakeY;
    int snakeLength = 3;
    int appleX;
    int appleY;

    //size in pixels of the game board
    int blockSize;
    int numBlocksWide;
    int numBlocksHeight;


    //for giving a direction to each snake part, [snake part]
    int [] bodyDirection;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("DATANAME", MODE_PRIVATE);
        editor = prefs.edit();
        hi = prefs.getInt("INTNAME", 0);

        bodyDirection = new int[200];
        bodyDirection[0] = 1;
        bodyDirection[1] = 0;
        bodyDirection[2] = 3;

        loadSound();
        configureDisplay();
        snakeView = new SnakeView(this);
        setContentView(snakeView);
    }


    class SnakeView extends SurfaceView implements Runnable {

        Thread ourThread = null;
        SurfaceHolder ourHolder;
        volatile boolean playingSnake;
        Paint paint;

        public SnakeView(Context context) {
            super(context);
            ourHolder = getHolder();
            paint = new Paint();

            snakeX = new int[200];
            snakeY = new int[200];

            getSnake();
        }

        public void getSnake() {
            snakeLength = 3;
            //start the snake head at the middle of the screen
            snakeX[0] = numBlocksWide/2;
            snakeY[0] = numBlocksHeight/2;
            //add a body part and a tail to the left of the snake
            snakeX[1] = snakeX[0] - 1;
            snakeY[1] = snakeY[0];
            snakeX[2] = snakeX[1] - 1;
            snakeY[2] = snakeY[1];

            //Set the direction of the first three parts of the snake
            //0 = up, 1 = right, 2 = down, 3 = left
            bodyDirection[0] = 1;
            bodyDirection[1] = bodyDirection[0];
            bodyDirection[2] = bodyDirection[1];
            getApple();

        }

        public void getApple() {
            Random random = new Random();
            appleX = random.nextInt(numBlocksWide -1) +1;
            appleY = random.nextInt(numBlocksHeight -1) +1;

            //make sure the apple doesn't spawn on top of the snake
            for (int i = 0; i < snakeLength - 1; i++) {
                if (appleX == snakeX[i] && appleY == snakeY[i]) {
                    appleX = random.nextInt(numBlocksWide -1) +1;
                    appleY = random.nextInt(numBlocksHeight -1) +1;
                }
            }

        }

        @Override
        public void run() {
            while (playingSnake) {
                updateGame();
                drawGame();
                controlFPS();
            }
        }

        public void updateGame() {
            //did the player get the apple
            if (snakeX[0] == appleX && snakeY[0] == appleY) {
                snakeLength++;
                getApple();
                score = score + snakeLength;
                soundPool.play(sample1, 1, 1, 0, 0, 1);
                if (score > hi) {
                    hi = score;
                    editor.putInt("INTNAME", hi);
                    editor.commit();
                }

            }


            //move the body of the snake starting from the back
            for(int i = snakeLength -1; i > 0; i--){
                snakeX[i] = snakeX[i-1];
                snakeY[i] = snakeY[i-1];

                //the new direction of a body part is the same as the old one
                // of the body part before it
                bodyDirection[i] = bodyDirection[i - 1];
            }

            //move the head in the correct direction
            switch (directionOfTravel) {
                case 0: //up
                    snakeY[0] --;
                    break;

                case 1: //right
                    snakeX[0] ++;
                    break;

                case 2: //down
                    snakeY[0] ++;
                    break;

                case 3: //left
                    snakeX[0] --;
                    break;
            }
            //sets the direction of the head to the current direction of travel
            bodyDirection[0] = directionOfTravel;
//            Log.d("HEEERRRRRRRREE: ", " " + bodyDirection[0]);




            //check for any collisions
            boolean dead = false;
            //with a wall
            if(snakeX[0] == -1 || snakeX[0] >= numBlocksWide || snakeY[0] == -1 || snakeY[0] == numBlocksHeight)
                dead = true;
            //with other parts of the snake
            for (int i = snakeLength - 1; i > 0; i--) {
                if ((i > 4) && (snakeX[0] == snakeX[i]) && (snakeY[0] == snakeY[i])) {
                    dead = true;
                }
            }

            if (dead) {
                //restart the snake
                soundPool.play(sample4, 1, 1, 0, 0, 1);
                score = 0;
                getSnake();
            }

            configureDisplay();
        }

        public void drawGame() {

            if (ourHolder.getSurface().isValid()) {
                canvas = ourHolder.lockCanvas();
                canvas.drawColor(Color.BLACK);
                paint.setColor(Color.argb(255, 255, 255, 255));
                paint.setTextSize(topGap/2);
                canvas.drawText("Score: " + score + "  Highscore: " + hi, 10, topGap - 6, paint);

                //draw the borders
                {   paint.setStrokeWidth(3);
                    canvas.drawLine(1, topGap, screenWidth - 1,
                            topGap, paint);//up
                    canvas.drawLine(screenWidth - 1, topGap, screenWidth - 1,
                            topGap + (numBlocksHeight * blockSize), paint);//right
                    canvas.drawLine(1, topGap, 1,
                            topGap + (numBlocksHeight * blockSize), paint);//left
                    canvas.drawLine(screenWidth - 1, topGap + (numBlocksHeight * blockSize),
                            1, topGap + (numBlocksHeight * blockSize), paint);//down
                }


                //draw the snake
                canvas.drawBitmap(headBitmap,snakeX[0] * blockSize,
                        (snakeY[0] * blockSize) + topGap,paint);


                for (int i = 1; i < snakeLength - 1; i++) {
                    canvas.drawBitmap(bodyBitmap, snakeX[i] * blockSize,
                            (snakeY[i] * blockSize) + topGap, paint);
                }
                //draw the tail
                canvas.drawBitmap(tailBitmap, snakeX[snakeLength - 1] * blockSize,
                        (snakeY[snakeLength - 1] * blockSize) + topGap, paint);
                //draw the apple
                canvas.drawBitmap(appleBitmap, appleX * blockSize,
                        (appleY * blockSize) + topGap,paint);

                ourHolder.unlockCanvasAndPost(canvas);
            }
        }

        public void controlFPS() {
            long timeThisFrame = (System.currentTimeMillis() - lastFrameTime);
            long timeToSleep = 100 - timeThisFrame;
            if (timeThisFrame > 0) {
                fps = (int) (1000 / timeThisFrame);
            }
            if (timeToSleep > 0) {
                try {
                    ourThread.sleep(timeToSleep);
                } catch (InterruptedException e){
                    Log.e("ERRRRRROOOOORRRR: ", "problem in sleeping of ourThread");
                }
            }
            lastFrameTime = System.currentTimeMillis();
        }

        public void pause() {
            playingSnake = false;
            try {
                ourThread.join();
            } catch (InterruptedException e) {
                Log.e("ERRRRRROOOOORRRR: ", "problem in joining of ourThread on pause");
            }
        }

        public void resume() {
            playingSnake = true;
            ourThread = new Thread(this);
            ourThread.start();
        }

        @Override
        public boolean onTouchEvent(MotionEvent motionEvent) {

            switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_UP:
                    if (motionEvent.getX() >= screenWidth / 2) {
                        //turn right
                        directionOfTravel++;
                        if (directionOfTravel == 4) directionOfTravel = 0;
                    } else {
                        directionOfTravel --;
                        if(directionOfTravel == -1) directionOfTravel = 3;
                    }
            }
            return true;
        }

    }


    @Override
    protected void onStop() {
        super.onStop();
        snakeView.pause();
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        snakeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        snakeView.pause();
    }

    //in case the user presses the back button
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            snakeView.pause();

            Intent i = new Intent(this, MainActivity.class);
            startActivity(i);
            finish();
            return true;
        }
        return false;
    }

    public void loadSound() {
        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC,0);
        try {
            AssetManager assetManager = getAssets();
            AssetFileDescriptor descriptor;

            descriptor = assetManager.openFd("sample1.ogg");
            sample1 = soundPool.load(descriptor, 0);
            descriptor = assetManager.openFd("sample2.ogg");
            sample2 = soundPool.load(descriptor, 0);
            descriptor = assetManager.openFd("sample3.ogg");
            sample3 = soundPool.load(descriptor, 0);
            descriptor = assetManager.openFd("sample4.ogg");
            sample4 = soundPool.load(descriptor, 0);
        } catch (IOException e) {
            Log.e("ERRRRRROOOOOOORRRR: ", "IOException when loading sounds");
        }
    }

    public void configureDisplay() {

        //find the dimensions of the screen
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenHeight = size.y;
        screenWidth = size.x;
        topGap = screenHeight/14;
        //set size and number of blocks in the screen
        blockSize = screenWidth/40;
        numBlocksWide = 40;
        numBlocksHeight = (screenHeight - topGap)/blockSize;
        //load and scale bitmaps ?????????????????????????????????????????????????????????
        Log.i("HHHHEEEEERRRREEEE:", "" + bodyDirection[0]);
        headBitmap = BitmapFactory.decodeResource(getResources(), getHeadDirection(bodyDirection[0]));
        tailBitmap = BitmapFactory.decodeResource(getResources(), getTailDirection(bodyDirection[snakeLength -1]));
        appleBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.apple);

        headBitmap = Bitmap.createScaledBitmap(headBitmap, blockSize, blockSize, false);
        tailBitmap = Bitmap.createScaledBitmap(tailBitmap, blockSize, blockSize, false);
        appleBitmap = Bitmap.createScaledBitmap(appleBitmap, blockSize, blockSize, false);

        bodyBitmap = BitmapFactory.decodeResource(getResources(),R.drawable.body_5);
        bodyBitmap = Bitmap.createScaledBitmap(bodyBitmap, blockSize, blockSize, false);



    }

    public int getHeadDirection(int i) {
        switch (i){
        case 0:
            return R.drawable.head_up;
        case 1:
            return R.drawable.head_right;
        case 2:
            return R.drawable.head_down;
        case 3:
            return R.drawable.head_left;
        default:
            return -1;
        }
    }

    public int getTailDirection(int i) {
        switch (i){
            case 0:
                return R.drawable.tail_down;
            case 1:
                return R.drawable.tail_left;
            case 2:
                return R.drawable.tail_up;
            case 3:
                return R.drawable.tail_right;
            default:
                return -1;
        }
    }

//    public int getBodyDirection(int i) {
//        int bodyImage = -1;
//
//        if ((bodyDirection[i - 1]) == (bodyDirection[i + 1])) {
//
//            switch (bodyDirection[i]) {
//                case 0:
//                case 2:
//                    bodyImage =  R.drawable.body_5;
//                case 1:
//                case 3:
//                    bodyImage =  R.drawable.body_2;
//            }
//        } else {
//            switch (bodyDirection[i]) {
//                case 0:
//                    bodyImage =  R.drawable.body_6;
//                case 1:
//                    bodyImage =  R.drawable.body_4;
//                case 2:
//                    bodyImage = R.drawable.body_3;
//                case 3:
//                    bodyImage = R.drawable.body_1;
//            }
//        }
//
//        return bodyImage;
//    }


}
