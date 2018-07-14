package com.dlsc.workbenchfx;

import static com.dlsc.workbenchfx.Workbench.WorkbenchBuilder;
import static com.dlsc.workbenchfx.testing.MockFactory.createMockModule;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.dlsc.workbenchfx.model.WorkbenchDialog;
import com.dlsc.workbenchfx.model.WorkbenchModule;
import com.dlsc.workbenchfx.testing.MockDialogControl;
import com.dlsc.workbenchfx.testing.MockNavigationDrawer;
import com.dlsc.workbenchfx.testing.MockPage;
import com.dlsc.workbenchfx.testing.MockTab;
import com.dlsc.workbenchfx.testing.MockTile;
import com.dlsc.workbenchfx.view.controls.Dropdown;
import com.dlsc.workbenchfx.view.controls.GlassPane;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIcon;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import java.util.concurrent.CompletableFuture;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.collections.ObservableSet;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationTest;

/**
 * Tests for {@link Workbench}.
 */
@Tag("fast")
class WorkbenchTest extends ApplicationTest {

  private static final int SIZE = 3;

  private static final int FIRST_INDEX = 0;
  private static final int SECOND_INDEX = 1;
  private static final int LAST_INDEX = SIZE - 1;
  Workbench workbench;

  WorkbenchModule[] mockModules = new WorkbenchModule[SIZE];
  Node[] moduleNodes = new Node[SIZE];

  WorkbenchModule first;
  WorkbenchModule second;
  WorkbenchModule last;
  private ObservableMap<Node, GlassPane> overlays;
  private ObservableSet<Node> blockingOverlaysShown;
  private ObservableSet<Node> overlaysShown;
  private Node overlay1;
  private Node overlay2;
  private Node overlay3;

  private MenuItem menuItem;
  private ObservableList<MenuItem> navigationDrawerItems;

  private FxRobot robot;

  // Dropdown items
  private String dropdownText;
  private FontAwesomeIconView dropdownIconView;
  private ImageView dropdownImageView;
  private MenuItem dropdownMenuItem;
  private Dropdown dropdownLeft;
  private Dropdown dropdownRight;

  private MockNavigationDrawer navigationDrawer;
  private MockDialogControl dialogControl;

  @Mock
  private WorkbenchDialog mockDialog;
  @Mock
  private CompletableFuture<ButtonType> mockDialogResult;

  @Override
  public void start(Stage stage) {
    MockitoAnnotations.initMocks(this);

    robot = new FxRobot();

    for (int i = 0; i < moduleNodes.length; i++) {
      moduleNodes[i] = new Label("Module Content");
    }

    for (int i = 0; i < mockModules.length; i++) {
      mockModules[i] = createMockModule(moduleNodes[i], null, true, "Module " + i);
    }

    FontAwesomeIconView fontAwesomeIconView = new FontAwesomeIconView(FontAwesomeIcon.QUESTION);
    fontAwesomeIconView.getStyleClass().add("icon");
    menuItem = new MenuItem("Item 1.1", fontAwesomeIconView);

    // Initialization of items for Dropdown testing
    dropdownText = "Dropdown Text";
    dropdownIconView = new FontAwesomeIconView(FontAwesomeIcon.QUESTION);
    dropdownImageView = new ImageView(
        new Image(WorkbenchTest.class.getResource("date-picker.png").toExternalForm())
    );
    dropdownMenuItem = new MenuItem("Menu Item");

    dropdownLeft = Dropdown.of(dropdownText, dropdownIconView, dropdownMenuItem);
    dropdownRight = Dropdown.of(dropdownText, dropdownImageView, dropdownMenuItem);

    // Setup WorkbenchDialog Mock
    when(mockDialog.getResult()).thenReturn(mockDialogResult);
    when(mockDialog.getButtonTypes()).thenReturn(
        FXCollections.observableArrayList(ButtonType.PREVIOUS, ButtonType.NEXT)
    );
    when(mockDialogResult.complete(any())).then(invocation -> {
          when(mockDialogResult.isDone()).thenReturn(true);
          return true;
        }
    );

    navigationDrawer = new MockNavigationDrawer();
    dialogControl = new MockDialogControl();

    workbench = Workbench.builder(
        mockModules[FIRST_INDEX],
        mockModules[SECOND_INDEX],
        mockModules[LAST_INDEX])
        .tabFactory(MockTab::new)
        .tileFactory(MockTile::new)
        .pageFactory(MockPage::new)
        .dialogControl(dialogControl)
        .navigationDrawer(navigationDrawer)
        .navigationDrawerItems(menuItem)
        .toolbarLeft(dropdownLeft)
        .toolbarRight(dropdownRight)
        .build();

    first = mockModules[FIRST_INDEX];
    second = mockModules[SECOND_INDEX];
    last = mockModules[LAST_INDEX];

    overlays = workbench.getOverlays();
    blockingOverlaysShown = workbench.getBlockingOverlaysShown();
    overlaysShown = workbench.getNonBlockingOverlaysShown();
    overlay1 = new Label();
    overlay1.setVisible(false);
    overlay2 = new Label();
    overlay2.setVisible(false);
    overlay3 = new Label();
    overlay3.setVisible(false);

    navigationDrawerItems = workbench.getNavigationDrawerItems();

    Scene scene = new Scene(workbench, 100, 100);
    stage.setScene(scene);
    stage.show();
  }

  @Test
  void testCtor() {
    robot.interact(() -> {
      assertEquals(mockModules.length, workbench.getModules().size());
      for (int i = 0; i < mockModules.length; i++) {
        assertSame(mockModules[i], workbench.getModules().get(i));
      }

      assertEquals(0, workbench.getOpenModules().size());

      assertNull(workbench.activeModuleViewProperty().get());
    });
  }

