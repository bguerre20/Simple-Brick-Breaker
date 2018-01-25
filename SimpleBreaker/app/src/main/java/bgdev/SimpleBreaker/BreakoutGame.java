package bgdev.SimpleBreaker;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.io.IOException;

public class BreakoutGame extends Activity {

    // gameView will be the view of the game
    // It will also hold the logic of the game
    // and respond to screen touches as well
    BreakoutView breakoutView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize gameView and set it as the view
        breakoutView = new BreakoutView(this);
        setTitle("Simple Breaker");
        setContentView(breakoutView);

    }

    // Here is our implementation of BreakoutView
    // It is an inner class.
    // Note how the final closing curly brace }
    // is inside the BreakoutGame class

    // Notice we implement runnable so we have
    // A thread and can override the run method.
    class BreakoutView extends SurfaceView implements Runnable {

        // This is our thread
        Thread gameThread = null;

        // This is new. We need a SurfaceHolder
        // When we use Paint and Canvas in a thread
        // We will see it in action in the draw method soon.
        SurfaceHolder ourHolder;

        // A boolean which we will set and unset
        // when the game is running- or not.
        volatile boolean playing;

        // Game is paused at the start
        boolean paused = true;

        // A Canvas and a Paint object
        Canvas canvas;
        Paint paint;

        // This variable tracks the game frame rate
        long fps;

        // This is used to help calculate the fps
        private long timeThisFrame;

        // The size of the screen in pixels
        int screenX;
        int screenY;

        //lives sprite
        Bitmap heart;

        //background music
        MediaPlayer mediaPlayer;


        // The players paddle
        Paddle paddle;
        Border border;

        // A ball
        Ball ball;

        // Up to 200 bricks
        Brick[] bricks = new Brick[200];
        int numBricks = 0;

        // For sound FX
        SoundPool soundPool;
        int beep1ID = -1;
        int beep2ID = -1;
        int beep3ID = -1;
        int loseLifeID = -1;
        int explodeID = -1;
        int victoryID = -1;

        // The score
        int score = 0;

        // Lives
        int lives = 3;

        // When the we initialize (call new()) on gameView
        // This special constructor method runs
        public BreakoutView(Context context) {
            // The next line of code asks the
            // SurfaceView class to set up our object.
            // How kind.



            super(context);

            this.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

            // Initialize ourHolder and paint objects
            ourHolder = getHolder();
            paint = new Paint();

            // Get a Display object to access screen details
            Display display = getWindowManager().getDefaultDisplay();
            // Load the resolution into a Point object
            Point size = new Point();
            display.getRealSize(size);

            screenX = size.x;
            screenY = size.y;

            paddle = new Paddle(screenX, screenY);
            border = new Border(screenX, screenY);

            // Create a ball
            ball = new Ball(screenX, screenY);

            // Load the sprites
            heart = BitmapFactory.decodeResource(this.getResources(), R.drawable.heart);

            // This SoundPool is deprecated but don't worry
            soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);

            try {
                // Create objects of the 2 required classes
                AssetManager assetManager = context.getAssets();
                AssetFileDescriptor descriptor;

                // Load our fx in memory ready for use
                descriptor = assetManager.openFd("beep1.ogg");
                beep1ID = soundPool.load(descriptor, 0);

                descriptor = assetManager.openFd("beep2.ogg");
                beep2ID = soundPool.load(descriptor, 0);

                descriptor = assetManager.openFd("beep3.ogg");
                beep3ID = soundPool.load(descriptor, 0);

                descriptor = assetManager.openFd("loseLife.ogg");
                loseLifeID = soundPool.load(descriptor, 0);

                descriptor = assetManager.openFd("explode.ogg");
                explodeID = soundPool.load(descriptor, 0);

                descriptor = assetManager.openFd("victory.ogg");
                victoryID = soundPool.load(descriptor, 0);



            } catch (IOException e) {
                // Print an error message to the console
                Log.e("error", "failed to load sound files");
            }

            mediaPlayer = MediaPlayer.create(context, R.raw.backgroundmusic);
            mediaPlayer.setLooping(true);
            mediaPlayer.start();

            createBricksAndRestart();

        }

        public void createBricksAndRestart() {

            // Put the ball back to the start
            ball.reset(screenX, screenY);
            paddle.reset(screenX, screenY);
            border.reset(screenX, screenY);

            int brickWidth = screenX / 10;
            int brickHeight = screenY / 12;

            // Build a wall of bricks
            numBricks = 0;
            for (int column = 1; column < 9; column++) {
                for (int row = 1; row < 4; row++) {
                    bricks[numBricks] = new Brick(row, column, brickWidth, brickHeight);
                    numBricks++;
                }
            }
            // if game over reset scores and lives
            if (lives == 0) {
                score = 0;
                lives = 3;
            }

        }

        @Override
        public void run() {
            while (playing) {
                // Capture the current time in milliseconds in startFrameTime
                long startFrameTime = System.currentTimeMillis();
                // Update the frame
                if (!paused) {
                    update();
                }
                // Draw the frame
                draw();
                // Calculate the fps this frame
                // We can then use the result to
                // time animations and more.
                timeThisFrame = System.currentTimeMillis() - startFrameTime;
                if (timeThisFrame >= 1) {
                    fps = 1000 / timeThisFrame;
                }

            }

        }



        // Everything that needs to be updated goes in here
        // Movement, collision detection etc.
        public void update() {

            // Move the paddle if required
            paddle.update(fps);

            collisions();

            ball.update(fps);



        }

        // Draw the newly updated scene
        public void draw() {

            // Make sure our drawing surface is valid or we crash
            if (ourHolder.getSurface().isValid()) {
                // Lock the canvas ready to draw
                canvas = ourHolder.lockCanvas();

                // Draw the background color
                canvas.drawColor(Color.argb(255, 0, 0, 0));

                // Choose the brush color for drawing
                paint.setColor(Color.argb(255, 255, 255, 255));

                //draw the border
                canvas.drawRect(border.getRect(), paint);
                paint.setColor(Color.argb(255, 0, 0, 0));
                canvas.drawRect(border.getInnerRect(), paint);

                paint.setColor(Color.argb(255, 255, 255, 255));

                // Draw the paddle
                canvas.drawRect(paddle.getRect(), paint);

                // Draw the ball
                canvas.drawRect(ball.getRect(), paint);

                // Change the brush color for drawing
                paint.setColor(Color.argb(255, 249, 129, 0));

                // Draw the bricks if visible
                for (int i = 0; i < numBricks; i++) {
                    if (bricks[i].getVisibility()) {
                        canvas.drawRect(bricks[i].getRect(), paint);
                    }
                }

                // Choose the brush color for drawing
                paint.setColor(Color.argb(255, 255, 255, 255));

                // Draw the score
                if (lives >= 1) {
                    canvas.drawBitmap(heart, 0, 0, paint);
                    if (lives >= 2) {
                        canvas.drawBitmap(heart, 0, 75, paint);
                        if (lives >= 3) {
                            canvas.drawBitmap(heart, 0, 150, paint);
                        }
                    }
                }


                paint.setTextSize(40);
                canvas.drawText("" + score, screenX - 75, 50, paint);
                // Has the player cleared the screen?
                if (score == numBricks * 10) {
                    paint.setTextSize(90);
                    mediaPlayer.pause();
                    soundPool.play(victoryID, 1, 1, 0, 0, 1);
                    canvas.drawText("VICTORY! SCORE: " + score, screenX / 2 - 400, screenY / 2, paint);
                }

                // Has the player lost?
                if (lives <= 0) {
                    paint.setTextSize(90);
                    canvas.drawText("YOU HAVE LOST!", 10, screenY / 2, paint);
                }

                // Draw everything to the screen
                ourHolder.unlockCanvasAndPost(canvas);
            }
        }

        // If SimpleGameEngine Activity is paused/stopped
        // shutdown our thread.
        public void pause() {
            playing = false;
            mediaPlayer.pause();
            try {
                gameThread.join();
            } catch (InterruptedException e) {
                Log.e("Error:", "joining thread");
            }
        }

        // If SimpleGameEngine Activity is started then
        // start our thread.
        public void resume() {
            playing = true;
            gameThread = new Thread(this);
            gameThread.start();
            mediaPlayer.start();

        }

        // The SurfaceView class implements onTouchListener
        // So we can override this method and detect screen touches.
        @Override
        public boolean onTouchEvent(MotionEvent motionEvent) {
            switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
                // Player has touched the screen
                case MotionEvent.ACTION_DOWN:
                    paused = false;
                    if (motionEvent.getX() > screenX / 2) {

                        paddle.setMovementState(paddle.RIGHT);
                    } else

                    {
                        paddle.setMovementState(paddle.LEFT);
                    }

                    break;

                // Player has removed finger from screen
                case MotionEvent.ACTION_UP:

                    paddle.setMovementState(paddle.STOPPED);
                    break;
            }

            return true;
        }

        public void collisions() {
            // Check for ball colliding with a brick
            for (int i = 0; i < numBricks; i++) {
                if (bricks[i].getVisibility()) {
                    if (RectF.intersects(bricks[i].getRect(), ball.getRect())) {
                        bricks[i].setInvisible();
                        ball.reverseYVelocity();
                        score = score + 10;
                        soundPool.play(explodeID, 1, 1, 0, 0, 1);
                    }
                }
            }
            // Check for ball colliding with paddle
            if (RectF.intersects(paddle.getRect(), ball.getRect())) {
                ball.setRandomXVelocity();
                ball.reverseYVelocity();
                ball.clearObstacleY(paddle.getRect().top - 2);
                soundPool.play(beep1ID, 1, 1, 0, 0, 1);
            }
            // Bounce the ball back when it hits the bottom of screen
            if (ball.getRect().bottom > screenY - 40) {
                ball.reverseYVelocity();
                ball.clearObstacleY(screenY - 40);

                // Lose a life
                lives--;
                soundPool.play(loseLifeID, 1, 1, 0, 0, 1);

                if (lives == 0) {
                    paused = true;
                    createBricksAndRestart();
                }
            }

            // Bounce the ball back when it hits the top of screen
            if (ball.getRect().top < 40)

            {
                ball.reverseYVelocity();
                ball.clearObstacleY(50);

                soundPool.play(beep2ID, 1, 1, 0, 0, 1);
            }

            //stop the paddle from going off the left of the screen
            if (paddle.getRect().left < 100) {
                paddle.setMovementState(paddle.STOPPED);
            }

            if (paddle.getRect().right > screenX - 100) {
                paddle.setMovementState(paddle.STOPPED);
            }

            // If the ball hits left wall bounce
            if (ball.getRect().left < 100)

            {
                ball.reverseXVelocity();
                ball.clearObstacleX(102);
                soundPool.play(beep3ID, 1, 1, 0, 0, 1);
            }

            // If the ball hits right wall bounce
            if (ball.getRect().right > screenX - 110) {

                ball.reverseXVelocity();
                ball.clearObstacleX(screenX - 122);

                soundPool.play(beep3ID, 1, 1, 0, 0, 1);
            }

            // Pause if cleared screen
            if (score == numBricks * 10)

            {
                paused = true;
                createBricksAndRestart();
            }
        }

    }
    // This is the end of our BreakoutView inner class

    // This method executes when the player starts the game
    @Override
    protected void onResume() {
        super.onResume();

        // Tell the gameView resume method to execute
        breakoutView.resume();

    }

    // This method executes when the player quits the game
    @Override
    protected void onPause() {
        super.onPause();

        // Tell the gameView pause method to execute
        breakoutView.pause();
    }



}
// This is the end of the BreakoutGame class