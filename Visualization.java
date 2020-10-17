import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 * Super simple javafx visulization rendering rectangles and windows
 */
public class Visualization extends Application {

    private static optalayout.Layout layout;

    @Override
    public void start(Stage stage) {

        Group root = new Group();
        Group possibleAreas = new Group();
        root.getChildren().add(possibleAreas);
        for (optalayout.Area area : layout.getPossibleAreas()) {
            Rectangle rectangle = new Rectangle(area.x, area.y, area.w, area.h);
            rectangle.setFill(Color.TRANSPARENT);
            rectangle.setStroke(Color.RED);
            rectangle.setStrokeWidth(3);
            possibleAreas.getChildren().add(rectangle);
        }

        Group windows = new Group();
        root.getChildren().add(windows);
        for (optalayout.Window window : layout.getWindows()) {
            optalayout.Area a = window.getArea();
            if (a != null) {

                Region rectangle = new Region();
                rectangle.setOpacity(0.1);
                rectangle.setStyle(
                        "-fx-background-color: red; -fx-border-style: dashed; -fx-border-width: 5; -fx-border-color: black");

                Text text = new Text(window.getTitle() + "(" + a.w + "," + a.h + ")");

                StackPane sp = new StackPane();
                sp.setLayoutX(rectangle.getLayoutX());
                sp.setLayoutY(rectangle.getLayoutY());
                sp.setMaxWidth(a.w);
                sp.setMinWidth(a.w);
                sp.setMaxHeight(a.h);
                sp.setMinHeight(a.h);
                sp.getChildren().addAll(rectangle, text);

                windows.getChildren().add(sp);

            } else {
                System.out.println(window.getTitle() + " not in layout!");
            }
        }

        StackPane pane = new StackPane(root);

        pane.setMinSize(layout.maxHeight, layout.maxWidth);
        pane.setMaxSize(layout.maxHeight, layout.maxWidth);

        NumberBinding maxScale = Bindings.min(pane.widthProperty().divide(layout.maxHeight),
                pane.heightProperty().divide(layout.maxWidth));

        pane.scaleXProperty().bind(maxScale);
        pane.scaleYProperty().bind(maxScale);

        Scene scene = new Scene(pane, layout.maxHeight, layout.maxWidth);
        stage.setScene(scene);
        stage.show();
    }

    public static void setLayout(optalayout.Layout l) {
        layout = l;
    }

 
}