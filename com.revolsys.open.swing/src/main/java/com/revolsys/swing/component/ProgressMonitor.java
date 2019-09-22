package com.revolsys.swing.component;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.UIManager;

import org.jdesktop.swingx.VerticalLayout;
import org.jeometry.common.logging.Logs;

import com.revolsys.beans.PropertyChangeSupport;
import com.revolsys.swing.SwingUtil;
import com.revolsys.swing.action.RunnableAction;
import com.revolsys.swing.parallel.Invoke;
import com.revolsys.util.Cancellable;

public class ProgressMonitor extends JDialog implements Cancellable, WindowListener {
  private static final long serialVersionUID = -5843323756390303783L;

  public static void background(final Component component, final String title, final String note,
    final Consumer<ProgressMonitor> task, final int max) {
    background(component, title, note, task, max, null);
  }

  public static void background(final Component component, final String title, final String note,
    final Consumer<ProgressMonitor> task, final int max, final Runnable doneTask) {
    Invoke.later(() -> {
      final ProgressMonitor progressMonitor = new ProgressMonitor(component, title, note, true,
        max);
      Invoke.background(title, () -> {
        try {
          task.accept(progressMonitor);
        } catch (final Throwable e) {
          Logs.error(ProgressMonitor.class, e);
        }
        return null;
      }, r -> {
        progressMonitor.done = true;
        progressMonitor.setVisible(false);
        if (!progressMonitor.cancelled && doneTask != null) {
          component.requestFocus();
          doneTask.run();
        }
      });
      if (!progressMonitor.done) {
        progressMonitor.setVisible(true);
      }
    });
  }

  public static <V> void background(final String title, final Collection<V> objects,
    final Consumer<V> action) {
    background(SwingUtil.getActiveWindow(), title, null, monitor -> {
      for (final V object : monitor.cancellable(objects)) {
        action.accept(object);
        monitor.addProgress();
      }
    }, objects.size());
  }

  public static void background(final String title, final String note,
    final Consumer<ProgressMonitor> task, final int max) {
    background(SwingUtil.getActiveWindow(), title, note, task, max);
  }

  public static void ui(final Component component, final String title, final String note,
    final boolean canCancel, final Consumer<ProgressMonitor> task) {
    final ProgressMonitor progressMonitor = new ProgressMonitor(component, title, note, canCancel);
    Invoke.workerDone(title, () -> {
      task.accept(progressMonitor);
    });
    progressMonitor.setVisible(true);
  }

  private final JButton cancelButton;

  private boolean cancelled = false;

  private final JLabel noteLabel;

  private final JProgressBar progressBar = new JProgressBar();

  private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

  private final AtomicInteger counter = new AtomicInteger();

  private int step;

  private boolean done;

  private final int max;

  private ProgressMonitor(final Component component, final String title, final String note,
    final boolean canCancel) {
    this(component, title, note, canCancel, 0);
  }

  private ProgressMonitor(final Component component, final String title, final String note,
    final boolean canCancel, final int max) {
    super(SwingUtil.getWindowAncestor(component), title, ModalityType.APPLICATION_MODAL);
    this.max = max;
    setMinimumSize(new Dimension(title.length() * 20, 100));
    setLayout(new VerticalLayout(5));
    getRootPane().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
    this.noteLabel = new JLabel(note);
    add(this.noteLabel);
    if (max <= 0) {
      this.progressBar.setIndeterminate(true);
    } else {
      final int progressMax = Math.min(max, 100);
      this.progressBar.setMinimum(0);
      this.progressBar.setMaximum(progressMax);
      this.step = Math.floorDiv(max, progressMax);
    }
    final String cancelText = UIManager.getString("OptionPane.cancelButtonText");
    final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    this.cancelButton = RunnableAction.newButton(cancelText, this::cancel);
    this.cancelButton.setEnabled(canCancel);
    buttonPanel.add(this.cancelButton);
    add(this.progressBar);
    add(buttonPanel);
    setLocationRelativeTo(component);
    pack();
    setAlwaysOnTop(true);
    setResizable(false);
  }

  public void addProgress() {
    final int count = this.counter.incrementAndGet();
    if (this.max <= 100 || count % this.step == 0) {
      Invoke.later(() -> this.progressBar.setValue(this.progressBar.getValue() + 1));
    }
  }

  public void cancel() {
    this.cancelled = true;
    this.propertyChangeSupport.firePropertyChange("cancelled", false, true);
  }

  public void finish() {
    Invoke.later(() -> this.progressBar.setValue(this.progressBar.getMaximum()));
  }

  public PropertyChangeSupport getPropertyChangeSupport() {
    return this.propertyChangeSupport;
  }

  @Override
  public boolean isCancelled() {
    return this.cancelled;
  }

  public boolean isDone() {
    return this.done;
  }

  public void setNote(final String note) {
    this.noteLabel.setText(note);
    pack();
  }

  public void setVisible(final Window window) {
    window.addWindowListener(this);
    SwingUtil.setVisible(window, true);
  }

  @Override
  public void windowActivated(final WindowEvent e) {
  }

  @Override
  public void windowClosed(final WindowEvent e) {
  }

  @Override
  public void windowClosing(final WindowEvent e) {
  }

  @Override
  public void windowDeactivated(final WindowEvent e) {
  }

  @Override
  public void windowDeiconified(final WindowEvent e) {
  }

  @Override
  public void windowIconified(final WindowEvent e) {
  }

  @Override
  public void windowOpened(final WindowEvent e) {
    final Window window = e.getWindow();
    window.removeWindowListener(this);
    SwingUtil.dispose(this);
    window.toFront();
  }
}
