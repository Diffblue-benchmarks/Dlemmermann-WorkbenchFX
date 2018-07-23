package com.dlsc.workbenchfx.view;

import static com.dlsc.workbenchfx.Workbench.STYLE_CLASS_ACTIVE_HOME;

import com.dlsc.workbenchfx.Workbench;
import com.dlsc.workbenchfx.model.WorkbenchModule;
import com.dlsc.workbenchfx.util.WorkbenchUtils;
import com.dlsc.workbenchfx.view.controls.selectionstrip.TabCell;
import java.util.Objects;
import javafx.beans.InvalidationListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Represents the presenter of the corresponding {@link ToolbarView}.
 *
 * @author François Martin
 * @author Marco Sanfratello
 */
public class ToolbarPresenter extends Presenter {

  private static final Logger LOGGER =
      LogManager.getLogger(ToolbarPresenter.class.getName());
  private final Workbench model;
  private final ToolbarView view;

  // Strong reference to prevent garbage collection
  private final ObservableList<MenuItem> navigationDrawerItems;
  private final ObservableSet<Node> toolbarControlsLeft;
  private final ObservableSet<Node> toolbarControlsRight;
  private final ObservableList<WorkbenchModule> openModules;

  /**
   * Creates a new {@link ToolbarPresenter} object for a corresponding {@link ToolbarView}.
   */
  public ToolbarPresenter(Workbench model, ToolbarView view) {
    this.model = model;
    this.view = view;
    navigationDrawerItems = model.getNavigationDrawerItems();
    toolbarControlsLeft = model.getToolbarControlsLeft();
    toolbarControlsRight = model.getToolbarControlsRight();
    openModules = model.getOpenModules();
    init();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void initializeViewParts() {
    view.tabBar.setCellFactory(tab -> new TabCell());

    toolbarControlsLeft.stream().forEachOrdered(view::addToolbarControlLeft);
    toolbarControlsRight.stream().forEachOrdered(view::addToolbarControlRight);

    // only add the menu button, if there is at least one navigation drawer item
    if (model.getNavigationDrawerItems().size() > 0) {
      view.addMenuButton();
    }

    view.homeBtn.requestFocus();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setupEventHandlers() {
    // When the home button is clicked, the view changes
    view.homeBtn.setOnAction(event -> model.openHomeScreen());
    // When the menu button is clicked, the navigation drawer gets shown
    view.menuBtn.setOnAction(event -> model.showNavigationDrawer());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setupValueChangedListeners() {
    // When the List of the currently open toolbarControlsLeft is changed, the view is updated.
    WorkbenchUtils.addSetListener(
        toolbarControlsLeft,
        change -> view.addToolbarControlLeft(change.getElementAdded()),
        change -> view.removeToolbarControlLeft(change.getElementRemoved())
    );
    // When the List of the currently open toolbarControlsRight is changed, the view is updated.
    WorkbenchUtils.addSetListener(
        toolbarControlsRight,
        change -> view.addToolbarControlRight(change.getElementAdded()),
        change -> view.removeToolbarControlRight(change.getElementRemoved())
    );

    model.activeModuleProperty().addListener((observable, oldModule, newModule) -> {
      if (Objects.isNull(oldModule)) {
        // Home is the old value
        view.homeBtn.getStyleClass().remove(STYLE_CLASS_ACTIVE_HOME);
      }
      if (Objects.isNull(newModule)) {
        // Home is the new value
        view.homeBtn.getStyleClass().add(STYLE_CLASS_ACTIVE_HOME);
      }
    });

    // makes sure the menu button is only being displayed if there are navigation drawer items
    navigationDrawerItems.addListener((InvalidationListener) observable -> {
      if (navigationDrawerItems.size() == 0) {
        view.removeMenuButton();
      } else {
        view.addMenuButton();
      }
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setupBindings() {
    // Binds content of the SelectionStrip to the Workbench content
    view.tabBar.itemsProperty().bindContent(openModules);
    view.tabBar.selectedItemProperty().bindBidirectional(model.activeModuleProperty());
  }

}