  @Test
  void openModule() {
    robot.interact(() -> {
      // Open first
      workbench.openModule(first);
      assertSame(first, workbench.getActiveModule());
      assertSame(moduleNodes[FIRST_INDEX], workbench.getActiveModuleView());
      assertEquals(1, workbench.getOpenModules().size());
      InOrder inOrder = inOrder(first);
      inOrder.verify(first).init(workbench);
      inOrder.verify(first).activate();
      // Open last
      workbench.openModule(last);
      assertSame(last, workbench.getActiveModule());
      assertSame(moduleNodes[LAST_INDEX], workbench.getActiveModuleView());
      assertEquals(2, workbench.getOpenModules().size());
      inOrder = inOrder(first, last);
      inOrder.verify(first).deactivate();
      inOrder.verify(last).init(workbench);
      inOrder.verify(last).activate();
      // Open last again
      workbench.openModule(last);
      assertSame(last, workbench.getActiveModule());
      assertSame(moduleNodes[LAST_INDEX], workbench.getActiveModuleView());
      assertEquals(2, workbench.getOpenModules().size());
      verify(last, times(1)).init(workbench);
      verify(last, times(1)).activate();
      verify(last, never()).deactivate();
      // Open first (already initialized)
      workbench.openModule(first);
      assertSame(first, workbench.getActiveModule());
      assertSame(moduleNodes[FIRST_INDEX], workbench.getActiveModuleView());
      assertEquals(2, workbench.getOpenModules().size());
      verify(first, times(1)).init(workbench); // no additional init on first
      verify(last, times(1)).init(workbench); // no additional init on last
      inOrder = inOrder(first, last);
      inOrder.verify(last).deactivate();
      inOrder.verify(first).activate();
      verify(first, times(2)).activate();
      // Switch to home screen
      workbench.openHomeScreen();
      assertSame(null, workbench.getActiveModule());
      assertSame(null, workbench.getActiveModuleView());
      assertEquals(2, workbench.getOpenModules().size());
      verify(first, times(1)).init(workbench); // no additional init on first
      verify(last, times(1)).init(workbench); // no additional init on last
      verify(first, times(2)).deactivate();
      // Open second
      workbench.openModule(second);
      assertSame(second, workbench.getActiveModule());
      assertSame(moduleNodes[SECOND_INDEX], workbench.getActiveModuleView());
      assertEquals(3, workbench.getOpenModules().size());
      inOrder = inOrder(second);
      inOrder.verify(second).init(workbench);
      inOrder.verify(second).activate();
      inOrder.verifyNoMoreInteractions();
    });
  }

  @Test
  void openModuleInvalid() {
    /* Test if opening a module which has not been passed in the constructor of WorkbenchFxModel
    throws an exception */
    robot.interact(() -> {
      assertThrows(IllegalArgumentException.class,
          () -> workbench.openModule(mock(WorkbenchModule.class)));
    });
  }

  /**
   * Precondition: openModule tests pass.
   */
  @Test
  void closeModuleOne() {
    // open and close module
    robot.interact(() -> {
      workbench.openModule(first);
      workbench.closeModule(first);

      assertSame(null, workbench.getActiveModule());
      assertSame(null, workbench.getActiveModuleView());
      assertEquals(0, workbench.getOpenModules().size());

      InOrder inOrder = inOrder(first);
      // Call: workbench.openModule(first)
      inOrder.verify(first).init(workbench);
      inOrder.verify(first).activate();
      // Call: workbench.closeModule(first)
      inOrder.verify(first).destroy();
      inOrder.verifyNoMoreInteractions();
    });
  }

  /**
   * Precondition: openModule tests pass.
   */
  @Test
  void closeModuleLeft1() {
    robot.interact(() -> {
      // open two modules, close left module
      // right active
      workbench.openModule(first);
      workbench.openModule(second);
      workbench.closeModule(first);

      assertSame(second, workbench.getActiveModule());
      assertSame(moduleNodes[SECOND_INDEX], workbench.getActiveModuleView());
      assertEquals(1, workbench.getOpenModules().size());
      verify(second, never()).deactivate();

      InOrder inOrder = inOrder(first, second);
      // Call: workbench.openModule(first)
      inOrder.verify(first).init(workbench);
      inOrder.verify(first).activate();
      // Call: workbench.openModule(second)
      inOrder.verify(first).deactivate();
      inOrder.verify(second).init(workbench);
      inOrder.verify(second).activate();
      // Call: workbench.closeModule(first)
      inOrder.verify(first).destroy();
      inOrder.verifyNoMoreInteractions();
    });
  }

  /**
   * Precondition: openModule tests pass.
   */
  @Test
  void closeModuleLeft2() {
    robot.interact(() -> {
      // open two modules, close left module
      // left active
      workbench.openModule(first);
      workbench.openModule(second);
      workbench.openModule(first);
      workbench.closeModule(first);

      assertSame(second, workbench.getActiveModule());
      assertSame(moduleNodes[SECOND_INDEX], workbench.getActiveModuleView());
      assertEquals(1, workbench.getOpenModules().size());

      InOrder inOrder = inOrder(first, second);
      // Call: workbench.openModule(first)
      inOrder.verify(first).init(workbench);
      inOrder.verify(first).activate();
      // Call: workbench.openModule(second)
      inOrder.verify(first).deactivate();
      inOrder.verify(second).init(workbench);
      inOrder.verify(second).activate();
      // Call: workbench.openModule(first)
      inOrder.verify(second).deactivate();
      inOrder.verify(first).activate();
      // Call: workbench.closeModule(first)
      inOrder.verify(first).destroy();
      inOrder.verify(second).activate();
      inOrder.verifyNoMoreInteractions();
    });
  }

  /**
   * Precondition: openModule tests pass.
   */
  @Test
  void closeModuleRight1() {
    // open two modules, close right module
    // right active
    robot.interact(() -> {
      workbench.openModule(first);
      workbench.openModule(second);
      workbench.closeModule(second);

      assertSame(first, workbench.getActiveModule());
      assertSame(moduleNodes[FIRST_INDEX], workbench.getActiveModuleView());
      assertEquals(1, workbench.getOpenModules().size());

      InOrder inOrder = inOrder(first, second);
      // Call: workbench.openModule(first)
      inOrder.verify(first).init(workbench);
      inOrder.verify(first).activate();
      // Call: workbench.openModule(second)
      inOrder.verify(first).deactivate();
      inOrder.verify(second).init(workbench);
      inOrder.verify(second).activate();
      // Call: workbench.closeModule(second)
      inOrder.verify(second).destroy();
      inOrder.verify(first).activate();
      inOrder.verifyNoMoreInteractions();
    });
  }

