package com.revolsys.swing.parallel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.jdesktop.swingx.JXBusyLabel;

import com.revolsys.swing.component.BusyLabelPainter;

public class BackgroundTaskProgressBar extends JPanel implements PropertyChangeListener {
  private static final long serialVersionUID = -5112492385171847107L;

  private final JXBusyLabel busyLabel = new JXBusyLabel(new Dimension(16, 16));

  private final AtomicBoolean busy = new AtomicBoolean();

  public BackgroundTaskProgressBar() {
    super(new BorderLayout(2, 2));
    this.busyLabel.setBusyPainter(new BusyLabelPainter());
    this.busyLabel.setDelay(400);
    this.busyLabel.setFocusable(false);
    this.busyLabel.setBusy(false);
    Invoke.getPropertyChangeSupport().addPropertyChangeListener("taskTime", this);
    add(this.busyLabel, BorderLayout.WEST);
    setVisible(false);
  }

  @Override
  public void propertyChange(final PropertyChangeEvent event) {
    final boolean hasTask = Invoke.hasTasks();
    if (this.busy.compareAndSet(!hasTask, hasTask)) {
      updateVisible(hasTask);
    }
  }

  @Override
  public void removeNotify() {
    super.removeNotify();
    Invoke.getPropertyChangeSupport().removePropertyChangeListener("taskTime", this);
  }

  private void updateVisible(boolean visible) {
    if (SwingUtilities.isEventDispatchThread()) {
      this.busyLabel.setBusy(visible);
      setVisible(visible);
    } else {
      Invoke.laterQueue(() -> updateVisible(visible));
    }
  }
}
