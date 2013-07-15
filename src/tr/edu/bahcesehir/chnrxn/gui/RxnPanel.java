/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package tr.edu.bahcesehir.chnrxn.gui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

import java.awt.image.BufferedImage;

import java.text.DecimalFormat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import tr.edu.bahcesehir.chnrxn.ball.Ball;
import tr.edu.bahcesehir.chnrxn.ball.BallManager;
import tr.edu.bahcesehir.chnrxn.ball.CollisionListener;
import tr.edu.bahcesehir.chnrxn.ball.GameEndListener;

/**
 *
 * @author ToGo
 */
public class RxnPanel extends JPanel implements Runnable, GameEndListener, CollisionListener {

    private static final ExecutorService executorService = Executors.newCachedThreadPool();

    private static BufferedImage sphereCursor;
    private static BufferedImage nullCursor;

    private static final Font pointFont = new Font("Impact", Font.PLAIN, 12);
    private static final Font scoreFont = new Font("Impact", Font.PLAIN, 30);
    
    public static DecimalFormat decimalFormat = new DecimalFormat("###,###,###,###");

    private static Image getSphereCursor() {
        if (sphereCursor == null) {
            int radius = 40;
            sphereCursor = new BufferedImage(2*radius, 2*radius, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = sphereCursor.createGraphics();
            Point center = new Point(radius, radius);
            g.setPaint(new RadialGradientPaint(center, radius,
                    new float[]{0.1f, 0.2f, 0.9f},
                    new Color[]{
                        new Color(0.5f, 0.5f, 0.5f, 0.7f),
                        new Color(0.7f, 0.7f, 0.7f, 0.5f),
                        new Color(0.5f, 0.5f, 0.5f, 0.01f)}));
            g.fillOval(0, 0, 2*radius, 2*radius);
            g.dispose();
        }
        return sphereCursor;
    }

    private static Image getNullCursor() {
        if (nullCursor == null) {
            nullCursor = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        }
        return nullCursor;
    }

    private JFrame frame;
    private Dimension preferredSize = new Dimension(640, 400);
    private BallManager manager;
    private Point mouseLocation;
    private Color backgroundColor = new Color(0.1f, 0.1f, 0.12f);
    private Color achievementColor = new Color(1f, 1f, 1f, 0.5f);

    @Override
    public Dimension getPreferredSize() {
        return preferredSize;
    }

    public RxnPanel(JFrame frame) {
        super();
        this.frame = frame;
        RxnMouseListener rxnMouseListener = new RxnMouseListener();
        addMouseListener(rxnMouseListener);
        addMouseMotionListener(rxnMouseListener);
        Cursor cursor = Toolkit.getDefaultToolkit().createCustomCursor(getNullCursor(), new Point(), "null");
        setCursor(cursor);
        executorService.execute(this);
    }
    
    private int initialRadius, finalRadius, expandDuration,
            minSpeed, maxSpeed, totalBall, goal;

    public void restartGame(int initialRadius, int finalRadius, int expandDuration,
            int minSpeed, int maxSpeed, int totalBall, int goal, Dimension size, Color background) {
        if (manager != null) {
            manager.stop();
        }
        this.initialRadius = initialRadius;
        this.finalRadius = finalRadius;
        this.expandDuration = expandDuration;
        this.minSpeed = minSpeed;
        this.maxSpeed = maxSpeed;
        this.totalBall = totalBall;
        this.backgroundColor = background;
        this.goal = goal;
        Ball.setInitialRadius(initialRadius);
        Ball.setFinalRadius(finalRadius);
        Ball.setExpandDuration(expandDuration);
        this.preferredSize = size;
        manager = new BallManager(new Rectangle(size), minSpeed, maxSpeed, totalBall, goal);
        manager.addGameEndListener(this);
        manager.addCollisionListener(this);
        collided(null);
        executorService.execute(manager);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(backgroundColor);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setFont(pointFont);
        if (manager != null) {
            manager.draw(g2);
            if (!manager.isInitiallyClicked() && mouseLocation != null) {
                Image img = getSphereCursor();
                g2.drawImage(img, mouseLocation.x - img.getWidth(null) / 2,
                        mouseLocation.y - img.getHeight(null) / 2, null);
            }
            if (manager.isGoalAchieved()) {
                g2.setColor(achievementColor);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            g2.setColor(Color.BLACK);
            g2.setFont(scoreFont);
            String score = decimalFormat.format(manager.getTotalPoint());
            Rectangle2D rect = g2.getFontMetrics().getStringBounds(score, g);
            g2.drawString(score, (float) (preferredSize.width - rect.getWidth()),
                    (float) (preferredSize.height));
        }
    }

    public void run() {
        while(true) {
            repaint();
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                Logger.getLogger(RxnPanel.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void gameEnded(int remainingBalls) {
        if (remainingBalls <= 0) {
            JOptionPane.showMessageDialog(this,
                    "Congratulations!\n" +
                    "You've made " + decimalFormat.format(manager.getTotalPoint())
                    + " points!", "Game Over!",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Sorry, you had " + remainingBalls + " balls remained.",
                    "Game Over!", JOptionPane.INFORMATION_MESSAGE);
        }
        restartGame(initialRadius, finalRadius, expandDuration, minSpeed,
                maxSpeed, totalBall, goal, preferredSize, backgroundColor);
    }

    public void collided(Ball ball) {
        frame.setTitle("ChnRxn " + manager.getNExplodedBalls() + "/" + manager.getTotalBallCount());
    }

    public BallManager getManager() {
        return manager;
    }

    private class RxnMouseListener extends MouseAdapter {

        @Override
        public void mouseReleased(MouseEvent e) {
            if (manager != null) {
                manager.initialClick(e.getX(), e.getY(), System.nanoTime());
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            mouseMoved(e);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            mouseLocation = e.getPoint();
        }
        
    }
    
}