  /**
   * Precondition: openModule tests pass.
   */
  @Test
  void closeModuleRight2() {
    // open two modules, close right module
    // left active
    robot.interact(() -> {
      workbench.openModule(first);
      workbench.openModule(second);
      workbench.openModule(first);
      workbench.closeModule(second);

      assertSame(first, workbench.getActiveModule());
      assertSame(moduleNodes[FIRST_INDEX], workbench.getActiveModuleView());
      assertEquals(1, workbench.getOpenModules().size());

      InOrder inOrder = inOrder(first, second);
      // Call: workbench.openModule(first)
      inOrder.verify(first).init(workbench);
      inOrder.verify(first).activate();
      // Call: workbench.openModule(second)
      inOrder.verify(first).deactivate();
      inOrder.verify(second).init(workbench);
      inOrder.verify(second).activate();
      // Call: workbench.openModule(first)
      inOrder.verify(second).deactivate();
      inOrder.verify(first).activate();
      // Call: workbench.closeModule(second)
      inOrder.verify(second).destroy();
      inOrder.verifyNoMoreInteractions();
    });
  }

  /**
   * Precondition: openModule tests pass.
   */
  @Test
  void closeModuleMiddleActive() {
    // open three modules and close middle module
    // middle active
    robot.interact(() -> {
      workbench.openModule(first);
      workbench.openModule(second);
      workbench.openModule(last);
      workbench.openModule(second);
      workbench.closeModule(second);

      assertSame(first, workbench.getActiveModule());
      assertSame(moduleNodes[FIRST_INDEX], workbench.getActiveModuleView());
      assertEquals(2, workbench.getOpenModules().size());

      InOrder inOrder = inOrder(first, second, last);
      // Call: workbench.openModule(first)
      inOrder.verify(first).init(workbench);
      inOrder.verify(first).activate();
      // Call: workbench.openModule(second)
      inOrder.verify(first).deactivate();
      inOrder.verify(second).init(workbench);
      inOrder.verify(second).activate();
      // Call: workbench.openModule(last)
      inOrder.verify(second).deactivate();
      inOrder.verify(last).init(workbench);
      inOrder.verify(last).activate();
      // Call: workbench.openModule(second)
      inOrder.verify(last).deactivate();
      inOrder.verify(second).activate();
      // Call: workbench.closeModule(second)
      inOrder.verify(second).destroy();
      inOrder.verify(first).activate();
      inOrder.verifyNoMoreInteractions();
    });
  }

  /**
   * Precondition: openModule tests pass.
   */
  @Test
  void closeModulePreventDestroyActive() {
    // open two modules, close second (active) module
    // destroy() on second module will return false, so the module shouldn't get closed
    when(second.destroy()).thenReturn(false);
    robot.interact(() -> {
      workbench.openModule(first);
      workbench.openModule(second);
      workbench.closeModule(second);

      assertSame(second, workbench.getActiveModule());
      assertSame(moduleNodes[SECOND_INDEX], workbench.getActiveModuleView());
      assertEquals(2, workbench.getOpenModules().size());

      InOrder inOrder = inOrder(first, second);
      // Call: workbench.openModule(first)
      inOrder.verify(first).init(workbench);
      inOrder.verify(first).activate();
      // Call: workbench.openModule(second)
      inOrder.verify(first).deactivate();
      inOrder.verify(second).init(workbench);
      inOrder.verify(second).activate();
      // Call: workbench.closeModule(second)
      // destroy second
      inOrder.verify(second).destroy();
      // notice destroy() was unsuccessful, keep focus on second
      inOrder.verifyNoMoreInteractions();
    });
  }

  /**
   * Precondition: openModule tests pass.
   */
  @Test
  void closeModulePreventDestroyInactive() {
    // open two modules, close first (inactive) module
    // destroy() on first module will return false, so the module shouldn't get closed
    when(first.destroy()).thenReturn(false);
    robot.interact(() -> {
      workbench.openModule(first);
      workbench.openModule(second);
      workbench.closeModule(first);

      assertSame(second, workbench.getActiveModule());
      assertSame(moduleNodes[SECOND_INDEX], workbench.getActiveModuleView());
      assertEquals(2, workbench.getOpenModules().size());

      InOrder inOrder = inOrder(first, second);
      // Call: workbench.openModule(first)
      inOrder.verify(first).init(workbench);
      inOrder.verify(first).activate();
      // Call: workbench.openModule(second)
      inOrder.verify(first).deactivate();
      inOrder.verify(second).init(workbench);
      inOrder.verify(second).activate();
      // Call: workbench.closeModule(second)
      // destroy second
      inOrder.verify(first).destroy();
      // notice destroy() was unsuccessful, keep focus on second
      inOrder.verifyNoMoreInteractions();
    });
  }

  /**
   * Example of what happens in case of a closing dialog in the destroy() method of a module with
   * the user confirming the module should get closed. Precondition: openModule tests pass.
   */
  @Test
  void closeModuleDestroyInactiveDialogClose() {
    // open two modules, close first (inactive) module
    // destroy() on first module will return false, so the module shouldn't get closed
    when(first.destroy()).then(invocation -> {
      robot.interact(() -> {
        workbench.openModule(first);
      });
      // dialog opens, user confirms closing module
      return true;
    });
    robot.interact(() -> {
      workbench.openModule(first);
      workbench.openModule(second);
      workbench.closeModule(first);

      assertSame(second, workbench.getActiveModule());
      assertSame(moduleNodes[SECOND_INDEX], workbench.getActiveModuleView());
      assertEquals(1, workbench.getOpenModules().size());

      InOrder inOrder = inOrder(first, second);
      // Call: workbench.openModule(first)
      inOrder.verify(first).init(workbench);
      inOrder.verify(first).activate();
      // Call: workbench.openModule(second)
      inOrder.verify(first).deactivate();
      inOrder.verify(second).init(workbench);
      inOrder.verify(second).activate();
      // Call: workbench.closeModule(first)
      // attempt to destroy first
      inOrder.verify(first).destroy();
      // destroy() opens itself: workbench.openModule(first)
      inOrder.verify(second).deactivate();
      inOrder.verify(first).activate();
      // destroy() returns true, switch to second
      inOrder.verify(second).activate();
      inOrder.verifyNoMoreInteractions();
    });
  }

