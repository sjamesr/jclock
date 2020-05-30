package org.jdns;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.TimeZone;

/**
 * A fancy analog clock.
 *
 * The clock's appearance and behavior may be customized in the following ways:
 *
 * <ul>
 * <li>may be paused or unpaused (see {@link #setPaused})</li>
 * <li>can display an arbitrary time (see {@link #setTime}</li>
 * <li>may display the time in an arbitrary time zone (see {@link #setTimeZone})</li>
 * <li>can hide the sweep-second hand (see {@link #setDrawSecondHand})</li>
 * </ul>
 *
 * <p>
 * This is a Swing component and, as such, users should make all calls to the clock's methods on the
 * event dispatch thread.
 */
public class JClock extends JComponent {

  private final DateTimeFormatter formatter =
      DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL);
  private final int DEFAULT_HZ = 40;

  private ZonedDateTime time = ZonedDateTime.now();
  private Point center;
  private final Timer timer;
  private boolean drawSecondHand = true;
  private int horizAxisLength;
  private int vertAxisLength;
  private boolean allowEllipticalClock = false;
  private boolean sweepSecond = true;

  public static void main(String[] args) {
    JFrame frame = new JFrame("Clock!");
    JClock clock = new JClock();
    clock.setSweepSecond(false);
    clock.setTimeZone(ZoneId.of("Australia/Sydney"));
    frame.getContentPane().add(clock, BorderLayout.CENTER);
    JPanel bottomPanel = new JPanel();
    JToggleButton sweepSecondButton = new JToggleButton("Sweep Second", clock.isSweepSecond());
    sweepSecondButton.addActionListener(e -> clock.setSweepSecond(!clock.isSweepSecond()));
    bottomPanel.add(sweepSecondButton);
    JComboBox<String> timeZoneComboBox = new JComboBox<>(TimeZone.getAvailableIDs());
    timeZoneComboBox.setSelectedItem(ZoneId.systemDefault().toString());
    timeZoneComboBox.addActionListener(
        e -> {
          if (timeZoneComboBox.getSelectedItem() != null) {
            clock.setTimeZone(ZoneId.of(timeZoneComboBox.getSelectedItem().toString()));
          }
        });
    bottomPanel.add(timeZoneComboBox);

    frame.getContentPane().add(bottomPanel, BorderLayout.SOUTH);

    frame.pack();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setVisible(true);
  }

  /**
   * Creates a new {@code JClock}, unpaused JClock that displays the current system time and uses
   * the system default time zone.
   */
  public JClock() {
    setPreferredSize(new Dimension(100, 100));
    setLayout(new BorderLayout());

    timer = new Timer(1000 / DEFAULT_HZ, e -> setTime(ZonedDateTime.now(time.getZone())));
    timer.start();
  }

  /**
   * Sets the displayed time. You almost certainly want to {@link #setPaused pause} the clock before
   * calling {@code setTime}, because an unpaused clock will display the current time in the next
   * rendering tick.
   */
  public void setTime(ZonedDateTime when) {
    if (SwingUtilities.isEventDispatchThread()) {
      time = when;
      repaint();
    } else {
      SwingUtilities.invokeLater(
          () -> {
            time = when;
            repaint();
          });
    }
  }

  /**
   * Sets the time zone of the clock.
   */
  public void setTimeZone(ZoneId timeZone) {
    SwingUtilities.invokeLater(() -> time = time.withZoneSameInstant(timeZone));
  }

  private void drawFace(Graphics g) {
    // Draw an ellipse representing the face of the clock.
    g.drawOval(
        center.x - horizAxisLength,
        center.y - vertAxisLength,
        horizAxisLength * 2,
        vertAxisLength * 2);
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

    double lengthX = 5 * horizAxisLength / 7.0f;
    double lengthY = 5 * vertAxisLength / 7.0f;

    g.setColor(Color.BLACK);
    g.drawLine(
        center.x,
        center.y,
        center.x + (int) (lengthX * Math.cos(Math.toRadians(angle))),
        center.y - (int) (lengthY * Math.sin(Math.toRadians(angle))));
  }

  private void drawMinuteHand(Graphics g) {
    int minute = time.getMinute();
    double angle = -(6 * minute);
    int second = time.getSecond();
    angle -= second * 0.1;
    int nano = time.getNano();
    angle -= nano * (0.1 / 1e9);
    angle += 90;
    g.setColor(Color.BLACK);
    g.drawLine(
        center.x,
        center.y,
        center.x + (int) (horizAxisLength * Math.cos(Math.toRadians(angle))),
        center.y - (int) (vertAxisLength * Math.sin(Math.toRadians(angle))));
  }

  private void drawSecondHand(Graphics g) {
    int second = time.getSecond();
    double angle = -(6 * second);
    if (sweepSecond) {
      angle -= 6 * (time.getNano() / 1e9);
    }
    angle += 90;
    g.setColor(Color.RED);
    g.drawLine(
        center.x,
        center.y,
        center.x + (int) (horizAxisLength * Math.cos(Math.toRadians(angle))),
        center.y - (int) (vertAxisLength * Math.sin(Math.toRadians(angle))));
  }

  private void drawTicks(Graphics g) {
    for (int hour = 0; hour < 12; hour++) {
      double angle = -(30 * hour);
      g.setColor(Color.BLACK);
      double hourDistanceFromCenterX = horizAxisLength * 13.0 / 14;
      double hourDistanceFromCenterY = vertAxisLength * 13.0 / 14;
      double tickCenterX =
          center.x + (int) (hourDistanceFromCenterX * Math.cos(Math.toRadians(angle)));
      double tickCenterY =
          center.y - (int) (hourDistanceFromCenterY * Math.sin(Math.toRadians(angle)));
      double tickRadiusX = horizAxisLength / 30.0;
      double tickRadiusY = vertAxisLength / 30.0;
      g.fillOval(
          (int) (tickCenterX - tickRadiusX),
          (int) (tickCenterY - tickRadiusY),
          (int) (2 * tickRadiusX),
          (int) (2 * tickRadiusY));
    }

    for (int minute = 0; minute < 60; minute++) {
      if (minute % 5 == 0) {
        continue;
      }

      double angle = -(6 * minute);
      g.setColor(Color.BLACK);
      double minuteDistanceFromCenterX = horizAxisLength * 13.0 / 14;
      double minuteDistanceFromCenterY = vertAxisLength * 13.0 / 14;
      double tickCenterX =
          center.x + (int) (minuteDistanceFromCenterX * Math.cos(Math.toRadians(angle)));
      double tickCenterY =
          center.y - (int) (minuteDistanceFromCenterY * Math.sin(Math.toRadians(angle)));
      double tickRadiusX = horizAxisLength / 60.0;
      double tickRadiusY = vertAxisLength / 60.0;
      g.fillOval(
          (int) (tickCenterX - tickRadiusX),
          (int) (tickCenterY - tickRadiusY),
          (int) (2 * tickRadiusX),
          (int) (2 * tickRadiusY));
    }
  }

  @Override
  public void paint(Graphics g) {
    super.paint(g);
    if (allowEllipticalClock) {
      horizAxisLength = getWidth() / 2;
      vertAxisLength = getHeight() / 2;
    } else {
      horizAxisLength = Math.min(getWidth(), getHeight()) / 2;
      vertAxisLength = horizAxisLength;
    }
    center = new Point(getWidth() / 2, getHeight() / 2);

    var hints = new HashMap<RenderingHints.Key, Object>();
    hints.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    RenderingHints rh = new RenderingHints(hints);

    Graphics2D g2 = (Graphics2D) g;
    g2.setRenderingHints(rh);
    g2.setStroke(
        new BasicStroke(
            2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1.0f, new float[] {6.0f, 6.0f}, 0));
    drawFace(g);
    g2.setStroke(new BasicStroke(2));
    drawTicks(g);
    drawHourHand(g);
    drawMinuteHand(g);
    if (drawSecondHand) {
      drawSecondHand(g);
    }

    String timeString = time.format(formatter);
    Rectangle2D bounds = g.getFontMetrics().getStringBounds(timeString, g);
    g.drawString(
        timeString,
        (int) (center.x - bounds.getWidth() / 2),
        (int) ((0.1 * vertAxisLength) + (center.y - bounds.getHeight() / 2)));
  }

  /**
   * Pauses or unpauses the clock. You should pause the clock before setting the time, as an
   * unpaused clock will display the current system time every rendering tick.
   */
  public void setPaused(boolean paused) {
    if (paused) {
      timer.stop();
    } else {
      timer.start();
    }
  }

  /**
   * Returns whether the clock is paused.
   */
  public boolean isPaused() {
    return timer.isRunning();
  }

  /**
   * Sets whether the second hand will be drawn.
   */
  public void setDrawSecondHand(boolean drawSecondHand) {
    this.drawSecondHand = drawSecondHand;
  }

  /**
   * Sets whether the clock is allowed to have an aspect ratio other than 1.
   *
   * <p>
   * By default, {@code JClock} will have a circular face irrespective of the size of its container,
   * its radius determined by the shortest side of the container. If {@code allowEllipticalClock} is
   * {@code true}, the clock will be drawn to fit the container in both axes.
   */
  public void setAllowEllipticalClock(boolean allowEllipticalClock) {
    this.allowEllipticalClock = allowEllipticalClock;
  }

  public void setSweepSecond(boolean sweepSecond) {
    this.sweepSecond = sweepSecond;
  }

  public boolean isSweepSecond() {
    return sweepSecond;
  }
}
