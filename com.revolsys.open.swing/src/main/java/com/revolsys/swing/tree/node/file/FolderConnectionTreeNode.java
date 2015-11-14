package com.revolsys.swing.tree.node.file;

import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import javax.swing.JOptionPane;

import com.revolsys.io.file.FolderConnection;
import com.revolsys.io.file.Paths;
import com.revolsys.swing.SwingUtil;
import com.revolsys.swing.menu.MenuFactory;
import com.revolsys.swing.tree.TreeNodes;
import com.revolsys.swing.tree.node.BaseTreeNode;
import com.revolsys.swing.tree.node.LazyLoadTreeNode;
import com.revolsys.util.UrlProxy;
import com.revolsys.util.UrlUtil;

public class FolderConnectionTreeNode extends LazyLoadTreeNode implements UrlProxy {
  private static final MenuFactory MENU = new MenuFactory("Folder Connection");

  static {
    addRefreshMenuItem(MENU);

    TreeNodes.addMenuItem(MENU, "default", "Delete Folder Connection", "delete",
      FolderConnectionTreeNode::isEditable, FolderConnectionTreeNode::deleteConnection);
  }

  public FolderConnectionTreeNode(final FolderConnection connection) {
    super(connection);
    setType("Folder Connection");
    setName(connection.getName());
    setIcon(PathTreeNode.ICON_FOLDER_LINK);
  }

  private void deleteConnection() {
    final FolderConnection connection = getConnection();
    final int confirm = JOptionPane.showConfirmDialog(SwingUtil.getActiveWindow(),
      "Delete folder connection '" + connection.getName() + "'? This action cannot be undone.",
      "Delete Layer", JOptionPane.OK_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE);
    if (confirm == JOptionPane.OK_OPTION) {
      connection.delete();
    }
  }

  @Override
  protected List<BaseTreeNode> doLoadChildren() {
    final Path path = getPath();
    return PathTreeNode.getPathNodes(this, path);
  }

  public FolderConnection getConnection() {
    return getUserData();
  }

  @Override
  public MenuFactory getMenu() {
    return MENU;
  }

  public Path getPath() {
    final FolderConnection connection = getConnection();
    return connection.getPath();
  }

  @Override
  public URL getUrl() {
    final String name = getName();
    final String urlstring = "folderconnection://" + UrlUtil.percentEncode(name) + "/";
    try {
      final URL url = new URL(urlstring);
      return url;
    } catch (final Throwable e) {
      throw new IllegalArgumentException("Invalid URL: " + urlstring, e);
    }
  }

  public boolean isEditable() {
    return !isReadOnly();
  }

  @Override
  public boolean isExists() {
    final Path path = getPath();
    return Paths.exists(path) && super.isExists();
  }

  public boolean isReadOnly() {
    final FolderConnection connection = getConnection();
    return connection.isReadOnly();
  }
}