  /**
   * Example of what happens in case of a closing dialog in the destroy() method of a module with
   * the user confirming the module should NOT get closed. Precondition: openModule tests pass.
   */
  @Test
  void closeModulePreventDestroyInactiveDialogClose() {
    // open two modules, close first (inactive) module
    // destroy() on first module will return false, so the module shouldn't get closed
    when(first.destroy()).then(invocation -> {
      robot.interact(() -> {
        workbench.openModule(first);
      });
      // dialog opens, user confirms NOT closing module
      return false;
    });

    robot.interact(() -> {
      workbench.openModule(first);
      workbench.openModule(second);
      workbench.closeModule(first);

      assertSame(first, workbench.getActiveModule());
      assertSame(moduleNodes[FIRST_INDEX], workbench.getActiveModuleView());
      assertEquals(2, workbench.getOpenModules().size());

      InOrder inOrder = inOrder(first, second);
      // Call: workbench.openModule(first)
      inOrder.verify(first).init(workbench);
      inOrder.verify(first).activate();
      // Call: workbench.openModule(second)
      inOrder.verify(first).deactivate();
      inOrder.verify(second).init(workbench);
      inOrder.verify(second).activate();
      // Call: workbench.closeModule(first)
      // attempt to destroy first
      inOrder.verify(first).destroy();
      // destroy() opens itself: workbench.openModule(first)
      inOrder.verify(second).deactivate();
      inOrder.verify(first).activate();
      // destroy() returns false, first stays the active module
      inOrder.verifyNoMoreInteractions();
    });
  }

  @Test
  void closeModuleInvalid() {
    robot.interact(() -> {
      // Test for null
      assertThrows(NullPointerException.class, () -> workbench.closeModule(null));
      // Test if closing a module not included in the modules at all throws an exception
      assertThrows(IllegalArgumentException.class,
          () -> workbench.closeModule(mock(WorkbenchModule.class)));
      // Test if closing a module not opened throws an exception
      assertThrows(IllegalArgumentException.class, () -> workbench.closeModule(mockModules[0]));
    });
  }

  @Test
  void closeInactiveModule() {
    robot.interact(() -> {
      workbench.openModule(first);
      workbench.openModule(second);
      workbench.openModule(last);
      workbench.closeModule(second);

      assertSame(last, workbench.getActiveModule());
      assertSame(moduleNodes[LAST_INDEX], workbench.getActiveModuleView());
      assertEquals(2, workbench.getOpenModules().size());

      InOrder inOrder = inOrder(first, second, last);
      // Call: workbench.openModule(first)
      inOrder.verify(first).init(workbench);
      inOrder.verify(first).activate();
      // Call: workbench.openModule(second)
      inOrder.verify(first).deactivate();
      inOrder.verify(second).init(workbench);
      inOrder.verify(second).activate();
      // Call: workbench.openModule(last)
      inOrder.verify(second).deactivate();
      inOrder.verify(last).init(workbench);
      inOrder.verify(last).activate();
      // Call: workbench.closeModule(second)
      inOrder.verify(second).destroy();
      inOrder.verifyNoMoreInteractions();
    });
  }

  @Test
  void getOpenModules() {
    // Test if unmodifiable list is returned
    robot.interact(() -> {
      assertThrows(UnsupportedOperationException.class, () -> workbench.getOpenModules().remove(0));
    });
  }

  @Test
  void activeModuleViewProperty() {
    assertTrue(workbench.activeModuleViewProperty() instanceof ReadOnlyObjectProperty);
  }

  @Test
  void activeModuleProperty() {
    assertTrue(workbench.activeModuleProperty() instanceof ReadOnlyObjectProperty);
  }

  @Test
  void getAmountOfPages() {
    robot.interact(() -> {
      int modulesPerPage = 1;
      assertEquals(1, prepareWorkbench(1, modulesPerPage).getAmountOfPages());
      assertEquals(2, prepareWorkbench(2, modulesPerPage).getAmountOfPages());
      assertEquals(3, prepareWorkbench(3, modulesPerPage).getAmountOfPages());

      modulesPerPage = 2;
      assertEquals(1, prepareWorkbench(1, modulesPerPage).getAmountOfPages());
      assertEquals(1, prepareWorkbench(2, modulesPerPage).getAmountOfPages());
      assertEquals(2, prepareWorkbench(3, modulesPerPage).getAmountOfPages());
      assertEquals(2, prepareWorkbench(4, modulesPerPage).getAmountOfPages());
      assertEquals(3, prepareWorkbench(5, modulesPerPage).getAmountOfPages());

      modulesPerPage = 3;
      assertEquals(1, prepareWorkbench(1, modulesPerPage).getAmountOfPages());
      assertEquals(1, prepareWorkbench(2, modulesPerPage).getAmountOfPages());
      assertEquals(1, prepareWorkbench(3, modulesPerPage).getAmountOfPages());
      assertEquals(2, prepareWorkbench(4, modulesPerPage).getAmountOfPages());
      assertEquals(2, prepareWorkbench(5, modulesPerPage).getAmountOfPages());
      assertEquals(2, prepareWorkbench(6, modulesPerPage).getAmountOfPages());
      assertEquals(3, prepareWorkbench(7, modulesPerPage).getAmountOfPages());
    });
  }

  private Workbench prepareWorkbench(int moduleAmount, int modulesPerPage) {
    WorkbenchModule[] modules = new WorkbenchModule[moduleAmount];
    for (int i = 0; i < moduleAmount; i++) {
      modules[i] = mock(WorkbenchModule.class);
    }
    return Workbench.builder(modules).modulesPerPage(modulesPerPage).build();
  }

  @Test
  void builder() {
    WorkbenchBuilder builder = Workbench.builder();
    assertNotNull(builder);
  }

  @Test
  void getOverlays() {
    robot.interact(() -> {
      ObservableMap<Node, GlassPane> overlays = workbench.getOverlays();
      // Test if unmodifiable map is returned
      assertThrows(UnsupportedOperationException.class,
          () -> overlays.put(new Label(), new GlassPane()));
    });
  }

