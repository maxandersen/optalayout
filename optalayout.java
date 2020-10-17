///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.5.0
//DEPS org.optaplanner:optaplanner-core:7.45.0.t20201014
// //DEPS ch.qos.logback:logback-classic:1.2.3
//DEPS org.openjfx:javafx-controls:11.0.2:${os.detected.jfxname}
//DEPS org.openjfx:javafx-graphics:11.0.2:${os.detected.jfxname}

//SOURCES Visualization.java
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.domain.variable.PlanningVariable;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.Joiners;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.score.director.ScoreDirectorFactoryConfig;
import org.optaplanner.core.config.solver.SolverConfig;
import org.optaplanner.core.config.solver.termination.TerminationConfig;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "optalayout", mixinStandardHelpOptions = true, version = "optalayout 0.1", description = "optalayout made with jbang")
public class optalayout implements Callable<Integer> {

    public static void main(String... args) {
        int exitCode = new CommandLine(new optalayout()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        SolverConfig sc = new SolverConfig().withSolutionClass(Layout.class).withEntityClasses(Window.class)
                .withScoreDirectorFactory(
                        new ScoreDirectorFactoryConfig().withConstraintProviderClass(LayoutConstraintProvider.class))
                .withTerminationConfig(new TerminationConfig().withSecondsSpentLimit(2L));

        SolverFactory<Layout> factory = SolverFactory.create(sc);
        Solver<Layout> solver = factory.buildSolver();

        Layout l = new Layout();
        l.windows.add(new Window("Opera"));
        l.windows.add(new Window("iTerm2"));
        l.windows.add(new Window("BusyCal"));
        l.windows.add(new Window("PDFPen"));

        Layout solution = solver.solve(l);

        System.out.println(solution);

        Visualization.setLayout(solution);
        Visualization.launch(Visualization.class);

        return 0;
    }

    public static class Area {

        int x, y, w, h;

        public Area(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        @Override
        public String toString() {
            return String.format("%d, %d, %d, %d", x, y, w, h);
        }

        @Override
        public boolean equals(Object obj) {
            Area other = (Area) obj;
            return other.x == x && other.y == y && other.w == w && other.h == h;
        }

        public boolean overlaps(optalayout.Area other) {
            boolean overlaps = false;
            if (this == other) {
                overlaps = true;
            } else {
                // https://silentmatt.com/rectangle-intersection/
                overlaps = x < other.x + other.w && x + w > other.x && y < other.y + other.h && y + h > other.y;
            }

           // System.out.println(this + " overlaps " + (overlaps?"":" not ") + other);

            return overlaps;
        }
    }

    @PlanningEntity
    public static class Window {
        String title;
        int x, y, w, h;

        @PlanningVariable(valueRangeProviderRefs = { "areas" })
        Area area;

        public Window() {

        }

        public Window(String title) {
            this.title = title;
        }

        public Area getArea() {
            return area;
        }

        @Override
        public String toString() {
            return title + "->" + area;
        }

        public String getTitle() {
            return title;
        }

    }

    @PlanningSolution
    public static class Layout {
        public static final int maxWidth = 1080;
        public static final int maxHeight = 720;

        @PlanningScore
        HardSoftScore score;

        private List<Window> windows = new ArrayList<Window>();

        @PlanningEntityCollectionProperty
        public List<Window> getWindows() {
            return windows;
        }

        List<Area> split(Area input) {
            List<Area> results = new ArrayList<>();
            if (input.w / 2 < 40) {
                return results;
            } else {
                Area left = new Area(input.x, input.y, input.w / 2, input.h);
                Area right = new Area(input.x + input.w / 2, input.y, input.w / 2, input.h);
                results.add(left);
                results.add(right);
                results.addAll(split(left));
                results.addAll(split(right));
            }
            return results;
        }

        @ValueRangeProvider(id = "areas")
        public List<Area> getPossibleAreas() {

            List<Area> areas = new ArrayList<>();
            int width = maxWidth, height = maxHeight;

            Area a = new Area(1, 1, width, height);
            areas.add(a);
            areas.addAll(split(a));

            return areas;
        }

        @Override
        public String toString() {
            return score + " -> " + getWindows();
        }
    }

    public static class LayoutConstraintProvider implements ConstraintProvider {

        @Override
        public Constraint[] defineConstraints(ConstraintFactory cf) {

            return new Constraint[] { 
                                        areaEquals(cf), 
                                        areaOverlaps(cf)
            };
        }

        private Constraint areaOverlaps(ConstraintFactory cf) {
            return cf.from(Window.class).join(Window.class).filter((w1, w2) -> w1.getArea().overlaps(w2.getArea()))
                    .penalize("Window overlaps", HardSoftScore.ONE_HARD;
        }

        private Constraint areaEquals(ConstraintFactory cf) {
            return cf.from(Window.class).join(Window.class, Joiners.equal(Window::getArea))
                    .penalize("Window in same area", HardSoftScore.ONE_HARD);
        }
    }

}
