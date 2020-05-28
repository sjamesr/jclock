package org.jdns;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;

public class Clock {

  boolean antialiasing = true;

  public static void main(String[] args) {
    new Clock();
  }

  public Clock() {
    JFrame frame = new JFrame("Clock!");
    frame.getContentPane().setLayout(new BorderLayout());
    JClock clock = new JClock();
    frame.getContentPane().add(clock, BorderLayout.CENTER);

    Timer t = new Timer(16, e -> clock.setTime(ZonedDateTime.now()));
    t.start();

    JPanel bottomPanel = new JPanel(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    c.weightx = 0;
    JToggleButton pauseButton = new JToggleButton();

    class PauseAction extends AbstractAction {
      public PauseAction() {
        super("Pause");
        putValue(SELECTED_KEY, false);
      }

      @Override
      public void actionPerformed(ActionEvent e) {
        setPaused(((boolean) getValue(SELECTED_KEY)));
      }

      public void pause() {
        setPaused(true);
      }

      public void setPaused(boolean paused) {
        putValue(SELECTED_KEY, paused);
        if (paused) {t.stop();}
        else {t.start();}
      }
    }

    PauseAction p = new PauseAction();

    pauseButton.setAction(p);
    bottomPanel.add(pauseButton, c);

    var antialiasingButton = new JToggleButton("Anti-aliasing");
    antialiasingButton.setSelected(true);
    antialiasingButton.addActionListener(
        l -> {
          antialiasing = !antialiasing;
          clock.repaint();
        });
    bottomPanel.add(antialiasingButton);

    c.weightx = 1;
    c.fill = GridBagConstraints.HORIZONTAL;

    JSlider timeOfDaySelector = new JSlider(0, 86400);
    timeOfDaySelector.addChangeListener(
        e -> {
          p.pause();
          clock.setTime(
              ZonedDateTime.now()
                  .truncatedTo(ChronoUnit.DAYS).withEarlierOffsetAtOverlap()
                  .plus(timeOfDaySelector.getValue(), ChronoUnit.SECONDS));
        });

    bottomPanel.add(timeOfDaySelector, c);

    frame.getContentPane().add(bottomPanel, BorderLayout.SOUTH);

    frame.pack();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setVisible(true);
  }

  private class JClock extends JComponent {
    private ZonedDateTime time = ZonedDateTime.now();
    private DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL);
    private Point center;
    private int radius;

    public JClock() {
      setPreferredSize(new Dimension(100, 100));
    }

    public void setTime(ZonedDateTime when) {
      time = when;
      repaint();
    }

    private void drawFace(Graphics g) {
      // Draw an ellipse representing the face of the clock.
      Graphics2D g2 = (Graphics2D) g;
      g.drawOval(center.x - radius, center.y - radius, radius * 2, radius * 2);
    }

    private void drawHourHand(Graphics g) {
      Point center = new Point(getWidth() / 2, getHeight() / 2);

      int hour = time.getHour() % 12;
      double angle = -(30 * hour);

      int minute = time.getMinute();
      angle -= minute * 0.5;

      int second = time.getSecond();
      angle -= second * (0.5 / 60);

      angle += 90;

      double length = 5 * radius / 7.0f;

      g.setColor(Color.BLACK);
      g.drawLine(
          center.x,
          center.y,
          center.x + (int) (length * Math.cos(Math.toRadians(angle))),
          center.y - (int) (length * Math.sin(Math.toRadians(angle))));
    }

    private void drawMinuteHand(Graphics g) {
      int minute = time.getMinute();
      double angle = -(6 * minute);
      int second = time.getSecond();
      angle -= second * 0.1;
      int nano = time.getNano();
      angle -= nano * (0.1 / 1e9);
      angle += 90;
      double length = radius;
      g.setColor(Color.BLACK);
      g.drawLine(
          center.x,
          center.y,
          center.x + (int) (radius * Math.cos(Math.toRadians(angle))),
          center.y - (int) (radius * Math.sin(Math.toRadians(angle))));
    }

    private void drawSecondHand(Graphics g) {
      int second = time.getSecond();
      double angle = -(6 * second);
      angle -= 6 * (time.getNano() / 1e9);
      angle += 90;
      double length = radius;
      g.setColor(Color.RED);
      g.drawLine(
          center.x,
          center.y,
          center.x + (int) (length * Math.cos(Math.toRadians(angle))),
          center.y - (int) (length * Math.sin(Math.toRadians(angle))));
    }

    private void drawTicks(Graphics g) {
      for (int hour = 0; hour < 12; hour++) {
        double angle = -(30 * hour);
        g.setColor(Color.BLACK);
        double hourDistanceFromCenter = radius * 13.0 / 14;
        double tickCenterX = center.x + (int) (hourDistanceFromCenter * Math.cos(Math.toRadians(angle)));
        double tickCenterY = center.y - (int) (hourDistanceFromCenter * Math.sin(Math.toRadians(angle)));
        double tickRadius = radius / 30.0;
        g.fillOval(
            (int) (tickCenterX - tickRadius),
            (int) (tickCenterY - tickRadius),
            (int) (2 * tickRadius),
            (int) (2 * tickRadius));
      }

      for (int minute = 0; minute < 60; minute++) {
        if (minute % 5 == 0) {
          continue;
        }

        double angle = -(6 * minute);
        g.setColor(Color.BLACK);
        double minuteDistanceFromCenter = radius * 13.0 / 14;
        double tickCenterX = center.x + (int) (minuteDistanceFromCenter * Math.cos(Math.toRadians(angle)));
        double tickCenterY = center.y - (int) (minuteDistanceFromCenter * Math.sin(Math.toRadians(angle)));
        double tickRadius = radius / 60.0;
        g.fillOval(
            (int) (tickCenterX - tickRadius),
            (int) (tickCenterY - tickRadius),
            (int) (2 * tickRadius),
            (int) (2 * tickRadius));
      }
    }

    @Override
    public void paint(Graphics g) {
      super.paint(g);

      if (antialiasing) {
        Graphics2D g2 = (Graphics2D) g;
        var hints = new HashMap<RenderingHints.Key, Object>();
        hints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        RenderingHints rh = new RenderingHints(hints);
        g2.setRenderingHints(rh);
      }

      center = new Point(getWidth() / 2, getHeight() / 2);
      radius = Math.min(center.x, center.y);

      Graphics2D g2 = (Graphics2D) g;
      g2.setStroke(new BasicStroke(2, 2, BasicStroke.JOIN_ROUND, 1.0f, new float[] {6.0f, 6.0f}, 0));
      drawFace(g);
      g2.setStroke(new BasicStroke(2));
      drawTicks(g);
      drawHourHand(g);
      drawMinuteHand(g);
      drawSecondHand(g);

      String timeString = time.format(formatter);
      Rectangle2D bounds = g.getFontMetrics().getStringBounds(timeString, g);
      g.drawString(timeString, (int)(center.x - bounds.getWidth() / 2),(int) ((0.1 * radius) + (center.y - bounds.getHeight() / 2)));
    }
  }
}