  @Test
  void showOverlayBlocking() {
    robot.interact(() -> {
      assertEquals(0, overlays.size());
      assertEquals(0, blockingOverlaysShown.size());
      assertEquals(0, overlaysShown.size());

      workbench.showOverlay(overlay1, true);

      assertEquals(1, overlays.size());
      assertEquals(1, blockingOverlaysShown.size());
      assertEquals(0, overlaysShown.size());
      assertTrue(overlay1.isVisible()); // overlay1 has been made visible
      GlassPane glassPane = overlays.get(overlay1);
      assertFalse(glassPane.isHide());
      assertNull(glassPane.onMouseClickedProperty().get()); // no closing handler has been attached
      assertTrue(glassPane.hideProperty().isBound());

      // test visibility binding to GlassPane
      overlay1.setVisible(false);
      assertTrue(glassPane.isHide());

      // test if calling showOverlay again, even though it's already showing, does anything
      workbench.showOverlay(overlay1, true);

      assertEquals(1, overlays.size());
      assertEquals(1, blockingOverlaysShown.size());
      assertEquals(0, overlaysShown.size());
      assertFalse(overlay1.isVisible()); // overlay1 is still invisible
    });
  }

  @Test
  void showOverlayNonBlocking() {
    robot.interact(() -> {
      assertEquals(0, overlays.size());
      assertEquals(0, blockingOverlaysShown.size());
      assertEquals(0, overlaysShown.size());

      workbench.showOverlay(overlay1, false);

      assertEquals(1, overlays.size());
      assertEquals(0, blockingOverlaysShown.size());
      assertEquals(1, overlaysShown.size());
      assertTrue(overlay1.isVisible()); // overlay1 has been made visible
      GlassPane glassPane = overlays.get(overlay1);
      assertFalse(glassPane.isHide());
      assertNotNull(glassPane.onMouseClickedProperty().get()); // closing handler has been attached
      assertTrue(glassPane.hideProperty().isBound());

      // test visibility binding to GlassPane
      overlay1.setVisible(false);
      assertTrue(glassPane.isHide());

      // test if calling showOverlay again, even though it's already showing, does anything
      workbench.showOverlay(overlay1, false);

      assertEquals(1, overlays.size());
      assertEquals(0, blockingOverlaysShown.size());
      assertEquals(1, overlaysShown.size());
      assertFalse(overlay1.isVisible()); // overlay1 is still invisible
    });
  }

  @Test
  void showOverlayMultiple() {
    robot.interact(() -> {
      assertEquals(0, overlays.size());
      assertEquals(0, blockingOverlaysShown.size());
      assertEquals(0, overlaysShown.size());

      workbench.showOverlay(overlay1, false);
      workbench.showOverlay(overlay2, true);

      assertEquals(2, overlays.size());
      assertEquals(1, blockingOverlaysShown.size());
      assertEquals(1, overlaysShown.size());
      assertTrue(overlay1.isVisible()); // overlay1 has been made visible
      assertTrue(overlay2.isVisible()); // overlay1 has been made visible
      GlassPane glassPane1 = overlays.get(overlay1);
      assertFalse(glassPane1.isHide());
      assertNotNull(glassPane1.onMouseClickedProperty().get()); // closing handler has been attached
      assertTrue(glassPane1.hideProperty().isBound());
      GlassPane glassPane2 = overlays.get(overlay2);
      assertFalse(glassPane2.isHide());
      assertNull(glassPane2.onMouseClickedProperty().get()); // no closing handler has been attached
      assertTrue(glassPane2.hideProperty().isBound());

      // test visibility binding to GlassPane
      overlay1.setVisible(false);
      assertTrue(glassPane1.isHide());
      overlay2.setVisible(false);
      assertTrue(glassPane2.isHide());

      // test if calling showOverlay again, even though it's already showing, does anything
      workbench.showOverlay(overlay1, false);
      workbench.showOverlay(overlay2, true);

      assertEquals(2, overlays.size());
      assertEquals(1, blockingOverlaysShown.size());
      assertEquals(1, overlaysShown.size());
      assertFalse(overlay1.isVisible()); // overlay1 is still invisible
      assertFalse(overlay2.isVisible()); // overlay1 is still invisible
    });
  }

  /**
   * Precondition: showOverlay tests pass.
   */
  @Test
  void hideOverlayBlocking() {
    robot.interact(() -> {
      workbench.showOverlay(overlay1, true);
      boolean result = workbench.hideOverlay(overlay1);

      assertTrue(result);
      assertEquals(1, overlays.size()); // still loaded
      assertEquals(0, blockingOverlaysShown.size()); // none shown
      assertEquals(0, overlaysShown.size());
      assertFalse(overlay1.isVisible()); // overlay1 is invisible
      GlassPane glassPane = overlays.get(overlay1);
      assertTrue(glassPane.isHide());
      assertTrue(glassPane.hideProperty().isBound());

      // test if calling hideOverlay again, even though it's already hidden, does anything
      result = workbench.hideOverlay(overlay1);

      assertFalse(result);
      assertEquals(1, overlays.size());
      assertEquals(0, blockingOverlaysShown.size());
      assertEquals(0, overlaysShown.size());
      assertFalse(overlay1.isVisible()); // overlay1 is still invisible

      // test if calling showOverlay again, doesn't load the overlay1 into the map again
      result = workbench.showOverlay(overlay1, true);

      assertTrue(result);
      assertEquals(1, overlays.size());
      assertEquals(1, blockingOverlaysShown.size());
      assertEquals(0, overlaysShown.size());
      assertTrue(overlay1.isVisible()); // overlay1 is visible again
    });
  }

  /**
   * Precondition: showOverlay tests pass.
   */
  @Test
  void hideOverlayNonBlocking() {
    robot.interact(() -> {
      workbench.showOverlay(overlay1, false);
      boolean result = workbench.hideOverlay(overlay1);

      assertTrue(result);
      assertEquals(1, overlays.size()); // still loaded
      assertEquals(0, blockingOverlaysShown.size()); // none shown
      assertEquals(0, overlaysShown.size());
      assertFalse(overlay1.isVisible()); // overlay1 is invisible
      GlassPane glassPane = overlays.get(overlay1);
      assertTrue(glassPane.isHide());
      assertTrue(glassPane.hideProperty().isBound());

      // test if calling hideOverlay again, even though it's already hidden, does anything
      result = workbench.hideOverlay(overlay1);

      assertFalse(result);
      assertEquals(1, overlays.size());
      assertEquals(0, blockingOverlaysShown.size());
      assertEquals(0, overlaysShown.size());
      assertFalse(overlay1.isVisible()); // overlay1 is still invisible

      // test if calling showOverlay again, doesn't load the overlay1 into the map again
      result = workbench.showOverlay(overlay1, false);

      assertTrue(result);
      assertEquals(1, overlays.size());
      assertEquals(0, blockingOverlaysShown.size());
      assertEquals(1, overlaysShown.size());
      assertTrue(overlay1.isVisible()); // overlay1 is visible again
    });
  }

