/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tr.edu.bahcesehir.chnrxn.ball;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;

import java.util.LinkedList;
import java.util.List;


/**
 *
 * @author ToGo
 */
public class BallManager implements CollisionListener, Runnable {

    private float minSpeed = 0.00000005f;
    private float maxSpeed = 0.00000008f;

    private final List<CollisionListener> collisionListeners = new LinkedList<CollisionListener>();
    private final List<GameEndListener> gameEndListeners = new LinkedList<GameEndListener>();

    private final List<Ball> balls = new LinkedList<Ball>();
    private final int ballsToExplode;
    private final int totalBallCount;

    private final Rectangle boundries;

    private int totalPoint;
    private int nExplodedBalls;

    private boolean initiallyClicked;

    private Thread logicThread;

    public void setMaxSpeed(int maxSpeed) {
        this.maxSpeed = 0.0000001f * maxSpeed / 1000;
    }

    public void setMinSpeed(int minSpeed) {
        this.minSpeed = 0.0000001f * minSpeed / 1000;
    }
    
    public int getTotalPoint() {
        return totalPoint;
    }

    public int getNExplodedBalls() {
        return nExplodedBalls;
    }

    public int getTotalBallCount() {
        return totalBallCount;
    }

    public BallManager(Rectangle boundries, int maxSpeed, int minSpeed, int totalBallCount, int ballsToExplode) {
        this.boundries = boundries;
        setMaxSpeed(maxSpeed);
        setMinSpeed(minSpeed);
        this.initiallyClicked = false;
        this.totalBallCount = totalBallCount;
        this.ballsToExplode = ballsToExplode;
        this.nExplodedBalls = 0;
        for (int i = 0; i < totalBallCount; ++i) {
            balls.add(createBall());
        }
        addCollisionListener(this);
    }

    public void initialClick(int x, int y, long time) {
        if (!initiallyClicked) {
            Ball ball = new Ball(x, y, 0, 0, new Color(0.5f, 0.5f, 0.5f), time);
            ball.manager = this;
            balls.add(ball);
            initiallyClicked = true;
            System.out.println(Ball.getExpandDuration());
        }
    }

    public boolean isInitiallyClicked() {
        return initiallyClicked;
    }

    private Ball createBall() {
        float x = (float) (Ball.getInitialRadius() + (boundries.width - 2 * Ball.getInitialRadius()) * Math.random());
        float y = (float) (Ball.getInitialRadius() + (boundries.height - 2 * Ball.getInitialRadius()) * Math.random());
        double speed = (maxSpeed - minSpeed)*Math.random() + minSpeed;
        double angle = 2*Math.PI*Math.random();
        float sX = (float) (Math.cos(angle)*speed);
        float sY = (float) (Math.sin(angle)*speed);
        Color color = new Color(
                (int) (255 * Math.random()),
                (int) (255 * Math.random()),
                (int) (255 * Math.random()));
        Ball b = new Ball(x, y, sX, sY, color);
        b.manager = this;
        return b;
    }

    public Rectangle getBoundries() {
        return boundries;
    }

    public void update(long time) {
        List<Ball> endeds = new LinkedList<Ball>();
        synchronized (balls) {
            for (Ball b : balls) {
                b.update(time);
                if (b.isEnded()) {
                    endeds.add(b);
                }
            }
            balls.removeAll(endeds);
        }
        boolean gameEnded = initiallyClicked;
        for (Ball b : balls) {
            if (b.isExploded()) {
                gameEnded = false;
                break;
            }
        }
        if (gameEnded) {
            for (GameEndListener gameEndListener : gameEndListeners) {
                gameEndListener.gameEnded(ballsToExplode - nExplodedBalls);
            }
            stop();
        }
    }

    public boolean isGoalAchieved() {
        return ballsToExplode <= nExplodedBalls;
    }

    public void draw(Graphics2D g) {
        synchronized (balls) {
            for (Ball b : balls) {
                b.draw(g);
            }
        }
    }

    Ball checkCollision(Ball ball) {
        Shape shape = ball.getBounds();
        synchronized (balls) {
            for (Ball b : balls) {
                if (b.isExploded() && b.getBounds().intersects(shape.getBounds2D())) {
                    return b;
                }
            }
        }
        return null;
    }

    public void addCollisionListener(CollisionListener listener) {
        collisionListeners.add(listener);
    }

    public void addGameEndListener(GameEndListener listener) {
        gameEndListeners.add(listener);
    }

    void informCollision(Ball ball) {
        for (CollisionListener listener : collisionListeners) {
            listener.collided(ball);
        }
    }

    public void collided(Ball ball) {
        totalPoint += ball.getPoint();
        ++nExplodedBalls;
    }

    public void run() {
        logicThread = Thread.currentThread();
        while(true) {
            update(System.nanoTime());
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                return;
            }
        }
    }

    public void stop() {
        if (logicThread == null) {
            throw new IllegalStateException("Game is not started.");
        }
        logicThread.interrupt();
    }
    
}
