package tr.edu.bahcesehir.chnrxn.ball;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import tr.edu.bahcesehir.chnrxn.gui.RxnPanel;

public class Ball {

    private float x;
    private float y;
    private float radius;
    private float sX;
    private float sY;

    private final transient Ellipse2D.Float ellipse = new Ellipse2D.Float();

    private static float initialRadius = 6f;
    private static float finalRadius = 40f;
    private static long expandDuration = 900000000L; // 1.5 sec expand

    static double getInitialRadius() {
        return initialRadius;
    }

    public static void setInitialRadius(int initialRadius) {
        Ball.initialRadius = initialRadius / 10f;
    }

    static double getFinalRadius() {
        return finalRadius;
    }

    public static void setFinalRadius(int finalRadius) {
        Ball.finalRadius = finalRadius / 10f;
    }

    static long getExpandDuration() {
        return expandDuration;
    }

    public static void setExpandDuration(int expandDuration) {
        Ball.expandDuration = (long) ((expandDuration / 1000f + 0.5f)*1000000000);
    }

    private int chainCount;
    private long explodeTime;
    private long lastUpdateTime;
    private Color color;
    private boolean ended;
    
    BallManager manager;

    public int getChainCount() {
        return chainCount;
    }

    Ball(float x, float y, float sX, float sY, Color color) {
        this(x, y, sX, sY, color, 0);
    }

    Ball(float x, float y, float sX, float sY, Color color, long explodeTime) {
        this.x = x;
        this.y = y;
        this.radius = initialRadius;
        this.sX = sX;
        this.sY = sY;
        this.explodeTime = explodeTime;
        this.chainCount = 0;
        this.color = new Color(color.getRed(), color.getGreen(), color.getBlue(),
                127);
        this.ended = false;
    }

    public boolean isExploded() {
        return explodeTime != 0;
    }

    private void move(long time) {
        if (lastUpdateTime == 0) {
            return;
        }
        long delta = time - lastUpdateTime;
        x += delta * sX;
        y += delta * sY;
        
        Rectangle boundries = manager.getBoundries();
        if (x < radius) {
            x = radius + 0.01f;
            sX = -sX;
        } else if (x > boundries.width - radius) {
            x = boundries.width - radius - 0.01f;
            sX = -sX;
        }

        if (y < radius) {
            y = radius + 0.01f;
            sY = -sY;
        } else if (y > boundries.height - radius) {
            y = boundries.height - radius - 0.01f;
            sY = -sY;
        }
        
        Ball collider = manager.checkCollision(this);
        if (collider != null) {
            explodeTime = time;
            chainCount = collider.chainCount + 1;
            manager.informCollision(this);
        }
    }

    private float getExpansionFraction(long time) {
        return (float) (time - explodeTime) / expandDuration;
    }

    private void expand(long time) {
        float expansionFraction = getExpansionFraction(time);
        if (expansionFraction < 0.75f) {
            radius = initialRadius + (finalRadius - initialRadius) * expansionFraction * 4/3;
        } else if (expansionFraction < 2f) {
            radius = finalRadius;
        } else if (expansionFraction < 2.33f) {
            float inverseFraction = 3 * (2.33f - expansionFraction);
            radius = initialRadius + (finalRadius - initialRadius) * inverseFraction;
            color = new Color(color.getRed(), color.getGreen(), color.getBlue(),
                    (int) (127 * inverseFraction));
        } else {
            ended = true;
        }
    }

    public void update(long time) {
        if (isExploded()) {
            expand(time);
        } else {
            move(time);
        }
        ellipse.x = x - radius;
        ellipse.y = y - radius;
        ellipse.width = ellipse.height = radius * 2;
        lastUpdateTime = time;
    }

    public int getPoint() {
        return 100 * chainCount * chainCount * chainCount;
    }

    public Ellipse2D getBounds() {
        return ellipse;
    }

    public boolean isEnded() {
        return ended;
    }

    public void draw(Graphics2D g) {
        if (!ended) {
            Color c = g.getColor();
            g.setColor(color);
            g.fill(getBounds());
            if (isExploded() && getPoint() != 0 &&
                    System.nanoTime() < explodeTime + expandDuration * 0.75) {
                String point = "+"+RxnPanel.decimalFormat.format(getPoint());
                Rectangle2D bounds = g.getFontMetrics().getStringBounds(point, g);
                g.setColor(Color.WHITE);
                g.drawString(point, (float) (x - bounds.getWidth()/2),
                                    (float) (y + bounds.getHeight()/2));
            }
            g.setColor(c);
        }
    }

}