  /**
   * Precondition: showOverlay tests pass.
   */
  @Test
  void clearOverlaysShowing() {
    robot.interact(() -> {
      workbench.showOverlay(overlay1, false);
      workbench.showOverlay(overlay2, true);
      workbench.showOverlay(overlay3, false);

      assertEquals(3, overlays.size());
      assertEquals(1, blockingOverlaysShown.size());
      assertEquals(2, overlaysShown.size());
      assertTrue(overlay1.isVisible());
      assertTrue(overlay2.isVisible());
      assertTrue(overlay3.isVisible());

      final GlassPane glassPane1 = overlays.get(overlay1);
      final GlassPane glassPane2 = overlays.get(overlay2);
      final GlassPane glassPane3 = overlays.get(overlay3);

      workbench.clearOverlays();

      assertEquals(0, overlays.size());
      assertEquals(0, blockingOverlaysShown.size());
      assertEquals(0, overlaysShown.size());
      assertFalse(overlay1.isVisible());
      assertFalse(overlay2.isVisible());
      assertFalse(overlay3.isVisible());
      // closing handler was removed for non-blocking
      assertNull(glassPane1.onMouseClickedProperty().get());
      assertNull(glassPane3.onMouseClickedProperty().get());
      // glass panes were unbound
      assertFalse(glassPane1.hideProperty().isBound());
      assertFalse(glassPane2.hideProperty().isBound());
      assertFalse(glassPane3.hideProperty().isBound());
    });
  }

  /**
   * Precondition: showOverlay tests pass.
   */
  @Test
  void clearOverlaysHiding() {
    robot.interact(() -> {
      workbench.showOverlay(overlay1, false);
      workbench.showOverlay(overlay2, true);
      workbench.showOverlay(overlay3, false);

      assertEquals(3, overlays.size());
      assertEquals(1, blockingOverlaysShown.size());
      assertEquals(2, overlaysShown.size());
      assertTrue(overlay1.isVisible());
      assertTrue(overlay2.isVisible());
      assertTrue(overlay3.isVisible());

      workbench.hideOverlay(overlay1);
      workbench.hideOverlay(overlay2);
      workbench.hideOverlay(overlay3);

      assertEquals(3, overlays.size());
      assertEquals(0, blockingOverlaysShown.size());
      assertEquals(0, overlaysShown.size());
      assertFalse(overlay1.isVisible());
      assertFalse(overlay2.isVisible());
      assertFalse(overlay3.isVisible());

      final GlassPane glassPane1 = overlays.get(overlay1);
      final GlassPane glassPane2 = overlays.get(overlay2);
      final GlassPane glassPane3 = overlays.get(overlay3);

      workbench.clearOverlays();

      assertEquals(0, overlays.size());
      assertEquals(0, blockingOverlaysShown.size());
      assertEquals(0, overlaysShown.size());
      assertFalse(overlay1.isVisible());
      assertFalse(overlay2.isVisible());
      assertFalse(overlay3.isVisible());
      // closing handler was removed for non-blocking
      assertNull(glassPane1.onMouseClickedProperty().get());
      assertNull(glassPane3.onMouseClickedProperty().get());
      // glass panes were unbound
      assertFalse(glassPane1.hideProperty().isBound());
      assertFalse(glassPane2.hideProperty().isBound());
      assertFalse(glassPane3.hideProperty().isBound());
    });
  }

  @Test
  void showNavigationDrawer() {
    robot.interact(() -> {
      Node navigationDrawer = workbench.getNavigationDrawer();
      navigationDrawer.setVisible(false);

      workbench.showNavigationDrawer();

      assertEquals(1, overlays.size());
      assertEquals(0, blockingOverlaysShown.size());
      assertEquals(1, overlaysShown.size());
      assertTrue(navigationDrawer.isVisible());
    });
  }

  @Test
  void hideNavigationDrawer() {
    robot.interact(() -> {
      Node navigationDrawer = workbench.getNavigationDrawer();
      navigationDrawer.setVisible(false);

      workbench.showNavigationDrawer();
      workbench.hideNavigationDrawer();

      assertEquals(1, overlays.size());
      assertEquals(0, blockingOverlaysShown.size());
      assertEquals(0, overlaysShown.size());
      assertFalse(navigationDrawer.isVisible());
    });
  }

  @Test
  void getNavigationDrawerItems() {
    robot.interact(() -> {
      assertEquals(1, navigationDrawerItems.size());
      assertEquals(menuItem, navigationDrawerItems.get(0));
    });
  }

  /**
   * Precondition: getNavigationDrawerItems tests pass.
   */
  @Test
  void addNavigationDrawerItems() {
    robot.interact(() -> {
      workbench.getNavigationDrawerItems().add(menuItem);
      assertEquals(2, navigationDrawerItems.size());
      assertEquals(menuItem, navigationDrawerItems.get(1));
    });
  }

  /**
   * Precondition: getNavigationDrawerItems tests pass.
   */
  @Test
  void removeNavigationDrawerItems() {
    robot.interact(() -> {
      workbench.getNavigationDrawerItems().remove(menuItem);
      assertEquals(0, navigationDrawerItems.size());
    });
  }

  @Test
  void removeToolbarControlsLeftAndRight() {
    robot.interact(() -> {
      Dropdown d = Dropdown.of(dropdownText, dropdownIconView, dropdownMenuItem);

      int initialSizeLeft = workbench.getToolbarControlsLeft().size();
      assertFalse(workbench.getToolbarControlsLeft().remove(d));
      assertSame(initialSizeLeft, workbench.getToolbarControlsLeft().size());

      int initialSizeRight = workbench.getToolbarControlsRight().size();
      assertFalse(workbench.getToolbarControlsRight().remove(d));
      assertSame(initialSizeRight, workbench.getToolbarControlsRight().size());

      assertTrue(workbench.getToolbarControlsLeft().remove(dropdownLeft));
      assertSame(initialSizeLeft - 1, workbench.getToolbarControlsLeft().size());
      assertTrue(workbench.getToolbarControlsRight().remove(dropdownRight));
      assertSame(initialSizeRight - 1, workbench.getToolbarControlsRight().size());
    });
  }

  @Test
  void addToolbarControlsLeftAndRight() {
    robot.interact(() -> {
      int initialSizeLeft = workbench.getToolbarControlsLeft().size();
      Dropdown d = Dropdown.of(dropdownIconView, dropdownMenuItem);
      assertTrue(workbench.getToolbarControlsLeft().add(d));
      assertSame(initialSizeLeft + 1, workbench.getToolbarControlsLeft().size());
      assertFalse(workbench.getToolbarControlsLeft().add(d));
      assertSame(initialSizeLeft + 1, workbench.getToolbarControlsLeft().size());

      int initialSizeRight = workbench.getToolbarControlsRight().size();
      d = Dropdown.of(dropdownText, dropdownMenuItem);
      assertTrue(workbench.getToolbarControlsRight().add(d));
      assertSame(initialSizeRight + 1, workbench.getToolbarControlsRight().size());
      assertFalse(workbench.getToolbarControlsRight().add(d));
      assertSame(initialSizeRight + 1, workbench.getToolbarControlsRight().size());
    });
  }

  @Test
  void addModule() {
    robot.interact(() -> {
      ObservableList<WorkbenchModule> modules = workbench.getModules();
      int currentSize = modules.size();
      String mockModuleName = "Mock Module";
      WorkbenchModule mockModule = createMockModule(new Label(), null, true, mockModuleName);

      assertTrue(workbench.getModules().add(mockModule));

      assertSame(currentSize + 1, modules.size());
    });
  }

  @Test
  void removeModule() {
    robot.interact(() -> {
      ObservableList<WorkbenchModule> modules = workbench.getModules();
      int currentSize = modules.size();

      assertTrue(workbench.getModules().remove(mockModules[0]));

      assertSame(currentSize - 1, modules.size());

      // removing same module again should not remove it
      assertFalse(workbench.getModules().remove(mockModules[0]));

      assertSame(currentSize - 1, modules.size());
    });
  }

  /**
   * Test for {@link Workbench#setupCleanup()}.
   * Simulates all modules returning {@code true} when {@link WorkbenchModule#destroy()} is being
   * called on them during the cleanup.
   */
  @Test
  void closeStageSuccess() {
    robot.interact(() -> {
      workbench.openModule(first);
      workbench.openModule(second);

      // simulate closing of the stage by pressing the X of the application
      closeStage();

      // all open modules should get closed before the application ends
      InOrder inOrder = inOrder(first, second);
      // Call: workbench.openModule(first)
      inOrder.verify(first).init(workbench);
      inOrder.verify(first).activate();
      // Call: workbench.openModule(second)
      inOrder.verify(first).deactivate();
      inOrder.verify(second).init(workbench);
      inOrder.verify(second).activate();

      // Effects caused by "Workbench#setupCleanup" -> setOnCloseRequest
      // Implicit Call: workbench.closeModule(first)
      inOrder.verify(first).destroy();
      // Implicit Call: workbench.closeModule(second)
      inOrder.verify(second).destroy();

      inOrder.verifyNoMoreInteractions();

      assertEquals(0, workbench.getOpenModules().size());
    });
  }

  /**
   * Test for {@link Workbench#setupCleanup()}.
   * Simulates the first (inactive) module returning {@code false} and the second (active) module
   * returning {@code true}, when {@link WorkbenchModule#destroy()} is being called on them during
   * cleanup.
   */
  @Test
  void closeStageFailFirstModule() {
    robot.interact(() -> {
      workbench.openModule(first);
      workbench.openModule(second);

      // make sure closing of the stage gets interrupted, if destroy returns false on a module
      when(first.destroy()).thenReturn(false);

      // simulate closing of the stage like when pressing the X of the application
      closeStage();

      // all open modules should get closed before the application ends
      InOrder inOrder = inOrder(first, second);
      // Call: workbench.openModule(first)
      inOrder.verify(first).init(workbench);
      inOrder.verify(first).activate();
      // Call: workbench.openModule(second)
      inOrder.verify(first).deactivate();
      inOrder.verify(second).init(workbench);
      inOrder.verify(second).activate();

      // Effects caused by "Workbench#setupCleanup" -> setOnCloseRequest
      // Implicit Call: workbench.closeModule(first)
      inOrder.verify(first).destroy(); // returns false
      // closing should be interrupted
      inOrder.verifyNoMoreInteractions();

      assertEquals(2, workbench.getOpenModules().size());
    });
  }

  /**
   * Test for {@link Workbench#setupCleanup()}.
   * Simulates the first (inactive) module returning {@code true} and the second (active) module
   * returning {@code false}, when {@link WorkbenchModule#destroy()} is being called on them during
   * cleanup.
   */
  @Test
  void closeStageFailSecondModule() {
    robot.interact(() -> {
      workbench.openModule(first);
      workbench.openModule(second);

      // make sure closing of the stage gets interrupted, if destroy returns false on a module
      when(second.destroy()).thenReturn(false);

      // simulate closing of the stage by pressing the X of the application
      closeStage();

      // all open modules should get closed before the application ends
      InOrder inOrder = inOrder(first, second);
      // Call: workbench.openModule(first)
      inOrder.verify(first).init(workbench);
      inOrder.verify(first).activate();
      // Call: workbench.openModule(second)
      inOrder.verify(first).deactivate();
      inOrder.verify(second).init(workbench);
      inOrder.verify(second).activate();

      // Effects caused by "Workbench#setupCleanup" -> setOnCloseRequest
      // Implicit Call: workbench.closeModule(first)
      inOrder.verify(first).destroy(); // returns true
      // Implicit Call: workbench.closeModule(second)
      inOrder.verify(second).destroy(); // returns false
      // closing should be interrupted
      inOrder.verifyNoMoreInteractions();

      assertEquals(1, workbench.getOpenModules().size());
      assertEquals(second, workbench.getOpenModules().get(0));
    });
  }

  /**
   * Internal utility method for testing.
   * Simulates closing the stage, which fires a close request to test logic
   * inside of {@link Stage#setOnCloseRequest(EventHandler)}.
   * Using {@link FxRobot#closeCurrentWindow()} would be better, but it only works on Windows
   * because of its implementation, so this approach was chosen as a workaround.
   * @see <a href="https://github.com/TestFX/TestFX/issues/447>
   *   closeCurrentWindow() doesn't work headless</a>
   */
  private void closeStage() {
    Stage stage = ((Stage) workbench.getScene().getWindow());
    stage.fireEvent(
        new WindowEvent(
            stage,
            WindowEvent.WINDOW_CLOSE_REQUEST
        )
    );
  }

  @Test
  void initNavigationDrawer() {
    // verify no NPE is thrown by the listener when setting a null control
    workbench.setNavigationDrawer(null);
  }

  @Test
  void initDialog() {
    // verify correct initialization
    assertSame(dialogControl, workbench.getDialogControl());
    assertSame(workbench, dialogControl.getWorkbench());

    // verify no NPE is thrown by the listener when setting a null control
    workbench.setDialogControl(null);
  }

  @Test
  @DisplayName("Show non-blocking dialog and close by clicking on the GlassPane")
  void showDialogNonBlockingCloseGlassPane() {
    robot.interact(() -> {
      assertDialogNotShown();

      CompletableFuture<ButtonType> result = workbench.showDialog(mockDialog);

      assertDialogShown(result, false);
      verify(mockDialog, atLeastOnce()).getButtonTypes();
      verify(mockDialog).getResult();
      verify(mockDialogResult, never()).complete(any());

      // hiding by GlassPane click
      simulateGlassPaneClick(dialogControl);

      verify(mockDialog, times(3)).getResult();
      verify(mockDialogResult).isDone();
      verify(mockDialogResult).complete(ButtonType.CANCEL);
      verifyNoMoreInteractions(mockDialogResult);
      assertDialogNotShown();
    });
  }

  @Test
  @DisplayName("Show non-blocking dialog and close by clicking on one of the dialog buttons")
  void showDialogNonBlockingCloseButton() {
    robot.interact(() -> {
      assertDialogNotShown();

      CompletableFuture<ButtonType> result = workbench.showDialog(mockDialog);

      assertDialogShown(result, false);
      verify(mockDialog, atLeastOnce()).getButtonTypes();
      verify(mockDialog).getResult();
      verify(mockDialogResult, never()).complete(any());

      // hiding by button press
      Button pressedButton = (Button) dialogControl.getButtons().get(0);
      pressedButton.fire(); // simulate button getting pressed

      verify(mockDialog, times(3)).getResult();
      verify(mockDialogResult).isDone();
      verify(mockDialogResult).complete(mockDialog.getButtonTypes().get(0));
      verifyNoMoreInteractions(mockDialogResult);

      assertDialogNotShown();
    });
  }

  @Test
  @DisplayName("Show blocking dialog and try to close by clicking on the GlassPane")
  void showDialogBlockingCloseGlassPane() {
    robot.interact(() -> {
      when(mockDialog.isBlocking()).thenReturn(true);

      assertDialogNotShown();

      CompletableFuture<ButtonType> result = workbench.showDialog(mockDialog);

      assertDialogShown(result, true);
      verify(mockDialog, atLeastOnce()).getButtonTypes();
      verify(mockDialog).getResult();
      verify(mockDialogResult, never()).complete(any());

      // try hiding by clicking on GlassPane
      simulateGlassPaneClick(dialogControl); // simulates a click on GlassPane

      verify(mockDialog, times(1)).getResult();
      verify(mockDialogResult, never()).complete(any());
      verifyNoMoreInteractions(mockDialogResult);
      // verify dialog hasn't been hidden
      assertDialogShown(result, true);
    });
  }

  @Test
  @DisplayName("Show blocking dialog and close by clicking on one of the dialog buttons")
  void showDialogBlockingCloseButton() {
    robot.interact(() -> {
      when(mockDialog.isBlocking()).thenReturn(true);

      assertDialogNotShown();

      CompletableFuture<ButtonType> result = workbench.showDialog(mockDialog);

      assertDialogShown(result, true);
      verify(mockDialog, atLeastOnce()).getButtonTypes();
      verify(mockDialog).getResult();
      verify(mockDialogResult, never()).complete(any());

      // hiding by button press
      Button pressedButton = (Button) dialogControl.getButtons().get(0);
      pressedButton.fire(); // simulate button getting pressed

      verify(mockDialog, times(3)).getResult();
      verify(mockDialogResult).isDone();
      verify(mockDialogResult).complete(mockDialog.getButtonTypes().get(0));
      verifyNoMoreInteractions(mockDialogResult);
      assertDialogNotShown();
    });
  }

  private void assertDialogShown(CompletableFuture<ButtonType> result, boolean blocking) {
    assertTrue(workbench.isDialogShown());
    assertSame(mockDialogResult, result);
    assertSame(mockDialog, workbench.getDialog());
    if (blocking) {
      assertSame(1, workbench.getBlockingOverlaysShown().size());
      assertSame(0, workbench.getNonBlockingOverlaysShown().size());
    } else {
      assertSame(0, workbench.getBlockingOverlaysShown().size());
      assertSame(1, workbench.getNonBlockingOverlaysShown().size());
    }
  }

  private void assertDialogNotShown() {
    assertFalse(workbench.isDialogShown());
    assertSame(null, workbench.getDialog());
    assertSame(0, workbench.getBlockingOverlaysShown().size());
    assertSame(0, workbench.getNonBlockingOverlaysShown().size());
  }

  /**
   * Internal testing method that will simulate a click on a {@link GlassPane} of
   * an {@code overlayNode}.
   * @param overlayNode of which the GlassPane should be clicked
   */
  private void simulateGlassPaneClick(Node overlayNode) {
    GlassPane glassPane = workbench.getOverlays().get(overlayNode);
    glassPane.fireEvent(new MouseEvent(MouseEvent.MOUSE_CLICKED, 0, 0, 0, 0,
        MouseButton.PRIMARY, 1,
        false, false, false, false, true, false, false, false, false, false,
        null)
    );
  }
}
